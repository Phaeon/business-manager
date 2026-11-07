<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppShell from '@/components/AppShell.vue'
import BarChart from '@/components/BarChart.vue'
import DonutChart from '@/components/DonutChart.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { errorMessage } from '@/services/api'
import { statsApi, tasksApi } from '@/services/resources'
import { useAuthStore } from '@/stores/auth'
import type { DashboardSummary, Task } from '@/types/domain'
import { dateTimeFr, money, monthLabel, priorityLabels, projectLabels, taskLabels } from '@/utils/format'

const authStore = useAuthStore()
const canSeeFinance = authStore.hasAnyRole('ADMIN', 'ACCOUNTING')
const canSeeProjects = authStore.hasAnyRole('ADMIN', 'EMPLOYEE')

const summary = ref<DashboardSummary | null>(null)
const myTasks = ref<Task[]>([])
const loading = ref(true)
const error = ref('')

const PROJECT_COLORS: Record<string, string> = {
  PLANNED: '#94a3b8', IN_PROGRESS: '#3b82f6', ON_HOLD: '#f59e0b', COMPLETED: '#22c55e'
}

const revenueBars = computed(() =>
  (summary.value?.monthlyRevenue ?? []).map((m) => ({ label: monthLabel(m.month), value: m.amount }))
)
const projectSlices = computed(() =>
  Object.entries(summary.value?.projectDistribution ?? {}).map(([status, value]) => ({
    label: projectLabels[status] ?? status, value, color: PROJECT_COLORS[status] ?? '#94a3b8'
  }))
)

onMounted(async () => {
  try {
    if (canSeeFinance) summary.value = await statsApi.dashboard()
    if (canSeeProjects) myTasks.value = await tasksApi.mine()
  } catch (e) {
    error.value = errorMessage(e, 'Impossible de charger le tableau de bord.')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <AppShell>
    <div class="container">
      <h1 class="page-title">Tableau de bord</h1>
      <p v-if="loading" class="muted">Chargement…</p>
      <p v-else-if="error" class="error-text">{{ error }}</p>

      <template v-if="summary">
        <div class="grid-cards">
          <div class="card stat"><span class="label">Chiffre d'affaires encaissé</span><span class="value">{{ money(summary.revenue) }}</span></div>
          <div class="card stat"><span class="label">Reste à encaisser</span><span class="value">{{ money(summary.pendingAmount) }}</span></div>
          <div class="card stat">
            <span class="label">Devis</span><span class="value">{{ summary.quotesCount }}</span>
            <span class="hint">{{ summary.quotesConverted }} transformé(s) · {{ summary.conversionRate }} %</span>
          </div>
          <div class="card stat"><span class="label">Factures en attente</span><span class="value">{{ summary.pendingInvoices }}</span></div>
          <div class="card stat"><span class="label">Factures payées</span><span class="value">{{ summary.paidInvoices }}</span></div>
          <div class="card stat">
            <span class="label">Nouveaux clients (30 j)</span><span class="value">{{ summary.newCustomers }}</span>
            <span class="hint">{{ summary.activeCustomers }} client(s) actif(s)</span>
          </div>
        </div>

        <div class="grid-2">
          <section class="card"><h2>Chiffre d'affaires mensuel (12 mois)</h2><BarChart :data="revenueBars" /></section>
          <section class="card"><h2>Répartition des projets</h2><DonutChart :data="projectSlices" /></section>
        </div>

        <section class="card activity">
          <h2>Activité récente</h2>
          <p v-if="summary.recentActivity.length === 0" class="muted">Aucune activité pour le moment.</p>
          <ul>
            <li v-for="a in summary.recentActivity" :key="a.id">
              <RouterLink v-if="a.link" :to="a.link">{{ a.message }}</RouterLink>
              <span v-else>{{ a.message }}</span>
              <small class="muted">{{ dateTimeFr(a.createdAt) }}</small>
            </li>
          </ul>
        </section>
      </template>

      <section v-if="canSeeProjects && !loading" class="card tasks">
        <h2>Mes tâches en cours</h2>
        <p v-if="myTasks.length === 0" class="muted">Aucune tâche ne vous est assignée.</p>
        <div v-else class="table-wrap">
          <table class="table">
            <thead><tr><th>Tâche</th><th>Projet</th><th>Priorité</th><th>Statut</th></tr></thead>
            <tbody>
              <tr v-for="t in myTasks" :key="t.id">
                <td>{{ t.title }}</td>
                <td><RouterLink :to="`/projets/${t.projectId}`">{{ t.projectName }}</RouterLink></td>
                <td><StatusBadge :status="t.priority" :labels="priorityLabels" /></td>
                <td><StatusBadge :status="t.status" :labels="taskLabels" /></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </AppShell>
</template>

<style scoped>
.stat { display: flex; flex-direction: column; gap: 0.35rem; }
.label { font-size: 0.8rem; color: var(--muted); }
.value { font-size: 1.55rem; font-weight: 700; }
.hint { font-size: 0.78rem; color: var(--muted); }
.activity, .tasks { margin-top: 1rem; }
.activity ul { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.6rem; }
.activity li { display: flex; justify-content: space-between; gap: 1rem; font-size: 0.9rem; flex-wrap: wrap; }
</style>
