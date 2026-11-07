<script setup lang="ts">
import type { Invoice, Quote } from '@/types/domain'
import { dateFr, money } from '@/utils/format'

defineProps<{ doc: Quote | Invoice }>()

function isInvoice(doc: Quote | Invoice): doc is Invoice {
  return 'payments' in doc
}
</script>

<template>
  <div>
    <p class="meta">
      <strong>{{ doc.customerName }}</strong><br />
      Émis le {{ dateFr(doc.issueDate) }}
      <template v-if="isInvoice(doc)"> · Échéance {{ dateFr(doc.dueDate) }}</template>
      <template v-else-if="doc.validUntil"> · Valable jusqu'au {{ dateFr(doc.validUntil) }}</template>
      <template v-if="isInvoice(doc) && doc.quoteNumber"> · Issu du devis {{ doc.quoteNumber }}</template>
    </p>

    <div class="table-wrap">
      <table class="table">
        <thead>
          <tr><th>Description</th><th class="right">Qté</th><th class="right">PU HT</th><th class="right">TVA</th><th class="right">Total HT</th></tr>
        </thead>
        <tbody>
          <tr v-for="(line, i) in doc.lines" :key="i">
            <td>{{ line.description }}</td>
            <td class="right">{{ line.quantity }}</td>
            <td class="right">{{ money(line.unitPrice) }}</td>
            <td class="right">{{ line.vatRate }} %</td>
            <td class="right">{{ money(line.totalHt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <dl class="totals">
      <div><dt>Total HT</dt><dd>{{ money(doc.totalHt) }}</dd></div>
      <div><dt>TVA</dt><dd>{{ money(doc.totalVat) }}</dd></div>
      <div class="strong"><dt>Total TTC</dt><dd>{{ money(doc.totalTtc) }}</dd></div>
      <template v-if="isInvoice(doc)">
        <div><dt>Déjà réglé</dt><dd>{{ money(doc.amountPaid) }}</dd></div>
        <div class="strong"><dt>Reste à payer</dt><dd>{{ money(doc.balance) }}</dd></div>
      </template>
    </dl>

    <template v-if="isInvoice(doc) && doc.payments.length">
      <h3>Historique des paiements</h3>
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>Date</th><th>Mode</th><th>Référence</th><th class="right">Montant</th></tr></thead>
          <tbody>
            <tr v-for="p in doc.payments" :key="p.id">
              <td>{{ dateFr(p.paidAt) }}</td><td>{{ p.method || '—' }}</td><td>{{ p.reference || '—' }}</td>
              <td class="right">{{ money(p.amount) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <p v-if="doc.notes" class="muted notes">{{ doc.notes }}</p>
  </div>
</template>

<style scoped>
.meta { margin-top: 0; line-height: 1.6; }
.totals { margin: 1rem 0 0 auto; width: 280px; }
.totals div { display: flex; justify-content: space-between; padding: 0.2rem 0; }
.totals dt { color: var(--muted); }
.totals dd { margin: 0; }
.totals .strong { font-weight: 700; border-top: 1px solid var(--border); margin-top: 0.3rem; padding-top: 0.4rem; }
.totals .strong dt { color: var(--text); }
h3 { margin: 1.25rem 0 0.6rem; font-size: 0.95rem; }
.notes { white-space: pre-wrap; margin-top: 1rem; }
</style>
