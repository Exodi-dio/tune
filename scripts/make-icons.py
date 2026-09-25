#!/usr/bin/env python3
"""Generate the Tune launcher icon set from brand/icon-source.png (GPLv3).

Adaptive backdrop handling (the source may be transparent, dark-backed,
light-backed, or full-bleed opaque):
- rim alpha > 30% transparent  -> keep-alpha (autocrop to content)
- corners dark (lum < 60)      -> key out dark (lum < 24, feathered)
- corners light (lum > 195)    -> key out light (lum > 232, feathered)
- otherwise                    -> opaque full-bleed (centered at 92% on transparent)

Outputs: legacy mipmap PNGs (transparent), adaptive foreground/background/
monochrome, drawable-nodpi mirrors, about drawable, Play-Store listing icon.
Rewrites mipmap-anydpi-v26 XMLs and the background color value (sampled
brand color). Deletes stale legacy .webp icons. Fails loudly on bad input.
"""
import os
import sys

from PIL import Image, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "brand", "icon-source.png")
RES = os.path.join(ROOT, "androidApp", "src", "main", "res")
PLAYSTORE = os.path.join(ROOT, "androidApp", "src", "main", "ic_launcher-playstore.png")

DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
FG_SIZE = 432
ABOUT_SIZE = 512
STORE_SIZE = 512


def luminance(px):
    return 0.2126 * px[0] + 0.7152 * px[1] + 0.0722 * px[2]


def load_adaptive():
    import numpy as np

    img = Image.open(SRC).convert("RGBA")
    w, h = img.size
    print(f"source: {w}x{h} mode=RGBA")
    a = np.array(img)
    alpha = a[..., 3]
    rim = np.concatenate([alpha[0:8, :].ravel(), alpha[-8:, :].ravel(),
                          alpha[:, 0:8].ravel(), alpha[:, -8:].ravel()])
    transparent_frac = float((rim < 128).mean())
    corners = [img.getpixel((x, y)) for x, y in
               [(4, 4), (w - 5, 4), (4, h - 5), (w - 5, h - 5)]]
    corner_lum = [luminance(c) for c in corners]
    print(f"rim transparent frac={transparent_frac:.2f} corner lum={[round(v) for v in corner_lum]}")

    if transparent_frac > 0.30:
        mode = "keep-alpha"
        box = img.split()[3].getbbox()
        assert box, "source is fully transparent"
        content = img.crop(box)
        full_bleed = False
    elif all(v < 60 for v in corner_lum):
        mode = "key-dark"
        lum = 0.2126 * a[..., 0] + 0.7152 * a[..., 1] + 0.0722 * a[..., 2]
        keyed = np.where(lum < 24, 0, 255).astype("uint8")
        a[..., 3] = np.array(Image.fromarray(keyed, "L").filter(ImageFilter.GaussianBlur(1.2)))
        img = Image.fromarray(a)
        box = img.split()[3].getbbox()
        assert box, "key-out removed everything"
        content = img.crop(box)
        full_bleed = False
    elif all(v > 195 for v in corner_lum):
        mode = "key-light"
        lum = 0.2126 * a[..., 0] + 0.7152 * a[..., 1] + 0.0722 * a[..., 2]
        keyed = np.where(lum > 232, 0, 255).astype("uint8")
        a[..., 3] = np.array(Image.fromarray(keyed, "L").filter(ImageFilter.GaussianBlur(1.2)))
        img = Image.fromarray(a)
        box = img.split()[3].getbbox()
        assert box, "key-out removed everything"
        content = img.crop(box)
        full_bleed = False
    else:
        mode = "opaque"
        box = (0, 0, w, h)
        content = img
        full_bleed = True
    print(f"mode={mode} content box={box}")
    return content, full_bleed


def brand_color(content):
    import numpy as np

    a = np.array(content.convert("RGBA"))
    opaque = a[a[..., 3] > 128][..., :3]
    assert len(opaque) > 1000, "almost no opaque pixels"
    med = [int(np.median(opaque[..., i])) for i in range(3)]
    assert not (max(med) < 24 or min(med) > 232), f"brand color degenerate: {med}"
    return "#%02X%02X%02X" % (med[0], med[1], med[2])


def fit(content, size, fill=1.0):
    art = content.copy()
    art.thumbnail((int(size * fill), int(size * fill)), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.alpha_composite(art, ((size - art.width) // 2, (size - art.height) // 2))
    return canvas


def main():
    content, full_bleed = load_adaptive()
    red = brand_color(content)
    print(f"brand: {red} full_bleed={full_bleed}")
    if full_bleed:
        art = content.copy()
    else:
        art = content

    expected = []
    for density, size in DENSITIES.items():
        d = os.path.join(RES, f"mipmap-{density}")
        os.makedirs(d, exist_ok=True)
        icon = fit(art, size, 1.0 if full_bleed else 0.96)
        icon.save(os.path.join(d, "ic_launcher.png"))
        icon.save(os.path.join(d, "ic_launcher_round.png"))
        expected += [(f"mipmap-{density}/ic_launcher.png", size), (f"mipmap-{density}/ic_launcher_round.png", size)]
        for stale in ("ic_launcher.webp", "ic_launcher_round.webp", "ic_launcher_foreground.webp"):
            p = os.path.join(d, stale)
            if os.path.exists(p):
                os.remove(p)

    xxx = os.path.join(RES, "mipmap-xxxhdpi")
    fg = fit(art, FG_SIZE, 1.0 if full_bleed else 0.93)
    fg.save(os.path.join(xxx, "ic_launcher_foreground.png"))
    expected.append(("mipmap-xxxhdpi/ic_launcher_foreground.png", FG_SIZE))

    alpha = fg.split()[3]
    mono = Image.new("RGBA", (FG_SIZE, FG_SIZE), (0, 0, 0, 0))
    mono.paste(Image.new("RGBA", (FG_SIZE, FG_SIZE), (255, 255, 255, 255)), (0, 0), alpha)
    mono.save(os.path.join(xxx, "ic_launcher_monochrome.png"))
    expected.append(("mipmap-xxxhdpi/ic_launcher_monochrome.png", FG_SIZE))

    with open(os.path.join(RES, "values", "ic_launcher_background.xml"), "w") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">%s</color>\n</resources>' % red)

    for stale in ("drawable/ic_launcher_background.xml", "drawable/ic_launcher_foreground.xml",
                  "drawable/ic_launcher_monochrome.xml"):
        p = os.path.join(RES, stale)
        if os.path.exists(p):
            os.remove(p)

    adaptive = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
    <monochrome android:drawable="@mipmap/ic_launcher_monochrome"/>
</adaptive-icon>"""
    for name in ("mipmap-anydpi-v26/ic_launcher.xml", "mipmap-anydpi-v26/ic_launcher_round.xml"):
        with open(os.path.join(RES, name), "w") as f:
            f.write(adaptive)

    nodpi = os.path.join(RES, "drawable-nodpi")
    os.makedirs(nodpi, exist_ok=True)
    nodpi_mono = fit(art, FG_SIZE, 0.93)
    white = Image.new("RGBA", (FG_SIZE, FG_SIZE), (255, 255, 255, 255))
    nodpi_mono.paste(white, (0, 0), nodpi_mono.split()[3])
    nodpi_mono.save(os.path.join(nodpi, "ic_launcher_monochrome.png"))
    fg.save(os.path.join(nodpi, "ic_launcher_foreground.png"))
    Image.new("RGBA", (FG_SIZE, FG_SIZE), red).save(os.path.join(nodpi, "ic_launcher_background.png"))
    expected += [("drawable-nodpi/ic_launcher_monochrome.png", FG_SIZE),
                 ("drawable-nodpi/ic_launcher_foreground.png", FG_SIZE),
                 ("drawable-nodpi/ic_launcher_background.png", FG_SIZE)]

    about = fit(art, ABOUT_SIZE, 1.0 if full_bleed else 0.96)
    about.save(os.path.join(nodpi, "tune_about_app_icon.png"))
    expected.append(("drawable-nodpi/tune_about_app_icon.png", ABOUT_SIZE))

    store_bg = Image.new("RGBA", (STORE_SIZE, STORE_SIZE), red)
    if full_bleed:
        store = art.copy()
        store.thumbnail((STORE_SIZE, STORE_SIZE), Image.Resampling.LANCZOS)
    else:
        store = fit(art, STORE_SIZE, 0.72)
        canvas = store_bg.copy()
        canvas.alpha_composite(store, (0, 0))
        store = canvas.convert("RGB")
    store.save(PLAYSTORE)

    for rel, size in expected:
        p = os.path.join(RES, rel)
        got = Image.open(p).size
        assert got == (size, size), f"{rel}: got {got}, want {(size, size)}"
    w, h = Image.open(PLAYSTORE).size
    assert (w, h) == (STORE_SIZE, STORE_SIZE), f"playstore icon size {(w, h)}"
    print(f"OK: {len(expected)} icon assets + playstore, brand {red}")


if __name__ == "__main__":
    sys.exit(main())
