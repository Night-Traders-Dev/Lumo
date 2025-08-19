# Lumo/src/windows/panel.py
import time
from gi.repository import Gtk, GLib, Gdk
from config.config import PANEL_HEIGHT
from shell.utils import get_display_geo
from styles.styles import load_css



class TopPanel(Gtk.Window):
    def __init__(self, shell):
        super().__init__(title="Panel")
        self.shell = shell
        self.set_type_hint(Gdk.WindowTypeHint.DOCK)
        self.set_decorated(False)
        self.set_resizable(False)
        self.set_keep_above(True)
        sw, sh = get_display_geo()
        self.set_default_size(sw, PANEL_HEIGHT)
        self.move(0, 0)
        load_css()

        box = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=8)
        box.set_name("panel")
        self.add(box)

        self.menu_btn = Gtk.Button.new_with_label("≡")
        self.menu_btn.connect("clicked", lambda *_: self.shell.toggle())
        box.pack_start(self.menu_btn, False, False, 8)

        self.clock = Gtk.Label(label="--:--")
        self.clock.get_style_context().add_class("title")
        box.set_center_widget(self.clock)
        GLib.timeout_add_seconds(1, self._tick)

        right = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=12)
        right_lbl = Gtk.Label(label="🔋 100%   📶 LTE")
        right.pack_end(right_lbl, False, False, 12)
        box.pack_end(right, False, False, 8)

        self.show_all()

    def _tick(self):
        self.clock.set_text(time.strftime("%H:%M"))
        return True
