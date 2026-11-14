"""Runs the dev screenshot tour on a real game client for one layer and collects the screenshots.

Usage: python scripts/client_tour.py <layer> [<layer> ...]     e.g. neoforge-26.2 forge-26.2

Starts `gradlew runClient -PdevTools` (through the Gradle slot wrapper) on the hidden Windows desktop of launch-isolated.ps1, so no window,
focus or cursor change ever reaches the real desktop, with THOROUGHCHESTS_SHOTS set. The tour quits the game itself when it is done.
Screenshots land in <layer>/run/screenshots and are copied to docs/frames/<layer>/.
"""
import os
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LAUNCH = r"C:\Users\casey\.claude\tools\launch-isolated.ps1"
SLOT = "C:/Users/casey/.claude/tools/gradle-slot.sh"
TIMEOUT = 900
OPTIONS = (
    "pauseOnLostFocus:false\nonboardAccessibility:false\nskipMultiplayerWarning:true\njoinedFirstServer:true\ntutorialStep:none\n"
    "fullscreen:false\nguiScale:3\nlang:en_us\n"
)


def tour(layer, viewers=None):
    folder = ROOT / layer
    run = folder / "run"
    run.mkdir(exist_ok=True)
    (run / "options.txt").write_text(OPTIONS)
    shots = run / "screenshots"
    if shots.exists():
        shutil.rmtree(shots)
    latest = run / "logs" / "latest.log"
    if latest.exists():
        latest.unlink()
    gradle = f"bash {SLOT} ./gradlew runClient --no-daemon -q -PdevTools" + (f" -Pviewers={viewers}" if viewers else "")
    command = f"set THOROUGHCHESTS_SHOTS=1&& {gradle}"
    start = time.time()
    subprocess.run(["powershell", "-NoProfile", "-File", LAUNCH, "-Run", "-WorkDir", str(folder), "-Command", command],
                   capture_output=True, text=True)
    done = False
    while time.time() - start < TIMEOUT:
        time.sleep(5)
        if latest.exists():
            text = latest.read_text(encoding="utf-8", errors="replace")
            if "THOROUGHCHESTS-SHOTS DONE" in text:
                done = True
                break
    time.sleep(8)
    subprocess.run(["powershell", "-NoProfile", "-File", LAUNCH, "-Kill", "-WorkDir", str(folder)], capture_output=True, text=True)
    text = latest.read_text(encoding="utf-8", errors="replace") if latest.exists() else ""
    names = sorted(p.name for p in shots.glob("*.png")) if shots.exists() else []
    target = ROOT / "docs" / "frames" / layer
    if names:
        target.mkdir(parents=True, exist_ok=True)
        for p in shots.glob("*.png"):
            shutil.copyfile(p, target / p.name.replace("thoroughchests_", ""))
    errors = [l for l in text.splitlines() if re.search(r"THOROUGHCHESTS-SHOTS failed|thoroughchests.*(Exception|Error)|ModLoadingException|Crash report saved", l, re.I)]
    status = "TOUR PASS" if done and not errors else "TOUR FAIL"
    detail = "" if status == "TOUR PASS" else f": done={done} first_error={errors[:1]}"
    return f"{status} {layer} ({len(names)} screenshots, {int(time.time() - start)}s){detail}"


if __name__ == "__main__":
    bad = 0
    args = [a for a in sys.argv[1:] if not a.startswith("--viewers=")]
    viewers = next((a.split("=", 1)[1] for a in sys.argv[1:] if a.startswith("--viewers=")), None)
    for layer in args:
        result = tour(layer, viewers)
        print(result, flush=True)
        bad += result.startswith("TOUR FAIL")
    sys.exit(1 if bad else 0)
