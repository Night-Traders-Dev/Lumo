import gi, subprocess
gi.require_version("Gtk", "3.0")
from gi.repository import Gtk, Gdk, GLib
from shell.utils import get_display_geo

# Global reference for toggle
_keyboard_window = None

# Screen geometry and sizing factors
sw, sh = get_display_geo()
HEIGHT_MULTIPLIER = 0.25
WIDTH_MULTIPLIER  = 1.0


class VirtualKeyboard(Gtk.Window):
    def __init__(self):
        super().__init__(title="Virtual Keyboard")
        self.set_keep_above(True)
        self.set_decorated(False)
        self.set_resizable(False)
        self.set_accept_focus(False)
        self.set_border_width(10)


        # Fixed keyboard size
        self.kb_width  = int(sw * WIDTH_MULTIPLIER)
        self.kb_height = int(sh * HEIGHT_MULTIPLIER)
        self.set_default_size(self.kb_width, self.kb_height)

        self.shift = False
        self.ctrl = False
        self.repeat_id = None

        # Main container
        self.vbox = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL)
        self.add(self.vbox)

        # Grid container for keys
        self.grid = Gtk.Grid()
        self.grid.set_column_spacing(0)
        self.grid.set_row_spacing(0)
        self.vbox.pack_start(self.grid, True, True, 0)

        # Full QWERTY layout
        self.keys_layout = [
            ["Q","W","E","R","T","Y","U","I","O","P"],
            ["A","S","D","F","G","H","J","K","L",";"],
            ["Shift","Z","X","C","V","B","N","M",",","."],
            ["Ctrl","Space","Enter","Backspace"]
        ]

        self.create_keys()
        self.connect("size-allocate", self.on_size_allocate)


    def create_keys(self):
        """Create all key buttons dynamically with proportional sizing"""
        self.grid.foreach(lambda w: self.grid.remove(w))

        row_height = int(self.kb_height / len(self.keys_layout))

        for r, key_row in enumerate(self.keys_layout):
            col = 0
            for key in key_row:
                btn = Gtk.Button(label=key)
                btn.set_size_request(-1, row_height)
                btn.connect("pressed", self.on_key_pressed, key)
                btn.connect("released", self.on_key_released)

                # Proportional spans
                if key == "Space":
                    span = 5
                elif key in ["Ctrl", "Enter", "Backspace", "Shift"]:
                    span = 2
                else:
                    span = 1

                self.grid.attach(btn, col, r, span, 1)
                col += span


    def on_size_allocate(self, widget, allocation):
        """Dock keyboard at bottom-center with fixed size"""
        self.resize(self.kb_width, self.kb_height)
        x_pos = int((sw - self.kb_width) / 2)
        y_pos = sh - self.kb_height
        self.move(x_pos, y_pos)
        self.set_size_request(self.kb_width, self.kb_height)
        self.grid.set_size_request(self.kb_width, self.kb_height)


    def send_key(self, key):
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
        # Optional: visually toggle Shift state
        pass

    def update_ctrl_appearance(self):
        # Optional: visually toggle Ctrl state
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
            if key not in ["Shift", "Ctrl"]:
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
