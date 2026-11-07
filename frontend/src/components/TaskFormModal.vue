<script setup lang="ts">
import { ref } from 'vue'
import { tasksApi } from '@/services/resources'
import { errorMessage } from '@/services/api'
import type { Assignee, Task, TaskPriority, TaskStatus } from '@/types/domain'
import { priorityLabels, taskLabels } from '@/utils/format'
import BaseModal from './BaseModal.vue'

const props = defineProps<{
  projectId: string
  task?: Task | null
  users: Assignee[]
  defaultStatus?: TaskStatus
}>()
const emit = defineEmits<{ saved: [task: Task]; close: [] }>()

const title = ref(props.task?.title ?? '')
const description = ref(props.task?.description ?? '')
const priority = ref<TaskPriority>(props.task?.priority ?? 'MEDIUM')
const status = ref<TaskStatus>(props.task?.status ?? props.defaultStatus ?? 'TODO')
const estimate = ref<number | null>(props.task?.estimateHours ?? null)
const spent = ref<number>(props.task?.spentHours ?? 0)
const assigneeId = ref<string>(props.task?.assigneeId ?? '')
const saving = ref(false)
const error = ref('')

async function save() {
  error.value = ''
  saving.value = true
  const payload = {
    projectId: props.projectId,
    title: title.value,
    description: description.value,
    priority: priority.value,
    status: status.value,
    estimateHours: estimate.value,
    spentHours: spent.value,
    assigneeId: assigneeId.value || null
  }
  try {
    const saved = props.task ? await tasksApi.update(props.task.id, payload) : await tasksApi.create(payload)
    emit('saved', saved)
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseModal :title="task ? 'Modifier la tâche' : 'Nouvelle tâche'" @close="emit('close')">
    <form id="task-form" class="form-grid" @submit.prevent="save">
      <label class="field span-2">Titre<input v-model="title" required /></label>
      <label class="field span-2">Description<textarea v-model="description" rows="3" /></label>
      <label class="field">
        Priorité
        <select v-model="priority">
          <option v-for="(label, key) in priorityLabels" :key="key" :value="key">{{ label }}</option>
        </select>
      </label>
      <label class="field">
        Statut
        <select v-model="status">
          <option v-for="(label, key) in taskLabels" :key="key" :value="key">{{ label }}</option>
        </select>
      </label>
      <label class="field">Estimation (h)<input v-model.number="estimate" type="number" min="0" step="0.5" /></label>
      <label class="field">Temps passé (h)<input v-model.number="spent" type="number" min="0" step="0.5" /></label>
      <label class="field span-2">
        Responsable
        <select v-model="assigneeId">
          <option value="">— Non assignée —</option>
          <option v-for="u in users" :key="u.id" :value="u.id">{{ u.name }}</option>
        </select>
      </label>
      <p v-if="error" class="error-text span-2">{{ error }}</p>
    </form>
    <template #footer>
      <button class="btn" type="button" @click="emit('close')">Annuler</button>
      <button class="btn btn-primary" type="submit" form="task-form" :disabled="saving">Enregistrer</button>
    </template>
  </BaseModal>
</template>
