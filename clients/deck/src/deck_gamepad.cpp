#include "deck_gamepad.h"

#ifdef __linux__
#include <linux/joystick.h>
#include <sys/ioctl.h>
#endif

namespace nova::deck {

DeckGamepadHatAxes readDeckGamepadHatAxes(int fd, DeckGamepadIoctl query) {
    DeckGamepadHatAxes result;
#ifdef __linux__
    if (!query) {
        query = [](int device, unsigned long request, void* data) {
            return ::ioctl(device, request, data);
        };
    }
    unsigned char axes[ABS_CNT]{};
    unsigned char axisCount = 0;
    // JSIOCGAXMAP may return the number of bytes copied (64 on SteamOS).
    // Only a negative ioctl return indicates failure.
    if (query(fd, JSIOCGAXES, &axisCount) < 0 || query(fd, JSIOCGAXMAP, axes) < 0) {
        return result;
    }
    for (int i = 0; i < axisCount && i < ABS_CNT; ++i) {
        if (axes[i] == ABS_HAT0X) result.horizontal = i;
        if (axes[i] == ABS_HAT0Y) result.vertical = i;
    }
#else
    (void)fd;
    (void)query;
#endif
    return result;
}

DeckGamepadAction decodeGamepadAction(const DeckGamepadEvent& event) {
    if ((event.type & kDeckGamepadInitEvent) != 0) {
        return DeckGamepadAction::None;
    }

    const auto eventType = static_cast<unsigned char>(event.type & ~kDeckGamepadInitEvent);
    if (eventType != kDeckGamepadButtonEvent) {
        return DeckGamepadAction::None;
    }

    if (event.number == kDeckGamepadPrimaryButton && event.value == 1) {
        return DeckGamepadAction::PrimaryPressed;
    }
    if (event.number == kDeckGamepadSecondaryButton && event.value == 1) {
        return DeckGamepadAction::SecondaryPressed;
    }

    return DeckGamepadAction::None;
}

DeckGamepadAction DeckGamepadNavigation::decode(const DeckGamepadEvent& event) {
    if ((event.type & ~kDeckGamepadInitEvent) != kDeckGamepadAxisEvent) {
        return DeckGamepadAction::None;
    }
    const bool horizontal = event.number == horizontalAxis_;
    if (!horizontal && event.number != verticalAxis_) {
        return DeckGamepadAction::None;
    }
    int& previous = horizontal ? horizontalValue_ : verticalValue_;
    const int direction = event.value < -16000 ? -1 : event.value > 16000 ? 1 : 0;
    const bool changed = previous != direction;
    previous = direction;
    if ((event.type & kDeckGamepadInitEvent) || !changed || direction == 0) {
        return DeckGamepadAction::None;
    }
    if (horizontal) {
        return direction < 0 ? DeckGamepadAction::LeftPressed : DeckGamepadAction::RightPressed;
    }
    return direction < 0 ? DeckGamepadAction::UpPressed : DeckGamepadAction::DownPressed;
}

} // namespace nova::deck
