# Toonflow 全自动流水线与 Python 服务设计

## 一、全自动流水线（M20）

### 1.1 状态机

```
                    ┌─────────────────────────────────────────────────────┐
                    │                                                     │
pending ──> novel_generating ──> novel_complete                          │
                                    │                                    │
                              storyline_generating ──> storyline_complete │
                                                          │              │
                                                outline_generating       │
                                                    │                    │
                                              outline_reviewing          │
                                                    │                    │
                                              outline_complete           │
                                                    │                    │
                                            assets_extracting            │
                                                    │                    │
                                        assets_image_generating          │
                                                    │                    │
                                            assets_complete              │
                                                    │                    │
                                          script_generating              │
                                                    │                    │
                                          script_complete                │
                                                    │                    │
                                      storyboard_generating              │
                                                    │                    │
                                      storyboard_complete                │
                                                    │                    │
                                  storyboard_image_generating            │
                                                    │                    │
                                    image_quality_checking               │
                                                    │                    │
                                  storyboard_image_complete              │
                                                    │                    │
                                      tts_generating (可选)              │
                                                    │                    │
                                        tts_complete                     │
                                                    │                    │
                                      video_generating                   │
                                                    │                    │
                                      video_complete                     │
                                                    │                    │
                                        composing                        │
                                                    │                    │
                                    compose_complete                     │
                                                    │                    │
                                    all_complete ◄───┘                   │
                                                                         │
                    任意步骤 ──> step_failed ──> 重试/跳过/终止 ─────────┘
```

### 1.2 PipelineExecutor 核心逻辑

```java
@Service
public class PipelineExecutor {

    private final Map<String, PipelineStep> steps = new LinkedHashMap<>();

    @PostConstruct
    void init() {
        steps.put("novel_generating",           new NovelGenerationStep());
        steps.put("storyline_generating",       new StorylineGenerationStep());
        steps.put("outline_generating",         new OutlineGenerationStep());
        steps.put("assets_extracting",          new AssetExtractionStep());
        steps.put("assets_image_generating",    new AssetImageStep());
        steps.put("script_generating",          new ScriptGenerationStep());
        steps.put("storyboard_generating",      new StoryboardGenerationStep());
        steps.put("storyboard_image_generating",new StoryboardImageStep());
        steps.put("tts_generating",             new TtsGenerationStep());      // 可选
        steps.put("video_generating",           new VideoGenerationStep());
        steps.put("composing",                  new VideoComposeStep());
    }

    /**
     * 启动全自动流水线（Virtual Thread 中执行）
     */
    public void start(Long projectId, PipelineRequest request) {
        Thread.startVirtualThread(() -> {
            emitter.pipelineStart(steps.size());

            for (var entry : steps.entrySet()) {
                String stepName = entry.getKey();
                PipelineStep step = entry.getValue();

                // 跳过可选步骤
                if (stepName.equals("tts_generating") && !request.isTtsEnabled()) continue;

                // 检查审核节点配置
                ReviewMode reviewMode = request.getReviewConfig().getOrDefault(stepName, ReviewMode.SKIP);

                emitter.stepStart(stepName, step.displayName(), stepIndex);
                try {
                    step.execute(projectId, request, emitter);
                    emitter.stepComplete(stepName, step.displayName(), elapsed);

                    // 审核节点
                    if (reviewMode == ReviewMode.HUMAN_REQUIRED) {
                        emitter.reviewRequired(stepName, step.getOutput());
                        waitForHumanReview(projectId, stepName); // 阻塞等待
                    } else if (reviewMode == ReviewMode.AI_AUTO) {
                        step.aiReview(projectId, emitter);
                    }

                } catch (Exception e) {
                    emitter.stepFailed(stepName, e.getMessage(), step.canRetry());
                    handleStepFailure(projectId, stepName, step, e);
                    return; // 终止或等待用户决定
                }

                // 保存检查点
                checkpoint.save(projectId, stepName);
            }

            emitter.pipelineComplete(totalDuration, outputs);
        });
    }
}
```

### 1.3 审核节点配置

```java
public record PipelineRequest(
    Long projectId,
    String genre,
    String prompt,
    int episodeCount,           // 默认 5
    int chaptersPerEpisode,     // 默认 2
    String videoMode,           // 默认 singleImage
    boolean autoCompose,        // 默认 true
    boolean ttsEnabled,         // 默认 false

    // 审核配置: stepName → ReviewMode
    Map<String, ReviewMode> reviewConfig
    // 默认:
    //   novel_generating → AI_AUTO (全书质检, <70分自动修复)
    //   outline_generating → AI_AUTO (导演审核)
    //   storyboard_image_generating → AI_AUTO (图片质检, <60分重新生成)
    //   其余 → SKIP
) {}

public enum ReviewMode {
    SKIP,           // 跳过
    AI_AUTO,        // AI自动审核
    HUMAN_REQUIRED  // 暂停等待人工
}
```

### 1.4 批量生产

```java
// POST /api/batch/full-pipeline
// 入参: PipelineRequest[]
// 每个任务创建独立项目，入 Redis Stream 队列
// Worker 池并行消费，共享模型资源
// 任务间互不阻塞
```

---

## 二、Python 微服务

### 2.1 服务结构

```
python-service/
├── main.py                  # FastAPI 入口
├── requirements.txt
├── routers/
│   ├── image_router.py      # 图片处理路由
│   ├── video_router.py      # 视频合成路由
│   └── export_router.py     # 文档导出路由
├── services/
│   ├── image_service.py     # 图片处理逻辑
│   ├── video_service.py     # FFmpeg 视频合成
│   └── export_service.py    # PDF/Word 导出
└── Dockerfile
```

### 2.2 API 清单

```
图片处理:
  POST /image/split-grid     宫格图分割(行列数 → 单张图片数组)
  POST /image/merge           多图拼接
  POST /image/compress        压缩到指定大小(≤3MB单张, ≤10MB总计)
  POST /image/resize          按比例缩放
  POST /image/super-resolution 超分辨率(Real-ESRGAN, 可选)

视频合成:
  POST /video/compose         视频合成(拼接+转场+字幕+BGM+配音+水印+片头片尾)
  POST /video/extract-frame   提取视频指定帧(用于首尾帧衔接)

文档导出:
  POST /export/pdf            小说导出 PDF (reportlab)
  POST /export/docx           小说导出 Word (python-docx)
  POST /export/epub           小说导出 EPUB (ebooklib)
```

### 2.3 视频合成参数

```python
class ComposeRequest(BaseModel):
    video_paths: list[str]          # 视频片段路径列表(按顺序)
    output_path: str                # 输出路径
    transition: str = "fadeInOut"   # none/fadeInOut/crossDissolve/blackScreen
    transition_duration: int = 500  # 转场时长 ms
    bgm_path: str = ""             # BGM 路径
    bgm_volume: int = 30           # BGM 音量 0-100
    tts_tracks: list[TtsTrack] = [] # 配音轨道
    tts_volume: int = 80           # 配音音量
    subtitle_srt: str = ""         # SRT 字幕内容
    subtitle_style: dict = {}      # 字幕样式
    watermark_text: str = ""       # 文字水印
    watermark_image: str = ""      # 图片水印路径
    watermark_position: str = "bottomRight"
    watermark_opacity: int = 30
    intro_text: str = ""           # 片头文字
    intro_duration: int = 3
    outro_text: str = ""           # 片尾文字
    outro_duration: int = 3
    resolution: str = "1080p"
    fps: int = 30

class TtsTrack(BaseModel):
    audio_path: str                 # 音频文件路径
    start_time: float               # 开始时间(秒)
    duration: float                 # 时长(秒)
```

### 2.4 依赖

```
# requirements.txt
fastapi==0.115.0
uvicorn==0.32.0
Pillow==11.0.0
opencv-python-headless==4.10.0.84
python-docx==1.1.2
reportlab==4.2.5
ebooklib==0.18.1
ffmpeg-python==0.2.0
# 可选: realesrgan, torch (超分辨率)
```
