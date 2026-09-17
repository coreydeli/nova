#include "deck_gamepad.h"
#include <cstdlib>
#include <iostream>
#include <linux/joystick.h>

using namespace nova::deck;

namespace {
struct JoystickQuery {
    static inline int countResult = 0;
    static inline int mapResult = ABS_CNT;
    static int query(int, unsigned long request, void* data) {
        if (request == JSIOCGAXES) {
            *static_cast<unsigned char*>(data) = 8;
            return countResult;
        }
        if (request == JSIOCGAXMAP) {
            auto* axes = static_cast<unsigned char*>(data);
            axes[6] = ABS_HAT0X;
            axes[7] = ABS_HAT0Y;
            return mapResult;
        }
        return -1;
    }
};
}

int main() {
    DeckGamepadNavigation navigation(6, 7); // Steam's virtual Xbox controller.
    const auto expect = [&](int axis, short value, DeckGamepadAction expected,
                            unsigned char type = kDeckGamepadAxisEvent) {
        const auto actual = navigation.decode({0, value, type, static_cast<unsigned char>(axis)});
        if (actual != expected) {
            std::cerr << "Unexpected navigation action for axis " << axis << '\n';
            std::exit(EXIT_FAILURE);
        }
    };
    expect(0, 32767, DeckGamepadAction::None); // Stick and trigger axes are not the D-pad.
    expect(6, -32767, DeckGamepadAction::None, kDeckGamepadAxisEvent | kDeckGamepadInitEvent);
    expect(6, -32767, DeckGamepadAction::None); // No phantom press after opening a held device.
    expect(6, 0, DeckGamepadAction::None);
    expect(6, -32767, DeckGamepadAction::LeftPressed);
    expect(6, -32767, DeckGamepadAction::None); // Duplicate events don't move twice.
    expect(6, 32767, DeckGamepadAction::RightPressed);
    expect(7, -32767, DeckGamepadAction::UpPressed);
    expect(7, 0, DeckGamepadAction::None);
    expect(7, 32767, DeckGamepadAction::DownPressed);
    expect(7, 100, DeckGamepadAction::None);
    expect(7, 32767, DeckGamepadAction::DownPressed);
    expect(6, 1, DeckGamepadAction::None, kDeckGamepadButtonEvent);
    navigation = DeckGamepadNavigation(2, 3); // Device reconnect can change axis ordering.
    expect(6, -32767, DeckGamepadAction::None);
    expect(2, -32767, DeckGamepadAction::LeftPressed);
    expect(3, 32767, DeckGamepadAction::DownPressed);
    navigation = DeckGamepadNavigation(); // A controller with no hats must not map axis zero.
    expect(0, -32767, DeckGamepadAction::None);
    expect(1, 32767, DeckGamepadAction::None);
    for (const int mapResult : {ABS_CNT, 0, -1}) {
        JoystickQuery::mapResult = mapResult;
        const auto axes = readDeckGamepadHatAxes(0, JoystickQuery::query);
        navigation = DeckGamepadNavigation(axes.horizontal, axes.vertical);
        expect(6, 32767, mapResult >= 0 ? DeckGamepadAction::RightPressed : DeckGamepadAction::None);
        expect(7, -32767, mapResult >= 0 ? DeckGamepadAction::UpPressed : DeckGamepadAction::None);
    }
    JoystickQuery::countResult = -1;
    JoystickQuery::mapResult = ABS_CNT;
    const auto failedAxes = readDeckGamepadHatAxes(0, JoystickQuery::query);
    navigation = DeckGamepadNavigation(failedAxes.horizontal, failedAxes.vertical);
    expect(6, 32767, DeckGamepadAction::None);
    std::cout << "D-pad mapping, initialization, release and reconnect checks passed\n";
}
