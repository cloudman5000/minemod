from pathlib import Path
from PIL import Image, ImageDraw
import random

SIZE = 16
OUT_DIR = Path("src/main/resources/assets/examplemod/textures/block")


def _noise(img: Image.Image, amount: int, low: int = -10, high: int = 10, seed: int = 1337) -> None:
    rng = random.Random(seed)
    px = img.load()
    for _ in range(amount):
        x = rng.randrange(SIZE)
        y = rng.randrange(SIZE)
        r, g, b, a = px[x, y]
        d = rng.randint(low, high)
        px[x, y] = (
            max(0, min(255, r + d)),
            max(0, min(255, g + d)),
            max(0, min(255, b + d)),
            a,
        )


def _frame(img: Image.Image, hi=(120, 126, 138, 255), lo=(32, 36, 44, 255)) -> None:
    px = img.load()
    for x in range(SIZE):
        px[x, 0] = hi
        px[x, SIZE - 1] = lo
    for y in range(SIZE):
        px[0, y] = hi
        px[SIZE - 1, y] = lo


def make_front() -> Image.Image:
    img = Image.new("RGBA", (SIZE, SIZE), (58, 64, 74, 255))
    d = ImageDraw.Draw(img)

    _frame(img)

    # Upper bezel + screen
    d.rectangle((2, 1, 13, 9), fill=(39, 44, 52, 255), outline=(108, 114, 125, 255))
    d.rectangle((3, 2, 12, 8), fill=(12, 50, 22, 255))
    d.rectangle((4, 3, 11, 7), fill=(14, 74, 30, 255))

    # Scanlines and phosphor glow
    for y in range(3, 8):
        if y % 2 == 0:
            d.line((4, y, 11, y), fill=(10, 44, 20, 255))
    d.rectangle((5, 3, 6, 4), fill=(130, 255, 124, 255))
    d.rectangle((8, 3, 9, 4), fill=(110, 240, 115, 255))
    d.rectangle((10, 4, 10, 4), fill=(140, 255, 130, 255))

    # Lower vent panel
    d.rectangle((2, 10, 13, 14), fill=(68, 74, 85, 255), outline=(107, 112, 121, 255))
    for x in range(3, 13):
        if x % 2 == 1:
            d.line((x, 11, x, 13), fill=(53, 57, 65, 255))

    # Corner bolts
    bolt = (138, 142, 150, 255)
    for pos in ((1, 1), (14, 1), (1, 14), (14, 14)):
        img.putpixel(pos, bolt)

    _noise(img, amount=24, seed=7)
    return img


def make_side() -> Image.Image:
    img = Image.new("RGBA", (SIZE, SIZE), (56, 62, 72, 255))
    d = ImageDraw.Draw(img)
    _frame(img)

    # Recessed side panel
    d.rectangle((2, 2, 13, 13), fill=(47, 53, 62, 255), outline=(100, 106, 116, 255))
    for y in range(4, 12):
        if y % 2 == 0:
            d.line((4, y, 11, y), fill=(70, 76, 86, 255))

    # Vertical seam
    d.line((8, 3, 8, 12), fill=(36, 40, 47, 255))

    for pos in ((2, 2), (13, 2), (2, 13), (13, 13)):
        img.putpixel(pos, (132, 137, 145, 255))

    _noise(img, amount=20, seed=19)
    return img


def make_top() -> Image.Image:
    img = Image.new("RGBA", (SIZE, SIZE), (62, 67, 76, 255))
    d = ImageDraw.Draw(img)
    _frame(img, hi=(128, 134, 144, 255), lo=(40, 45, 53, 255))

    # Main top panel
    d.rectangle((2, 2, 13, 13), fill=(51, 56, 64, 255), outline=(103, 109, 119, 255))

    # Vent grill
    d.rectangle((4, 4, 11, 11), fill=(44, 49, 57, 255))
    for x in range(4, 12):
        if x % 2 == 0:
            d.line((x, 4, x, 11), fill=(84, 91, 100, 255))

    for pos in ((1, 1), (14, 1), (1, 14), (14, 14)):
        img.putpixel(pos, (138, 143, 151, 255))

    _noise(img, amount=22, seed=31)
    return img


def make_bottom() -> Image.Image:
    img = Image.new("RGBA", (SIZE, SIZE), (71, 76, 84, 255))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 15, 15), outline=(101, 106, 114, 255))
    d.rectangle((4, 4, 11, 11), fill=(58, 63, 71, 255), outline=(92, 97, 105, 255))
    _noise(img, amount=12, seed=43)
    return img


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    make_front().save(OUT_DIR / "hacking_terminal_front.png")
    make_side().save(OUT_DIR / "hacking_terminal_side.png")
    make_top().save(OUT_DIR / "hacking_terminal_top.png")
    make_bottom().save(OUT_DIR / "hacking_terminal_bottom.png")
    print("Generated terminal textures in", OUT_DIR)


if __name__ == "__main__":
    main()
