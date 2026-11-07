import { defineStore } from 'pinia'
import axios from 'axios'
import type { AuthResponse, LoginPayload, Role, UserInfo } from '@/types/auth'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: UserInfo | null
}

function readStoredUser(): UserInfo | null {
  try {
    return JSON.parse(localStorage.getItem('user') ?? 'null') as UserInfo | null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: localStorage.getItem('accessToken'),
    refreshToken: localStorage.getItem('refreshToken'),
    user: readStoredUser()
  }),

  getters: {
    isAuthenticated: (state) => !!state.accessToken,
    hasAnyRole: (state) => (...roles: Role[]): boolean => !!state.user && roles.includes(state.user.role)
  },

  actions: {
    setSession(data: AuthResponse) {
      this.accessToken = data.accessToken
      this.refreshToken = data.refreshToken
      this.user = data.user

      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      localStorage.setItem('user', JSON.stringify(data.user))
    },

    async login(payload: LoginPayload) {
      const { data } = await axios.post<AuthResponse>('/api/auth/login', payload)
      this.setSession(data)
    },

    async refresh() {
      const { data } = await axios.post<AuthResponse>('/api/auth/refresh', {
        refreshToken: this.refreshToken
      })
      this.setSession(data)
    },

    logout() {
      this.accessToken = null
      this.refreshToken = null
      this.user = null
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
    }
  }
})
