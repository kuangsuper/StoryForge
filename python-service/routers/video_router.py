from fastapi import APIRouter, UploadFile, File, Form
from fastapi.responses import Response
from pydantic import BaseModel
from services.video_service import compose_video, extract_frame

router = APIRouter()


class TtsTrack(BaseModel):
    audio_path: str
    start_time: float
    duration: float


class ComposeRequest(BaseModel):
    video_paths: list[str]
    output_path: str
    transition: str = "fadeInOut"
    transition_duration: int = 500
    bgm_path: str = ""
    bgm_volume: int = 30
    tts_tracks: list[TtsTrack] = []
    tts_volume: int = 80
    subtitle_srt: str = ""
    subtitle_style: dict = {}
    watermark_text: str = ""
    watermark_image: str = ""
    watermark_position: str = "bottomRight"
    watermark_opacity: int = 30
    intro_text: str = ""
    intro_duration: int = 3
    outro_text: str = ""
    outro_duration: int = 3
    resolution: str = "1080p"
    fps: int = 30


@router.post("/compose")
async def api_compose(req: ComposeRequest):
    output = compose_video(
        video_paths=req.video_paths,
        output_path=req.output_path,
        transition=req.transition,
        transition_duration=req.transition_duration,
        bgm_path=req.bgm_path,
        bgm_volume=req.bgm_volume,
        tts_tracks=[t.model_dump() for t in req.tts_tracks],
        tts_volume=req.tts_volume,
        subtitle_srt=req.subtitle_srt,
        subtitle_style=req.subtitle_style,
        watermark_text=req.watermark_text,
        watermark_image=req.watermark_image,
        watermark_position=req.watermark_position,
        watermark_opacity=req.watermark_opacity,
        intro_text=req.intro_text,
        intro_duration=req.intro_duration,
        outro_text=req.outro_text,
        outro_duration=req.outro_duration,
        resolution=req.resolution,
        fps=req.fps,
    )
    return {"output_path": output}


class ExtractFrameRequest(BaseModel):
    video_path: str
    time_sec: float = 0


@router.post("/extract-frame")
async def api_extract_frame(req: ExtractFrameRequest):
    frame = extract_frame(req.video_path, req.time_sec)
    return Response(content=frame, media_type="image/png")


class GetDurationRequest(BaseModel):
    video_path: str


@router.post("/get-duration")
async def api_get_duration(req: GetDurationRequest):
    from utils.ffmpeg_utils import get_duration
    duration = get_duration(req.video_path)
    return {"duration": duration}


class AddAudioRequest(BaseModel):
    video_path: str
    audio_path: str
    output_path: str
    volume: float = 1.0


@router.post("/add-audio")
async def api_add_audio(req: AddAudioRequest):
    from utils.ffmpeg_utils import add_audio_to_video
    output = add_audio_to_video(req.video_path, req.audio_path, req.output_path, req.volume)
    return {"output_path": output}
