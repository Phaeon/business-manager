import { ref } from 'vue'

type Theme = 'light' | 'dark'

const stored = localStorage.getItem('theme') as Theme | null
const theme = ref<Theme>(stored ?? (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'))

function apply(): void {
  document.documentElement.dataset.theme = theme.value
}

export function initTheme(): void {
  apply()
}

export function useTheme() {
  function toggle(): void {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
    localStorage.setItem('theme', theme.value)
    apply()
  }
  return { theme, toggle }
}
