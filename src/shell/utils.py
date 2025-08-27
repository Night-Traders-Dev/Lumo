import os, subprocess, configparser, socket, re, gi
gi.require_version("Gdk", "3.0")
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


def trigger_phone(host="127.0.0.1", port=12345):
    """
    Sends the command to open the Android dialer (am start -a android.intent.action.DIAL)
    via a local TCP connection to Termux's netcat listener.

    Args:
        host (str): The host address to connect to (default: 127.0.0.1)
        port (int): The port number where Termux listener is running (default: 12345)
    """
    tst = 'termux-toast "Opening Dialer"\n'
    cmd = 'am start -a android.intent.action.DIAL\n'
    try:
        with socket.create_connection((host, port), timeout=5) as s:
            s.sendall(tst.encode())
            s.sendall(cmd.encode())
            s.shutdown(socket.SHUT_WR)
    except Exception as e:
        print(f"Error triggering phone: {e}")



def get_active_network():
    try:
        out = subprocess.check_output(
            ["ifconfig"], stderr=subprocess.DEVNULL
        ).decode()
    except (FileNotFoundError, subprocess.CalledProcessError):
        try:
            out = subprocess.check_output(
                ["ip", "addr"], stderr=subprocess.DEVNULL
            ).decode()
        except Exception:
            return "Unknown"

    # Check for wlan0 with IPv4
    wlan_match = re.search(r"wlan0:.*?\n\s+inet (\d+\.\d+\.\d+\.\d+)", out, re.S)
    if wlan_match:
        return "wlan0"

    # Check for rmnet_dataX with IPv4 (mobile data)
    rmnet_match = re.search(r"(rmnet_data\d+):.*?\n\s+inet (\d+\.\d+\.\d+\.\d+)", out, re.S)
    if rmnet_match:
        return rmnet_match.group(1)

    return "Unknown"
