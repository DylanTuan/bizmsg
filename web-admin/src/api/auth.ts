import type { LoginRequest, LoginResult, UserInfo } from '@/types/api'

/**
 * 认证接口占位：后端目前没有独立的认证服务（网关也未接入 Spring Security），
 * 因此这里用本地模拟登录打通前端鉴权链路。接入真实服务时，把实现换成
 * `request<LoginResult>({ url: '/auth/login', method: 'post', data })` 即可，调用方无需改动。
 */
const MOCK_ACCOUNT = { username: 'admin', password: '123456' }

const MOCK_USER: UserInfo = {
  username: 'admin',
  nickname: '报文管理员',
  roles: ['ADMIN']
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export async function login(payload: LoginRequest): Promise<LoginResult> {
  await delay(300)
  if (payload.username !== MOCK_ACCOUNT.username || payload.password !== MOCK_ACCOUNT.password) {
    throw new Error('用户名或密码错误（演示账号：admin / 123456）')
  }
  return { token: `mock-token-${Date.now()}`, userInfo: MOCK_USER }
}

export async function fetchUserInfo(): Promise<UserInfo> {
  await delay(100)
  return MOCK_USER
}

export async function logout(): Promise<void> {
  await delay(100)
}
