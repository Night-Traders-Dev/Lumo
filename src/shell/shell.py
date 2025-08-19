from styles.styles import load_css
from windows.panel import TopPanel
from windows.app_drawer import AppDrawer
from windows.favorites import LeftFavorites, LeftEdgeTrigger

class TouchShell:
    def __init__(self):
        load_css()
        self.panel = TopPanel(self)
        self.drawer = AppDrawer(self)
        self.favorites = LeftFavorites(self)
        self.edge_trigger = LeftEdgeTrigger(self)

    def toggle(self, close_only=False):
        if self.drawer.is_visible() or close_only:
            self.drawer.slide_out()
        else:
            if self.favorites.is_visible():
                self.toggle_favorites(close_only=True)
            self.drawer.slide_in()

    def toggle_favorites(self, close_only=False):
        if self.favorites.is_visible() or close_only:
            self.favorites.slide_out()
        else:
            if self.drawer.is_visible():
                self.toggle(close_only=True)
            self.favorites.slide_in()

    # Optional helpers
    def open_favorites(self):
        if not self.favorites.is_visible():
            if self.drawer.is_visible():
                self.toggle(close_only=True)
            self.favorites.slide_in()

    def close_favorites(self):
        if self.favorites.is_visible():
            self.favorites.slide_out()

