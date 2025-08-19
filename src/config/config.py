# UI settings & favorites

from shell.utils import trigger_phone
from apps import keyboard, filemanager

FAVORITES = [
    {"name": "Phone",    "icon": "call-start",          "cmd": trigger_phone},
    {"name": "Terminal", "icon": "utilities-terminal", "cmd": "xfce4-terminal"},
    {"name": "Files",    "icon": "system-file-manager", "cmd": filemanager.launch},
    {"name": "Browser",  "icon": "web-browser",         "cmd": "firefox"},
    {"name": "Keyboard", "icon": "input-keyboard",      "cmd": keyboard.launch}
]

DOCK_WIDTH = 64
PANEL_HEIGHT = 48
DRAWER_WIDTH_RATIO = 0.6
DRAWER_BG_ALPHA = 0.7
DOCK_BG_ALPHA = 0.7
FAVORITES_BG_ALPHA = 0.7
EDGE_TRIGGER_W = 10
