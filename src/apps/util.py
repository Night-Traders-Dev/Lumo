# apps/util.py
"""
Utility functions for GTK apps
"""

from gi.repository import GLib

def shrink_for_keyboard(window, shrink_ratio=0.25):
    """
    Instantly shrink a GTK window height by shrink_ratio (default 25%)
    from the bottom, keeping the top edge fixed.
    """
    shrink_h = int(window.original_h * (1.0 - shrink_ratio))
    window.move(window.original_x, window.original_y)
    window.resize(window.original_w, shrink_h)


def restore_size(window):
    """
    Instantly restore the original geometry of a GTK window.
    """
    window.move(window.original_x, window.original_y)
    window.resize(window.original_w, window.original_h)


def animate_resize(window, target_h, duration=200, steps=10):
    """
    Smoothly resize a window height to target_h over duration (ms).

    Args:
        window (Gtk.Window): Window instance with original geometry.
        target_h (int): Final height to animate to.
        duration (int): Animation duration in milliseconds.
        steps (int): Number of animation steps.
    """
    start_h = window.get_size()[1]
    delta_h = target_h - start_h
    step = 0
    interval = duration // steps

    def tick():
        nonlocal step
        step += 1
        progress = step / steps
        new_h = int(start_h + delta_h * progress)
        window.move(window.original_x, window.original_y)
        window.resize(window.original_w, new_h)

        return step < steps  # stop when done

    GLib.timeout_add(interval, tick)


def animate_shrink_for_keyboard(window, shrink_ratio=0.25, duration=200, steps=10):
    """
    Animate shrinking window height by shrink_ratio (default 25%) from bottom.
    """
    target_h = int(window.original_h * (1.0 - shrink_ratio))
    animate_resize(window, target_h, duration, steps)


def animate_restore_size(window, duration=200, steps=10):
    """
    Animate restoring window to original geometry.
    """
    animate_resize(window, window.original_h, duration, steps)
