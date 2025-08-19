from gi.repository import Gtk, Gdk
from config.config import DOCK_BG_ALPHA, DRAWER_BG_ALPHA, FAVORITES_BG_ALPHA

CSS = f"""
#panel {{
    background: rgba(40, 0, 50, 0.95);
    color: #eee;
}}
#panel .title {{
    font-weight: 600;
}}
#dock {{
    background: rgba(0,0,0,{FAVORITES_BG_ALPHA});
}}
#dock GtkButton {{
    background: transparent;
    border: none;
}}
#dock GtkButton:hover {{
    background: rgba(255,255,255,0.06);
}}
#drawer {{
    background: rgba(0,0,0,{DRAWER_BG_ALPHA});
}}
#drawer .app-label {{
    color: #eee;
    font-size: 10.5pt;
}}
#edge-trigger {{
    background: rgba(0,0,0,0.01);
}}
"""

def load_css():
    provider = Gtk.CssProvider()
    provider.load_from_data(CSS.encode("utf-8"))
    Gtk.StyleContext.add_provider_for_screen(
        Gdk.Screen.get_default(),
        provider,
        Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION
    )
