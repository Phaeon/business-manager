<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import ProjectFormModal from '@/components/ProjectFormModal.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import TaskFormModal from '@/components/TaskFormModal.vue'
import { errorMessage } from '@/services/api'
import { projectsApi, tasksApi, usersApi } from '@/services/resources'
import type { Assignee, Project, ProjectComment, Task, TaskStatus } from '@/types/domain'
import { dateFr, dateTimeFr, money, priorityLabels, projectLabels, taskLabels } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const projectId = route.params.id as string

const project = ref<Project | null>(null)
const tasks = ref<Task[]>([])
const comments = ref<ProjectComment[]>([])
const users = ref<Assignee[]>([])
const error = ref('')
const loading = ref(true)

const editProject = ref(false)
const taskModal = ref(false)
const editingTask = ref<Task | null>(null)
const newComment = ref('')

async function loadAll() {
  try {
    const [p, t, c] = await Promise.all([
      projectsApi.get(projectId), projectsApi.tasks(projectId), projectsApi.comments(projectId)
    ])
    project.value = p
    tasks.value = t
    comments.value = c
  } catch (e) {
    error.value = errorMessage(e, 'Projet introuvable.')
  } finally {
    loading.value = false
  }
}

/** Recharge le projet (la progression est recalculée côté serveur) et les tâches. */
async function refresh() {
  try {
    const [p, t] = await Promise.all([projectsApi.get(projectId), projectsApi.tasks(projectId)])
    project.value = p
    tasks.value = t
  } catch (e) {
    error.value = errorMessage(e)
  }
}

function openTask(task: Task | null) {
  editingTask.value = task
  taskModal.value = true
}

async function onTaskSaved() {
  taskModal.value = false
  await refresh()
}

async function changeStatus(task: Task, status: TaskStatus) {
  try {
    await tasksApi.updateStatus(task.id, status)
    await refresh()
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function removeTask(task: Task) {
  if (!window.confirm(`Supprimer la tâche « ${task.title} » ?`)) return
  try {
    await tasksApi.remove(task.id)
    await refresh()
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function addComment() {
  if (!newComment.value.trim()) return
  try {
    const created = await projectsApi.addComment(projectId, newComment.value)
    comments.value.unshift(created)
    newComment.value = ''
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function onProjectSaved(saved: Project) {
  project.value = saved
  editProject.value = false
  await refresh()
}

onMounted(async () => {
  await loadAll()
  try {
    users.value = await usersApi.assignable()
  } catch {
    users.value = []
  }
})
</script>

<template>
  <AppShell>
    <div class="container">
      <p><a href="#" @click.prevent="router.push('/projets')">← Tous les projets</a></p>
      <p v-if="loading" class="muted">Chargement…</p>
      <p v-if="error" class="error-text">{{ error }}</p>

      <template v-if="project">
        <div class="toolbar">
          <div>
            <h1 class="page-title title">{{ project.name }}</h1>
            <span class="muted">{{ project.customerName }}</span>
          </div>
          <div class="filters">
            <RouterLink class="btn" :to="`/kanban?project=${project.id}`">Ouvrir le Kanban</RouterLink>
            <button class="btn" @click="editProject = true">Modifier</button>
          </div>
        </div>

        <div class="grid-cards">
          <div class="card info"><span class="label">Statut</span><StatusBadge :status="project.status" :labels="projectLabels" /></div>
          <div class="card info"><span class="label">Budget</span><strong>{{ project.budget === null ? '—' : money(project.budget) }}</strong></div>
          <div class="card info"><span class="label">Début</span><strong>{{ dateFr(project.startDate) }}</strong></div>
          <div class="card info"><span class="label">Échéance</span><strong>{{ dateFr(project.dueDate) }}</strong></div>
          <div class="card info">
            <span class="label">Progression</span>
            <div class="progress"><span :style="{ width: project.progress + '%' }" /></div>
            <strong>{{ project.progress }} %</strong>
          </div>
        </div>

        <p v-if="project.description" class="card desc">{{ project.description }}</p>

        <div class="toolbar section">
          <h2>Tâches ({{ tasks.length }})</h2>
          <button class="btn btn-primary" @click="openTask(null)">+ Nouvelle tâche</button>
        </div>

        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>Tâche</th><th>Priorité</th><th>Statut</th><th>Responsable</th><th class="right">Estimé</th><th class="right">Passé</th><th /></tr></thead>
            <tbody>
              <tr v-for="t in tasks" :key="t.id">
                <td><strong>{{ t.title }}</strong></td>
                <td><StatusBadge :status="t.priority" :labels="priorityLabels" /></td>
                <td>
                  <select class="inline" :value="t.status" :aria-label="`Statut de ${t.title}`"
                    @change="changeStatus(t, ($event.target as HTMLSelectElement).value as TaskStatus)">
                    <option v-for="(label, key) in taskLabels" :key="key" :value="key">{{ label }}</option>
                  </select>
                </td>
                <td>{{ t.assigneeName || '—' }}</td>
                <td class="right">{{ t.estimateHours === null ? '—' : t.estimateHours + ' h' }}</td>
                <td class="right">{{ t.spentHours }} h</td>
                <td>
                  <div class="actions">
                    <button class="btn btn-sm" @click="openTask(t)">Modifier</button>
                    <button class="btn btn-sm btn-danger" @click="removeTask(t)">Supprimer</button>
                  </div>
                </td>
              </tr>
              <tr v-if="tasks.length === 0"><td colspan="7" class="empty">Aucune tâche pour ce projet.</td></tr>
            </tbody>
          </table>
        </div>

        <section class="card comments">
          <h2>Commentaires</h2>
          <form @submit.prevent="addComment">
            <textarea v-model="newComment" rows="2" placeholder="Ajouter un commentaire…" />
            <button class="btn btn-primary" type="submit" :disabled="!newComment.trim()">Publier</button>
          </form>
          <p v-if="comments.length === 0" class="muted">Aucun commentaire.</p>
          <ul>
            <li v-for="c in comments" :key="c.id">
              <strong>{{ c.authorName }}</strong> <small class="muted">{{ dateTimeFr(c.createdAt) }}</small>
              <p>{{ c.content }}</p>
            </li>
          </ul>
        </section>

        <ProjectFormModal v-if="editProject" :project="project" @saved="onProjectSaved" @close="editProject = false" />
        <TaskFormModal v-if="taskModal" :project-id="project.id" :task="editingTask" :users="users"
          @saved="onTaskSaved" @close="taskModal = false" />
      </template>
    </div>
  </AppShell>
</template>

<style scoped>
.title { margin-bottom: 0.2rem; }
.info { display: flex; flex-direction: column; gap: 0.4rem; align-items: flex-start; }
.info .progress { width: 100%; }
.label { font-size: 0.78rem; color: var(--muted); }
.desc { white-space: pre-wrap; margin: 0 0 1rem; }
.section { margin-top: 1.5rem; }
.section h2 { margin: 0; font-size: 1.1rem; }
select.inline { width: auto; padding: 0.25rem 0.4rem; font-size: 0.82rem; }
.comments { margin-top: 1.5rem; }
.comments form { display: flex; gap: 0.5rem; align-items: flex-start; margin-bottom: 1rem; }
.comments ul { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.9rem; }
.comments li p { margin: 0.2rem 0 0; white-space: pre-wrap; }
</style>
