import request from './request'
import type { Outline } from '@/types/novel'

export function getOutlines(projectId: number, mode = 'full'): Promise<Outline[]> {
  return request.get(`/projects/${projectId}/outlines`, { params: { mode } })
}

export function createOutline(projectId: number, data: any): Promise<void> {
  return request.post(`/projects/${projectId}/outlines`, data)
}

export function updateOutline(projectId: number, id: number, data: any): Promise<void> {
  return request.put(`/projects/${projectId}/outlines/${id}`, data)
}

export function deleteOutlines(projectId: number, ids: number[]): Promise<void> {
  return request.delete(`/projects/${projectId}/outlines`, { params: { ids: ids.join(',') } })
}

export function extractAssets(projectId: number): Promise<void> {
  return request.post(`/projects/${projectId}/outlines/extract-assets`)
}
