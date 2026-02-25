package com.toonflow.service.pipeline;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.dto.request.PipelineRequest;
import com.toonflow.dto.response.PipelineStatus;
import com.toonflow.entity.*;
import com.toonflow.mapper.*;
import com.toonflow.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineService {

    private final TaskListMapper taskListMapper;
    private final AssetsService assetsService;
    private final ScriptService scriptService;
    private final ImageService imageService;
    private final TtsService ttsService;
    private final VideoService videoService;
    private final VideoComposeService videoComposeService;
    private final StoryboardService storyboardService;
    private final OutlineMapper outlineMapper;
    private final ScriptMapper scriptMapper;
    private final NovelMapper novelMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final PipelineStateStore stateStore;

    /**
     * 内存中保留 StateMachine 用于运行中的状态转换校验，
     * 同时每次转换都同步写入 Redis，保证重启后可恢复。
     */
    private final Map<Long, PipelineStateMachine> stateMachines = new ConcurrentHashMap<>();

    /**
     * 用于 Step 10 视频轮询的调度器，不占用 @Async 线程池
     */
    private final ScheduledExecutorService pollScheduler =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "pipeline-poll");
                t.setDaemon(true);
                return t;
            });

    public Long startPipeline(Long projectId, PipelineRequest request) {
        TaskList task = new TaskList();
        task.setProjectId(projectId);
        task.setName("全自动流水线");
        task.setPrompt(request.getPrompt());
        task.setState(PipelineState.PENDING.name());
        task.setStartTime(LocalDateTime.now());
        taskListMapper.insert(task);

        PipelineStateMachine sm = new PipelineStateMachine();
        stateMachines.put(projectId, sm);
        stateStore.saveState(projectId, PipelineState.PENDING);

        executePipelineAsync(projectId, request);
        return task.getId();
    }

    @Async
    public void executePipelineAsync(Long projectId, PipelineRequest request) {
        PipelineStateMachine sm = stateMachines.get(projectId);
        if (sm == null) return;

        emitEvent(projectId, "pipelineStart", Map.of("projectId", projectId));

        try {
            // Step 1: 小说生成
            executeStep(sm, projectId, PipelineState.NOVEL_GENERATING, PipelineState.NOVEL_COMPLETE, "小说生成", () -> {
                if (Boolean.TRUE.equals(request.getSkipNovel())) {
                    log.info("[Pipeline] Step 1: Skipping novel (skipNovel=true)");
                    return;
                }
                long count = novelMapper.selectCount(
                        new LambdaQueryWrapper<Novel>().eq(Novel::getProjectId, projectId));
                if (count > 0) {
                    log.info("[Pipeline] Step 1: Novel already exists ({} records), skipping", count);
                } else {
                    log.info("[Pipeline] Step 1: No novel found, please generate novel first");
                }
            });

            // Step 2: 故事线生成
            executeStep(sm, projectId, PipelineState.STORYLINE_GENERATING, PipelineState.STORYLINE_COMPLETE, "故事线生成", () -> {
                if (Boolean.TRUE.equals(request.getSkipStoryline())) {
                    log.info("[Pipeline] Step 2: Skipping storyline");
                    return;
                }
                log.info("[Pipeline] Step 2: Storyline check for project={}", projectId);
            });

            // Step 3: 大纲生成
            executeStep(sm, projectId, PipelineState.OUTLINE_GENERATING, PipelineState.OUTLINE_COMPLETE, "大纲生成", () -> {
                if (Boolean.TRUE.equals(request.getSkipOutline())) {
                    log.info("[Pipeline] Step 3: Skipping outline");
                    return;
                }
                long count = outlineMapper.selectCount(
                        new LambdaQueryWrapper<Outline>().eq(Outline::getProjectId, projectId));
                if (count > 0) {
                    log.info("[Pipeline] Step 3: Outline already exists ({} records), skipping", count);
                } else {
                    log.info("[Pipeline] Step 3: No outline found, please generate outline first");
                }
            });

            // Step 4: 资产提取
            executeStep(sm, projectId, PipelineState.ASSETS_EXTRACTING, PipelineState.ASSETS_COMPLETE, "资产提取", () -> {
                assetsService.extractFromOutlines(projectId);
            });

            // Step 5: 资产图片生成
            executeStep(sm, projectId, PipelineState.ASSETS_IMAGE_GENERATING, PipelineState.ASSETS_COMPLETE, "资产图片生成", () -> {
                var assets = assetsService.list(projectId, null);
                for (var asset : assets) {
                    try { imageService.generateAssetImage(projectId, asset.getId()); }
                    catch (Exception e) { log.warn("[Pipeline] Asset image gen failed: {}", e.getMessage()); }
                }
            });

            // Step 6: 剧本生成
            executeStep(sm, projectId, PipelineState.SCRIPT_GENERATING, PipelineState.SCRIPT_COMPLETE, "剧本生成", () -> {
                if (Boolean.TRUE.equals(request.getSkipScript())) {
                    log.info("[Pipeline] Step 6: Skipping script");
                    return;
                }
                List<Outline> outlines = outlineMapper.selectList(
                        new LambdaQueryWrapper<Outline>().eq(Outline::getProjectId, projectId));
                for (Outline outline : outlines) {
                    long existing = scriptMapper.selectCount(
                            new LambdaQueryWrapper<Script>()
                                    .eq(Script::getProjectId, projectId)
                                    .eq(Script::getOutlineId, outline.getId()));
                    if (existing > 0) {
                        log.info("[Pipeline] Script already exists for outlineId={}, skipping", outline.getId());
                        continue;
                    }
                    try {
                        Script script = new Script();
                        script.setProjectId(projectId);
                        script.setOutlineId(outline.getId());
                        script.setName("第" + outline.getEpisode() + "集剧本");
                        scriptMapper.insert(script);
                        scriptService.generate(script.getId(), outline.getId());
                    } catch (Exception e) {
                        log.warn("[Pipeline] Script gen failed for outlineId={}: {}", outline.getId(), e.getMessage());
                    }
                }
            });

            // Step 7: 分镜生成
            executeStep(sm, projectId, PipelineState.STORYBOARD_GENERATING, PipelineState.STORYBOARD_COMPLETE, "分镜生成", () -> {
                if (Boolean.TRUE.equals(request.getSkipStoryboard())) {
                    log.info("[Pipeline] Step 7: Skipping storyboard");
                    return;
                }
                List<Script> scripts = scriptMapper.selectList(
                        new LambdaQueryWrapper<Script>().eq(Script::getProjectId, projectId));
                for (Script script : scripts) {
                    try { storyboardService.autoGenerate(projectId, script.getId()); }
                    catch (Exception e) { log.warn("[Pipeline] Storyboard gen failed for scriptId={}: {}", script.getId(), e.getMessage()); }
                }
            });

            // Step 8: 分镜图片生成
            executeStep(sm, projectId, PipelineState.STORYBOARD_IMAGE_GENERATING, PipelineState.STORYBOARD_IMAGE_COMPLETE, "分镜图片生成", () -> {
                var assets = assetsService.list(projectId, null);
                for (var asset : assets) {
                    if (asset.getFilePath() == null || asset.getFilePath().isEmpty()) {
                        try { imageService.generateAssetImage(projectId, asset.getId()); }
                        catch (Exception e) { log.warn("[Pipeline] Storyboard image gen failed: {}", e.getMessage()); }
                    }
                }
            });

            // Step 9: TTS 配音
            if (Boolean.TRUE.equals(request.getTtsEnabled())) {
                executeStep(sm, projectId, PipelineState.TTS_GENERATING, PipelineState.TTS_COMPLETE, "TTS配音", () -> {
                    List<Script> scripts = scriptMapper.selectList(
                            new LambdaQueryWrapper<Script>().eq(Script::getProjectId, projectId));
                    for (Script script : scripts) {
                        try { ttsService.generate(projectId, script.getId()); }
                        catch (Exception e) { log.warn("[Pipeline] TTS gen failed for scriptId={}: {}", script.getId(), e.getMessage()); }
                    }
                });
            }

            // Step 10: 视频生成 — 提交任务后用 ScheduledExecutorService 非阻塞轮询
            executeStep(sm, projectId, PipelineState.VIDEO_GENERATING, PipelineState.VIDEO_COMPLETE, "视频生成", () -> {
                List<Script> scripts = scriptMapper.selectList(
                        new LambdaQueryWrapper<Script>().eq(Script::getProjectId, projectId));
                for (Script script : scripts) {
                    Map<String, Object> storyboard = storyboardService.getByScript(projectId, script.getId());
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> shots = (List<Map<String, Object>>) storyboard.get("shots");
                    if (shots == null || shots.isEmpty()) continue;
                    for (Map<String, Object> shot : shots) {
                        String shotId = String.valueOf(shot.get("id"));
                        String prompt = (String) shot.get("videoPrompt");
                        try {
                            videoService.submitGenerate(projectId, script.getId(), shotId, null, prompt, null);
                        } catch (Exception e) {
                            log.warn("[Pipeline] Video submit failed for shotId={}: {}", shotId, e.getMessage());
                        }
                    }
                }
                // 非阻塞轮询：用 ScheduledExecutorService 每10秒检查一次，最多30分钟
                awaitVideoPending(projectId, 30 * 60);
            });

            // Step 11: 视频合成
            if (Boolean.TRUE.equals(request.getAutoCompose())) {
                executeStep(sm, projectId, PipelineState.COMPOSING, PipelineState.COMPOSE_COMPLETE, "视频合成", () -> {
                    List<Script> scripts = scriptMapper.selectList(
                            new LambdaQueryWrapper<Script>().eq(Script::getProjectId, projectId));
                    for (Script script : scripts) {
                        try { videoComposeService.submit(projectId, script.getId(), null); }
                        catch (Exception e) { log.warn("[Pipeline] Compose failed for scriptId={}: {}", script.getId(), e.getMessage()); }
                    }
                });
            }

            transitionAndPersist(sm, projectId, PipelineState.ALL_COMPLETE);
            updateTaskListState(projectId, PipelineState.ALL_COMPLETE);
            emitEvent(projectId, "pipelineComplete", Map.of("projectId", projectId));

        } catch (Exception e) {
            log.error("[Pipeline] Failed for project={}", projectId, e);
            updateTaskListState(projectId, PipelineState.STEP_FAILED);
            emitEvent(projectId, "pipelineError", Map.of("error", e.getMessage() != null ? e.getMessage() : "未知错误"));
        } finally {
            // 运行结束后清理内存中的 StateMachine（Redis 状态保留供查询）
            stateMachines.remove(projectId);
        }
    }

    /**
     * 非阻塞等待视频任务完成。
     * 使用 ScheduledExecutorService 每10秒检查一次，不占用 @Async 线程池。
     * 通过 CompletableFuture.get() 阻塞当前 pipeline 线程，但调度线程是独立的。
     */
    private void awaitVideoPending(Long projectId, int timeoutSeconds) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;

        ScheduledFuture<?> scheduled = pollScheduler.scheduleWithFixedDelay(() -> {
            try {
                if (System.currentTimeMillis() > deadline) {
                    future.complete(null);
                    return;
                }
                long pending = videoService.countPending(projectId);
                if (pending == 0) {
                    future.complete(null);
                } else {
                    log.info("[Pipeline] Waiting for {} video tasks for project={}...", pending, projectId);
                }
            } catch (Exception e) {
                log.warn("[Pipeline] Poll error for project={}: {}", projectId, e.getMessage());
                future.complete(null);
            }
        }, 0, 10, TimeUnit.SECONDS);

        try {
            // 阻塞等待轮询完成，但轮询本身在独立调度线程
            future.get(timeoutSeconds + 30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("[Pipeline] Video poll timed out for project={}", projectId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.warn("[Pipeline] Video poll error for project={}: {}", projectId, e.getMessage());
        } finally {
            scheduled.cancel(false);
        }
    }

    private void executeStep(PipelineStateMachine sm, Long projectId,
                             PipelineState startState, PipelineState completeState,
                             String stepName, Runnable action) {
        transitionAndPersist(sm, projectId, startState);
        emitEvent(projectId, "stepStart", Map.of("step", stepName, "state", startState.name()));
        try {
            action.run();
            transitionAndPersist(sm, projectId, completeState);
            emitEvent(projectId, "stepComplete", Map.of("step", stepName, "state", completeState.name()));
        } catch (Exception e) {
            transitionAndPersist(sm, projectId, PipelineState.STEP_FAILED);
            stateStore.saveFailedStep(projectId, startState);
            updateTaskListState(projectId, PipelineState.STEP_FAILED);
            emitEvent(projectId, "stepFailed", Map.of("step", stepName, "error", e.getMessage() != null ? e.getMessage() : "未知错误"));
            throw e;
        }
    }

    /**
     * 状态转换 + 同步写入 Redis
     */
    private void transitionAndPersist(PipelineStateMachine sm, Long projectId, PipelineState target) {
        sm.transition(target);
        stateStore.saveState(projectId, target);
    }

    /**
     * 同步更新 TaskList 表中的 state 字段
     */
    private void updateTaskListState(Long projectId, PipelineState state) {
        try {
            TaskList task = taskListMapper.selectOne(
                    new LambdaQueryWrapper<TaskList>()
                            .eq(TaskList::getProjectId, projectId)
                            .orderByDesc(TaskList::getCreateTime)
                            .last("LIMIT 1"));
            if (task != null) {
                task.setState(state.name());
                if (state == PipelineState.ALL_COMPLETE || state == PipelineState.STEP_FAILED) {
                    task.setEndTime(LocalDateTime.now());
                }
                taskListMapper.updateById(task);
            }
        } catch (Exception e) {
            log.warn("[Pipeline] Failed to update TaskList state for project={}: {}", projectId, e.getMessage());
        }
    }

    /**
     * 批量启动流水线，使用 Semaphore 控制并发度
     */
    public List<Long> startBatch(List<PipelineRequest> requests) {
        int concurrency = requests.stream()
                .mapToInt(r -> r.getBatchConcurrency() != null ? r.getBatchConcurrency() : 3)
                .max().orElse(3);
        Semaphore semaphore = new Semaphore(concurrency);
        List<Long> taskIds = Collections.synchronizedList(new ArrayList<>());

        List<CompletableFuture<Void>> futures = requests.stream().map(req ->
                CompletableFuture.runAsync(() -> {
                    try {
                        semaphore.acquire();
                        Long taskId = startPipeline(req.getProjectId(), req);
                        taskIds.add(taskId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        semaphore.release();
                    }
                })
        ).collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return taskIds;
    }

    public void retryStep(Long projectId) {
        PipelineState currentState = stateStore.getState(projectId);
        if (currentState == null) throw new BizException(ErrorCode.NOT_FOUND, "流水线不存在");
        if (currentState != PipelineState.STEP_FAILED) {
            throw new BizException(ErrorCode.BIZ_ERROR, "当前状态不可重试");
        }
        PipelineState failedState = stateStore.getFailedStep(projectId);
        if (failedState != null) {
            // 重建内存状态机并回退到失败步骤
            PipelineStateMachine sm = new PipelineStateMachine();
            sm.setCurrentState(PipelineState.STEP_FAILED);
            sm.transition(failedState);
            stateMachines.put(projectId, sm);
            stateStore.saveState(projectId, failedState);
        }
    }

    public void skipStep(Long projectId) {
        PipelineState currentState = stateStore.getState(projectId);
        if (currentState == null) throw new BizException(ErrorCode.NOT_FOUND);
        log.info("[Pipeline] Skipping step for project={}", projectId);
    }

    public void terminate(Long projectId) {
        stateMachines.remove(projectId);
        stateStore.remove(projectId);
        log.info("[Pipeline] Terminated for project={}", projectId);
    }

    public void approveReview(Long projectId) {
        PipelineState currentState = stateStore.getState(projectId);
        if (currentState == null) throw new BizException(ErrorCode.NOT_FOUND);
        log.info("[Pipeline] Review approved for project={}", projectId);
    }

    public PipelineStatus getStatus(Long projectId) {
        PipelineStatus status = new PipelineStatus();

        // 优先从内存读（运行中），否则从 Redis 读（重启后恢复）
        PipelineStateMachine sm = stateMachines.get(projectId);
        if (sm != null) {
            status.setCurrentState(sm.getCurrentState());
            status.setCurrentStepName(sm.getCurrentState().name());
        } else {
            PipelineState redisState = stateStore.getState(projectId);
            if (redisState != null) {
                status.setCurrentState(redisState);
                status.setCurrentStepName(redisState.name());
            } else {
                // 回退到数据库查询
                TaskList task = taskListMapper.selectOne(
                        new LambdaQueryWrapper<TaskList>()
                                .eq(TaskList::getProjectId, projectId)
                                .orderByDesc(TaskList::getCreateTime)
                                .last("LIMIT 1"));
                if (task != null && task.getState() != null) {
                    try {
                        PipelineState dbState = PipelineState.valueOf(task.getState());
                        status.setCurrentState(dbState);
                        status.setCurrentStepName(dbState.name());
                    } catch (IllegalArgumentException e) {
                        status.setCurrentState(PipelineState.PENDING);
                    }
                } else {
                    status.setCurrentState(PipelineState.PENDING);
                }
            }
        }
        status.setTotalSteps(11);
        return status;
    }

    private void emitEvent(Long projectId, String type, Object data) {
        messagingTemplate.convertAndSend(
                "/topic/agent/pipeline/" + projectId + "/event",
                Map.of("type", type, "data", data));
    }
}
