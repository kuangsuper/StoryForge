import request from './request'

export function getPrompts(): Promise<any[]> {
  return request.get('/prompts')
}

export function updatePrompt(id: number, customValue: string): Promise<void> {
  return request.put(`/prompts/${id}`, { customValue })
}
