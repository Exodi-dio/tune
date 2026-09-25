#!/usr/bin/env python3
"""Generate the Tune launcher icon set from brand/icon-source.png (GPLv3).

- Keys out the near-black backdrop to transparent (feathered).
- Legacy mipmap PNGs (transparent), adaptive foreground/background/monochrome,
  and the about-screen drawable.
- Rewrites mipmap-anydpi-v26 XMLs to reference the generated assets and sets
  values/ic_launcher_background.xml to the sampled brand red.
- Deletes stale legacy .webp icons and the unused green-grid background vector.
- Fails loudly on any bad assumption (corner transparency, opacity, sizes).
"""
import os
import sys

from PIL import Image, ImageFilter, ImageStat

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "brand", "icon-source.png")
RES = os.path.join(ROOT, "androidApp", "src", "main", "res")

DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
FG_SIZE = 432
ABOUT_SIZE = 512


def load_keyed():
    import numpy as np

    img = Image.open(SRC).convert("RGBA")
    a = np.array(img)
    lum = 0.2126 * a[..., 0] + 0.7152 * a[..., 1] + 0.0722 * a[..., 2]
    keyed = np.where(lum < 20, 0, 255).astype("uint8")
    feathered = Image.fromarray(keyed, "L").filter(ImageFilter.GaussianBlur(1.2))
    a[..., 3] = np.array(feathered)
    return Image.fromarray(a)


def brand_red(img):
    import numpy as np

    a = np.array(img)
    opaque = a[a[..., 3] > 128][..., :3]
    assert len(opaque) > 1000, "source has almost no opaque pixels"
    med = [int(np.median(opaque[..., i])) for i in range(3)]
    r, g, b = med
    assert r > 120 and r > g + 30 and r > b + 30, f"brand color not red-ish: {med}"
    return "#%02X%02X%02X" % (r, g, b)


def main():
    img = load_keyed()
    w, h = img.size
    corners = [img.getpixel((x, y))[3] for x, y in [(2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)]]
    assert all(c < 128 for c in corners), f"corners not transparent: {corners}"
    box = img.split()[3].getbbox()
    assert box, "source is fully transparent after key-out"
    content = img.crop(box)
    cx, cy = img.getpixel((w // 2, h // 2))[3], None
    assert img.getpixel((w // 2, h // 2))[3] > 200, "center pixel not opaque"

    red = brand_red(img)
    print(f"brand red: {red}, content box: {box}")

    # Legacy mipmap PNGs (transparent squircle).
    for density, size in DENSITIES.items():
        d = os.path.join(RES, f"mipmap-{density}")
        os.makedirs(d, exist_ok=True)
        icon = content.copy()
        icon.thumbnail((size, size), Image.Resampling.LANCZOS)
        canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        canvas.alpha_composite(icon, ((size - icon.width) // 2, (size - icon.height) // 2))
        canvas.save(os.path.join(d, "ic_launcher.png"))
        canvas.save(os.path.join(d, "ic_launcher_round.png"))
        for stale in ("ic_launcher.webp", "ic_launcher_round.webp", "ic_launcher_foreground.webp"):
            p = os.path.join(d, stale)
            if os.path.exists(p):
                os.remove(p)

    # Adaptive foreground (squircle art on transparent, xxxhdpi canvas).
    fg = content.copy()
    fg.thumbnail((400, 400), Image.Resampling.LANCZOS)
    fg_canvas = Image.new("RGBA", (FG_SIZE, FG_SIZE), (0, 0, 0, 0))
    fg_canvas.alpha_composite(fg, ((FG_SIZE - fg.width) // 2, (FG_SIZE - fg.height) // 2))
    xxx = os.path.join(RES, "mipmap-xxxhdpi")
    fg_canvas.save(os.path.join(xxx, "ic_launcher_foreground.png"))

    # Monochrome: white silhouette from alpha.
    alpha = fg_canvas.split()[3]
    mono = Image.new("RGBA", (FG_SIZE, FG_SIZE), (0, 0, 0, 0))
    white = Image.new("RGBA", (FG_SIZE, FG_SIZE), (255, 255, 255, 255))
    mono.paste(white, (0, 0), alpha)
    mono.save(os.path.join(xxx, "ic_launcher_monochrome.png"))
    # drawable-nodpi mirrors keep R.drawable references working
    # (notification small icon, legacy layouts).
    nodpi_dir = os.path.join(RES, "drawable-nodpi")
    os.makedirs(nodpi_dir, exist_ok=True)
    mono.save(os.path.join(nodpi_dir, "ic_launcher_monochrome.png"))
    fg_canvas.save(os.path.join(nodpi_dir, "ic_launcher_foreground.png"))
    Image.new("RGBA", (FG_SIZE, FG_SIZE), red).save(os.path.join(nodpi_dir, "ic_launcher_background.png"))

    # Background color value.
    with open(os.path.join(RES, "values", "ic_launcher_background.xml"), "w") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">%s</color>\n</resources>' % red)

    # Remove unused green-grid background vector + old vector foreground.
    for stale in ("drawable/ic_launcher_background.xml", "drawable/ic_launcher_foreground.xml",
                  "drawable/ic_launcher_monochrome.xml"):
        p = os.path.join(RES, stale)
        if os.path.exists(p):
            os.remove(p)

    # Point adaptive-icon XMLs at generated assets.
    adaptive = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
    <monochrome android:drawable="@mipmap/ic_launcher_monochrome"/>
</adaptive-icon>"""
    for name in ("mipmap-anydpi-v26/ic_launcher.xml", "mipmap-anydpi-v26/ic_launcher_round.xml"):
        with open(os.path.join(RES, name), "w") as f:
            f.write(adaptive)

    # About-screen drawable (new canonical name + legacy filename until Phase 6).
    about = content.copy()
    about.thumbnail((ABOUT_SIZE, ABOUT_SIZE), Image.Resampling.LANCZOS)
    about_canvas = Image.new("RGBA", (ABOUT_SIZE, ABOUT_SIZE), (0, 0, 0, 0))
    about_canvas.alpha_composite(about, ((ABOUT_SIZE - about.width) // 2, (ABOUT_SIZE - about.height) // 2))
    nodpi = os.path.join(RES, "drawable-nodpi")
    about_canvas.save(os.path.join(nodpi, "tune_about_app_icon.png"))
    about_canvas.save(os.path.join(nodpi, "airmedy_about_app_icon.png"))

    # Verify every expected output.
    expected = []
    for density, size in DENSITIES.items():
        expected += [(f"mipmap-{density}/ic_launcher.png", size), (f"mipmap-{density}/ic_launcher_round.png", size)]
    expected += [("mipmap-xxxhdpi/ic_launcher_foreground.png", FG_SIZE),
                 ("mipmap-xxxhdpi/ic_launcher_monochrome.png", FG_SIZE),
                 ("drawable-nodpi/tune_about_app_icon.png", ABOUT_SIZE),
                 ("drawable-nodpi/ic_launcher_monochrome.png", FG_SIZE),
                 ("drawable-nodpi/ic_launcher_foreground.png", FG_SIZE),
                 ("drawable-nodpi/ic_launcher_background.png", FG_SIZE)]
    for rel, size in expected:
        p = os.path.join(RES, rel)
        got = Image.open(p).size
        assert got == (size, size), f"{rel}: got {got}, want {(size, size)}"
    print(f"OK: {len(expected)} icon assets written, brand {red}")


if __name__ == "__main__":
    sys.exit(main())