# Lumo/src/windows/file_manager.py
import gi, os, subprocess
gi.require_version("Gtk", "3.0")
from gi.repository import Gtk, Gdk

sw = Gdk.Screen.get_default().get_width()
sh = Gdk.Screen.get_default().get_height()

class FileManager(Gtk.Window):
    def __init__(self, start_path=None):
        super().__init__(title="Lumo File Manager")
        self.set_keep_above(True)
        self.set_resizable(True)
        self.set_decorated(False)
        self.set_default_size(sw, sh)

        # Start path
        self.current_path = start_path or os.path.expanduser("~")

        # Main vertical box
        vbox = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=4)
        self.add(vbox)

        # Header: path entry + back button
        header = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=4)
        self.path_entry = Gtk.Entry()
        self.path_entry.set_text(self.current_path)
        self.path_entry.connect("activate", self.on_path_entered)
        back_btn = Gtk.Button(label="◀ Back")
        back_btn.connect("clicked", self.go_back)
        header.pack_start(back_btn, False, False, 0)
        header.pack_start(self.path_entry, True, True, 0)
        vbox.pack_start(header, False, False, 0)

        # Scrollable file list
        scrolled = Gtk.ScrolledWindow()
        scrolled.set_policy(Gtk.PolicyType.AUTOMATIC, Gtk.PolicyType.AUTOMATIC)
        vbox.pack_end(scrolled, True, True, 0)

        self.listbox = Gtk.ListBox()
        self.listbox.connect("row-activated", self.on_row_activated)  # double-tap/double-click to open
        scrolled.add(self.listbox)

        # Populate initial directory
        self.populate_files(self.current_path)

        # Reposition like the keyboard
        self.connect("size-allocate", self.on_size_allocate)

    def populate_files(self, path):
        self.listbox.foreach(lambda child: self.listbox.remove(child))
        self.current_path = path
        self.path_entry.set_text(path)

        try:
            items = sorted(
                os.listdir(path),
                key=lambda x: (not os.path.isdir(os.path.join(path, x)), x.lower())
            )
        except PermissionError:
            items = []

        for item in items:
            full_path = os.path.join(path, item)
            row = Gtk.ListBoxRow()
            row.full_path = full_path   # store path on row

            hbox = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=6)
            icon = Gtk.Image.new_from_icon_name(
                "folder" if os.path.isdir(full_path) else "text-x-generic",
                Gtk.IconSize.DIALOG
            )
            label = Gtk.Label(label=item, xalign=0)
            hbox.pack_start(icon, False, False, 0)
            hbox.pack_start(label, True, True, 0)
            row.add(hbox)

            self.listbox.add(row)

        self.listbox.show_all()

    def on_row_activated(self, listbox, row):
        """Triggered on double-tap/double-click"""
        full_path = getattr(row, "full_path", None)
        if not full_path:
            return
        if os.path.isdir(full_path):
            self.populate_files(full_path)
        else:
            subprocess.Popen(["xdg-open", full_path])

    def on_path_entered(self, entry):
        path = entry.get_text()
        if os.path.exists(path) and os.path.isdir(path):
            self.populate_files(path)

    def go_back(self, button):
        parent = os.path.dirname(self.current_path)

        # If already at top-level, close window
        if not parent or parent == self.current_path or parent == "/":
            self.destroy()
            global _file_manager_window
            _file_manager_window = None
            return

        if os.path.exists(parent):
            self.populate_files(parent)

    def on_size_allocate(self, widget, allocation):
        fm_height = int(sh * 0.97)
        fm_width = sw
        widget.resize(fm_width, fm_height)
        widget.move(0, sh - fm_height)


# Launch helper
_file_manager_window = None

def launch(start_path=None):
    global _file_manager_window
    if _file_manager_window is None:
        _file_manager_window = FileManager(start_path)
        _file_manager_window.show_all()
    else:
        _file_manager_window.destroy()
        _file_manager_window = None
