#!/usr/bin/env python3
"""
End-to-end UI walkthrough of StudyNest on a running emulator.

Drives the app through every screen using adb + uiautomator (Compose test tags are exposed as
resource ids), asserts what it sees along the way (printing a ✓ per check) and saves numbered
screenshots. Pair it with `scripts/record.sh` to capture a demo video.

Requirements: the API server on port 8787 with the demo account freshly seeded
(`bun run db:seed` in server/), the app installed, and one emulator visible to adb
(or set ANDROID_SERIAL).

Usage: python3 scripts/demo.py [--shots DIR] [--pace SECONDS]
"""
import argparse
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ADB = os.path.join(os.environ.get("ANDROID_HOME", "/opt/android-sdk"), "platform-tools", "adb")
PKG = "me.aysh.studynest"
DEMO_EMAIL = "demo@studynest.app"
DEMO_PASSWORD = "demo12345"
ALLOW = "com.android.permissioncontroller:id/permission_allow_button"

parser = argparse.ArgumentParser()
parser.add_argument("--shots", default="artifacts/screenshots")
parser.add_argument("--pace", type=float, default=1.0, help="pause between steps, for a watchable video")
args = parser.parse_args()
os.makedirs(args.shots, exist_ok=True)
for old in os.listdir(args.shots):
    if old.endswith(".png"):
        os.remove(os.path.join(args.shots, old))

checks: list[str] = []


# --- adb helpers -------------------------------------------------------------------------------

def adb(*cmd: str) -> str:
    return subprocess.run([ADB, *cmd], check=True, capture_output=True, text=True).stdout


def shell(*cmd: str) -> str:
    return adb("shell", *cmd)


def pause(factor: float = 1.0) -> None:
    time.sleep(args.pace * factor)


def dump() -> ET.Element:
    for _ in range(8):
        try:
            out = shell("uiautomator", "dump", "/sdcard/ui.xml")
            if "ERROR" in out:
                raise ValueError(out)
            xml = shell("cat", "/sdcard/ui.xml")
            return ET.fromstring(xml[xml.index("<?xml"):] if "<?xml" in xml else xml)
        except (subprocess.CalledProcessError, ET.ParseError, ValueError):
            time.sleep(0.6)
    raise RuntimeError("uiautomator dump failed")


def bounds(node: ET.Element) -> tuple[int, int, int, int]:
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.attrib["bounds"]))
    return x1, y1, x2, y2


def center(node: ET.Element) -> tuple[int, int]:
    x1, y1, x2, y2 = bounds(node)
    return (x1 + x2) // 2, (y1 + y2) // 2


def rid(node: ET.Element) -> str:
    value = node.attrib.get("resource-id", "")
    return value.split(":id/", 1)[1] if ":id/" in value else value


def label(node: ET.Element) -> str:
    return node.attrib.get("text", "") + " " + node.attrib.get("content-desc", "")


def find(tag=None, text=None, contains=None, prefix=None, root=None):
    root = root if root is not None else dump()
    for node in root.iter("node"):
        if tag and (rid(node) == tag or node.attrib.get("resource-id") == tag):
            return node
        if prefix and rid(node).startswith(prefix):
            return node
        if text and node.attrib.get("text") == text:
            return node
        if contains and contains.lower() in label(node).lower():
            return node
    return None


def find_all(prefix: str, root=None) -> list[ET.Element]:
    root = root if root is not None else dump()
    return [n for n in root.iter("node") if rid(n).startswith(prefix)]


def wait_for(timeout: float = 15, quiet: bool = False, **query) -> ET.Element:
    deadline = time.time() + timeout
    while time.time() < deadline:
        node = find(**query)
        if node is not None:
            return node
        time.sleep(0.4)
    if not quiet:
        shot("FAILED")
    raise AssertionError(f"Timed out waiting for {query}")


def gone(timeout: float = 10, **query) -> None:
    deadline = time.time() + timeout
    while time.time() < deadline:
        if find(**query) is None:
            return
        time.sleep(0.4)
    shot("FAILED")
    raise AssertionError(f"Still visible: {query}")


def tap_node(node: ET.Element, factor: float = 1.0) -> None:
    x, y = center(node)
    shell("input", "tap", str(x), str(y))
    pause(factor)


def tap(tag=None, factor: float = 1.0, timeout: float = 15, **query) -> None:
    if tag:
        query["tag"] = tag
    try:
        node = wait_for(timeout=min(timeout, 5), quiet=True, **query)
    except AssertionError:
        # Probably off-screen (e.g. behind the keyboard): dismiss it and scroll the target into view.
        hide_keyboard()
        node = find(**query)
        if node is None:
            node = scroll_to(**query)
    tap_node(node, factor)


def type_text(value: str) -> None:
    # Type in small chunks so it reads naturally on video.
    for chunk in re.findall(r".{1,4}", value):
        shell("input", "text", chunk.replace(" ", "%s").replace("&", "\\&").replace("'", "\\'"))
        time.sleep(0.1)
    pause(0.4)


def clear_field() -> None:
    shell("input", "keyevent", "KEYCODE_MOVE_END")
    shell("input", "keyevent", *(["KEYCODE_DEL"] * 45))


def fill(tag: str, value: str) -> None:
    tap(tag, factor=0.4)
    clear_field()
    if value:
        type_text(value)


def keyboard_shown() -> bool:
    return "mInputShown=true" in shell("dumpsys", "input_method")


def hide_keyboard() -> None:
    if keyboard_shown():
        shell("input", "keyevent", "KEYCODE_BACK")
        pause(0.6)


def back() -> None:
    shell("input", "keyevent", "KEYCODE_BACK")
    pause(1)


_size = None


def screen_size() -> tuple[int, int]:
    global _size
    if _size is None:
        w, h = re.search(r"(\d+)x(\d+)", shell("wm", "size")).groups()
        _size = (int(w), int(h))
    return _size


def swipe(x1, y1, x2, y2, ms=400) -> None:
    shell("input", "swipe", str(x1), str(y1), str(x2), str(y2), str(ms))
    pause(0.8)


def scroll_down(fraction: float = 0.4) -> None:
    w, h = screen_size()
    swipe(w // 2, int(h * 0.7), w // 2, int(h * (0.7 - fraction)), 600)


def scroll_up(fraction: float = 0.4) -> None:
    w, h = screen_size()
    swipe(w // 2, int(h * 0.3), w // 2, int(h * (0.3 + fraction)), 600)


def scroll_to(tries: int = 6, **query) -> ET.Element:
    for _ in range(tries):
        node = find(**query)
        if node is not None:
            return node
        scroll_down(0.35)
    shot("FAILED")
    raise AssertionError(f"Couldn't scroll to {query}")


shot_index = 0


def shot(name: str) -> None:
    global shot_index
    shot_index += 1
    path = os.path.join(args.shots, f"{shot_index:02d}-{name}.png")
    with open(path, "wb") as f:
        f.write(subprocess.run([ADB, "exec-out", "screencap", "-p"], check=True, capture_output=True).stdout)


def check(description: str, condition: bool) -> None:
    if not condition:
        shot("FAILED")
        raise AssertionError(description)
    checks.append(description)
    print(f"  ✓ {description}")


def step(title: str) -> None:
    print(f"\n▸ {title}")


def launch() -> None:
    shell("am", "start", "-W", "-n", f"{PKG}/.MainActivity")
    pause(2)


def go_tab(name: str, screen: str) -> None:
    tap(f"tab_{name}", factor=1.2)
    wait_for(tag=f"{screen}_screen")


# --- walkthrough --------------------------------------------------------------------------------

started = time.time()
new_email = f"maya.{int(time.time()) % 100000}@college.edu"

step("Fresh install and launch")
shell("cmd", "uimode", "night", "no")
# Revoking a permission kills the app, so do it before clearing data and launching.
shell("pm", "revoke", PKG, "android.permission.ACCESS_LOCAL_NETWORK")
shell("pm", "clear", PKG)
time.sleep(1)
launch()
allow = wait_for(tag=ALLOW)
check("Android 17 local network permission is requested on launch", allow is not None)
shot("permission")
tap(ALLOW)

step("Welcome")
wait_for(tag="welcome_get_started")
pause(2)
shot("welcome")
check("Welcome shows both actions", find(tag="welcome_sign_in") is not None)

step("Sign up — validation")
tap("welcome_get_started")
wait_for(tag="auth_name")
hide_keyboard()
shot("signup")
tap("auth_submit", factor=1.2)
root = dump()
check("Empty sign-up shows field errors", find(contains="Enter your name", root=root) is not None)
check("Errors appear only after the first submit", find(contains="Enter your email", root=root) is not None)
shot("signup-errors")
fill("auth_name", "Maya")
check("Errors update live while typing", find(contains="Enter your name") is None)
fill("auth_email", "maya@college")
fill("auth_password", "short")
hide_keyboard()
tap("auth_submit", factor=1.2)
root = dump()
check("Invalid email is rejected", find(contains="valid email", root=root) is not None)
check("Short password is rejected", find(contains="at least 8", root=root) is not None)
shot("signup-invalid")

step("Sign up — duplicate email")
fill("auth_email", DEMO_EMAIL)
fill("auth_password", "Study#Nest2026")
hide_keyboard()
tap("auth_password_toggle", factor=0.8)
check("Password can be revealed", find(text="Study#Nest2026") is not None)
shot("signup-strength")
tap("auth_submit", factor=1.5)
check("Duplicate email shows a clear error", wait_for(contains="already exists") is not None)
check("Form error banner is shown", find(tag="auth_form_error") is not None)
shot("signup-duplicate")

step("Sign up — success")
fill("auth_email", new_email)
hide_keyboard()
tap("auth_submit", factor=2)
wait_for(tag="today_screen", timeout=20)
check("New account lands on Today", wait_for(contains="Maya") is not None)
check("New user sees the empty state", wait_for(tag="today_empty_state") is not None)
pause(1.5)
shot("today-empty")

step("Add a subject")
tap("today_add_subject", factor=1.5)
wait_for(tag="subjects_screen")
shot("subjects-empty")
tap("subjects_add", factor=1.2)
wait_for(tag="subject_name")
tap("subject_save", factor=1)
check("Saving without a name shows an error", wait_for(contains="Give the subject a name") is not None)
fill("subject_name", "Physics")
hide_keyboard()
tap("subject_color_mint", factor=0.6)
tap("subject_icon_flask", factor=0.8)
shot("subject-sheet")
tap("subject_save", factor=1.5)
gone(tag="subject_sheet")
check("Subject appears in the grid", wait_for(text="Physics") is not None)
shot("subjects-one")
tap("subjects_back", factor=1.5)
wait_for(tag="today_screen")

step("Add an assignment")
go_tab("assignments", "assignments")
check("Empty list explains what to do", wait_for(tag="assignments_empty") is not None)
shot("assignments-empty")
tap("assignments_empty_add", factor=1.5)
wait_for(tag="editor_title")
hide_keyboard()
tap("editor_save", factor=1)
check("Saving without a title shows an error", wait_for(contains="Give it a title") is not None)
fill("editor_title", "Lab report: pendulum")
hide_keyboard()
subject_chip = find(prefix="editor_subject_")
check("Only subject is preselected or selectable", subject_chip is not None)
tap_node(subject_chip, 0.6)
tap("editor_priority_high", factor=0.6)
tap("editor_due_date", factor=1.5)
shot("editor-date-picker")
tap("date_confirm", factor=1.2)
fill("editor_notes", "Measure g with three string lengths")
hide_keyboard()
shot("editor-new")
tap("editor_save", factor=0.3)
check("Saved message is shown", wait_for(contains="Assignment added", timeout=6) is not None)
wait_for(tag="assignments_screen")
check("Assignment appears in the list", wait_for(contains="Lab report: pendulum") is not None)
shot("assignments-one")

step("Focus — a 1-minute session")
go_tab("focus", "focus")
subject = next((n for n in find_all("focus_subject_") if rid(n) != "focus_subject_none"), None)
check("Focus offers the new subject", subject is not None)
tap_node(subject, 0.6)
tap("focus_duration_custom", factor=1.2)
wait_for(tag="custom_minutes_field")
fill("custom_minutes_field", "1")
hide_keyboard()
shot("focus-custom")
tap("custom_minutes_save", factor=1)
shot("focus-setup")
tap("focus_start", factor=2)
check("Timer is running", wait_for(tag="focus_pause") is not None)
shot("focus-running")
tap("focus_pause", factor=2)
paused_time = wait_for(tag="focus_time").attrib.get("text", "")
pause(2)
check("Pausing freezes the countdown", wait_for(tag="focus_time").attrib.get("text", "") == paused_time)
shot("focus-paused")
tap("focus_resume", factor=1)
check("Session finishes and is saved", wait_for(tag="focus_result", timeout=80) is not None)
check("Result says it was saved", wait_for(contains="saved") is not None)
pause(1.5)
shot("focus-finished")
tap("focus_done", factor=1.2)

step("Mark the assignment done, then undo")
go_tab("assignments", "assignments")
row = wait_for(prefix="assignment_row_")
x1, y1, x2, y2 = bounds(row)
w, _ = screen_size()
swipe(int(w * 0.12), (y1 + y2) // 2, int(w * 0.9), (y1 + y2) // 2, 600)
undo, root = None, None
for _ in range(6):
    root = dump()
    undo = find(tag="snackbar_action", root=root)
    if undo is not None:
        break
check("Swipe shows an Undo snackbar", undo is not None)
check("Row leaves the open list", find(prefix="assignment_row_", root=root) is None)
shot("assignments-done-undo")
tap_node(undo, 2)
check("Undo brings the assignment back", wait_for(prefix="assignment_row_") is not None)

step("Set a daily goal")
go_tab("profile", "profile")
shot("profile-new-user")
tap("profile_goal", factor=1.2)
wait_for(tag="goal_field")
fill("goal_field", "45")
hide_keyboard()
shot("goal-sheet")
tap("goal_save", factor=1.5)
gone(tag="goal_sheet")
check("Goal saved", wait_for(contains="45 min") is not None)
go_tab("today", "today")
check("Today shows progress against the new goal", wait_for(contains="of 45 min") is not None)
pause(1)
shot("today-new-user")

step("Sign out")
go_tab("profile", "profile")
scroll_to(tag="profile_sign_out")
tap("profile_sign_out", factor=1.2)
shot("signout-confirm")
tap("confirm_sign_out", factor=2)
wait_for(tag="welcome_get_started")
check("Signing out returns to Welcome", True)

step("Sign in — wrong password")
tap("welcome_sign_in", factor=1)
fill("auth_email", DEMO_EMAIL)
fill("auth_password", "wrong-password")
hide_keyboard()
tap("auth_submit", factor=2)
check("Wrong password shows an error", wait_for(contains="Incorrect email or password") is not None)
shot("signin-error")

step("Sign in — demo account")
fill("auth_password", DEMO_PASSWORD)
hide_keyboard()
shot("signin")
tap("auth_submit", factor=2.5)
wait_for(tag="today_screen", timeout=20)
check("Demo account greets Ayush", wait_for(contains="Ayush") is not None)
check("Today shows the goal ring", wait_for(tag="today_goal_ring") is not None)
pause(2)
shot("today")
due = scroll_to(prefix="due_item_")
check("Due soon lists assignments", due is not None)
check("Overdue work is called out", find(contains="late") is not None)
pause(1)
shot("today-due-soon")
scroll_up(0.9)

step("Assignments — filter, search, swipe, edit")
go_tab("assignments", "assignments")
check("Overdue group is shown", wait_for(tag="group_overdue") is not None)
pause(1)
shot("assignments")
chip = wait_for(prefix="chip_subject_")
tap_node(chip, 1.5)
shot("assignments-filtered")
tap("chip_all", factor=1.2)
tap("assignments_search", factor=0.5)
type_text("essay")
pause(1.5)
check("Search finds the essay outline", wait_for(contains="Essay outline") is not None)
hide_keyboard()
shot("assignments-search")
fill("assignments_search", "")
hide_keyboard()
pause(1)
tap("assignments_show_completed", factor=1.5)
check("Completed work can be shown", scroll_to(tag="group_done") is not None)
shot("assignments-completed")
scroll_up(1.2)
tap("assignments_show_completed", factor=2.5)
gone(tag="group_done")
row = wait_for(prefix="assignment_row_")
x1, y1, x2, y2 = bounds(row)
swipe(int(w * 0.9), (y1 + y2) // 2, int(w * 0.1), (y1 + y2) // 2, 600)
undo = wait_for(tag="snackbar_action", timeout=8)
check("Swipe to done offers Undo", undo is not None)
tap_node(undo, 2)
row = wait_for(prefix="assignment_row_")
tap_node(row, 1.5)
wait_for(tag="editor_title")
pause(1)
shot("editor-edit")
fill("editor_title", "Eigenvalues set, Q4 to Q12")
hide_keyboard()
tap("editor_priority_medium", factor=0.8)
tap("editor_save", factor=2)
wait_for(tag="assignments_screen")
check("Edited title is shown", wait_for(contains="Eigenvalues set, Q4 to Q12") is not None)

step("Subjects")
tap("assignments_subjects", factor=1.5)
wait_for(tag="subjects_screen")
check("Seeded subjects are listed", wait_for(text="Data Structures") is not None)
pause(1)
shot("subjects")
tile = wait_for(prefix="subject_tile_")
tap_node(tile, 1.5)
wait_for(tag="subject_sheet")
shot("subject-edit")
tap("subject_delete", factor=1.2)
check("Deleting a subject asks for confirmation", wait_for(tag="confirm_delete_subject") is not None)
shot("subject-delete-confirm")
tap("dialog_cancel", factor=1)
back()
gone(tag="subject_sheet")
tap("subjects_back", factor=1.5)

step("Focus timer running")
go_tab("focus", "focus")
subject = next((n for n in find_all("focus_subject_") if rid(n) != "focus_subject_none"), None)
tap_node(subject, 0.6)
tap("focus_duration_25", factor=1)
shot("focus")
tap("focus_start", factor=4)
check("25-minute timer counts down", wait_for(tag="focus_time").attrib.get("text", "").startswith("24:"))
shot("focus-timer")
go_tab("today", "today")
check("Today shows the session in progress", wait_for(contains="in progress") is not None)
shot("today-session-running")
go_tab("focus", "focus")
tap("focus_stop", factor=2)
check("Stopping early under a minute isn't saved", wait_for(contains="under a minute") is not None)
tap("focus_done", factor=1)

step("Insights")
go_tab("insights", "insights")
check("Week total is shown", wait_for(tag="insights_total") is not None)
pause(2)
shot("insights")
scroll_to(tag="insights_donut")
pause(2)
shot("insights-charts")
scroll_down(0.5)
pause(1)
shot("insights-bottom")
scroll_up(1.5)
tap("insights_prev_week", factor=2.5)
check("Previous week has its own label", wait_for(tag="insights_week_label") is not None)
shot("insights-previous-week")
tap("insights_next_week", factor=1.5)

step("Profile")
go_tab("profile", "profile")
check("Profile shows the demo email", wait_for(contains=DEMO_EMAIL) is not None)
pause(1)
shot("profile")

step("Dark mode tour")
shell("cmd", "uimode", "night", "yes")
pause(2.5)
shot("profile-dark")
go_tab("today", "today")
pause(1.5)
shot("today-dark")
go_tab("assignments", "assignments")
pause(1.5)
shot("assignments-dark")
tap("assignments_subjects", factor=1.5)
wait_for(tag="subjects_screen")
pause(1)
shot("subjects-dark")
tap("subjects_back", factor=1.5)
tap("assignments_add", factor=1.5)
wait_for(tag="editor_title")
hide_keyboard()
shot("editor-dark")
tap("editor_close", factor=1.5)
go_tab("focus", "focus")
pause(1)
shot("focus-dark")
go_tab("insights", "insights")
pause(2)
shot("insights-dark")
scroll_to(tag="insights_donut")
pause(2)
shot("insights-charts-dark")
scroll_up(1.5)
shell("cmd", "uimode", "night", "no")
pause(2)

step("Session persists across restarts")
shell("am", "force-stop", PKG)
launch()
check("Still signed in after restart", wait_for(tag="today_screen", timeout=20) is not None)
pause(1.5)

step("Back from Today exits instead of returning to auth")
back()
pause(1)
check("Sign-in screen is not shown after Back", find(tag="auth_email") is None and find(tag="welcome_screen") is None)
launch()
check("Relaunch goes straight to Today", wait_for(tag="today_screen", timeout=20) is not None)

print(f"\nAll {len(checks)} checks passed in {int(time.time() - started)}s. Screenshots in {args.shots}")
