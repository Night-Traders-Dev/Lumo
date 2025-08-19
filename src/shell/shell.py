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

        self.drawer_open = False
        self.fav_open = False

    def toggle(self, close_only=False):
        if self.drawer_open or close_only:
            self.drawer.slide_out()
            self.drawer_open = False
        else:
            if self.fav_open:
                self.toggle_favorites(close_only=True)
            self.drawer.slide_in()
            self.drawer_open = True

    def toggle_favorites(self, close_only=False):
        if self.fav_open or close_only:
            self.favorites.slide_out()
            self.fav_open = False
        else:
            if self.drawer_open:
                self.toggle(close_only=True)
            self.favorites.slide_in()
            self.fav_open = True
