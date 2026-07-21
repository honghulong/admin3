from pathlib import Path

from PIL import Image, ImageDraw


source_dir = Path("rendered-word")
pages = sorted(source_dir.glob("page-*.png"))
for start in range(0, len(pages), 4):
    batch = pages[start : start + 4]
    images = [Image.open(page).convert("RGB") for page in batch]
    width = max(image.width for image in images)
    height = max(image.height for image in images)
    canvas = Image.new("RGB", (width * 2, (height + 32) * 2), "white")
    draw = ImageDraw.Draw(canvas)
    for index, (page, image) in enumerate(zip(batch, images)):
        x = (index % 2) * width
        y = (index // 2) * (height + 32)
        canvas.paste(image, (x, y + 32))
        draw.text((x + 8, y + 8), page.stem, fill="black")
    canvas.save(source_dir / f"contact-{start // 4 + 1:02d}.png")
