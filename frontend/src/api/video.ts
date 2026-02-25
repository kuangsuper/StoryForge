import request from './request'

export function generateVideo(projectId: number, data: any): Promise<void> {
  return request.post(`/projects/${projectId}/videos/generate`, data)
}

export function getVideos(projectId: number, scriptId?: number): Promise<any[]> {
  return request.get(`/projects/${projectId}/videos`, { params: { scriptId } })
}

export function getVideoVersions(projectId: number, shotId: number): Promise<any[]> {
  return request.get(`/projects/${projectId}/videos/versions`, { params: { shotId } })
}

export function selectVideo(projectId: number, id: number): Promise<void> {
  return request.put(`/projects/${projectId}/videos/${id}/select`)
}

export function batchRetryVideos(projectId: number): Promise<void> {
  return request.post(`/projects/${projectId}/videos/batch-retry`)
}

export function getVideoModels(): Promise<any[]> {
  return request.get('/videos/models')
}

export function getVideoManufacturers(): Promise<any[]> {
  return request.get('/videos/manufacturers')
}
