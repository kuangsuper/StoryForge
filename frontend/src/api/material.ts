import request from './request'

export function getMaterials(type?: string): Promise<any[]> {
  return request.get('/materials', { params: { type } })
}

export function uploadMaterial(formData: FormData): Promise<void> {
  return request.post('/materials', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}

export function deleteMaterial(id: number): Promise<void> {
  return request.delete(`/materials/${id}`)
}
