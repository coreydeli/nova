/* Optional receive timing for the unchanged upstream audio implementation.
 * Keep the vendored source intact. Only its socket call is observed here.
 */
#include <Limelight-internal.h>
#include <android/log.h>
#include <errno.h>
#include <inttypes.h>
#include <time.h>

static int novaRecvAudioSocket(SOCKET s, char* buffer, int size, bool useSelect);
#define recvUdpSocket novaRecvAudioSocket
#include "moonlight-common-c/src/AudioStream.c"
#undef recvUdpSocket

typedef struct {
    uint64_t start, previous_return, previous_data;
    uint64_t receive_max, outside_max, data_gap_max;
    unsigned data, fec, timeouts, errors, data_gaps_over_20ms;
} NOVA_AUDIO_RECEIVE_STATS;

static _Thread_local NOVA_AUDIO_RECEIVE_STATS novaAudioReceiveStats;

static uint64_t novaAudioNowUs(void) {
    struct timespec now;
    if (clock_gettime(CLOCK_MONOTONIC, &now) != 0) return 0;
    return (uint64_t)now.tv_sec * 1000000 + (uint64_t)now.tv_nsec / 1000;
}

static int novaRecvAudioSocket(SOCKET s, char* buffer, int size, bool useSelect) {
    uint64_t before = novaAudioNowUs();
    int result = recvUdpSocket(s, buffer, size, useSelect);
    int socket_error = errno;
    uint64_t after = novaAudioNowUs();
    NOVA_AUDIO_RECEIVE_STATS* stats = &novaAudioReceiveStats;

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
                    if (stats->previous_data != 0 && after >= stats->previous_data) {
                        uint64_t gap = after - stats->previous_data;
                        if (gap > stats->data_gap_max) stats->data_gap_max = gap;
                        if (gap > 20000) stats->data_gaps_over_20ms++;
                    }
                    stats->previous_data = after;
                }
                else if (payload_type == 127) stats->fec++;
            }
            if (after - stats->start >= 10000000) {
                __android_log_print(ANDROID_LOG_INFO, "moonlight-common-c",
                    "Nova: audio receive window_ms=%" PRIu64
                    " data_packets=%u fec_packets=%u timeouts=%u errors=%u"
                    " receive_max_us=%" PRIu64 " outside_max_us=%" PRIu64
                    " data_gap_max_us=%" PRIu64 " data_gaps_over_20ms=%u",
                    (after - stats->start) / 1000, stats->data, stats->fec,
                    stats->timeouts, stats->errors, stats->receive_max,
                    stats->outside_max, stats->data_gap_max, stats->data_gaps_over_20ms);
                stats->start = after;
                stats->data = stats->fec = stats->timeouts = stats->errors = 0;
                stats->data_gaps_over_20ms = 0;
                stats->receive_max = stats->outside_max = stats->data_gap_max = 0;
            }
        }
    }
    // Timing and logging must not replace the socket's reported error.
    errno = socket_error;
    return result;
}
