import request from './request'
import type { LoginRequest, LoginResult, CaptchaResult, User } from '@/types/api'

export function login(data: LoginRequest): Promise<LoginResult> {
  return request.post('/auth/login', data)
}

export function logout(): Promise<void> {
  return request.post('/auth/logout')
}

export function getCaptcha(): Promise<CaptchaResult> {
  return request.get('/auth/captcha')
}

export function getCurrentUser(): Promise<User> {
  return request.get('/users/me')
}
