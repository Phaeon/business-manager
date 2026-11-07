<script setup lang="ts">
import { computed } from 'vue'
import type { LineItem } from '@/types/domain'
import { computeTotals, lineHt } from '@/utils/pricing'
import { money } from '@/utils/format'

const lines = defineModel<LineItem[]>({ required: true })

const totals = computed(() => computeTotals(lines.value))

function addLine() {
  lines.value.push({ description: '', quantity: 1, unitPrice: 0, vatRate: 20, discountPercent: 0 })
}

function removeLine(index: number) {
  lines.value.splice(index, 1)
}
</script>

<template>
  <div class="editor">
    <div class="head">
      <span>Description</span><span>Qté</span><span>PU HT (€)</span><span>TVA %</span><span>Remise %</span>
      <span class="right">Total HT</span><span />
    </div>

    <div v-for="(line, index) in lines" :key="index" class="row">
      <input v-model="line.description" placeholder="Prestation, produit…" aria-label="Description" />
      <input v-model.number="line.quantity" type="number" min="0.01" step="0.01" aria-label="Quantité" />
      <input v-model.number="line.unitPrice" type="number" min="0" step="0.01" aria-label="Prix unitaire" />
      <input v-model.number="line.vatRate" type="number" min="0" max="100" step="0.1" aria-label="TVA" />
      <input v-model.number="line.discountPercent" type="number" min="0" max="100" step="0.1" aria-label="Remise" />
      <span class="right total">{{ money(lineHt(line)) }}</span>
      <button type="button" class="btn btn-sm btn-danger" :disabled="lines.length <= 1" aria-label="Supprimer la ligne"
        @click="removeLine(index)">×</button>
    </div>

    <button type="button" class="btn btn-sm add" @click="addLine">+ Ajouter une ligne</button>

    <dl class="totals">
      <div><dt>Total HT</dt><dd>{{ money(totals.ht) }}</dd></div>
      <div><dt>TVA</dt><dd>{{ money(totals.vat) }}</dd></div>
      <div class="ttc"><dt>Total TTC</dt><dd>{{ money(totals.ttc) }}</dd></div>
    </dl>
  </div>
</template>

<style scoped>
.head, .row {
  display: grid; grid-template-columns: minmax(160px, 3fr) 0.8fr 1fr 0.8fr 0.9fr 1fr 36px; gap: 0.5rem; align-items: center;
}
.head { font-size: 0.72rem; text-transform: uppercase; color: var(--muted); font-weight: 600; margin-bottom: 0.35rem; }
.row { margin-bottom: 0.4rem; }
.total { font-size: 0.85rem; white-space: nowrap; }
.add { margin-top: 0.4rem; }
.totals { margin: 1rem 0 0 auto; width: 260px; }
.totals div { display: flex; justify-content: space-between; padding: 0.2rem 0; font-size: 0.9rem; }
.totals dt { color: var(--muted); }
.totals dd { margin: 0; font-weight: 500; }
.totals .ttc { border-top: 1px solid var(--border); margin-top: 0.3rem; padding-top: 0.4rem; font-size: 1rem; }
.totals .ttc dt, .totals .ttc dd { color: var(--text); font-weight: 700; }
@media (max-width: 800px) {
  .head { display: none; }
  .row { grid-template-columns: 1fr 1fr; }
}
</style>
