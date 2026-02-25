import os
import subprocess
import tempfile
from typing import Optional


def compose_video(
    video_paths: list[str],
    output_path: str,
    transition: str = "fadeInOut",
    transition_duration: int = 500,
    bgm_path: str = "",
    bgm_volume: int = 30,
    tts_tracks: list[dict] = None,
    tts_volume: int = 80,
    subtitle_srt: str = "",
    subtitle_style: dict = None,
    watermark_text: str = "",
    watermark_image: str = "",
    watermark_position: str = "bottomRight",
    watermark_opacity: int = 30,
    intro_text: str = "",
    intro_duration: int = 3,
    outro_text: str = "",
    outro_duration: int = 3,
    resolution: str = "1080p",
    fps: int = 30,
) -> str:
    """使用 FFmpeg 合成视频"""
    if tts_tracks is None:
        tts_tracks = []
    if subtitle_style is None:
        subtitle_style = {}

    res_map = {"720p": "1280:720", "1080p": "1920:1080", "4k": "3840:2160"}
    scale = res_map.get(resolution, "1920:1080")

    # 构建 FFmpeg 滤镜链
    filter_parts = []
    inputs = []
    input_idx = 0

    # 片头
    intro_file = None
    if intro_text:
        intro_file = _create_text_video(intro_text, intro_duration, scale, fps)
        inputs.extend(["-i", intro_file])
        input_idx += 1

    # 视频片段
    video_start_idx = input_idx
    for vp in video_paths:
        inputs.extend(["-i", vp])
        input_idx += 1

    # 片尾
    outro_file = None
    if outro_text:
        outro_file = _create_text_video(outro_text, outro_duration, scale, fps)
        inputs.extend(["-i", outro_file])
        input_idx += 1

    # BGM
    bgm_input_idx = None
    if bgm_path and os.path.exists(bgm_path):
        inputs.extend(["-i", bgm_path])
        bgm_input_idx = input_idx
        input_idx += 1

    # TTS 音轨
    tts_input_indices = []
    for track in tts_tracks:
        if os.path.exists(track.get("audio_path", "")):
            inputs.extend(["-i", track["audio_path"]])
            tts_input_indices.append(input_idx)
            input_idx += 1

    # 构建 concat 列表
    total_streams = 0
    concat_inputs = []

    if intro_file:
        concat_inputs.append(f"[0:v]scale={scale},setsar=1[intro_v];[0:a]anull[intro_a];")
        total_streams += 1

    for i, _ in enumerate(video_paths):
        idx = video_start_idx + i
        concat_inputs.append(f"[{idx}:v]scale={scale},setsar=1[v{i}];")
        total_streams += 1

    if outro_file:
        outro_idx = video_start_idx + len(video_paths)
        concat_inputs.append(f"[{outro_idx}:v]scale={scale},setsar=1[outro_v];[{outro_idx}:a]anull[outro_a];")
        total_streams += 1

    # 简单拼接（无转场或 fadeInOut）
    concat_labels = []
    if intro_file:
        concat_labels.append("[intro_v][intro_a]")
    for i in range(len(video_paths)):
        concat_labels.append(f"[v{i}][{video_start_idx + i}:a]")
    if outro_file:
        concat_labels.append("[outro_v][outro_a]")

    filter_complex = "".join(concat_inputs)
    filter_complex += "".join(concat_labels) + f"concat=n={total_streams}:v=1:a=1[outv][outa]"

    # 水印
    if watermark_text:
        pos = _watermark_position(watermark_position)
        filter_complex += f";[outv]drawtext=text='{watermark_text}':fontsize=24:fontcolor=white@0.{watermark_opacity}:{pos}[outv]"

    # 字幕
    srt_file = None
    if subtitle_srt:
        srt_file = tempfile.NamedTemporaryFile(suffix=".srt", delete=False, mode="w", encoding="utf-8")
        srt_file.write(subtitle_srt)
        srt_file.close()
        font_size = subtitle_style.get("fontSize", 20)
        filter_complex += f";[outv]subtitles={srt_file.name}:force_style='FontSize={font_size}'[outv]"

    # TTS 音轨混入
    audio_label = "[outa]"
    if tts_input_indices:
        tts_labels = []
        for i, idx in enumerate(tts_input_indices):
            vol = tts_volume / 100
            label = f"tts{i}"
            filter_complex += f";[{idx}:a]volume={vol}[{label}]"
            tts_labels.append(f"[{label}]")
        if len(tts_labels) == 1:
            filter_complex += f";{audio_label}{tts_labels[0]}amix=inputs=2:duration=first[outa_tts]"
        else:
            all_tts = "".join(tts_labels)
            filter_complex += f";{all_tts}amix=inputs={len(tts_labels)}:duration=longest[tts_mixed]"
            filter_complex += f";{audio_label}[tts_mixed]amix=inputs=2:duration=first[outa_tts]"
        audio_label = "[outa_tts]"

    # BGM 混音
    if bgm_input_idx is not None:
        vol = bgm_volume / 100
        filter_complex += f";[{bgm_input_idx}:a]volume={vol}[bgm];{audio_label}[bgm]amix=inputs=2:duration=first[outa_mix]"
        audio_label = "[outa_mix]"

    # 构建命令
    cmd = ["ffmpeg", "-y"] + inputs + [
        "-filter_complex", filter_complex,
        "-map", "[outv]",
        "-map", audio_label,
        "-c:v", "libx264",
        "-preset", "medium",
        "-crf", "23",
        "-c:a", "aac",
        "-b:a", "192k",
        "-r", str(fps),
        output_path,
    ]

    subprocess.run(cmd, check=True, capture_output=True)

    # 清理临时文件
    temp_files = [intro_file, outro_file]
    if srt_file:
        temp_files.append(srt_file.name if hasattr(srt_file, "name") else srt_file)
    for f in temp_files:
        if f and isinstance(f, str) and os.path.exists(f):
            os.unlink(f)

    return output_path


def extract_frame(video_path: str, time_sec: float = 0) -> bytes:
    """提取视频指定时间的帧"""
    cmd = [
        "ffmpeg", "-y",
        "-ss", str(time_sec),
        "-i", video_path,
        "-vframes", "1",
        "-f", "image2pipe",
        "-vcodec", "png",
        "pipe:1",
    ]
    result = subprocess.run(cmd, capture_output=True, check=True)
    return result.stdout


def _create_text_video(text: str, duration: int, scale: str, fps: int) -> str:
    """创建纯文字视频片段"""
    w, h = scale.split(":")
    tmp = tempfile.NamedTemporaryFile(suffix=".mp4", delete=False)
    tmp.close()
    cmd = [
        "ffmpeg", "-y",
        "-f", "lavfi", "-i", f"color=c=black:s={w}x{h}:d={duration}:r={fps}",
        "-f", "lavfi", "-i", f"anullsrc=r=44100:cl=stereo",
        "-t", str(duration),
        "-vf", f"drawtext=text='{text}':fontsize=48:fontcolor=white:x=(w-text_w)/2:y=(h-text_h)/2",
        "-c:v", "libx264", "-c:a", "aac",
        tmp.name,
    ]
    subprocess.run(cmd, check=True, capture_output=True)
    return tmp.name


def _watermark_position(position: str) -> str:
    pos_map = {
        "topLeft": "x=10:y=10",
        "topRight": "x=w-tw-10:y=10",
        "bottomLeft": "x=10:y=h-th-10",
        "bottomRight": "x=w-tw-10:y=h-th-10",
        "center": "x=(w-tw)/2:y=(h-th)/2",
    }
    return pos_map.get(position, pos_map["bottomRight"])
