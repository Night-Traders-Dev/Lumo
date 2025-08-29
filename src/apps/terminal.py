import gi, os
gi.require_version("Gtk", "3.0")
gi.require_version("Vte", "2.91")
from gi.repository import Gtk, Vte, GLib, Gdk, Pango
from apps import keyboard

class LumoTerminal(Gtk.Window):
    def __init__(self):
        super().__init__(title="Lumo Terminal")

        self.set_keep_above(True)
        self.set_decorated(False)

        # Get screen size and set height = 97%
        screen = Gdk.Screen.get_default()
        width = screen.get_width()
        height = screen.get_height()
        target_height = int(height * 0.97)

        # Place at bottom to leave top panel visible
        self.move(0, height - target_height)
        self.set_default_size(width, target_height)

        # Create VTE terminal
        self.terminal = Vte.Terminal()

        # Bigger font for touch devices (Pango)
        font_desc = Pango.FontDescription("Monospace 12")
        self.terminal.set_font(font_desc)

        # Spawn bash shell
        self.terminal.spawn_async(
            Vte.PtyFlags.DEFAULT,
            os.environ['HOME'],              # Working directory
            ["/usr/bin/fish"],                   # Command
            [],                              # Env
            GLib.SpawnFlags.DEFAULT,
            None,                            # Child setup
            None,                            # Child setup data
            -1,                              # Timeout
            None,                            # Cancellable
            None                             # Callback
        )

        # Close when shell exits (typing "exit")
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
        """Close the terminal when the shell exits (e.g., user types 'exit')."""
        self.destroy()

    def on_focus(self, *args):
        keyboard.launch()

    def on_blur(self, *args):
        if keyboard._keyboard_window:
            keyboard._keyboard_window.hide()

def launch():
    win = LumoTerminal()
    win.show_all()
    Gtk.main()
