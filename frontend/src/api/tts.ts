import request from './request'

export function generateTts(projectId: number, scriptId: number): Promise<void> {
  return request.post(`/projects/${projectId}/tts/generate`, null, { params: { scriptId } })
}

export function getTtsVoices(projectId: number, manufacturer?: string): Promise<any[]> {
  return request.get(`/projects/${projectId}/tts/voices`, { params: { manufacturer } })
}

export function previewTts(projectId: number, data: { text: string; voiceId: string; manufacturer?: string }): Promise<any> {
  return request.post(`/projects/${projectId}/tts/preview`, data, { responseType: 'blob' })
}

export function getTtsAudio(projectId: number, scriptId: number): Promise<any[]> {
  return request.get(`/projects/${projectId}/tts/audio`, { params: { scriptId } })
}

export function getTtsConfigs(projectId: number): Promise<any[]> {
  return request.get(`/projects/${projectId}/tts/config`)
}

export function createTtsConfig(projectId: number, data: any): Promise<any> {
  return request.post(`/projects/${projectId}/tts/config`, data)
}

export function updateTtsConfig(projectId: number, id: number, data: any): Promise<void> {
  return request.put(`/projects/${projectId}/tts/config/${id}`, data)
}

export function deleteTtsConfig(projectId: number, id: number): Promise<void> {
  return request.delete(`/projects/${projectId}/tts/config/${id}`)
}
