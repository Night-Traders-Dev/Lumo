# apps/keyboard.py
import gi, subprocess
gi.require_version("Gtk", "3.0")
from gi.repository import Gtk, Gdk, GLib

hLimit = 0.25
wLimit = 0.95  # Changed from 0.01 to 0.95 for proper width

_keyboard_window = None  # global reference for toggle

class VirtualKeyboard(Gtk.Window):
    def __init__(self):
        super().__init__(title="Virtual Keyboard")
        self.set_keep_above(True)
        self.set_decorated(False)
        self.set_resizable(True)
        self.set_accept_focus(False)
        self.set_default_size(800, 300)
        self.set_border_width(10)

        self.shift = False
        self.ctrl = False
        self.repeat_id = None  # for key repeat

        # Main container
        self.vbox = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=8)
        self.vbox.set_margin_top(1)
        self.vbox.set_margin_bottom(1)
        self.vbox.set_margin_start(1)
        self.vbox.set_margin_end(1)
        self.add(self.vbox)

        # Grid container for keys
        self.grid = Gtk.Grid()
        self.grid.set_column_homogeneous(True)
        self.grid.set_row_homogeneous(True)
        self.grid.set_column_spacing(2)  # Add spacing between columns
        self.grid.set_row_spacing(2)     # Add spacing between rows
        self.grid.set_hexpand(True)
        self.grid.set_vexpand(True)
        self.vbox.pack_start(self.grid, True, True, 0)

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
        if screen:
            screen.connect("size-changed", lambda *_: self.position_keyboard())
        else:
            self.position_keyboard()

    def create_keys(self):
        """Create all key buttons dynamically with proper expansion"""
        # Clear previous buttons
        self.grid.foreach(lambda w: self.grid.remove(w))

        for r, key_row in enumerate(self.keys_layout):
            col = 0
            for key in key_row:
                btn = Gtk.Button(label=key)
                btn.set_hexpand(True)
                btn.set_vexpand(True)
                btn.connect("pressed", self.on_key_pressed, key)
                btn.connect("released", self.on_key_released)

                # Set minimum button size for better visibility
                btn.set_size_request(50, 35)

                # Special widths for certain keys
                if key == "Space":
                    self.grid.attach(btn, col, r, 4, 1)  # Space spans 4 columns
                    col += 4
                elif key in ["Shift", "Backspace"]:
                    self.grid.attach(btn, col, r, 2, 1)  # These span 2 columns
                    col += 2
                else:
                    self.grid.attach(btn, col, r, 1, 1)
                    col += 1

    def position_keyboard(self):
        """Position and size keyboard based on current screen"""
        screen = Gdk.Screen.get_default()
        if screen:
            sw, sh = screen.get_width(), screen.get_height()
        else:
            sw, sh = 1440, 3000
        
        width = int(sw * wLimit)
        height = int(sh * hLimit)
        
        # Ensure minimum size
        width = max(width, 600)
        height = max(height, 200)
        
        self.set_default_size(width, height)
        # Center horizontally, position at bottom
        x = (sw - width) // 2
        y = sh - height - 20  # 20px margin from bottom
        self.move(x, y)

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
                self.update_shift_appearance()
            if self.ctrl:
                args += ["key", f"ctrl+{key.lower()}"]
                self.ctrl = False
                self.update_ctrl_appearance()
            else:
                args += ["key", key.lower()]
        subprocess.run(args)

    def update_shift_appearance(self):
        """Update shift key appearance to show state"""
        # This could be enhanced to visually show shift state
        pass

    def update_ctrl_appearance(self):
        """Update ctrl key appearance to show state"""
        # This could be enhanced to visually show ctrl state
        pass

    def repeat_key(self, key):
        self.send_key(key)
        self.repeat_id = GLib.timeout_add(100, self.repeat_key, key)

    def on_key_pressed(self, widget, key):
        if key == "Shift":
            self.shift = not self.shift
            self.update_shift_appearance()
        elif key == "Ctrl":
            self.ctrl = not self.ctrl
            self.update_ctrl_appearance()
        else:
            self.send_key(key)
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
