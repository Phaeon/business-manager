<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppShell from '@/components/AppShell.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { downloadFile, errorMessage } from '@/services/api'
import { customersApi, documentsApi } from '@/services/resources'
import type { Customer } from '@/types/customer'
import type { DocumentCategory, DocumentItem } from '@/types/domain'
import { categoryLabels, dateFr, fileSize } from '@/utils/format'

const documents = ref<DocumentItem[]>([])
const customers = ref<Customer[]>([])
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const error = ref('')
const loading = ref(false)

const file = ref<File | null>(null)
const category = ref<DocumentCategory>('OTHER')
const customerId = ref('')
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

async function load() {
  loading.value = true
  try {
    const result = await documentsApi.list({ page: page.value, size: 15 })
    documents.value = result.content
    totalPages.value = result.totalPages
    totalElements.value = result.totalElements
  } catch (e) {
    error.value = errorMessage(e, 'Impossible de charger les documents.')
  } finally {
    loading.value = false
  }
}

function onFileChange(e: Event) {
  file.value = (e.target as HTMLInputElement).files?.[0] ?? null
}

async function upload() {
  if (!file.value) return
  error.value = ''
  uploading.value = true
  const form = new FormData()
  form.append('file', file.value)
  form.append('category', category.value)
  if (customerId.value) form.append('customerId', customerId.value)
  try {
    await documentsApi.upload(form)
    file.value = null
    if (fileInput.value) fileInput.value.value = ''
    page.value = 0
    await load()
  } catch (e) {
    error.value = errorMessage(e, "Échec de l'envoi du fichier.")
  } finally {
    uploading.value = false
  }
}

async function remove(d: DocumentItem) {
  if (!window.confirm(`Supprimer « ${d.fileName} » ?`)) return
  try {
    await documentsApi.remove(d.id)
    await load()
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function download(d: DocumentItem) {
  try {
    await downloadFile(`/documents/${d.id}/download`, d.fileName)
  } catch (e) {
    error.value = errorMessage(e, 'Téléchargement impossible.')
  }
}

onMounted(async () => {
  load()
  try {
    customers.value = (await customersApi.list({ size: 200, sort: 'name,asc' })).content
  } catch {
    customers.value = []
  }
})
</script>

<template>
  <AppShell>
    <div class="container">
      <h1 class="page-title">Documents</h1>

      <form class="card upload" @submit.prevent="upload">
        <label class="field grow">Fichier (10 Mo max)<input ref="fileInput" type="file" required @change="onFileChange" /></label>
        <label class="field">
          Catégorie
          <select v-model="category">
            <option v-for="(label, key) in categoryLabels" :key="key" :value="key">{{ label }}</option>
          </select>
        </label>
        <label class="field">
          Client (facultatif)
          <select v-model="customerId">
            <option value="">— Aucun —</option>
            <option v-for="c in customers" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </label>
        <button class="btn btn-primary" type="submit" :disabled="!file || uploading">
          {{ uploading ? 'Envoi…' : 'Téléverser' }}
        </button>
      </form>

      <p v-if="error" class="error-text">{{ error }}</p>

      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>Fichier</th><th>Catégorie</th><th>Client</th><th>Taille</th><th>Ajouté</th><th /></tr></thead>
          <tbody>
            <tr v-if="loading"><td colspan="6" class="empty">Chargement…</td></tr>
            <tr v-for="d in documents" :key="d.id">
              <td><strong>{{ d.fileName }}</strong><br /><small class="muted">par {{ d.uploadedBy || '—' }}</small></td>
              <td>{{ categoryLabels[d.category] }}</td>
              <td>{{ d.customerName || '—' }}</td>
              <td class="nowrap">{{ fileSize(d.size) }}</td>
              <td>{{ dateFr(d.createdAt) }}</td>
              <td>
                <div class="actions">
                  <button class="btn btn-sm" @click="download(d)">Télécharger</button>
                  <button class="btn btn-sm btn-danger" @click="remove(d)">Supprimer</button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && documents.length === 0"><td colspan="6" class="empty">Aucun document.</td></tr>
          </tbody>
        </table>
      </div>

      <PaginationBar :page="page" :total-pages="totalPages" :total-elements="totalElements"
        @change="(p) => { page = p; load() }" />
    </div>
  </AppShell>
</template>

<style scoped>
.upload { display: flex; gap: 1rem; align-items: flex-end; flex-wrap: wrap; margin-bottom: 1.25rem; }
.grow { flex: 1; min-width: 220px; }
.upload .field select { min-width: 160px; }
</style>
