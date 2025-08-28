# Lumo/src/windows/panel.py
import time
from gi.repository import Gtk, GLib, Gdk
from config.config import *
from shell.utils import get_display_geo, get_active_network, get_battery
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

        # Center clock
        self.clock = Gtk.Label(label="--:--")
        self.clock.get_style_context().add_class("title")
        box.set_center_widget(self.clock)

        # Right info (battery + network)
        right = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=12)
        self.sysinfo = Gtk.Label(label="🔋 --%   📶 --")
        netType = get_active_network()
        batteryLevel = get_battery()
        self.sysinfo.set_text(f"🔋 {batteryLevel}%   📶 {netType}")
        right.pack_end(self.sysinfo, False, False, 12)
        box.pack_end(right, False, False, 8)

        # Start periodic updates
        GLib.timeout_add_seconds(1, self._tick)

        self.show_all()

    def _tick(self):
        # Update clock
        self.clock.set_text(time.strftime("%H:%M"))

        # Update system info (every 10s)
        now = int(time.time())
        if now % 60 == 0:
            netType = get_active_network()
            batteryLevel = get_battery()
            self.sysinfo.set_text(f"🔋 {batteryLevel}%   📶 {netType}")

        return True
