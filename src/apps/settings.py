# src/apps/settings.py
import gi
gi.require_version("Gtk", "3.0")
from gi.repository import Gtk, GLib

from shell.utils import (
    get_display_geo, get_battery, get_active_network, get_packages,
    get_volume, set_volume, set_brightness
)


class SettingsApp(Gtk.Window):
    def __init__(self, parent=None):
        super().__init__(title="Settings")

        # Get screen geometry
        sw, sh = get_display_geo()
        self.set_default_size(int(sw * 0.95), int(sh * 0.97))  # Full width, 97% height
        self.set_decorated(False)
        self.set_keep_above(True)
        self.set_modal(True)
        self.set_skip_taskbar_hint(True)

        # Main container
        vbox = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=6)
        self.add(vbox)

        # Header bar with close button
        header = Gtk.HeaderBar(title="Settings")
        header.set_show_close_button(False)

        close_btn = Gtk.Button(label="Close")
        close_btn.connect("clicked", self.on_close_clicked)
        header.pack_start(close_btn)

        vbox.pack_start(header, False, False, 0)

        # Notebook for sections
        notebook = Gtk.Notebook()
        vbox.pack_start(notebook, True, True, 0)

        # --- System Info Page ---
        sys_box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=10)

        # Battery status
        battery = get_battery()
        battery_label = Gtk.Label(label=f"Battery: {battery}%" if battery is not None else "Battery: Unknown")
        sys_box.pack_start(battery_label, False, False, 0)

        # Network status
        net = get_active_network()
        net_label = Gtk.Label(label=f"Network: {net}")
        sys_box.pack_start(net_label, False, False, 0)

        # --- Brightness Slider ---
        brightness_label = Gtk.Label(label="Brightness")
        sys_box.pack_start(brightness_label, False, False, 0)
        brightness_adjustment = Gtk.Adjustment(value=128, lower=0, upper=255, step_increment=1)
        self.brightness_slider = Gtk.Scale(orientation=Gtk.Orientation.HORIZONTAL, adjustment=brightness_adjustment)
        self.brightness_slider.set_value_pos(Gtk.PositionType.RIGHT)
        self.brightness_slider.connect("value-changed", self.on_brightness_changed)
        sys_box.pack_start(self.brightness_slider, False, False, 0)

        # --- Volume Slider ---
        volume_label = Gtk.Label(label="Media Volume")
        sys_box.pack_start(volume_label, False, False, 0)
        current_vol_info = get_volume()
        initial_vol = 0
        if current_vol_info and "music" in current_vol_info:
            initial_vol = current_vol_info["music"]
        volume_adjustment = Gtk.Adjustment(value=initial_vol, lower=0, upper=15, step_increment=1)
        self.volume_slider = Gtk.Scale(orientation=Gtk.Orientation.HORIZONTAL, adjustment=volume_adjustment)
        self.volume_slider.set_value_pos(Gtk.PositionType.RIGHT)
        self.volume_slider.connect("value-changed", self.on_volume_changed)
        sys_box.pack_start(self.volume_slider, False, False, 0)

        notebook.append_page(sys_box, Gtk.Label(label="System"))

        # --- Apps Page ---
        apps_box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=10)

        apps = get_packages()
        if apps:
            for pkg in apps[:20]:  # Show only first 20 for now
                apps_box.pack_start(Gtk.Label(label=pkg), False, False, 0)
        else:
            apps_box.pack_start(Gtk.Label(label="No packages found."), False, False, 0)

        notebook.append_page(apps_box, Gtk.Label(label="Apps"))

    def on_close_clicked(self, button):
        self.destroy()

    def on_volume_changed(self, slider):
        level = int(slider.get_value())
        set_volume(level)

    def on_brightness_changed(self, slider):
        percent = int(slider.get_value())
        set_brightness(percent)


def launch():
    win = SettingsApp()
    win.show_all()
    return win
