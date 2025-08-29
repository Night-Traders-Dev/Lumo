# UI settings & favorites

from shell.utils import trigger_phone, trigger_messenger
from apps import keyboard, filemanager, terminal, settings, lumoide

FAVORITES = [
    {"name": "Phone",     "icon": "call-start",          "cmd": trigger_phone},
    {"name": "Messenger", "icon": "chat",                "cmd": trigger_messenger},
    {"name": "Terminal",  "icon": "utilities-terminal",  "cmd": terminal.launch},
    {"name": "Files",     "icon": "system-file-manager", "cmd": filemanager.launch},
    {"name": "Browser",   "icon": "web-browser",         "cmd": "firefox"},
    {"name": "Keyboard",  "icon": "input-keyboard",      "cmd": keyboard.launch},
    {"name": "Settings",  "icon": "preferences-system",  "cmd": settings.launch},
    {"name": "LumoIDE",   "icon": "accessories-text-editor", "cmd": lumoide.launch}
]


DOCK_WIDTH = 64
PANEL_HEIGHT = 48
DRAWER_WIDTH_RATIO = 0.6
DRAWER_BG_ALPHA = 0.7
DOCK_BG_ALPHA = 0.7
FAVORITES_BG_ALPHA = 0.7
EDGE_TRIGGER_W = 5
