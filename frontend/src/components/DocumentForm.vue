<script setup lang="ts">
import { ref } from 'vue'
import type { Customer } from '@/types/customer'
import type { DocumentFormValue, Invoice, LineItem, Quote } from '@/types/domain'
import { addDaysIso, todayIso } from '@/utils/format'
import LineItemsEditor from './LineItemsEditor.vue'

const props = defineProps<{
  mode: 'quote' | 'invoice'
  customers: Customer[]
  initial?: Quote | Invoice | null
  submitting?: boolean
  error?: string
}>()

const emit = defineEmits<{ submit: [value: DocumentFormValue]; cancel: [] }>()

function initialSecondDate(): string {
  const doc = props.initial
  if (doc) {
    return props.mode === 'quote' ? ((doc as Quote).validUntil ?? '') : (doc as Invoice).dueDate
  }
  return addDaysIso(30)
}

const customerId = ref(props.initial?.customerId ?? '')
const issueDate = ref(props.initial?.issueDate ?? todayIso())
const secondDate = ref(initialSecondDate())
const notes = ref(props.initial?.notes ?? '')
const lines = ref<LineItem[]>(
  props.initial
    ? props.initial.lines.map((l) => ({
        description: l.description, quantity: l.quantity, unitPrice: l.unitPrice,
        vatRate: l.vatRate, discountPercent: l.discountPercent
      }))
    : [{ description: '', quantity: 1, unitPrice: 0, vatRate: 20, discountPercent: 0 }]
)
const localError = ref('')

function submit() {
  localError.value = ''
  if (!customerId.value) {
    localError.value = 'Veuillez choisir un client.'
    return
  }
  if (lines.value.some((l) => !l.description.trim())) {
    localError.value = 'Chaque ligne doit avoir une description.'
    return
  }
  if (props.mode === 'invoice' && !secondDate.value) {
    localError.value = "Veuillez renseigner l'échéance."
    return
  }
  emit('submit', {
    customerId: customerId.value,
    issueDate: issueDate.value,
    secondDate: secondDate.value,
    notes: notes.value,
    lines: lines.value
  })
}
</script>

<template>
  <form @submit.prevent="submit">
    <div class="form-grid">
      <label class="field span-2">
        Client
        <select v-model="customerId" required>
          <option value="" disabled>Choisir un client…</option>
          <option v-for="c in customers" :key="c.id" :value="c.id">
            {{ c.name }}{{ c.company ? ` — ${c.company}` : '' }}
          </option>
        </select>
      </label>
      <label class="field">
        Date d'émission
        <input v-model="issueDate" type="date" required />
      </label>
      <label class="field">
        {{ mode === 'quote' ? "Valable jusqu'au" : 'Échéance de paiement' }}
        <input v-model="secondDate" type="date" :required="mode === 'invoice'" />
      </label>
    </div>

    <h3 class="section">Lignes</h3>
    <LineItemsEditor v-model="lines" />

    <label class="field notes">
      Notes (affichées sur le document)
      <textarea v-model="notes" rows="2" />
    </label>

    <p v-if="localError || error" class="error-text">{{ localError || error }}</p>

    <div class="buttons">
      <button type="button" class="btn" @click="emit('cancel')">Annuler</button>
      <button type="submit" class="btn btn-primary" :disabled="submitting">
        {{ submitting ? 'Enregistrement…' : 'Enregistrer' }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.section { margin: 1.25rem 0 0.6rem; font-size: 0.95rem; }
.notes { margin-top: 1rem; }
.buttons { display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 1.25rem; }
</style>
