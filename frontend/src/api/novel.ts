import request from './request'
import type { Novel } from '@/types/novel'

export function getNovels(projectId: number): Promise<Novel[]> {
  return request.get(`/projects/${projectId}/novels`)
}

export function getNovel(projectId: number, id: number): Promise<Novel> {
  return request.get(`/projects/${projectId}/novels/${id}`)
}

export function createNovel(projectId: number, data: Partial<Novel>): Promise<void> {
  return request.post(`/projects/${projectId}/novels`, data)
}

export function updateNovel(projectId: number, id: number, data: Partial<Novel>): Promise<void> {
  return request.put(`/projects/${projectId}/novels/${id}`, data)
}

export function deleteNovel(projectId: number, id: number): Promise<void> {
  return request.delete(`/projects/${projectId}/novels/${id}`)
}

export function exportNovel(projectId: number, format: 'txt' | 'docx'): Promise<Blob> {
  return request.get(`/projects/${projectId}/novel/export/${format}`, {
    responseType: 'blob',
  })
}

export function getNovelWorld(projectId: number): Promise<any> {
  return request.get(`/projects/${projectId}/novel/world`)
}

export function updateNovelWorld(projectId: number, data: any): Promise<void> {
  return request.put(`/projects/${projectId}/novel/world`, data)
}

export function getNovelCharacters(projectId: number): Promise<any[]> {
  return request.get(`/projects/${projectId}/novel/characters`)
}

export function getNovelProgress(projectId: number): Promise<any> {
  return request.get(`/projects/${projectId}/novel/progress`)
}

export function triggerQualityCheck(projectId: number, scope: string, index?: number): Promise<void> {
  return request.post(`/projects/${projectId}/novel/quality-check`, null, { params: { scope, index } })
}

export function getQualityReports(projectId: number): Promise<any[]> {
  return request.get(`/projects/${projectId}/novel/quality-reports`)
}
