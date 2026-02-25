import request from './request'

export function submitCompose(projectId: number, data: any): Promise<void> {
  return request.post(`/projects/${projectId}/videos/compose`, data)
}

export function getComposeStatus(projectId: number, id: number): Promise<any> {
  return request.get(`/projects/${projectId}/videos/compose/${id}`)
}

export function retryCompose(projectId: number, id: number): Promise<void> {
  return request.post(`/projects/${projectId}/videos/compose/${id}/retry`)
}

export function getComposeList(projectId: number): Promise<any[]> {
  return request.get(`/projects/${projectId}/videos/compose/list`)
}

export function getComposeConfig(projectId: number): Promise<any> {
  return request.get(`/projects/${projectId}/videos/compose/config`)
}

export function updateComposeConfig(projectId: number, data: any): Promise<void> {
  return request.put(`/projects/${projectId}/videos/compose/config`, data)
}
