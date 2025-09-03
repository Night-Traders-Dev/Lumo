import gi, os
gi.require_version("Gtk", "3.0")
gi.require_version("Vte", "2.91")
from gi.repository import Gtk, Vte, GLib, Gdk, Pango
from apps import keyboard, util

class LumoTerminal(Gtk.Window):
    def __init__(self):
        super().__init__(title="Lumo Terminal")

        self.set_keep_above(True)
        self.set_decorated(False)

        # Get screen size and set height = 97%
        screen = Gdk.Screen.get_default()
        self.width = screen.get_width()
        self.height = screen.get_height()
        self.target_height = int(self.height * 0.97)

        # Store original geometry
        self.original_x = 0
        self.original_y = self.height - self.target_height
        self.original_w = self.width
        self.original_h = self.target_height

        # Apply original geometry
        self.move(self.original_x, self.original_y)
        self.set_default_size(self.original_w, self.original_h)

        # Create VTE terminal
        self.terminal = Vte.Terminal()

        # Bigger font for touch devices (Pango)
        font_desc = Pango.FontDescription("Monospace 12")
        self.terminal.set_font(font_desc)

        # Spawn shell
        self.terminal.spawn_async(
            Vte.PtyFlags.DEFAULT,
            os.environ['HOME'],              # Working directory
            ["/usr/bin/fish"],               # Command
            [],                              # Env
            GLib.SpawnFlags.DEFAULT,
            None,                            # Child setup
            None,                            # Child setup data
            -1,                              # Timeout
            None,                            # Cancellable
            None                             # Callback
        )

        # Close when shell exits
        self.terminal.connect("child-exited", self.on_child_exit)

        # Scrolled container
        scroller = Gtk.ScrolledWindow()
        scroller.add(self.terminal)
        self.add(scroller)

        # Show keyboard when terminal is clicked
        self.terminal.connect("button-press-event", self.on_focus)

        # Hide keyboard when window loses focus
        self.connect("focus-out-event", self.on_blur)

        self.connect("destroy", Gtk.main_quit)
        self.show_all()


    def on_child_exit(self, *args):
        keyboard._keyboard_window.hide()
        self.destroy()

    def on_focus(self, *args):
        keyboard.launch()
        util.animate_shrink_for_keyboard(self, shrink_ratio=0.25, duration=300)

    def on_blur(self, *args):
        if keyboard._keyboard_window:
            util.animate_restore_size(self, duration=300)
            keyboard._keyboard_window.hide()


def launch():
    win = LumoTerminal()
    win.show_all()
    Gtk.main()
