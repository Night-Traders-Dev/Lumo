#!/usr/bin/env python3
import gi
gi.require_version("Gtk", "3.0")
from gi.repository import Gtk, Gdk
from shell.shell import TouchShell

if __name__ == "__main__":
    Gdk.set_program_class("TouchShell")
    shell = TouchShell()
    shell.panel.show_all()
    shell.edge_trigger.show_all()
    Gtk.main()
