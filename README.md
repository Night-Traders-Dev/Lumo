# Lumo

Lumo is a lightweight, GTK-based desktop environment for Linux designed to provide a mobile-friendly, touch-optimized interface. It includes a launcher, favorites bar, virtual keyboard, file manager, panel, and other essential desktop utilities — focused on small screens, touch devices, and minimal resource usage.

---

## What’s New (recent updates)

* **Virtual Keyboard:** docks at the bottom using the logical display size (via `get_display_geo()`), handles display scaling correctly, supports Shift, Ctrl, and key repeat.
* **File Manager:** single-tap selects, **double-tap opens** files/directories; Back button closes the file manager when already at the top level.
* **Top Panel:** clock updates every second; battery and network info update periodically (configurable cadence in code).

---

## Features

* **Launcher**: Clean icon dock for favorite applications.
* **Left Favorites Bar**: Slide-in favorites from the left edge.
* **Virtual Keyboard**: Bottom-docked on-screen keyboard, auto-resizes with screen, key repeat & modifiers.
* **File Manager**: Lightweight explorer with double-tap to open and path entry support.
* **Top Panel**: Dock-style top bar with clock, battery and network indicators.
* **Dynamic UI**: Uses logical screen geometry so UI works correctly with display scaling.
* **Touch & Mouse Support**: Designed to work well on mobile touchscreens and desktop input.
* **Extensible**: Add apps/shortcuts via the favorites configuration.

---

## Installation

### Requirements

* Python 3
* GTK 3 (PyGObject)
* `xdotool` (used by the virtual keyboard)
* Linux environment (tested on Ubuntu and Termux + proot setups)

### Installing dependencies (Ubuntu / Debian)

```bash
sudo apt update
sudo apt install python3 python3-gi python3-gi-cairo gir1.2-gtk-3.0 xdotool
```

### Termux (when using Ubuntu in proot)

```bash
pkg install python
# then inside proot/Ubuntu:
apt update
apt install python3-gi python3-gi-cairo gir1.2-gtk-3.0 xdotool
```

---

## Running Lumo

Clone the repo and start:

```bash
git clone https://github.com/Night-Traders-Dev/lumo.git
cd lumo/src
python3 main.py
```

(If your environment requires it, run under the appropriate X/Wayland session or an X server appropriate for your device.)

---

## Usage

### Launcher & Favorites

* Swipe or click the left edge to show the favorites bar.
* Click icons to launch applications.
* Trigger area and sizes are configurable via `config/config.py`.

### Virtual Keyboard

* Launch via the keyboard app icon in favorites:

  ```python
  from apps import keyboard
  keyboard.launch()
  ```
* Behavior:

  * Docked to bottom and auto-resizes to the logical screen size (`get_display_geo()` output), so it respects display scaling.
  * Shift and Ctrl modifiers supported.
  * Key repeat supported (press-and-hold).
  * Toggleable visibility.

### File Manager

* Launch from favorites or programmatically:

  ```python
  from windows import file_manager
  file_manager.launch("/home/user")
  ```
* Behavior:

  * **Single-tap** selects a row; **double-tap** (double-click) opens directories or files.
  * Back button navigates up — if already at top-level (root or no parent), Back will **close the file manager window**.
  * Path entry is available for jumping to arbitrary directories.
  * Files are opened with the system default (`xdg-open`).

### Top Panel

* Displays time (updated every second).
* Battery and network indicators update periodically (default cadence is implemented in code; adjust if needed).
* Panel docks to the top and uses `get_display_geo()` so it fits correctly on scaled displays.

---

## Configuration

Edit `config/config.py` for UI configuration:

```python
DOCK_WIDTH = 64
PANEL_HEIGHT = 48
EDGE_TRIGGER_W = 10
FAVORITES_BG_ALPHA = 0.7
```

Other parameters and refresh intervals (for battery/network) are implemented in their modules and can be tuned in code if you want different update cadence.

---

## Development

* Language: Python 3 + GTK 3 (PyGObject).
* Project structure:

  * `apps/` → app utilities (keyboard, launcher, etc.)
  * `windows/` → UI windows (favorites, file manager, panel)
  * `config/` → configuration and constants
  * `shell/` → utility functions (display geometry, battery, network helpers)
  * `styles/` → CSS styling
* Contributing:

  1. Fork the repo
  2. Create a branch
  3. Make changes & test
  4. Submit a pull request

### Tips & Troubleshooting

* If your UI elements look too large/small, Lumo reads the **logical** resolution (accounting for display scaling). Use `shell/utils.get_display_geo()` and tune sizes relative to those values.
* If double-tap isn't opening items on touchscreens, ensure your input driver/windowing environment sends proper click events (some Android/XWayland setups may need input configuration).
* If `xdotool` actions fail (keyboard input), ensure an X server is active and `xdotool` has permission to send events.

---

## License

MIT License — see `LICENSE` for details.

---

## Notes

* Optimized for mobile touchscreens but fully usable with keyboard and mouse.
* Known working environments: Ubuntu, Termux + proot Ubuntu. Other Linux distros with GTK 3 are likely compatible.

