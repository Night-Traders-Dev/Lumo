# apps/keyboard.py
import gi, subprocess
gi.require_version("Gtk", "3.0")
from gi.repository import Gtk, Gdk, GLib

_keyboard_window = None  # global reference for toggle

class VirtualKeyboard(Gtk.Window):
    def __init__(self):
        super().__init__(title="Virtual Keyboard")
        self.set_keep_above(True)
        self.set_decorated(False)
        self.set_resizable(False)
        self.set_accept_focus(False)

        self.shift = False
        self.ctrl = False
        self.repeat_id = None  # for key repeat

        self.vbox = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=4)
        self.vbox.set_margin_top(4)
        self.vbox.set_margin_bottom(4)
        self.vbox.set_margin_start(4)
        self.vbox.set_margin_end(4)
        self.add(self.vbox)

        # Header row with close button
        header = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL)
        close_btn = Gtk.Button(label="✕ Close")
        close_btn.connect("clicked", self.on_close)
        header.pack_end(close_btn, False, False, 2)
        self.vbox.pack_start(header, False, False, 0)

        # Grid container for keys
        self.grid = Gtk.Grid()
        self.vbox.pack_end(self.grid, True, True, 0)

        # Define keyboard layout
        self.keys_layout = [
            ["Q","W","E","R","T","Y","U","I","O","P"],
            ["A","S","D","F","G","H","J","K","L"],
            ["Shift","Z","X","C","V","B","N","M","Backspace"],
            ["Ctrl","Space","Enter"]
        ]

        self.create_keys()
        self.position_keyboard()
        # Connect to screen resize to adjust dynamically
        screen = Gdk.Screen.get_default()
        screen.connect("size-changed", lambda *_: self.position_keyboard())

    def position_keyboard(self):
        """Position and size keyboard based on current screen"""
        screen = Gdk.Screen.get_default()
        sw, sh = screen.get_width(), screen.get_height()
        height = int(sh * 0.25)
        width = sw
        self.set_default_size(width, height)
        self.move(0, sh - height)

    def create_keys(self):
        """Create all key buttons"""
        for row, key_row in enumerate(self.keys_layout):
            for col, key in enumerate(key_row):
                btn = Gtk.Button(label=key)
                btn.set_size_request(70, 60)
                btn.connect("pressed", self.on_key_pressed, key)
                btn.connect("released", self.on_key_released)
                if key == "Space":
                    self.grid.attach(btn, col, row, 5, 1)
                else:
                    self.grid.attach(btn, col, row, 1, 1)

    def send_key(self, key):
        """Send key using xdotool with shift/ctrl support"""
        args = ["xdotool"]
        if key == "Space":
            args += ["key", "space"]
        elif key == "Enter":
            args += ["key", "Return"]
        elif key == "Backspace":
            args += ["key", "BackSpace"]
        elif key in ["Shift", "Ctrl"]:
            return
        else:
            if self.shift:
                key = key.upper()
                self.shift = False
            if self.ctrl:
                args += ["key", f"ctrl+{key.lower()}"]
                self.ctrl = False
            else:
                args += ["key", key.lower()]
        subprocess.run(args)

    def repeat_key(self, key):
        self.send_key(key)
        self.repeat_id = GLib.timeout_add(100, self.repeat_key, key)

    def on_key_pressed(self, widget, key):
        if key == "Shift":
            self.shift = not self.shift
        elif key == "Ctrl":
            self.ctrl = not self.ctrl
        else:
            self.send_key(key)
            # repeat keys
            if key not in ["Shift","Ctrl"]:
                self.repeat_id = GLib.timeout_add(400, self.repeat_key, key)

    def on_key_released(self, widget):
        if self.repeat_id:
            GLib.source_remove(self.repeat_id)
            self.repeat_id = None

    def on_close(self, widget=None):
        global _keyboard_window
        self.destroy()
        _keyboard_window = None


def launch():
    """Toggle keyboard visibility"""
    global _keyboard_window
    if _keyboard_window is None:
        _keyboard_window = VirtualKeyboard()
        _keyboard_window.show_all()
    else:
        _keyboard_window.on_close()
