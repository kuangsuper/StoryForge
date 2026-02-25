package com.toonflow.service.pipeline;

import java.util.*;

public class PipelineStateMachine {

    private PipelineState currentState;
    private final Map<PipelineState, Set<PipelineState>> transitions;

    public PipelineStateMachine() {
        this.currentState = PipelineState.PENDING;
        this.transitions = new EnumMap<>(PipelineState.class);
        defineTransitions();
    }

    private void defineTransitions() {
        allow(PipelineState.PENDING, PipelineState.NOVEL_GENERATING, PipelineState.STEP_FAILED);
        allow(PipelineState.NOVEL_GENERATING, PipelineState.NOVEL_COMPLETE, PipelineState.STEP_FAILED);
        allow(PipelineState.NOVEL_COMPLETE, PipelineState.STORYLINE_GENERATING);
        allow(PipelineState.STORYLINE_GENERATING, PipelineState.STORYLINE_COMPLETE, PipelineState.STEP_FAILED);
        allow(PipelineState.STORYLINE_COMPLETE, PipelineState.OUTLINE_GENERATING);
        allow(PipelineState.OUTLINE_GENERATING, PipelineState.OUTLINE_REVIEWING, PipelineState.OUTLINE_COMPLETE, PipelineState.STEP_FAILED);
        allow(PipelineState.OUTLINE_REVIEWING, PipelineState.OUTLINE_COMPLETE, PipelineState.STEP_FAILED);
        allow(PipelineState.OUTLINE_COMPLETE, PipelineState.ASSETS_EXTRACTING);
        allow(PipelineState.ASSETS_EXTRACTING, PipelineState.ASSETS_IMAGE_GENERATING, PipelineState.STEP_FAILED);
        allow(PipelineState.ASSETS_IMAGE_GENERATING, PipelineState.ASSETS_COMPLETE, PipelineState.STEP_FAILED);
        allow(PipelineState.ASSETS_COMPLETE, PipelineState.SCRIPT_GENERATING);
        allow(PipelineState.SCRIPT_GENERATING, PipelineState.SCRIPT_COMPLETE, PipelineState.STEP_FAILED);
        allow(PipelineState.SCRIPT_COMPLETE, PipelineState.STORYBOARD_GENERATING);
        allow(PipelineState.STORYBOARD_GENERATING, PipelineState.STORYBOARD_COMPLETE, PipelineState.STEP_FAILED);
        allow(PipelineState.STORYBOARD_COMPLETE, PipelineState.STORYBOARD_IMAGE_GENERATING);
        allow(PipelineState.STORYBOARD_IMAGE_GENERATING, PipelineState.IMAGE_QUALITY_CHECKING, PipelineState.STORYBOARD_IMAGE_COMPLETE, PipelineState.STEP_FAILED);
        allow(PipelineState.IMAGE_QUALITY_CHECKING, PipelineState.STORYBOARD_IMAGE_COMPLETE, PipelineState.STEP_FAILED);
        allow(PipelineState.STORYBOARD_IMAGE_COMPLETE, PipelineState.TTS_GENERATING, PipelineState.VIDEO_GENERATING);
        allow(PipelineState.TTS_GENERATING, PipelineState.TTS_COMPLETE, PipelineState.STEP_FAILED);
        allow(PipelineState.TTS_COMPLETE, PipelineState.VIDEO_GENERATING);
        allow(PipelineState.VIDEO_GENERATING, PipelineState.VIDEO_COMPLETE, PipelineState.STEP_FAILED);
        allow(PipelineState.VIDEO_COMPLETE, PipelineState.COMPOSING);
        allow(PipelineState.COMPOSING, PipelineState.COMPOSE_COMPLETE, PipelineState.STEP_FAILED);
        allow(PipelineState.COMPOSE_COMPLETE, PipelineState.ALL_COMPLETE);
        // STEP_FAILED can retry to any generating state
        Set<PipelineState> retryTargets = new HashSet<>();
        for (PipelineState s : PipelineState.values()) {
            if (s.name().endsWith("_GENERATING") || s == PipelineState.ASSETS_EXTRACTING
                    || s == PipelineState.IMAGE_QUALITY_CHECKING || s == PipelineState.COMPOSING) {
                retryTargets.add(s);
            }
        }
        transitions.put(PipelineState.STEP_FAILED, retryTargets);
    }

    private void allow(PipelineState from, PipelineState... targets) {
        transitions.computeIfAbsent(from, k -> new HashSet<>()).addAll(Set.of(targets));
    }

    public void transition(PipelineState target) {
        if (!canTransition(target)) {
            throw new IllegalStateException("非法状态转换: " + currentState + " → " + target);
        }
        this.currentState = target;
    }

    public boolean canTransition(PipelineState target) {
        Set<PipelineState> allowed = transitions.get(currentState);
        return allowed != null && allowed.contains(target);
    }

    public PipelineState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(PipelineState state) {
        this.currentState = state;
    }
}
