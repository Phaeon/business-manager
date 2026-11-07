import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/auth'

const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.accessToken) {
    config.headers.Authorization = `Bearer ${authStore.accessToken}`
  }
  return config
})

// Une seule requête de refresh à la fois : les autres requêtes en 401 attendent la même promesse.
let refreshPromise: Promise<void> | null = null

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined
    const authStore = useAuthStore()

    if (error.response?.status === 401 && original && !original._retry && authStore.refreshToken) {
      original._retry = true
      try {
        refreshPromise = refreshPromise ?? authStore.refresh().finally(() => { refreshPromise = null })
        await refreshPromise
        return api(original)
      } catch (refreshError) {
        authStore.logout()
        window.location.href = '/login'
        return Promise.reject(refreshError)
      }
    }
    return Promise.reject(error)
  }
)

/** Extrait un message lisible d'une erreur Axios renvoyée par le backend. */
export function errorMessage(error: unknown, fallback = 'Une erreur est survenue'): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string; fieldErrors?: Record<string, string> } | undefined
    if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
      return Object.values(data.fieldErrors).join(' · ')
    }
    if (data?.message) return data.message
  }
  return fallback
}

/** Télécharge un fichier protégé par JWT (un simple lien <a> ne peut pas envoyer l'en-tête Authorization). */
export async function downloadFile(url: string, filename: string): Promise<void> {
  const response = await api.get<Blob>(url, { responseType: 'blob' })
  const href = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = href
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(href)
}

export default api
