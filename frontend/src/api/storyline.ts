import request from './request'

export function getStoryline(projectId: number): Promise<any> {
  return request.get(`/projects/${projectId}/storylines`)
}

export function saveStoryline(projectId: number, content: string): Promise<void> {
  return request.put(`/projects/${projectId}/storylines`, { content })
}

export function deleteStoryline(projectId: number): Promise<void> {
  return request.delete(`/projects/${projectId}/storylines`)
}
