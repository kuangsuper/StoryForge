import request from './request'
import type { Script } from '@/types/novel'

export function getScripts(projectId: number): Promise<Script[]> {
  return request.get(`/projects/${projectId}/scripts`)
}

export function getScript(projectId: number, scriptId: number): Promise<Script> {
  return request.get(`/projects/${projectId}/scripts/${scriptId}`)
}

export function updateScript(projectId: number, scriptId: number, data: Partial<Script>): Promise<void> {
  return request.put(`/projects/${projectId}/scripts/${scriptId}`, data)
}

export function generateScript(projectId: number, scriptId: number, outlineId?: number): Promise<void> {
  return request.post(`/projects/${projectId}/scripts/${scriptId}/generate`, { outlineId })
}
