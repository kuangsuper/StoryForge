import request from './request'

export function getStats(): Promise<any> {
  return request.get('/dashboard/stats')
}

export function getDailyMetrics(): Promise<any> {
  return request.get('/dashboard/daily')
}

export function getModelUsage(): Promise<any> {
  return request.get('/dashboard/models')
}

export function getPipelineStats(): Promise<any> {
  return request.get('/dashboard/pipelines')
}
