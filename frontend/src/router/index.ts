import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/types/auth'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    roles?: Role[]
  }
}

const FINANCE: Role[] = ['ADMIN', 'ACCOUNTING']
const PROJECTS: Role[] = ['ADMIN', 'EMPLOYEE']

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
    { path: '/', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
    { path: '/clients', name: 'customers', component: () => import('@/views/CustomersView.vue') },
    { path: '/devis', name: 'quotes', component: () => import('@/views/QuotesView.vue'), meta: { roles: FINANCE } },
    { path: '/factures', name: 'invoices', component: () => import('@/views/InvoicesView.vue'), meta: { roles: FINANCE } },
    { path: '/projets', name: 'projects', component: () => import('@/views/ProjectsView.vue'), meta: { roles: PROJECTS } },
    { path: '/projets/:id', name: 'project', component: () => import('@/views/ProjectDetailView.vue'), meta: { roles: PROJECTS } },
    { path: '/kanban', name: 'kanban', component: () => import('@/views/KanbanView.vue'), meta: { roles: PROJECTS } },
    { path: '/calendrier', name: 'calendar', component: () => import('@/views/CalendarView.vue') },
    { path: '/documents', name: 'documents', component: () => import('@/views/DocumentsView.vue') },
    { path: '/utilisateurs', name: 'users', component: () => import('@/views/UsersView.vue'), meta: { roles: ['ADMIN'] } },
    { path: '/profil', name: 'profile', component: () => import('@/views/ProfileView.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (!to.meta.public && !authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && authStore.isAuthenticated) {
    return { name: 'dashboard' }
  }
  if (to.meta.roles && !authStore.hasAnyRole(...to.meta.roles)) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
