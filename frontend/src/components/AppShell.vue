<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTheme } from '@/composables/useTheme'
import type { Role } from '@/types/auth'
import { roleLabels } from '@/utils/format'
import GlobalSearch from './GlobalSearch.vue'
import NotificationBell from './NotificationBell.vue'

const authStore = useAuthStore()
const router = useRouter()
const { theme, toggle } = useTheme()

interface NavItem { to: string; label: string; roles?: Role[] }

const items: NavItem[] = [
  { to: '/', label: 'Tableau de bord' },
  { to: '/clients', label: 'Clients' },
  { to: '/devis', label: 'Devis', roles: ['ADMIN', 'ACCOUNTING'] },
  { to: '/factures', label: 'Factures', roles: ['ADMIN', 'ACCOUNTING'] },
  { to: '/projets', label: 'Projets', roles: ['ADMIN', 'EMPLOYEE'] },
  { to: '/kanban', label: 'Kanban', roles: ['ADMIN', 'EMPLOYEE'] },
  { to: '/calendrier', label: 'Calendrier' },
  { to: '/documents', label: 'Documents' },
  { to: '/utilisateurs', label: 'Utilisateurs', roles: ['ADMIN'] }
]

const nav = computed(() => items.filter((i) => !i.roles || authStore.hasAnyRole(...i.roles)))

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">Business Manager</div>
      <nav>
        <RouterLink v-for="item in nav" :key="item.to" :to="item.to">{{ item.label }}</RouterLink>
      </nav>
      <div class="user-box">
        <RouterLink to="/profil" class="who">
          {{ authStore.user?.firstName }} {{ authStore.user?.lastName }}
          <small>{{ authStore.user ? roleLabels[authStore.user.role] : '' }}</small>
        </RouterLink>
        <button @click="handleLogout">Déconnexion</button>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <GlobalSearch />
        <div class="tools">
          <button class="btn icon" :aria-label="theme === 'dark' ? 'Mode clair' : 'Mode sombre'" @click="toggle">
            {{ theme === 'dark' ? '☀️' : '🌙' }}
          </button>
          <NotificationBell />
        </div>
      </header>
      <main class="content"><slot /></main>
    </div>
  </div>
</template>

<style scoped>
.shell { display: flex; min-height: 100vh; }
.sidebar { width: 220px; flex-shrink: 0; background: var(--sidebar); color: #fff; display: flex; flex-direction: column; padding: 1.5rem 1rem; }
.brand { font-weight: 700; margin-bottom: 2rem; font-size: 1.05rem; }
nav { display: flex; flex-direction: column; gap: 0.25rem; flex: 1; }
nav a { color: #cbd5e1; text-decoration: none; padding: 0.5rem 0.75rem; border-radius: 6px; font-size: 0.92rem; }
nav a:hover { background: rgba(255, 255, 255, 0.06); text-decoration: none; }
nav a.router-link-exact-active, nav a.router-link-active:not([href='/']) { background: rgba(255, 255, 255, 0.12); color: #fff; }
.user-box { display: flex; flex-direction: column; gap: 0.6rem; font-size: 0.85rem; border-top: 1px solid rgba(255, 255, 255, 0.1); padding-top: 1rem; }
.who { color: #fff; display: flex; flex-direction: column; text-decoration: none; }
.who small { color: #94a3b8; }
.user-box button { background: transparent; border: 1px solid rgba(255, 255, 255, 0.2); color: #fff; padding: 0.4rem; border-radius: 6px; cursor: pointer; }
.main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.topbar {
  display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 0.75rem 1.5rem;
  background: var(--surface); border-bottom: 1px solid var(--border); position: sticky; top: 0; z-index: 30;
}
.tools { display: flex; gap: 0.5rem; align-items: center; }
.icon { padding: 0.4rem 0.6rem; }
.content { flex: 1; }

@media (max-width: 900px) {
  .shell { flex-direction: column; }
  .sidebar { width: 100%; flex-direction: row; align-items: center; padding: 0.6rem 1rem; gap: 1rem; overflow-x: auto; }
  .brand { margin: 0; white-space: nowrap; }
  nav { flex-direction: row; }
  nav a { white-space: nowrap; }
  .user-box { border: none; padding: 0; flex-direction: row; align-items: center; }
  .topbar { padding: 0.6rem 1rem; }
}
</style>
