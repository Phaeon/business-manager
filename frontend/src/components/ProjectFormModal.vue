<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { customersApi, projectsApi } from '@/services/resources'
import { errorMessage } from '@/services/api'
import type { Customer } from '@/types/customer'
import type { Project, ProjectStatus } from '@/types/domain'
import { projectLabels } from '@/utils/format'
import BaseModal from './BaseModal.vue'

const props = defineProps<{ project?: Project | null }>()
const emit = defineEmits<{ saved: [project: Project]; close: [] }>()

const customers = ref<Customer[]>([])
const name = ref(props.project?.name ?? '')
const description = ref(props.project?.description ?? '')
const customerId = ref(props.project?.customerId ?? '')
const budget = ref<number | null>(props.project?.budget ?? null)
const startDate = ref(props.project?.startDate ?? '')
const dueDate = ref(props.project?.dueDate ?? '')
const status = ref<ProjectStatus>(props.project?.status ?? 'PLANNED')
const saving = ref(false)
const error = ref('')

onMounted(async () => {
  try {
    customers.value = (await customersApi.list({ size: 200, sort: 'name,asc' })).content
  } catch (e) {
    error.value = errorMessage(e, 'Impossible de charger les clients.')
  }
})

async function save() {
  error.value = ''
  saving.value = true
  const payload = {
    name: name.value,
    description: description.value,
    customerId: customerId.value,
    budget: budget.value,
    startDate: startDate.value || null,
    dueDate: dueDate.value || null,
    status: status.value
  }
  try {
    const saved = props.project
      ? await projectsApi.update(props.project.id, payload)
      : await projectsApi.create(payload)
    emit('saved', saved)
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseModal :title="project ? 'Modifier le projet' : 'Nouveau projet'" @close="emit('close')">
    <form id="project-form" class="form-grid" @submit.prevent="save">
      <label class="field span-2">Nom du projet<input v-model="name" required /></label>
      <label class="field span-2">
        Client
        <select v-model="customerId" required>
          <option value="" disabled>Choisir un client…</option>
          <option v-for="c in customers" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </label>
      <label class="field span-2">Description<textarea v-model="description" rows="3" /></label>
      <label class="field">Budget (€)<input v-model.number="budget" type="number" min="0" step="0.01" /></label>
      <label class="field">
        Statut
        <select v-model="status">
          <option v-for="(label, key) in projectLabels" :key="key" :value="key">{{ label }}</option>
        </select>
      </label>
      <label class="field">Début<input v-model="startDate" type="date" /></label>
      <label class="field">Échéance<input v-model="dueDate" type="date" /></label>
      <p v-if="error" class="error-text span-2">{{ error }}</p>
    </form>
    <template #footer>
      <button class="btn" type="button" @click="emit('close')">Annuler</button>
      <button class="btn btn-primary" type="submit" form="project-form" :disabled="saving">Enregistrer</button>
    </template>
  </BaseModal>
</template>
