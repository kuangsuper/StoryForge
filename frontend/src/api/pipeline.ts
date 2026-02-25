import request from './request'
import type { PipelineRequest, PipelineStatus } from '@/types/pipeline'

export function startPipeline(projectId: number, data: PipelineRequest): Promise<void> {
  return request.post(`/projects/${projectId}/pipeline/start`, data)
}

export function getPipelineStatus(projectId: number): Promise<PipelineStatus> {
  return request.get(`/projects/${projectId}/pipeline/status`)
}

export function retryStep(projectId: number): Promise<void> {
  return request.post(`/projects/${projectId}/pipeline/retry`)
}

export function skipStep(projectId: number): Promise<void> {
  return request.post(`/projects/${projectId}/pipeline/skip`)
}

export function terminatePipeline(projectId: number): Promise<void> {
  return request.post(`/projects/${projectId}/pipeline/terminate`)
}

export function approvePipeline(projectId: number): Promise<void> {
  return request.post(`/projects/${projectId}/pipeline/approve`)
}

export function startBatch(projectId: number, data: PipelineRequest[]): Promise<void> {
  return request.post(`/projects/${projectId}/pipeline/batch`, data)
}
