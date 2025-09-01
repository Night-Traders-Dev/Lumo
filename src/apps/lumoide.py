import gi
gi.require_version("Gtk", "3.0")
gi.require_version("GtkSource", "3.0")
from gi.repository import Gtk, GtkSource, Gdk
from shell.utils import get_display_geo
from apps import keyboard

class NotesApp(Gtk.Window):
    def __init__(self, parent=None):
        super().__init__(title="Notes")
        sw, sh = get_display_geo()
        self.set_default_size(sw, int(sh * 0.97))
        self.set_decorated(False)
        self.set_keep_above(True)
        self.set_modal(True)
        self.set_skip_taskbar_hint(True)

        # Main container
        vbox = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=6)
        self.add(vbox)

        # Header bar with close button
        header = Gtk.HeaderBar(title="Notes")
        header.set_show_close_button(False)
        close_btn = Gtk.Button(label="Close")
        close_btn.connect("clicked", self.on_close_clicked)
        header.pack_end(close_btn)
        vbox.pack_start(header, False, False, 0)

        # Text editor
        self.buffer = GtkSource.Buffer()
        self.view = GtkSource.View.new_with_buffer(self.buffer)
        self.view.set_show_line_numbers(True)
        self.view.set_highlight_current_line(True)
        self.view.set_auto_indent(True)
        self.view.set_monospace(True)
        self.view.set_can_focus(True)  # allow it to receive focus
        self.view.connect("button-press-event", self.on_focus)

        # Language manager
        lm = GtkSource.LanguageManager.get_default()
        self.buffer.set_language(lm.get_language("python"))

        # Scrolled window
        scrolled = Gtk.ScrolledWindow()
        scrolled.set_policy(Gtk.PolicyType.AUTOMATIC, Gtk.PolicyType.AUTOMATIC)
        scrolled.add(self.view)
        vbox.pack_start(scrolled, True, True, 0)

        # Focus the view when window shows
        self.connect("map", lambda w: self.view.grab_focus())

    def on_close_clicked(self, button):
        if keyboard._keyboard_window:
            keyboard._keyboard_window.hide()
        self.destroy()

    def on_focus(self, *args):
        self.view.grab_focus()  # ensure the view has focus
        keyboard.launch()

def launch():
    win = NotesApp()
    win.show_all()
    return win
