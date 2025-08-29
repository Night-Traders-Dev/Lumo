# Lumo/src/windows/app_drawer.py
from gi.repository import Gtk, GLib, Gdk
from config.config import *
from shell.utils import get_display_geo, discover_apps, exec_cmd, get_packages
from styles.styles import load_css

class AppDrawer(Gtk.Window):
    def __init__(self, shell):
        super().__init__(title="Apps")
        self.shell = shell
        self.set_type_hint(Gdk.WindowTypeHint.DOCK)
        self.set_decorated(False)
        self.set_keep_above(True)
        self.set_app_paintable(True)  # allow CSS bg
        self.set_name("drawer")
        load_css()

        sw, sh = get_display_geo()
        self.width = int(sw * DRAWER_WIDTH_RATIO)
        self.height = sh - PANEL_HEIGHT
        self.set_default_size(self.width, self.height)

        # Drawer slides in from LEFT edge
        self.anim_open_x = 0
        self.anim_closed_x = -self.width
        self.move(self.anim_closed_x, PANEL_HEIGHT)

        # Notebook for multiple pages
        self.notebook = Gtk.Notebook()
        self.add(self.notebook)

        # --- Pages ---
        self.lumo_grid = self._create_scroll_grid()
        self.linux_grid = self._create_scroll_grid()
        self.android_grid = self._create_scroll_grid()

        self.notebook.append_page(self.lumo_grid["scrolled_window"], Gtk.Label(label="Lumo Apps"))
        self.notebook.append_page(self.linux_grid["scrolled_window"], Gtk.Label(label="Linux Apps"))
        self.notebook.append_page(self.android_grid["scrolled_window"], Gtk.Label(label="Android Apps"))

        self.populate_lumo_apps()
        self.populate_linux_apps()
        self.populate_android_apps()

        self.hide()
        self.anim_id = None

    def _create_scroll_grid(self):
        sc = Gtk.ScrolledWindow()
        sc.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        sc.set_kinetic_scrolling(True)
        sc.set_capture_button_press(True)
        sc.set_overlay_scrolling(True)

        grid = Gtk.Grid()
        grid.set_row_spacing(16)
        grid.set_column_spacing(18)
        grid.set_margin_top(16)
        grid.set_margin_bottom(16)
        grid.set_margin_start(16)
        grid.set_margin_end(16)
        sc.add(grid)

        return {"scrolled_window": sc, "grid": grid}

    def populate_lumo_apps(self):
        # Lumo apps: just pick from FAVORITES that are local
        apps_per_row = 4
        r = c = 0
        for app in FAVORITES:
            btn = Gtk.Button()
            vb = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=2)
            img = Gtk.Image.new_from_icon_name(app["icon"], Gtk.IconSize.DIALOG)
            lbl = Gtk.Label(label=app["name"])
            lbl.set_ellipsize(3)
            lbl.set_lines(2)
            lbl.set_justify(Gtk.Justification.CENTER)
            lbl.get_style_context().add_class("app-label")
            vb.pack_start(img, True, True, 0)
            vb.pack_start(lbl, False, False, 0)
            btn.add(vb)
            btn.connect("clicked", self.launch, app["cmd"])
            self.lumo_grid["grid"].attach(btn, c, r, 1, 1)
            c += 1
            if c >= apps_per_row:
                c = 0
                r += 1
        self.lumo_grid["grid"].show_all()

    def populate_linux_apps(self):
        apps = discover_apps()
        apps_per_row = 4
        r = c = 0
        for app in apps:
            btn = Gtk.Button()
            vb = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=2)
            img = Gtk.Image.new_from_icon_name(app["icon"], Gtk.IconSize.DIALOG)
            lbl = Gtk.Label(label=app["name"])
            lbl.set_ellipsize(3)
            lbl.set_lines(2)
            lbl.set_justify(Gtk.Justification.CENTER)
            lbl.get_style_context().add_class("app-label")
            vb.pack_start(img, True, True, 0)
            vb.pack_start(lbl, False, False, 0)
            btn.add(vb)
            btn.connect("clicked", self.launch, app["cmd"])
            self.linux_grid["grid"].attach(btn, c, r, 1, 1)
            c += 1
            if c >= apps_per_row:
                c = 0
                r += 1
        self.linux_grid["grid"].show_all()

    def populate_android_apps(self):
        apps = get_packages()  # List of installed Android packages
        apps_per_row = 4
        r = c = 0
        for pkg in apps:
            btn = Gtk.Button()
            vb = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=2)
            lbl = Gtk.Label(label=pkg)
            lbl.set_ellipsize(3)
            lbl.set_lines(2)
            lbl.set_justify(Gtk.Justification.CENTER)
            lbl.get_style_context().add_class("app-label")
            vb.pack_start(lbl, True, True, 0)
            btn.add(vb)
            # Use Termux am start if possible
            cmd = f"am start -n {pkg}/.MainActivity"
            btn.connect("clicked", self.launch, cmd)
            self.android_grid["grid"].attach(btn, c, r, 1, 1)
            c += 1
            if c >= apps_per_row:
                c = 0
                r += 1
        self.android_grid["grid"].show_all()

    def launch(self, _btn, cmd):
        exec_cmd(cmd)
        self.shell.toggle(close_only=True)

    # === Touch Gestures ===
    def on_swipe(self, _gesture, velocity_x, _velocity_y):
        if velocity_x < -100:
            self.shell.toggle(close_only=True)

    def on_drag_update(self, gesture, offset_x, offset_y):
        adj = self.notebook.get_vadjustment()
        new_value = adj.get_value() - (offset_y - getattr(self, "drag_last_y", 0))
        self.drag_last_y = offset_y
        adj.set_value(max(0, min(new_value, adj.get_upper() - adj.get_page_size())))

    def on_drag_end(self, gesture, offset_x, offset_y):
        self.drag_last_y = 0

    # === Animations ===
    def slide_in(self):
        self.show_all()
        self.present()
        self._start_anim(opening=True)

    def slide_out(self):
        self._start_anim(opening=False)

    def _start_anim(self, opening=True, duration_ms=160):
        if hasattr(self, "anim_id") and self.anim_id:
            GLib.source_remove(self.anim_id)
        start = GLib.get_monotonic_time()
        start_x, end_x = (self.anim_closed_x, self.anim_open_x) if opening else (self.anim_open_x, self.anim_closed_x)

        def step():
            now = GLib.get_monotonic_time()
            t = min(1.0, (now - start) / (duration_ms * 1000.0))
            tt = 1 - (1 - t) * (1 - t)
            x = int(start_x + (end_x - start_x) * tt)
            self.move(x, PANEL_HEIGHT)
            if t >= 1.0 and not opening:
                self.hide()
                return False
            return True

        self.move(start_x, PANEL_HEIGHT)
        self.anim_id = GLib.timeout_add(10, step)
