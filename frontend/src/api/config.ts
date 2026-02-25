import request from './request'

export function getModelConfigs(): Promise<any[]> {
  return request.get('/settings/models')
}

export function createModelConfig(data: any): Promise<void> {
  return request.post('/settings/models', data)
}

export function updateModelConfig(id: number, data: any): Promise<void> {
  return request.put(`/settings/models/${id}`, data)
}

export function deleteModelConfig(id: number): Promise<void> {
  return request.delete(`/settings/models/${id}`)
}

export function testModelConfig(id: number): Promise<any> {
  return request.post(`/settings/models/${id}/test`)
}

export function getModelMaps(): Promise<any> {
  return request.get('/settings/model-maps')
}

export function updateModelMaps(data: any): Promise<void> {
  return request.put('/settings/model-maps', data)
}
