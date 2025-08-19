# Lumo/src/windows/favorites.py
from gi.repository import Gtk, GLib, Gdk
from config.config import *
from shell.utils import get_display_geo, exec_cmd, trigger_phone
from styles.styles import load_css

class LeftFavorites(Gtk.Window):
    """
    Favorites bar slides from the LEFT edge
    """
    def __init__(self, shell):
        super().__init__(title="Favorites")
        self.shell = shell
        self.set_type_hint(Gdk.WindowTypeHint.DOCK)
        self.set_decorated(False)
        self.set_resizable(False)
        self.set_keep_above(True)
        self.set_app_paintable(True)
        self.set_name("dock")

        # Load global CSS
        load_css()

        sw, sh = get_display_geo()
        self.sw = sw
        self.sh = sh

        # Width of favorites bar (can be adjusted)
        self.width = DOCK_WIDTH
        self.set_default_size(self.width, sh - PANEL_HEIGHT)
        self.move(-self.width, PANEL_HEIGHT)  # start offscreen left

        vb = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=6)
        vb.set_margin_top(8)
        vb.set_margin_bottom(8)
        self.add(vb)

        # Favorites buttons
        for app in FAVORITES:
            btn = Gtk.Button()
            col = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=2)
            img = Gtk.Image.new_from_icon_name(app["icon"], Gtk.IconSize.DIALOG)
            lbl = Gtk.Label(label=app["name"])
            lbl.set_xalign(0.5)
            lbl.set_justify(Gtk.Justification.CENTER)
            lbl.get_style_context().add_class("app-label")
            col.pack_start(img, True, True, 0)
            col.pack_start(lbl, False, False, 0)
            btn.add(col)
            btn.connect("clicked", self._launch_and_hide, app["cmd"])
            vb.pack_start(btn, False, False, 2)

        # Gesture swipe
        swipe = Gtk.GestureSwipe.new(self)
        swipe.connect("swipe", self.on_swipe)

        self.anim_id = None
        self.anim_open_x = 0
        self.anim_closed_x = -self.width
        self.hide()

    def _launch_and_hide(self, _btn, cmd):
        if callable(cmd):
            cmd()
        else:
            exec_cmd(cmd)
        self.shell.toggle_favorites(close_only=True)

    def on_swipe(self, _gesture, vx, _vy):
        if vx < -100:
            self.shell.toggle_favorites(close_only=True)
        elif vx > 100:
            self.shell.toggle_favorites(close_only=False)

    def slide_in(self):
        self.show_all()
        self.present()
        self._start_anim(opening=True)

    def slide_out(self):
        self._start_anim(opening=False)

    def _start_anim(self, opening=True, duration_ms=160):
        if self.anim_id:
            GLib.source_remove(self.anim_id)

        start = GLib.get_monotonic_time()
        start_x, end_x = (self.anim_closed_x, self.anim_open_x) if opening else (self.anim_open_x, self.anim_closed_x)

        def step():
            now = GLib.get_monotonic_time()
            t = min(1.0, (now - start) / (duration_ms * 1000.0))
            tt = 1 - (1 - t) * (1 - t)
            x = int(start_x + (end_x - start_x) * tt)
            self.move(x, PANEL_HEIGHT)
            if t >= 1.0:
                if not opening:
                    self.hide()
                return False
            return True

        self.move(start_x, PANEL_HEIGHT)
        self.anim_id = GLib.timeout_add(10, step)


class LeftEdgeTrigger(Gtk.Window):
    """
    Tiny LEFT-edge input window to open favorites
    """
    def __init__(self, shell):
        super().__init__(title="EdgeTrigger")
        self.shell = shell
        self.set_type_hint(Gdk.WindowTypeHint.DOCK)
        self.set_decorated(False)
        self.set_resizable(False)
        self.set_keep_above(True)
        self.set_accept_focus(False)
        self.set_app_paintable(True)
        self.set_name("edge-trigger")

        sw, sh = get_display_geo()
        self.sw = sw
        self.sh = sh
        self.width = EDGE_TRIGGER_W
        self.set_default_size(self.width, sh - PANEL_HEIGHT)
        self.move(0, PANEL_HEIGHT)

        # EventBox for click/hover
        eb = Gtk.EventBox()
        eb.set_visible_window(True)
        eb.set_size_request(self.width, sh - PANEL_HEIGHT)
        eb.connect("button-press-event", self._open)
        eb.connect("enter-notify-event", self._maybe_open)
        self.add(eb)
        self.show()

    def _open(self, *_):
        self.shell.toggle_favorites(close_only=False)

    def _maybe_open(self, *_):
        return False
