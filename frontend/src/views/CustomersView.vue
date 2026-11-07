<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import BaseModal from '@/components/BaseModal.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { downloadFile, errorMessage } from '@/services/api'
import { customersApi } from '@/services/resources'
import { useAuthStore } from '@/stores/auth'
import type { Customer, CustomerPayload } from '@/types/customer'
import type { HistoryEntry } from '@/types/domain'
import { dateFr, historyTypeLabels, invoiceLabels, money, projectLabels, quoteLabels } from '@/utils/format'

const route = useRoute()
const authStore = useAuthStore()
const canWrite = authStore.hasAnyRole('ADMIN', 'ACCOUNTING')
const isAdmin = authStore.hasAnyRole('ADMIN')

const customers = ref<Customer[]>([])
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const loading = ref(false)
const error = ref('')
const search = ref(typeof route.query.q === 'string' ? route.query.q : '')
const showArchived = ref(false)

const editing = ref<Customer | null>(null)
const formOpen = ref(false)
const form = ref<CustomerPayload>(emptyForm())
const saving = ref(false)
const formError = ref('')

const historyOf = ref<Customer | null>(null)
const history = ref<HistoryEntry[]>([])

function emptyForm(): CustomerPayload {
  return { name: '', company: '', email: '', phone: '', address: '', notes: '' }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await customersApi.list({
      search: search.value || undefined, archived: showArchived.value, page: page.value, size: 10
    })
    customers.value = result.content
    totalPages.value = result.totalPages
    totalElements.value = result.totalElements
  } catch (e) {
    error.value = errorMessage(e, 'Impossible de charger les clients.')
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 0
  load()
}

function openCreate() {
  editing.value = null
  form.value = emptyForm()
  formError.value = ''
  formOpen.value = true
}

function openEdit(c: Customer) {
  editing.value = c
  form.value = {
    name: c.name, company: c.company ?? '', email: c.email, phone: c.phone ?? '',
    address: c.address ?? '', notes: c.notes ?? ''
  }
  formError.value = ''
  formOpen.value = true
}

async function save() {
  saving.value = true
  formError.value = ''
  try {
    if (editing.value) await customersApi.update(editing.value.id, form.value)
    else await customersApi.create(form.value)
    formOpen.value = false
    await load()
  } catch (e) {
    formError.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}

async function toggleArchive(c: Customer) {
  try {
    if (c.archived) await customersApi.unarchive(c.id)
    else await customersApi.archive(c.id)
    await load()
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function remove(c: Customer) {
  if (!window.confirm(`Supprimer définitivement ${c.name} ? Préférez l'archivage si des documents lui sont liés.`)) return
  try {
    await customersApi.remove(c.id)
    await load()
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function openHistory(c: Customer) {
  historyOf.value = c
  history.value = []
  try {
    history.value = await customersApi.history(c.id)
  } catch (e) {
    error.value = errorMessage(e)
  }
}

const statusLabels: Record<string, string> = { ...quoteLabels, ...invoiceLabels, ...projectLabels }
const historyLink = computed(() => (e: HistoryEntry) =>
  e.type === 'PROJECT' ? `/projets/${e.id}` : e.type === 'QUOTE' ? '/devis' : '/factures')

watch(() => route.query.q, (q) => {
  search.value = typeof q === 'string' ? q : ''
  applyFilters()
})
onMounted(load)
</script>

<template>
  <AppShell>
    <div class="container">
      <h1 class="page-title">Clients</h1>

      <div class="toolbar">
        <div class="filters">
          <input v-model="search" placeholder="Rechercher (nom, société, email)…" @keyup.enter="applyFilters" />
          <label class="check"><input v-model="showArchived" type="checkbox" @change="applyFilters" /> Archivés</label>
          <button class="btn" @click="applyFilters">Rechercher</button>
        </div>
        <div class="filters">
          <button v-if="canWrite" class="btn" @click="downloadFile('/customers/export', 'clients.csv')">Exporter CSV</button>
          <button v-if="canWrite" class="btn btn-primary" @click="openCreate">+ Nouveau client</button>
        </div>
      </div>

      <p v-if="error" class="error-text">{{ error }}</p>

      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>Nom</th><th>Société</th><th>Email</th><th>Téléphone</th><th /></tr></thead>
          <tbody>
            <tr v-if="loading"><td colspan="5" class="empty">Chargement…</td></tr>
            <tr v-for="c in customers" :key="c.id">
              <td><strong>{{ c.name }}</strong></td>
              <td>{{ c.company || '—' }}</td>
              <td>{{ c.email }}</td>
              <td>{{ c.phone || '—' }}</td>
              <td>
                <div class="actions">
                  <button class="btn btn-sm" @click="openHistory(c)">Historique</button>
                  <template v-if="canWrite">
                    <button class="btn btn-sm" @click="openEdit(c)">Modifier</button>
                    <button class="btn btn-sm" @click="toggleArchive(c)">{{ c.archived ? 'Désarchiver' : 'Archiver' }}</button>
                  </template>
                  <button v-if="isAdmin" class="btn btn-sm btn-danger" @click="remove(c)">Supprimer</button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && customers.length === 0"><td colspan="5" class="empty">Aucun client.</td></tr>
          </tbody>
        </table>
      </div>

      <PaginationBar :page="page" :total-pages="totalPages" :total-elements="totalElements"
        @change="(p) => { page = p; load() }" />
    </div>

    <BaseModal v-if="formOpen" :title="editing ? 'Modifier le client' : 'Nouveau client'" @close="formOpen = false">
      <form id="customer-form" class="form-grid" @submit.prevent="save">
        <label class="field">Nom<input v-model="form.name" required /></label>
        <label class="field">Société<input v-model="form.company" /></label>
        <label class="field">Email<input v-model="form.email" type="email" required /></label>
        <label class="field">Téléphone<input v-model="form.phone" /></label>
        <label class="field span-2">Adresse<input v-model="form.address" /></label>
        <label class="field span-2">Notes<textarea v-model="form.notes" rows="3" /></label>
        <p v-if="formError" class="error-text span-2">{{ formError }}</p>
      </form>
      <template #footer>
        <button class="btn" type="button" @click="formOpen = false">Annuler</button>
        <button class="btn btn-primary" type="submit" form="customer-form" :disabled="saving">Enregistrer</button>
      </template>
    </BaseModal>

    <BaseModal v-if="historyOf" :title="`Historique — ${historyOf.name}`" wide @close="historyOf = null">
      <p v-if="history.length === 0" class="muted">Aucun devis, facture ou projet pour ce client.</p>
      <div v-else class="table-wrap">
        <table class="table">
          <thead><tr><th>Type</th><th>Élément</th><th>Date</th><th>Statut</th><th class="right">Montant</th></tr></thead>
          <tbody>
            <tr v-for="e in history" :key="e.type + e.id">
              <td>{{ historyTypeLabels[e.type] }}</td>
              <td><RouterLink :to="historyLink(e)" @click="historyOf = null">{{ e.label }}</RouterLink></td>
              <td>{{ dateFr(e.date) }}</td>
              <td><StatusBadge :status="e.status" :labels="statusLabels" /></td>
              <td class="right">{{ e.amount === null ? '—' : money(e.amount) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </BaseModal>
  </AppShell>
</template>

<style scoped>
.check { display: inline-flex; align-items: center; gap: 0.35rem; font-size: 0.88rem; }
</style>
