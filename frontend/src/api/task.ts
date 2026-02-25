import request from './request'

export function getTasks(state?: string): Promise<any[]> {
  return request.get('/tasks', { params: { state } })
}

export function getTask(id: number): Promise<any> {
  return request.get(`/tasks/${id}`)
}
