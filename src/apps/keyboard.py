import gi, subprocess
gi.require_version("Gtk", "3.0")
from gi.repository import Gtk, Gdk, GLib

hLimit = 0.35     # Increased for better touch area
wLimit = 0.80     # Good fit for most displays

_keyboard_window = None  # global reference for toggle

class VirtualKeyboard(Gtk.Window):
    def __init__(self):
        super().__init__(title="Virtual Keyboard")
        self.set_keep_above(True)
        self.set_decorated(False)
        self.set_resizable(False)
        self.set_accept_focus(False)
        self.set_default_geometry(-1, -1)
        self.set_default_size(50, 350)

        self.shift = False
        self.ctrl = False
        self.repeat_id = None  # for key repeat

        # Main container
        self.vbox = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL)
        self.add(self.vbox)
        self.vbox.set_size_request(20, 300)

        # Grid container for keys
        self.grid = Gtk.Grid()
        self.grid.set_column_homogeneous(True)
        self.grid.set_row_homogeneous(True)
        self.grid.set_column_spacing(1)
        self.grid.set_row_spacing(1)
        self.vbox.pack_start(self.grid, True, True, 0)

        # Define keyboard layout
        self.keys_layout = [
            ["Q","W","E","R","T","Y","U","I","O","P"],
            ["A","S","D","F","G","H","J","K","L"],
            ["Shift","Z","X","C","V","B","N","M","Backspace"],
            ["Ctrl","Space","Enter"]
        ]

        screen = Gdk.Screen.get_default()
        if screen:
            self.connect("size-allocate", self.on_size_allocate)
            self.create_keys()
        else:
            return 0

    def create_keys(self):
        """Create all key buttons dynamically with proper expansion"""
        self.grid.foreach(lambda w: self.grid.remove(w))

        for r, key_row in enumerate(self.keys_layout):
            col = 0
            for key in key_row:
                btn = Gtk.Button(label=key)
                btn.connect("pressed", self.on_key_pressed, key)
                btn.connect("released", self.on_key_released)

                if key == "Space":
                    self.grid.attach(btn, col, r, 4, 1)
                    col += 4
                elif key in ["Ctrl", "Enter"]:
                    self.grid.attach(btn, col, r, 2, 1)
                    col += 2
                elif key in ["Shift", "Backspace"]:
                    self.grid.attach(btn, col, r, 2, 1)
                    col += 2
                else:
                    self.grid.attach(btn, col, r, 1, 1)
                    col += 1


    def on_size_allocate(self, widget, allocation):
        """Positions the keyboard at the bottom-center of the work area after its size is set."""
        screen = Gdk.Screen.get_default()
        if not screen:
            return
        win = widget.get_window()
        if not win:
            return

        monitor = screen.get_monitor_at_window(win)
        work = screen.get_monitor_workarea(monitor)

        # Calculate X to center the window horizontally within the workarea
        x = work.x + (work.width - allocation.width) // 2
        # Calculate Y to place the window at the very bottom of the workarea
        y = work.y + work.height - allocation.height
        # Move the window to the calculated position
        widget.move(x, y)



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
        pass

    def update_ctrl_appearance(self):
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
