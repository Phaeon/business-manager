export type Role = 'ADMIN' | 'EMPLOYEE' | 'ACCOUNTING'

export interface UserInfo {
  id: string
  firstName: string
  lastName: string
  email: string
  role: Role
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  user: UserInfo
}

export interface LoginPayload {
  email: string
  password: string
}
