import request from './request'
import type { Asset } from '@/types/novel'

export function getAssets(projectId: number, type?: string): Promise<Asset[]> {
  return request.get(`/projects/${projectId}/assets`, { params: { type } })
}

export function createAsset(projectId: number, data: Partial<Asset>): Promise<void> {
  return request.post(`/projects/${projectId}/assets`, data)
}

export function updateAsset(projectId: number, id: number, data: Partial<Asset>): Promise<void> {
  return request.put(`/projects/${projectId}/assets/${id}`, data)
}

export function deleteAsset(projectId: number, id: number): Promise<void> {
  return request.delete(`/projects/${projectId}/assets/${id}`)
}

export function batchSaveAssets(projectId: number, assets: Partial<Asset>[]): Promise<void> {
  return request.post(`/projects/${projectId}/assets/batch`, assets)
}

export function generateAssetImage(projectId: number, id: number): Promise<void> {
  return request.post(`/projects/${projectId}/assets/${id}/generate-image`)
}

export function polishPrompt(projectId: number, id: number): Promise<string> {
  return request.post(`/projects/${projectId}/assets/${id}/polish-prompt`)
}
