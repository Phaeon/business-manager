<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import TaskFormModal from '@/components/TaskFormModal.vue'
import { errorMessage } from '@/services/api'
import { projectsApi, tasksApi, usersApi } from '@/services/resources'
import type { Assignee, Project, Task, TaskStatus } from '@/types/domain'
import { priorityLabels, taskLabels } from '@/utils/format'

const COLUMNS: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE']

const route = useRoute()
const router = useRouter()

const projects = ref<Project[]>([])
const users = ref<Assignee[]>([])
const projectId = ref(typeof route.query.project === 'string' ? route.query.project : '')
const tasks = ref<Task[]>([])
const error = ref('')
const dragging = ref<string | null>(null)
const hoverColumn = ref<TaskStatus | null>(null)

const modalOpen = ref(false)
const editingTask = ref<Task | null>(null)
const defaultStatus = ref<TaskStatus>('TODO')

async function loadTasks() {
  if (!projectId.value) {
    tasks.value = []
    return
  }
  try {
    tasks.value = await projectsApi.tasks(projectId.value)
  } catch (e) {
    error.value = errorMessage(e, 'Impossible de charger les tâches.')
  }
}

function tasksOf(status: TaskStatus): Task[] {
  return tasks.value.filter((t) => t.status === status)
}

function onDragStart(e: DragEvent, task: Task) {
  dragging.value = task.id
  e.dataTransfer?.setData('text/plain', task.id)
  if (e.dataTransfer) e.dataTransfer.effectAllowed = 'move'
}

function onDragEnd() {
  dragging.value = null
  hoverColumn.value = null
}

async function onDrop(status: TaskStatus) {
  const id = dragging.value
  hoverColumn.value = null
  dragging.value = null
  const task = tasks.value.find((t) => t.id === id)
  if (!task || task.status === status) return

  // Mise à jour optimiste, annulée si le serveur refuse
  const previous = task.status
  task.status = status
  try {
    await tasksApi.updateStatus(task.id, status)
  } catch (e) {
    task.status = previous
    error.value = errorMessage(e)
  }
}

function openTask(task: Task | null, status: TaskStatus = 'TODO') {
  editingTask.value = task
  defaultStatus.value = status
  modalOpen.value = true
}

async function onSaved() {
  modalOpen.value = false
  await loadTasks()
}

watch(projectId, (id) => {
  router.replace({ query: id ? { project: id } : {} })
  loadTasks()
})

onMounted(async () => {
  try {
    projects.value = (await projectsApi.list({ size: 100, sort: 'name,asc' })).content
    users.value = await usersApi.assignable()
    if (!projectId.value && projects.value.length > 0) projectId.value = projects.value[0].id
    else await loadTasks()
  } catch (e) {
    error.value = errorMessage(e)
  }
})
</script>

<template>
  <AppShell>
    <div class="container">
      <h1 class="page-title">Tableau Kanban</h1>

      <div class="toolbar">
        <div class="filters">
          <select v-model="projectId" aria-label="Projet">
            <option value="" disabled>Choisir un projet…</option>
            <option v-for="p in projects" :key="p.id" :value="p.id">{{ p.name }} — {{ p.customerName }}</option>
          </select>
        </div>
        <button class="btn btn-primary" :disabled="!projectId" @click="openTask(null)">+ Nouvelle tâche</button>
      </div>

      <p v-if="error" class="error-text">{{ error }}</p>
      <p v-if="!projectId" class="muted">Sélectionnez un projet pour afficher ses tâches.</p>

      <div v-else class="board">
        <section v-for="status in COLUMNS" :key="status" class="column" :class="{ over: hoverColumn === status }"
          @dragover.prevent="hoverColumn = status" @dragleave="hoverColumn = null" @drop.prevent="onDrop(status)">
          <header>
            <h2>{{ taskLabels[status] }}</h2>
            <span class="count">{{ tasksOf(status).length }}</span>
          </header>

          <article v-for="t in tasksOf(status)" :key="t.id" class="task" :class="{ ghost: dragging === t.id }"
            draggable="true" @dragstart="onDragStart($event, t)" @dragend="onDragEnd" @dblclick="openTask(t)">
            <div class="task-title">{{ t.title }}</div>
            <div class="task-meta">
              <StatusBadge :status="t.priority" :labels="priorityLabels" />
              <span class="muted">{{ t.assigneeName || 'Non assignée' }}</span>
            </div>
            <div v-if="t.estimateHours !== null" class="muted hours">{{ t.spentHours }} / {{ t.estimateHours }} h</div>
            <button class="edit" type="button" aria-label="Modifier la tâche" @click="openTask(t)">✎</button>
          </article>

          <button class="add" type="button" @click="openTask(null, status)">+ Ajouter</button>
        </section>
      </div>
      <p v-if="projectId" class="muted tip">Glissez-déposez les cartes d'une colonne à l'autre ; la progression du projet se met à jour automatiquement.</p>
    </div>

    <TaskFormModal v-if="modalOpen && projectId" :project-id="projectId" :task="editingTask" :users="users"
      :default-status="defaultStatus" @saved="onSaved" @close="modalOpen = false" />
  </AppShell>
</template>

<style scoped>
.board { display: grid; grid-template-columns: repeat(4, minmax(220px, 1fr)); gap: 1rem; align-items: start; overflow-x: auto; padding-bottom: 0.5rem; }
.column { background: var(--hover); border: 2px dashed transparent; border-radius: 12px; padding: 0.75rem; min-height: 220px; display: flex; flex-direction: column; gap: 0.6rem; }
.column.over { border-color: var(--primary); }
header { display: flex; justify-content: space-between; align-items: center; }
h2 { margin: 0; font-size: 0.9rem; text-transform: uppercase; letter-spacing: 0.03em; color: var(--muted); }
.count { background: var(--surface); border-radius: 999px; padding: 0.05rem 0.55rem; font-size: 0.78rem; font-weight: 600; }
.task { position: relative; background: var(--surface); border: 1px solid var(--border); border-radius: 10px; padding: 0.7rem 0.8rem; cursor: grab; display: grid; gap: 0.45rem; }
.task:active { cursor: grabbing; }
.task.ghost { opacity: 0.4; }
.task-title { font-weight: 600; font-size: 0.9rem; padding-right: 1.3rem; }
.task-meta { display: flex; justify-content: space-between; align-items: center; gap: 0.5rem; font-size: 0.78rem; }
.hours { font-size: 0.76rem; }
.edit { position: absolute; top: 0.35rem; right: 0.4rem; background: none; border: none; color: var(--muted); cursor: pointer; }
.add { background: none; border: 1px dashed var(--border); border-radius: 8px; color: var(--muted); padding: 0.4rem; cursor: pointer; font: inherit; font-size: 0.82rem; }
.add:hover { color: var(--text); background: var(--surface); }
.tip { margin-top: 1rem; font-size: 0.85rem; }
@media (max-width: 900px) { .board { grid-template-columns: repeat(4, 260px); } }
</style>
