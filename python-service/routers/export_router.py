from fastapi import APIRouter
from fastapi.responses import Response
from pydantic import BaseModel
from services.export_service import export_docx, export_pdf, export_epub

router = APIRouter()


class Chapter(BaseModel):
    chapterIndex: int
    chapter: str
    chapterData: str


class ExportRequest(BaseModel):
    chapters: list[Chapter]
    title: str = "小说导出"
    author: str = "Toonflow"


@router.post("/docx")
async def api_export_docx(req: ExportRequest):
    data = export_docx([c.model_dump() for c in req.chapters])
    return Response(
        content=data,
        media_type="application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        headers={"Content-Disposition": "attachment; filename=novel.docx"},
    )


@router.post("/pdf")
async def api_export_pdf(req: ExportRequest):
    data = export_pdf([c.model_dump() for c in req.chapters])
    return Response(
        content=data,
        media_type="application/pdf",
        headers={"Content-Disposition": "attachment; filename=novel.pdf"},
    )


@router.post("/epub")
async def api_export_epub(req: ExportRequest):
    data = export_epub([c.model_dump() for c in req.chapters], req.title, req.author)
    return Response(
        content=data,
        media_type="application/epub+zip",
        headers={"Content-Disposition": "attachment; filename=novel.epub"},
    )
