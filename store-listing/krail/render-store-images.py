"""Render the KRAIL store-listing panels to real store-spec PNGs.

Reads krail-screenshot-listing.html, extracts every .store-panel, re-renders each
one on its platform's exact store canvas via headless Chrome, and writes the PNGs
into store-listing/krail/store-images/<platform>/.
"""
import base64, html as htmllib, json, pathlib, re, shutil, subprocess, sys

ROOT = pathlib.Path(__file__).resolve().parent
SRC = ROOT / "krail-screenshot-listing.html"
OUT = ROOT / "store-images"
WORK = ROOT / ".render-work"
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

# Store canvases. css = layout size in CSS px, dsf = device scale factor.
# css * dsf must equal the exact store pixel spec.
#
# Chrome clamps a headless window to a 500px minimum width. A narrower
# --window-size still lays out at 500 while the screenshot stays at the width we
# asked for, silently cropping the right edge, so every css width here stays at
# or above MIN_WINDOW_WIDTH and the scale factor absorbs the difference.
MIN_WINDOW_WIDTH = 500

PLATFORMS = {
    "android-phone":  {"section": "android-phone",  "css": (540, 960),   "dsf": 2, "px": (1080, 1920)},
    "android-tablet": {"section": "android-tablet", "css": (960, 600),   "dsf": 2, "px": (1920, 1200)},
    "iphone-6-5":     {"section": "iphone",         "css": (621, 1344),  "dsf": 2, "px": (1242, 2688)},
    "ipad-13":        {"section": "ipad",           "css": (516, 688),   "dsf": 4, "px": (2064, 2752)},
}

SLUGS = {
    "android-phone": ["saved-trips", "live-times", "parking", "service-alerts",
                      "live-delays", "plan-your-day", "dark-mode"],
    "android-tablet": ["saved-trips", "parking", "whole-journey", "plan-your-day"],
    "iphone-6-5": ["saved-trips", "parking", "whole-journey", "plan-your-day"],
    "ipad-13": ["saved-trips", "parking", "whole-journey", "plan-your-day"],
}

# All sizes below are in cqw (1% of the canvas width) so one sheet serves every
# canvas; per-platform blocks only adjust what the aspect ratio actually changes.
BASE_CSS = """
*, *::before, *::after { box-sizing: border-box; }

html, body {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden;
  background: #000;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}

.store-panel {
  container-type: size;
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: linear-gradient(170deg, var(--g1), var(--g2));
  isolation: isolate;
  font-family: "Arial Black", "Helvetica Neue", Arial, sans-serif;
}

.store-panel::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: -2;
  opacity: .28;
  background-image: radial-gradient(rgba(255, 255, 255, .25) 0.28cqw, transparent 0.28cqw);
  background-size: 1.7cqw 1.7cqw;
}

.store-panel::after {
  content: "";
  position: absolute;
  width: var(--ring-size, 41cqw);
  height: var(--ring-size, 41cqw);
  right: var(--ring-right, -23cqw);
  bottom: var(--ring-bottom, -22cqw);
  left: var(--ring-left, auto);
  top: var(--ring-top, auto);
  z-index: -1;
  border: 6.2cqw solid rgba(255, 255, 255, .13);
  border-radius: 50%;
}

.panel-index {
  position: absolute;
  top: 4.8cqw;
  left: 4.8cqw;
  z-index: 3;
  color: rgba(255, 255, 255, .76);
  font-family: ui-monospace, Menlo, Consolas, monospace;
  font-size: 3.1cqw;
  letter-spacing: .1em;
}

.sticker {
  position: absolute;
  top: 4.2cqw;
  right: 4.5cqw;
  z-index: 3;
  min-width: 11.9cqw;
  padding: 1.1cqw 2.3cqw;
  border: 0.57cqw solid rgba(255, 255, 255, .9);
  border-radius: 1.4cqw;
  background: var(--g2);
  color: #fff;
  font-size: 2.85cqw;
  font-weight: 900;
  letter-spacing: .06em;
  text-align: center;
  text-transform: uppercase;
  transform: rotate(var(--sticker-tilt, -4deg));
  box-shadow: 0 0.85cqw 2.3cqw rgba(0, 0, 0, .2);
}

.sticker.live {
  background: #ffca16;
  color: #332500;
}

.panel-copy {
  position: relative;
  z-index: 2;
  width: 100%;
  flex: 0 0 var(--copy-height, 40.6cqw);
  height: var(--copy-height, 40.6cqw);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  padding: 13.3cqw 4.8cqw 3.4cqw;
  color: #fff;
  font-weight: 900;
  text-align: center;
  text-shadow: 0 0.28cqw 0.85cqw rgba(0, 0, 0, .2);
}

.panel-copy strong {
  display: block;
  max-width: var(--copy-max, 61.5cqw);
  font-size: var(--headline, 6.5cqw);
  line-height: 1.04;
  text-transform: uppercase;
}

.panel-line { display: block; }
.panel-line + .panel-line { margin-top: 0.3cqw; }

.panel-accent {
  display: inline-block;
  color: var(--word-accent, #ffe15a);
  white-space: nowrap;
}

.panel-copy small {
  display: block;
  margin-top: 1.7cqw;
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", Arial, sans-serif;
  font-size: var(--subline, 3.1cqw);
  font-weight: 700;
  text-transform: none;
  opacity: .94;
}

.parking-lockup {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.4cqw;
}

.parking-badge {
  width: 9cqw;
  height: 9cqw;
  display: grid;
  place-items: center;
  border-radius: 1.4cqw;
  background: #fff;
  color: #009b77;
  font-size: 5.9cqw;
  font-weight: 900;
  line-height: 1;
  box-shadow: 0 1.1cqw 2.8cqw rgba(0, 0, 0, .2);
}

.parking-lockup strong { font-size: var(--parking-headline, 5.7cqw); }

.alert-panel .panel-copy { padding-bottom: 2cqw; }
.alert-panel .panel-copy strong {
  max-width: 64cqw;
  font-size: calc(var(--headline, 6.5cqw) * .87);
}

.device {
  position: relative;
  z-index: 2;
  height: var(--device-height, 76.8cqw);
  margin-top: 2.8cqw;
  padding: 1.4cqw;
  border-radius: 4.8cqw;
  background: #fff;
  box-shadow: 0 4cqw 8.5cqw rgba(0, 0, 0, .38);
  transform: rotate(var(--device-tilt, -2deg));
}

.device img {
  display: block;
  width: auto;
  height: 100%;
  border-radius: 3.4cqw;
}

.ghost {
  position: absolute;
  left: 1.4cqw;
  bottom: 5cqw;
  z-index: 1;
  color: rgba(255, 255, 255, .17);
  font-size: 3.7cqw;
  font-weight: 900;
  letter-spacing: .16em;
  text-transform: uppercase;
  writing-mode: vertical-rl;
  transform: rotate(180deg);
}
"""

# Aspect-driven overrides. Values stay in cqw so they track the canvas width.
PLATFORM_CSS = {
    "android-phone": """
.store-panel { --copy-height: 40.6cqw; --device-height: 100cqw; }
""",
    "android-tablet": """
.store-panel {
  --copy-height: 14cqw;
  --headline: 3.3cqw;
  --subline: 1.5cqw;
  --parking-headline: 3cqw;
  --copy-max: 56cqw;
}
.panel-copy { padding: 4.6cqw 3cqw 1cqw; }
.panel-index { top: 2.4cqw; left: 2.4cqw; font-size: 1.55cqw; }
.sticker {
  top: 2.1cqw; right: 2.3cqw; min-width: 6cqw;
  padding: .55cqw 1.15cqw; border-width: .3cqw; border-radius: .7cqw;
  font-size: 1.45cqw; box-shadow: 0 .45cqw 1.2cqw rgba(0, 0, 0, .2);
}
.parking-lockup { flex-direction: row; gap: 1.5cqw; }
.parking-badge { width: 4.6cqw; height: 4.6cqw; border-radius: .8cqw; font-size: 3cqw; }
.panel-copy small { margin-top: .9cqw; }
.store-panel::before { background-image: radial-gradient(rgba(255,255,255,.25) .14cqw, transparent .14cqw); background-size: .9cqw .9cqw; }
.store-panel::after { width: var(--ring-size, 21cqw); height: var(--ring-size, 21cqw); border-width: 3.2cqw; }
.device {
  width: 67cqw; height: auto; margin-top: 1.4cqw; padding: .6cqw;
  border-radius: 2cqw; box-shadow: 0 2cqw 4.4cqw rgba(0, 0, 0, .38);
}
.device img { width: 100%; height: auto; border-radius: 1.7cqw; }
.ghost { left: .7cqw; bottom: 2.6cqw; font-size: 1.9cqw; }
""",
    # iPhone copy is written as single strings with no manual line breaks, so the
    # headline needs the full canvas width to wrap in rather than the phone's
    # hand-broken 61.5cqw column.
    "iphone-6-5": """
.store-panel {
  --copy-height: 46cqw;
  --device-height: 150cqw;
  --headline: 6cqw;
  --copy-max: 84cqw;
  --parking-headline: 5.4cqw;
}
.panel-copy { padding: 15cqw 6cqw 3.4cqw; }
.panel-accent { white-space: normal; }
""",
    "ipad-13": """
.store-panel {
  --copy-height: 30cqw;
  --headline: 5.6cqw;
  --subline: 2.5cqw;
  --parking-headline: 5cqw;
  --copy-max: 68cqw;
}
.panel-copy { padding: 9.5cqw 4.5cqw 2cqw; }
.parking-lockup { flex-direction: row; gap: 2.4cqw; }
.parking-badge { width: 7.4cqw; height: 7.4cqw; font-size: 4.8cqw; }
.store-panel { --device-height: 82cqw; }
.device { border-radius: 3.6cqw; padding: 1.1cqw; }
.device img { border-radius: 2.6cqw; }
""",
}

PAGE = """<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<style>{css}</style></head>
<body>{panel}</body></html>
"""


def datauri(relpath: str) -> str:
    p = ROOT / relpath
    if not p.exists():
        sys.exit(f"missing source capture: {p}")
    return "data:image/png;base64," + base64.b64encode(p.read_bytes()).decode()


def extract(section_id: str, doc: str):
    """Pull the .store-panel articles out of one platform section."""
    sec = re.search(
        r'<section class="platform" id="%s">(.*?)</section>' % section_id, doc, re.S)
    if not sec:
        sys.exit(f"section not found: {section_id}")
    return re.findall(r'(<article class="store-panel.*?</article>)', sec.group(1), re.S)


def main():
    doc = SRC.read_text()
    if WORK.exists():
        shutil.rmtree(WORK)
    WORK.mkdir(parents=True)

    manifest = {}
    for name, cfg in PLATFORMS.items():
        w, h = cfg["css"]
        if w < MIN_WINDOW_WIDTH:
            sys.exit(f"{name}: css width {w} is below Chrome's {MIN_WINDOW_WIDTH}px "
                     "minimum; the render would be cropped")
        if (w * cfg["dsf"], h * cfg["dsf"]) != tuple(cfg["px"]):
            sys.exit(f"{name}: css {cfg['css']} x dsf {cfg['dsf']} does not equal "
                     f"the store spec {cfg['px']}")
        panels = extract(cfg["section"], doc)
        slugs = SLUGS[name]
        if len(panels) != len(slugs):
            sys.exit(f"{name}: {len(panels)} panels but {len(slugs)} slugs")

        dest = OUT / name
        dest.mkdir(parents=True, exist_ok=True)
        css = BASE_CSS + PLATFORM_CSS[name]
        entries = []

        for i, (panel, slug) in enumerate(zip(panels, slugs), start=1):
            # alternate the sticker/device tilt exactly as the listing page does
            tilt = "4deg" if i % 2 == 0 else "-4deg"
            dev = "2deg" if i % 2 == 0 else "-2deg"
            panel = panel.replace(
                'style="', f'style="--sticker-tilt:{tilt};--device-tilt:{dev};', 1)
            panel = re.sub(
                r'<a class="device-link"[^>]*>(.*?)</a>', r"\1", panel, flags=re.S)
            panel = re.sub(
                r'src="(screenshots/[^"]+)"',
                lambda m: 'src="' + datauri(m.group(1)) + '"', panel)

            page = WORK / f"{name}_{i:02d}.html"
            page.write_text(PAGE.format(css=css, panel=panel))

            out_png = dest / f"{i:02d}_{slug}.png"
            subprocess.run([
                CHROME, "--headless", "--disable-gpu", "--hide-scrollbars",
                "--no-sandbox", "--default-background-color=00000000",
                f"--force-device-scale-factor={cfg['dsf']}",
                f"--window-size={w},{h}",
                f"--screenshot={out_png}",
                page.as_uri(),
            ], check=True, capture_output=True)

            entries.append(out_png.name)
            print(f"  {out_png.relative_to(ROOT)}")

        manifest[name] = {"spec": f"{cfg['px'][0]}x{cfg['px'][1]}", "files": entries}
        print(f"{name}: {len(entries)} panels at {cfg['px'][0]}x{cfg['px'][1]}")

    (OUT / "index.json").write_text(json.dumps(manifest, indent=2) + "\n")


if __name__ == "__main__":
    main()
