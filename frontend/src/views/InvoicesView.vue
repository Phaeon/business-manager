<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import BaseModal from '@/components/BaseModal.vue'
import DocumentDetail from '@/components/DocumentDetail.vue'
import DocumentForm from '@/components/DocumentForm.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { downloadFile, errorMessage } from '@/services/api'
import { customersApi, invoicesApi } from '@/services/resources'
import type { Customer } from '@/types/customer'
import type { DocumentFormValue, Invoice, InvoiceStatus } from '@/types/domain'
import { dateFr, invoiceLabels, money, todayIso } from '@/utils/format'

const route = useRoute()

const invoices = ref<Invoice[]>([])
const customers = ref<Customer[]>([])
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const loading = ref(false)
const error = ref('')
const search = ref(typeof route.query.q === 'string' ? route.query.q : '')
const status = ref<InvoiceStatus | ''>('')

const formOpen = ref(false)
const editing = ref<Invoice | null>(null)
const saving = ref(false)
const formError = ref('')
const viewing = ref<Invoice | null>(null)

const paying = ref<Invoice | null>(null)
const payAmount = ref(0)
const payDate = ref(todayIso())
const payMethod = ref('Virement')
const payReference = ref('')
const payError = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await invoicesApi.list({
      search: search.value || undefined, status: status.value || undefined, page: page.value, size: 10
    })
    invoices.value = result.content
    totalPages.value = result.totalPages
    totalElements.value = result.totalElements
  } catch (e) {
    error.value = errorMessage(e, 'Impossible de charger les factures.')
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

function openEdit(i: Invoice) {
  editing.value = i
  formError.value = ''
  formOpen.value = true
}

async function save(value: DocumentFormValue) {
  saving.value = true
  formError.value = ''
  const payload = {
    customerId: value.customerId, issueDate: value.issueDate, dueDate: value.secondDate,
    notes: value.notes, lines: value.lines
  }
  try {
    if (editing.value) await invoicesApi.update(editing.value.id, payload)
    else await invoicesApi.create(payload)
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

function openPay(i: Invoice) {
  paying.value = i
  payAmount.value = i.balance
  payDate.value = todayIso()
  payMethod.value = 'Virement'
  payReference.value = ''
  payError.value = ''
}

async function submitPayment() {
  if (!paying.value) return
  payError.value = ''
  try {
    await invoicesApi.pay(paying.value.id, {
      amount: payAmount.value, paidAt: payDate.value, method: payMethod.value, reference: payReference.value
    })
    paying.value = null
    await load()
  } catch (e) {
    payError.value = errorMessage(e)
  }
}

async function cancel(i: Invoice) {
  if (window.confirm(`Annuler la facture ${i.number} ?`)) await act(() => invoicesApi.cancel(i.id))
}

async function remove(i: Invoice) {
  if (window.confirm(`Supprimer la facture ${i.number} ?`)) await act(() => invoicesApi.remove(i.id))
}

function canPay(i: Invoice): boolean {
  return i.status === 'SENT' || i.status === 'OVERDUE'
}

function canCancel(i: Invoice): boolean {
  return i.status !== 'PAID' && i.status !== 'CANCELLED' && i.amountPaid === 0
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
      <h1 class="page-title">Factures</h1>

      <div class="toolbar">
        <div class="filters">
          <input v-model="search" placeholder="Numéro ou client…" @keyup.enter="applyFilters" />
          <select v-model="status" aria-label="Filtrer par statut" @change="applyFilters">
            <option value="">Tous les statuts</option>
            <option v-for="(label, key) in invoiceLabels" :key="key" :value="key">{{ label }}</option>
          </select>
          <button class="btn" @click="applyFilters">Rechercher</button>
        </div>
        <div class="filters">
          <button class="btn" @click="downloadFile('/invoices/export', 'factures.csv')">Exporter CSV</button>
          <button class="btn btn-primary" @click="openCreate">+ Nouvelle facture</button>
        </div>
      </div>

      <p v-if="error" class="error-text">{{ error }}</p>

      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr><th>Numéro</th><th>Client</th><th>Échéance</th><th>Statut</th><th class="right">Total TTC</th><th class="right">Reste dû</th><th /></tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="7" class="empty">Chargement…</td></tr>
            <tr v-for="i in invoices" :key="i.id">
              <td><a href="#" @click.prevent="viewing = i"><strong>{{ i.number }}</strong></a></td>
              <td>{{ i.customerName }}</td>
              <td>{{ dateFr(i.dueDate) }}</td>
              <td><StatusBadge :status="i.status" :labels="invoiceLabels" /></td>
              <td class="right nowrap">{{ money(i.totalTtc) }}</td>
              <td class="right nowrap">{{ i.status === 'CANCELLED' ? '—' : money(i.balance) }}</td>
              <td>
                <div class="actions">
                  <button v-if="i.status === 'DRAFT'" class="btn btn-sm" @click="openEdit(i)">Modifier</button>
                  <button v-if="i.status === 'DRAFT'" class="btn btn-sm" @click="act(() => invoicesApi.send(i.id))">Envoyer</button>
                  <button v-if="canPay(i)" class="btn btn-sm btn-primary" @click="openPay(i)">Régler</button>
                  <button class="btn btn-sm" @click="downloadFile(`/invoices/${i.id}/pdf`, `${i.number}.pdf`)">PDF</button>
                  <button v-if="canCancel(i)" class="btn btn-sm" @click="cancel(i)">Annuler</button>
                  <button v-if="i.status === 'DRAFT'" class="btn btn-sm btn-danger" @click="remove(i)">Supprimer</button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && invoices.length === 0"><td colspan="7" class="empty">Aucune facture.</td></tr>
          </tbody>
        </table>
      </div>

      <PaginationBar :page="page" :total-pages="totalPages" :total-elements="totalElements"
        @change="(p) => { page = p; load() }" />
    </div>

    <BaseModal v-if="formOpen" :title="editing ? `Modifier ${editing.number}` : 'Nouvelle facture'" wide @close="formOpen = false">
      <DocumentForm mode="invoice" :customers="customers" :initial="editing" :submitting="saving" :error="formError"
        @submit="save" @cancel="formOpen = false" />
    </BaseModal>

    <BaseModal v-if="viewing" :title="`Facture ${viewing.number}`" wide @close="viewing = null">
      <DocumentDetail :doc="viewing" />
    </BaseModal>

    <BaseModal v-if="paying" :title="`Régler la facture ${paying.number}`" @close="paying = null">
      <form id="pay-form" class="form-grid" @submit.prevent="submitPayment">
        <p class="span-2 muted">Reste à payer : <strong>{{ money(paying.balance) }}</strong></p>
        <label class="field">Montant (€)<input v-model.number="payAmount" type="number" min="0.01" step="0.01" required /></label>
        <label class="field">Date<input v-model="payDate" type="date" required /></label>
        <label class="field">
          Mode de paiement
          <select v-model="payMethod">
            <option>Virement</option><option>Chèque</option><option>Carte bancaire</option><option>Espèces</option><option>Autre</option>
          </select>
        </label>
        <label class="field">Référence<input v-model="payReference" /></label>
        <p v-if="payError" class="error-text span-2">{{ payError }}</p>
      </form>
      <template #footer>
        <button class="btn" type="button" @click="paying = null">Annuler</button>
        <button class="btn btn-primary" type="submit" form="pay-form">Enregistrer le paiement</button>
      </template>
    </BaseModal>
  </AppShell>
</template>
