import request from './request'

export function getStoryboards(scriptId: number): Promise<any> {
  return request.get(`/scripts/${scriptId}/storyboards`)
}

export function saveStoryboards(scriptId: number, data: any): Promise<void> {
  return request.post(`/scripts/${scriptId}/storyboards`, data)
}

export function retainStoryboards(scriptId: number): Promise<void> {
  return request.put(`/scripts/${scriptId}/storyboards/retain`)
}

export function generateImages(scriptId: number, cells: any[]): Promise<void> {
  return request.post(`/scripts/${scriptId}/storyboards/generate-images`, { cells })
}

export function superResolution(scriptId: number): Promise<void> {
  return request.post(`/scripts/${scriptId}/storyboards/super-resolution`)
}

export function replaceShotImage(scriptId: number, shotId: number, data: any): Promise<void> {
  return request.put(`/scripts/${scriptId}/storyboards/shots/${shotId}/image`, data)
}

export function generateVideoPrompts(scriptId: number): Promise<void> {
  return request.post(`/scripts/${scriptId}/storyboards/generate-video-prompts`)
}
