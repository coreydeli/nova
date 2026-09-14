/* Optional receive timing for the unchanged upstream audio implementation.
 * Keep the vendored source intact. Only its socket call is observed here.
 */
#include <Limelight-internal.h>
#include <android/log.h>
#include <errno.h>
#include <inttypes.h>
#include <time.h>
#include <sys/ioctl.h>
#include <linux/sockios.h>

static int novaRecvAudioSocket(SOCKET s, char* buffer, int size, bool useSelect);
#define recvUdpSocket novaRecvAudioSocket
#include "moonlight-common-c/src/AudioStream.c"
#undef recvUdpSocket

typedef struct {
    uint64_t start, previous_return, previous_data;
    uint64_t receive_max, outside_max, data_gap_max;
    unsigned data, fec, timeouts, errors, data_gaps_over_20ms;
    bool timestamp_primed;
    int timestamp_error;
    unsigned kernel_samples, kernel_unavailable, kernel_order_errors;
    unsigned kernel_gaps_over_20ms, kernel_ages_over_20ms;
    uint64_t previous_kernel_data, kernel_gap_max, kernel_age_max, observer_max;
    uint64_t gap_receive_unix_us;
    unsigned gap_sequence, gap_previous_sequence, previous_data_sequence;
    int64_t gap_kernel_us, gap_kernel_age_us;
} NOVA_AUDIO_RECEIVE_STATS;

static _Thread_local NOVA_AUDIO_RECEIVE_STATS novaAudioReceiveStats;

static uint64_t novaAudioNowUs(void) {
    struct timespec now;
    if (clock_gettime(CLOCK_MONOTONIC, &now) != 0) return 0;
    return (uint64_t)now.tv_sec * 1000000 + (uint64_t)now.tv_nsec / 1000;
}

static uint64_t novaAudioRealtimeUs(void) {
    struct timespec now;
    if (clock_gettime(CLOCK_REALTIME, &now) != 0) return 0;
    return (uint64_t)now.tv_sec * 1000000 + (uint64_t)now.tv_nsec / 1000;
}

static int novaRecvAudioSocket(SOCKET s, char* buffer, int size, bool useSelect) {
    NOVA_AUDIO_RECEIVE_STATS* stats = &novaAudioReceiveStats;
    if (!stats->timestamp_primed) {
        // SIOCGSTAMPNS enables socket receive stamping on Linux. Prime before
        // the first receive: the initial query may return ENOENT or a synthetic
        // current timestamp. Neither is a packet arrival observation.
        int saved_error = errno;
        struct timespec ignored;
        (void)ioctl(s, SIOCGSTAMPNS, &ignored);
        stats->timestamp_primed = true;
        errno = saved_error;
    }
    uint64_t before = novaAudioNowUs();
    int result = recvUdpSocket(s, buffer, size, useSelect);
    int socket_error = errno;
    uint64_t after = novaAudioNowUs();
    if (before != 0 && after >= before) {
        if (stats->start == 0 && result > 0) stats->start = after;
        if (stats->start != 0) {
            if (stats->previous_return != 0 && before >= stats->previous_return) {
                uint64_t outside = before - stats->previous_return;
                if (outside > stats->outside_max) stats->outside_max = outside;
                uint64_t receive = after - before;
                if (receive > stats->receive_max) stats->receive_max = receive;
            }
            stats->previous_return = after;
            if (result < 0) stats->errors++;
            else if (result == 0) stats->timeouts++;
            else if (result >= 2 && (((unsigned char)buffer[0] & 0xc0) == 0x80)) {
                unsigned payload_type = (unsigned char)buffer[1] & 0x7f;
                if (payload_type == 97) {
                    stats->data++;
                    // The upstream receiver is this socket's only reader. Do not
                    // enable SO_TIMESTAMP[NS], which would invalidate this ioctl.
                    // Its realtime clock is distinct from our monotonic gaps.
                    struct timespec stamp;
                    uint64_t kernel = 0;
                    uint64_t realtime = 0;
                    int64_t kernel_gap = -1, kernel_age = -1;
                    int timestamp_result = ioctl(s, SIOCGSTAMPNS, &stamp);
                    if (timestamp_result == 0 &&
                        stamp.tv_sec > 0 && stamp.tv_nsec >= 0 && stamp.tv_nsec < 1000000000) {
                        kernel = (uint64_t)stamp.tv_sec * 1000000 + (uint64_t)stamp.tv_nsec / 1000;
                        realtime = novaAudioRealtimeUs();
                        if (realtime >= kernel) {
                            kernel_age = (int64_t)(realtime - kernel);
                            stats->kernel_samples++;
                            if ((uint64_t)kernel_age > stats->kernel_age_max) stats->kernel_age_max = kernel_age;
                            if (kernel_age > 20000) stats->kernel_ages_over_20ms++;
                            if (stats->previous_kernel_data != 0) {
                                if (kernel >= stats->previous_kernel_data) {
                                    kernel_gap = (int64_t)(kernel - stats->previous_kernel_data);
                                    if ((uint64_t)kernel_gap > stats->kernel_gap_max) stats->kernel_gap_max = kernel_gap;
                                    if (kernel_gap > 20000) stats->kernel_gaps_over_20ms++;
                                }
                                else stats->kernel_order_errors++;
                            }
                        }
                        else {
                            stats->kernel_order_errors++;
                            kernel = 0;
                        }
                    }
                    else {
                        stats->kernel_unavailable++;
                        stats->timestamp_error = timestamp_result < 0 ? errno : EINVAL;
                    }
                    stats->previous_kernel_data = kernel;
                    if (stats->previous_data != 0 && after >= stats->previous_data) {
                        uint64_t gap = after - stats->previous_data;
                        if (gap > stats->data_gap_max) {
                            stats->data_gap_max = gap;
                            stats->gap_kernel_us = kernel_gap;
                            stats->gap_kernel_age_us = kernel_age;
                            stats->gap_receive_unix_us = realtime;
                            stats->gap_previous_sequence = stats->previous_data_sequence;
                            stats->gap_sequence = result >= 4 ?
                                ((unsigned)(unsigned char)buffer[2] << 8) | (unsigned char)buffer[3] : 0;
                        }
                        if (gap > 20000) stats->data_gaps_over_20ms++;
                    }
                    stats->previous_data = after;
                    stats->previous_data_sequence = result >= 4 ?
                        ((unsigned)(unsigned char)buffer[2] << 8) | (unsigned char)buffer[3] : 0;
                }
                else if (payload_type == 127) stats->fec++;
            }
            uint64_t observed = novaAudioNowUs();
            if (observed >= after && observed - after > stats->observer_max) {
                stats->observer_max = observed - after;
            }
            if (after - stats->start >= 10000000) {
                __android_log_print(ANDROID_LOG_INFO, "moonlight-common-c",
                    "Nova: audio receive window_ms=%" PRIu64
                    " data_packets=%u fec_packets=%u timeouts=%u errors=%u"
                    " receive_max_us=%" PRIu64 " outside_max_us=%" PRIu64
                    " data_gap_max_us=%" PRIu64 " data_gaps_over_20ms=%u"
                    " kernel_samples=%u kernel_unavailable=%u timestamp_errno=%d"
                    " kernel_order_errors=%u kernel_gap_max_us=%" PRIu64
                    " kernel_gaps_over_20ms=%u kernel_age_max_us=%" PRIu64
                    " kernel_ages_over_20ms=%u observer_max_us=%" PRIu64
                    " gap_sequence=%u gap_previous_sequence=%u gap_receive_unix_us=%" PRIu64
                    " gap_kernel_us=%" PRId64 " gap_kernel_age_us=%" PRId64,
                    (after - stats->start) / 1000, stats->data, stats->fec,
                    stats->timeouts, stats->errors, stats->receive_max,
                    stats->outside_max, stats->data_gap_max, stats->data_gaps_over_20ms,
                    stats->kernel_samples, stats->kernel_unavailable, stats->timestamp_error,
                    stats->kernel_order_errors, stats->kernel_gap_max, stats->kernel_gaps_over_20ms,
                    stats->kernel_age_max, stats->kernel_ages_over_20ms, stats->observer_max,
                    stats->gap_sequence, stats->gap_previous_sequence, stats->gap_receive_unix_us,
                    stats->gap_kernel_us, stats->gap_kernel_age_us);
                stats->start = after;
                stats->data = stats->fec = stats->timeouts = stats->errors = 0;
                stats->data_gaps_over_20ms = 0;
                stats->receive_max = stats->outside_max = stats->data_gap_max = 0;
                stats->kernel_samples = stats->kernel_unavailable = stats->kernel_order_errors = 0;
                stats->kernel_gaps_over_20ms = stats->kernel_ages_over_20ms = 0;
                stats->kernel_gap_max = stats->kernel_age_max = stats->observer_max = 0;
                stats->timestamp_error = 0;
                stats->gap_receive_unix_us = stats->gap_sequence = stats->gap_previous_sequence = 0;
                stats->gap_kernel_us = stats->gap_kernel_age_us = -1;
            }
        }
    }
    // Timing and logging must not replace the socket's reported error.
    errno = socket_error;
    return result;
}
