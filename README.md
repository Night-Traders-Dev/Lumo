# Lumo

Lumo is a lightweight, GTK-based desktop environment for Linux designed to provide a mobile-friendly, touch-optimized interface. It includes a launcher, favorites bar, virtual keyboard, file manager, and other essential desktop utilities.

---

## Features

- **Launcher**: Quickly access favorite applications with a clean, icon-based dock.
- **Left Favorites Bar**: Slide-in dock from the left edge for commonly used apps.
- **Virtual Keyboard**: Dockable on-screen keyboard with Shift, Ctrl, and key repeat support.
- **File Manager**: Lightweight GTK file manager for browsing directories and opening files.
- **Dynamic UI**: Auto-resizes based on screen dimensions.
- **Touch & Mouse Support**: Designed for both mobile touchscreens and desktop input.
- **Extensible**: Add new apps and utilities easily via the favorites configuration.

---

## Installation

### Requirements

- Python 3
- GTK 3 (PyGObject)
- xdotool (for virtual keyboard)
- Linux environment (tested on Ubuntu/Termux + proot setups)

### Installing Dependencies

```bash
sudo apt update
sudo apt install python3-gi python3-gi-cairo gir1.2-gtk-3.0 xdotool
````

If using Termux with Ubuntu:

```bash
pkg install python3
apt install python3-gi python3-gi-cairo gir1.2-gtk-3.0 xdotool
```

---

## Running Lumo

Clone the repository:

```bash
git clone https://github.com/Night-Traders-Dev/lumo.git
cd lumo/src
```

Start the main environment:

```bash
python3 main.py
```

---

## Usage

### Launcher & Favorites

* Swipe or click the left edge to open the favorites bar.
* Click icons to launch applications.
* Trigger size and dock width are configurable via `config/config.py`.

### Virtual Keyboard

* Launch via the keyboard app icon in favorites or programmatically:

```python
from apps import keyboard
keyboard.launch()
```

* Supports Shift, Ctrl, and key repeat.
* Docked at the bottom and dynamically adjusts to screen size.
* Close button toggles visibility.

### File Manager

* Launch from favorites or programmatically:

```python
from windows import file_manager
file_manager.launch("/home/user")
```

* Browse directories, open files via default system apps.
* Back button and path entry for easy navigation.

---

## Configuration

* `config/config.py` contains variables like:

```python
DOCK_WIDTH = 64
PANEL_HEIGHT = 48
EDGE_TRIGGER_W = 10  # Width of the edge trigger for favorites
FAVORITES_BG_ALPHA = 0.7
```

* Update these to adjust UI dimensions, transparency, and favorites behavior.

---

## Development

* Written in Python 3 with GTK 3.
* Modular structure:

  * `apps/` → app utilities (keyboard, launcher)
  * `windows/` → UI windows (favorites, file manager)
  * `config/` → configuration and constants
  * `shell/` → utility functions for launching and triggering apps
* Contributions are welcome! Follow standard GitHub workflow:

  1. Fork
  2. Branch
  3. Pull Request

---

## License

MIT License. See `LICENSE` for details.

---


## Notes

* Optimized for mobile touchscreens but fully usable with keyboard and mouse.
* Works on Ubuntu, Termux + Proot Ubuntu, and other Linux distributions with GTK 3.

