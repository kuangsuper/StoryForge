import io
import math
from PIL import Image


def split_grid(image_bytes: bytes, rows: int, cols: int) -> list[bytes]:
    """将宫格图分割为单张图片"""
    img = Image.open(io.BytesIO(image_bytes))
    w, h = img.size
    cell_w = w // cols
    cell_h = h // rows
    results = []
    for r in range(rows):
        for c in range(cols):
            box = (c * cell_w, r * cell_h, (c + 1) * cell_w, (r + 1) * cell_h)
            cell = img.crop(box)
            buf = io.BytesIO()
            cell.save(buf, format="PNG")
            results.append(buf.getvalue())
    return results


def merge_images(image_list: list[bytes], cols: int = 2) -> bytes:
    """多图拼接为一张"""
    images = [Image.open(io.BytesIO(b)) for b in image_list]
    if not images:
        raise ValueError("No images to merge")
    w, h = images[0].size
    rows = math.ceil(len(images) / cols)
    canvas = Image.new("RGB", (w * cols, h * rows), (255, 255, 255))
    for i, img in enumerate(images):
        r, c = divmod(i, cols)
        canvas.paste(img.resize((w, h)), (c * w, r * h))
    buf = io.BytesIO()
    canvas.save(buf, format="PNG")
    return buf.getvalue()


def compress_image(image_bytes: bytes, max_size_kb: int = 3072) -> bytes:
    """压缩图片到指定大小"""
    img = Image.open(io.BytesIO(image_bytes))
    quality = 95
    while quality > 10:
        buf = io.BytesIO()
        img.save(buf, format="JPEG", quality=quality)
        if buf.tell() <= max_size_kb * 1024:
            return buf.getvalue()
        quality -= 5
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=10)
    return buf.getvalue()


def resize_image(image_bytes: bytes, width: int, height: int) -> bytes:
    """按指定尺寸缩放"""
    img = Image.open(io.BytesIO(image_bytes))
    img = img.resize((width, height), Image.LANCZOS)
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


def super_resolution(image_bytes: bytes, scale: int = 2) -> bytes:
    """超分辨率放大（使用 OpenCV LANCZOS4 高质量插值）"""
    import cv2
    import numpy as np

    nparr = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    if img is None:
        raise ValueError("Invalid image")

    h, w = img.shape[:2]
    new_w, new_h = w * scale, h * scale
    upscaled = cv2.resize(img, (new_w, new_h), interpolation=cv2.INTER_LANCZOS4)

    success, encoded = cv2.imencode(".png", upscaled)
    if not success:
        raise ValueError("Failed to encode image")
    return encoded.tobytes()
