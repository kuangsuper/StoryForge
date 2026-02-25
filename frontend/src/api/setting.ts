import request from './request'

export function getSettings(): Promise<any> {
  return request.get('/system-settings')
}

export function updateSettings(data: any): Promise<void> {
  return request.put('/system-settings', data)
}
