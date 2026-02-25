from fastapi import APIRouter, UploadFile, File, Form
from fastapi.responses import Response
import json
from services.image_service import split_grid, merge_images, compress_image, resize_image, super_resolution

router = APIRouter()


@router.post("/split-grid")
async def api_split_grid(
    file: UploadFile = File(...),
    rows: int = Form(2),
    cols: int = Form(2),
):
    data = await file.read()
    results = split_grid(data, rows, cols)
    # 返回 base64 编码的图片列表
    import base64
    images = [base64.b64encode(r).decode() for r in results]
    return {"count": len(images), "images": images}


@router.post("/merge")
async def api_merge(
    files: list[UploadFile] = File(...),
    cols: int = Form(2),
):
    image_list = [await f.read() for f in files]
    result = merge_images(image_list, cols)
    return Response(content=result, media_type="image/png")


@router.post("/compress")
async def api_compress(
    file: UploadFile = File(...),
    max_size_kb: int = Form(3072),
):
    data = await file.read()
    result = compress_image(data, max_size_kb)
    return Response(content=result, media_type="image/jpeg")


@router.post("/resize")
async def api_resize(
    file: UploadFile = File(...),
    width: int = Form(...),
    height: int = Form(...),
):
    data = await file.read()
    result = resize_image(data, width, height)
    return Response(content=result, media_type="image/png")


@router.post("/super-resolution")
async def api_super_resolution(
    file: UploadFile = File(...),
    scale: int = Form(2),
):
    data = await file.read()
    result = super_resolution(data, scale)
    return Response(content=result, media_type="image/png")
