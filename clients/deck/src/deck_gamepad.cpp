#include "deck_gamepad.h"

namespace nova::deck {

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
