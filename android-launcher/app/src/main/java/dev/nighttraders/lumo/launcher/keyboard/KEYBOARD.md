# Keyboard (IME)

Custom input method service providing a full on-screen keyboard.

## Files

- `LumoInputMethodService.kt` — Main IME service: keyboard layouts (QWERTY, numbers, symbols, clipboard), key rendering, swipe typing, haptic feedback
- `KeyboardWordEngine.kt` — Word prediction / autocomplete engine
- `SwipeTrailView.kt` — Visual trail overlay for swipe-to-type gestures

## Features

- QWERTY layout with shift/caps lock
- Number and symbol layouts (two pages)
- Clipboard mode: Select All, Cut, Copy, Paste via `performContextMenuAction`
- Swipe-to-type with visual trail
- Word suggestions / autocomplete bar
- Haptic feedback on key press
- Dark theme matching Lumo aesthetic

## Known Issues / Bugs

- Word prediction dictionary is basic — no learning from user input yet
- Swipe typing accuracy could be improved with better path matching

## Plans

- User dictionary / learned words
- Emoji picker
- Multi-language support
- Improved swipe gesture recognition
