import os, subprocess, configparser
from gi.repository import Gdk

def get_display_geo():
    display = Gdk.Display.get_default()
    m = display.get_primary_monitor()
    g = m.get_geometry()
    return g.width, g.height

def exec_cmd(cmd):
    if not cmd: return
    try:
        subprocess.Popen(cmd.split())
    except Exception as e:
        print("Launch failed:", e)

def discover_apps():
    apps = []
    dirs = ["/usr/share/applications", os.path.expanduser("~/.local/share/applications")]
    for d in dirs:
        if not os.path.isdir(d): continue
        for file in os.listdir(d):
            if not file.endswith(".desktop"): continue
            path = os.path.join(d, file)
            cfg = configparser.ConfigParser(interpolation=None)
            try:
                cfg.read(path)
                entry = cfg["Desktop Entry"]
                if entry.get("Type") != "Application": continue
                if entry.get("NoDisplay", "false").lower() == "true": continue
                name = entry.get("Name", os.path.splitext(file)[0])
                exec0 = entry.get("Exec", "").split()[0]
                if not exec0: continue
                icon = entry.get("Icon", "application-x-executable")
                apps.append({"name": name, "cmd": exec0, "icon": icon})
            except Exception:
                continue
    return sorted(apps, key=lambda a: a["name"].lower())

def trigger_phone():
    try:
        with open("/data/data/com.termux/files/home/call_request.txt", "a") as f:
            f.write("\n")  # empty = open dialer
        print("Phone trigger sent")
    except Exception as e:
        print("Failed to trigger phone:", e)
