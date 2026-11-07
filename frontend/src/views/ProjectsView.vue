<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppShell from '@/components/AppShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import ProjectFormModal from '@/components/ProjectFormModal.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { errorMessage } from '@/services/api'
import { projectsApi } from '@/services/resources'
import type { Project, ProjectStatus } from '@/types/domain'
import { dateFr, money, projectLabels } from '@/utils/format'

const projects = ref<Project[]>([])
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const loading = ref(false)
const error = ref('')
const search = ref('')
const status = ref<ProjectStatus | ''>('')

const formOpen = ref(false)
const editing = ref<Project | null>(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await projectsApi.list({
      search: search.value || undefined, status: status.value || undefined, page: page.value, size: 10
    })
    projects.value = result.content
    totalPages.value = result.totalPages
    totalElements.value = result.totalElements
  } catch (e) {
    error.value = errorMessage(e, 'Impossible de charger les projets.')
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 0
  load()
}

function openForm(project: Project | null) {
  editing.value = project
  formOpen.value = true
}

async function onSaved() {
  formOpen.value = false
  await load()
}

async function remove(p: Project) {
  if (!window.confirm(`Supprimer le projet « ${p.name} » et toutes ses tâches ?`)) return
  try {
    await projectsApi.remove(p.id)
    await load()
  } catch (e) {
    error.value = errorMessage(e)
  }
}

onMounted(load)
</script>

<template>
  <AppShell>
    <div class="container">
      <h1 class="page-title">Projets</h1>

      <div class="toolbar">
        <div class="filters">
          <input v-model="search" placeholder="Projet ou client…" @keyup.enter="applyFilters" />
          <select v-model="status" aria-label="Filtrer par statut" @change="applyFilters">
            <option value="">Tous les statuts</option>
            <option v-for="(label, key) in projectLabels" :key="key" :value="key">{{ label }}</option>
          </select>
          <button class="btn" @click="applyFilters">Rechercher</button>
        </div>
        <button class="btn btn-primary" @click="openForm(null)">+ Nouveau projet</button>
      </div>

      <p v-if="error" class="error-text">{{ error }}</p>

      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr><th>Projet</th><th>Client</th><th>Statut</th><th>Progression</th><th>Échéance</th><th class="right">Budget</th><th /></tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="7" class="empty">Chargement…</td></tr>
            <tr v-for="p in projects" :key="p.id">
              <td><RouterLink :to="`/projets/${p.id}`"><strong>{{ p.name }}</strong></RouterLink></td>
              <td>{{ p.customerName }}</td>
              <td><StatusBadge :status="p.status" :labels="projectLabels" /></td>
              <td>
                <div class="prog"><div class="progress"><span :style="{ width: p.progress + '%' }" /></div>{{ p.progress }} %</div>
              </td>
              <td>{{ dateFr(p.dueDate) }}</td>
              <td class="right nowrap">{{ p.budget === null ? '—' : money(p.budget) }}</td>
              <td>
                <div class="actions">
                  <RouterLink class="btn btn-sm" :to="`/kanban?project=${p.id}`">Kanban</RouterLink>
                  <button class="btn btn-sm" @click="openForm(p)">Modifier</button>
                  <button class="btn btn-sm btn-danger" @click="remove(p)">Supprimer</button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && projects.length === 0"><td colspan="7" class="empty">Aucun projet.</td></tr>
          </tbody>
        </table>
      </div>

      <PaginationBar :page="page" :total-pages="totalPages" :total-elements="totalElements"
        @change="(p) => { page = p; load() }" />
    </div>

    <ProjectFormModal v-if="formOpen" :project="editing" @saved="onSaved" @close="formOpen = false" />
  </AppShell>
</template>

<style scoped>
.prog { display: flex; align-items: center; gap: 0.5rem; font-size: 0.82rem; min-width: 150px; }
.prog .progress { flex: 1; }
</style>
