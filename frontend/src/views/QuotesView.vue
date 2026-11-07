<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import BaseModal from '@/components/BaseModal.vue'
import DocumentDetail from '@/components/DocumentDetail.vue'
import DocumentForm from '@/components/DocumentForm.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { errorMessage } from '@/services/api'
import { customersApi, invoicesApi, quotesApi } from '@/services/resources'
import type { Customer } from '@/types/customer'
import type { DocumentFormValue, Quote, QuoteStatus } from '@/types/domain'
import { dateFr, money, quoteLabels } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const quotes = ref<Quote[]>([])
const customers = ref<Customer[]>([])
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const loading = ref(false)
const error = ref('')
const search = ref(typeof route.query.q === 'string' ? route.query.q : '')
const status = ref<QuoteStatus | ''>('')

const formOpen = ref(false)
const editing = ref<Quote | null>(null)
const saving = ref(false)
const formError = ref('')
const viewing = ref<Quote | null>(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await quotesApi.list({
      search: search.value || undefined, status: status.value || undefined, page: page.value, size: 10
    })
    quotes.value = result.content
    totalPages.value = result.totalPages
    totalElements.value = result.totalElements
  } catch (e) {
    error.value = errorMessage(e, 'Impossible de charger les devis.')
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
  formError.value = ''
  formOpen.value = true
}

function openEdit(q: Quote) {
  editing.value = q
  formError.value = ''
  formOpen.value = true
}

async function save(value: DocumentFormValue) {
  saving.value = true
  formError.value = ''
  const payload = {
    customerId: value.customerId, issueDate: value.issueDate, validUntil: value.secondDate || null,
    notes: value.notes, lines: value.lines
  }
  try {
    if (editing.value) await quotesApi.update(editing.value.id, payload)
    else await quotesApi.create(payload)
    formOpen.value = false
    await load()
  } catch (e) {
    formError.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}

async function act(action: () => Promise<unknown>) {
  error.value = ''
  try {
    await action()
    await load()
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function convert(q: Quote) {
  try {
    const invoice = await invoicesApi.fromQuote(q.id)
    router.push({ path: '/factures', query: { q: invoice.number } })
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function remove(q: Quote) {
  if (window.confirm(`Supprimer le devis ${q.number} ?`)) await act(() => quotesApi.remove(q.id))
}

watch(() => route.query.q, (q) => {
  search.value = typeof q === 'string' ? q : ''
  applyFilters()
})

onMounted(async () => {
  load()
  try {
    customers.value = (await customersApi.list({ size: 200, sort: 'name,asc' })).content
  } catch (e) {
    error.value = errorMessage(e, 'Impossible de charger les clients.')
  }
})
</script>

<template>
  <AppShell>
    <div class="container">
      <h1 class="page-title">Devis</h1>

      <div class="toolbar">
        <div class="filters">
          <input v-model="search" placeholder="Numéro ou client…" @keyup.enter="applyFilters" />
          <select v-model="status" aria-label="Filtrer par statut" @change="applyFilters">
            <option value="">Tous les statuts</option>
            <option v-for="(label, key) in quoteLabels" :key="key" :value="key">{{ label }}</option>
          </select>
          <button class="btn" @click="applyFilters">Rechercher</button>
        </div>
        <button class="btn btn-primary" @click="openCreate">+ Nouveau devis</button>
      </div>

      <p v-if="error" class="error-text">{{ error }}</p>

      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr><th>Numéro</th><th>Client</th><th>Émis le</th><th>Validité</th><th>Statut</th><th class="right">Total TTC</th><th /></tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="7" class="empty">Chargement…</td></tr>
            <tr v-for="q in quotes" :key="q.id">
              <td><a href="#" @click.prevent="viewing = q"><strong>{{ q.number }}</strong></a></td>
              <td>{{ q.customerName }}</td>
              <td>{{ dateFr(q.issueDate) }}</td>
              <td>{{ dateFr(q.validUntil) }}</td>
              <td><StatusBadge :status="q.status" :labels="quoteLabels" /></td>
              <td class="right nowrap">{{ money(q.totalTtc) }}</td>
              <td>
                <div class="actions">
                  <button v-if="q.status === 'DRAFT' || q.status === 'SENT'" class="btn btn-sm" @click="openEdit(q)">Modifier</button>
                  <button v-if="q.status === 'DRAFT'" class="btn btn-sm" @click="act(() => quotesApi.send(q.id))">Envoyer</button>
                  <button v-if="q.status === 'DRAFT' || q.status === 'SENT'" class="btn btn-sm" @click="act(() => quotesApi.accept(q.id))">Valider</button>
                  <button v-if="q.status === 'DRAFT' || q.status === 'SENT'" class="btn btn-sm" @click="act(() => quotesApi.reject(q.id))">Refuser</button>
                  <button v-if="q.status === 'ACCEPTED'" class="btn btn-sm btn-primary" @click="convert(q)">Facturer</button>
                  <button class="btn btn-sm" @click="act(() => quotesApi.duplicate(q.id))">Dupliquer</button>
                  <button v-if="q.status === 'DRAFT' || q.status === 'REJECTED'" class="btn btn-sm btn-danger" @click="remove(q)">Supprimer</button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && quotes.length === 0"><td colspan="7" class="empty">Aucun devis.</td></tr>
          </tbody>
        </table>
      </div>

      <PaginationBar :page="page" :total-pages="totalPages" :total-elements="totalElements"
        @change="(p) => { page = p; load() }" />
    </div>

    <BaseModal v-if="formOpen" :title="editing ? `Modifier ${editing.number}` : 'Nouveau devis'" wide @close="formOpen = false">
      <DocumentForm mode="quote" :customers="customers" :initial="editing" :submitting="saving" :error="formError"
        @submit="save" @cancel="formOpen = false" />
    </BaseModal>

    <BaseModal v-if="viewing" :title="`Devis ${viewing.number}`" wide @close="viewing = null">
      <DocumentDetail :doc="viewing" />
    </BaseModal>
  </AppShell>
</template>
