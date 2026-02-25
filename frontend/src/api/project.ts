import request from './request'
import type { Project, ProjectForm, PageResult } from '@/types/api'

export function getProjects(page = 1, size = 20): Promise<PageResult<Project>> {
  return request.get('/projects', { params: { page, size } })
}

export function getProject(id: number): Promise<Project> {
  return request.get(`/projects/${id}`)
}

export function createProject(data: ProjectForm): Promise<Project> {
  return request.post('/projects', data)
}

export function updateProject(id: number, data: ProjectForm): Promise<void> {
  return request.put(`/projects/${id}`, data)
}

export function deleteProject(id: number): Promise<void> {
  return request.delete(`/projects/${id}`)
}
