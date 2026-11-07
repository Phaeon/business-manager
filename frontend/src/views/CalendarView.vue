<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import BaseModal from '@/components/BaseModal.vue'
import { errorMessage } from '@/services/api'
import { appointmentsApi, calendarApi, customersApi } from '@/services/resources'
import type { Customer } from '@/types/customer'
import type { Appointment, CalendarEvent } from '@/types/domain'
import { toIsoDate } from '@/utils/format'

const router = useRouter()

const cursor = ref(new Date(new Date().getFullYear(), new Date().getMonth(), 1))
const events = ref<CalendarEvent[]>([])
const appointments = ref<Appointment[]>([])
const customers = ref<Customer[]>([])
const error = ref('')

const modalOpen = ref(false)
const editing = ref<Appointment | null>(null)
const form = ref({ title: '', startAt: '', endAt: '', customerId: '', location: '', notes: '' })
const formError = ref('')

const WEEKDAYS = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim']
const TYPE_LABELS: Record<string, string> = {
  APPOINTMENT: 'Rendez-vous', PROJECT_START: 'Début de projet', PROJECT_DUE: 'Échéance projet', INVOICE_DUE: 'Échéance facture'
}

const monthTitle = computed(() =>
  cursor.value.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' }))

/** 6 semaines complètes, lundi en premier. */
const days = computed(() => {
  const first = new Date(cursor.value)
  const offset = (first.getDay() + 6) % 7
  const start = new Date(first)
  start.setDate(first.getDate() - offset)
  return Array.from({ length: 42 }, (_, i) => {
    const d = new Date(start)
    d.setDate(start.getDate() + i)
    return { date: d, iso: toIsoDate(d), inMonth: d.getMonth() === cursor.value.getMonth() }
  })
})

const eventsByDay = computed(() => {
  const map = new Map<string, CalendarEvent[]>()
  for (const e of events.value) {
    const list = map.get(e.date) ?? []
    list.push(e)
    map.set(e.date, list)
  }
  return map
})

const todayIsoValue = toIsoDate(new Date())

async function load() {
  error.value = ''
  const from = days.value[0].iso
  const to = days.value[41].iso
  try {
    const [ev, ap] = await Promise.all([calendarApi.events(from, to), appointmentsApi.list(from, to)])
    events.value = ev
    appointments.value = ap
  } catch (e) {
    error.value = errorMessage(e, 'Impossible de charger le calendrier.')
  }
}

function shiftMonth(delta: number) {
  cursor.value = new Date(cursor.value.getFullYear(), cursor.value.getMonth() + delta, 1)
}

function goToday() {
  const now = new Date()
  cursor.value = new Date(now.getFullYear(), now.getMonth(), 1)
}

function openNew(iso: string) {
  editing.value = null
  form.value = { title: '', startAt: `${iso}T09:00`, endAt: `${iso}T10:00`, customerId: '', location: '', notes: '' }
  formError.value = ''
  modalOpen.value = true
}

function openEvent(e: CalendarEvent) {
  if (e.type === 'APPOINTMENT') {
    const appointment = appointments.value.find((a) => a.id === e.id)
    if (!appointment) return
    editing.value = appointment
    form.value = {
      title: appointment.title,
      startAt: appointment.startAt.slice(0, 16),
      endAt: appointment.endAt ? appointment.endAt.slice(0, 16) : '',
      customerId: appointment.customerId ?? '',
      location: appointment.location ?? '',
      notes: appointment.notes ?? ''
    }
    formError.value = ''
    modalOpen.value = true
  } else if (e.type === 'INVOICE_DUE') {
    router.push('/factures')
  } else {
    router.push(`/projets/${e.id}`)
  }
}

async function save() {
  formError.value = ''
  const payload = {
    title: form.value.title,
    startAt: form.value.startAt,
    endAt: form.value.endAt || null,
    customerId: form.value.customerId || null,
    location: form.value.location,
    notes: form.value.notes
  }
  try {
    if (editing.value) await appointmentsApi.update(editing.value.id, payload)
    else await appointmentsApi.create(payload)
    modalOpen.value = false
    await load()
  } catch (e) {
    formError.value = errorMessage(e)
  }
}

async function removeAppointment() {
  if (!editing.value || !window.confirm('Supprimer ce rendez-vous ?')) return
  try {
    await appointmentsApi.remove(editing.value.id)
    modalOpen.value = false
    await load()
  } catch (e) {
    formError.value = errorMessage(e)
  }
}

watch(cursor, load)
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
      <h1 class="page-title">Calendrier</h1>

      <div class="toolbar">
        <div class="filters">
          <button class="btn" aria-label="Mois précédent" @click="shiftMonth(-1)">‹</button>
          <strong class="month">{{ monthTitle }}</strong>
          <button class="btn" aria-label="Mois suivant" @click="shiftMonth(1)">›</button>
          <button class="btn" @click="goToday">Aujourd'hui</button>
        </div>
        <button class="btn btn-primary" @click="openNew(todayIsoValue)">+ Rendez-vous</button>
      </div>

      <p v-if="error" class="error-text">{{ error }}</p>

      <div class="legend muted">
        <span><i class="dot appointment" /> Rendez-vous</span>
        <span><i class="dot project" /> Projets</span>
        <span><i class="dot invoice" /> Factures</span>
      </div>

      <div class="grid">
        <div v-for="w in WEEKDAYS" :key="w" class="weekday">{{ w }}</div>
        <div v-for="d in days" :key="d.iso" class="day" :class="{ out: !d.inMonth, today: d.iso === todayIsoValue }"
          @click.self="openNew(d.iso)">
          <span class="num" @click="openNew(d.iso)">{{ d.date.getDate() }}</span>
          <button v-for="e in eventsByDay.get(d.iso) ?? []" :key="e.type + e.id" type="button" class="event"
            :class="e.type.toLowerCase()" :title="`${TYPE_LABELS[e.type]} — ${e.title}`" @click.stop="openEvent(e)">
            <span v-if="e.time" class="time">{{ e.time }}</span>{{ e.title }}
          </button>
        </div>
      </div>
    </div>

    <BaseModal v-if="modalOpen" :title="editing ? 'Modifier le rendez-vous' : 'Nouveau rendez-vous'" @close="modalOpen = false">
      <form id="appointment-form" class="form-grid" @submit.prevent="save">
        <label class="field span-2">Titre<input v-model="form.title" required /></label>
        <label class="field">Début<input v-model="form.startAt" type="datetime-local" required /></label>
        <label class="field">Fin<input v-model="form.endAt" type="datetime-local" /></label>
        <label class="field span-2">
          Client
          <select v-model="form.customerId">
            <option value="">— Aucun —</option>
            <option v-for="c in customers" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </label>
        <label class="field span-2">Lieu<input v-model="form.location" /></label>
        <label class="field span-2">Notes<textarea v-model="form.notes" rows="2" /></label>
        <p v-if="formError" class="error-text span-2">{{ formError }}</p>
      </form>
      <template #footer>
        <button v-if="editing" class="btn btn-danger" type="button" @click="removeAppointment">Supprimer</button>
        <button class="btn" type="button" @click="modalOpen = false">Annuler</button>
        <button class="btn btn-primary" type="submit" form="appointment-form">Enregistrer</button>
      </template>
    </BaseModal>
  </AppShell>
</template>

<style scoped>
.month { min-width: 150px; text-align: center; text-transform: capitalize; }
.legend { display: flex; gap: 1.25rem; font-size: 0.82rem; margin-bottom: 0.75rem; }
.dot { display: inline-block; width: 9px; height: 9px; border-radius: 50%; margin-right: 0.3rem; }
.appointment { background: var(--primary); }
.project { background: var(--warning); }
.invoice { background: var(--danger); }
.grid { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; background: var(--surface); }
.weekday { padding: 0.5rem; text-align: center; font-size: 0.75rem; font-weight: 600; text-transform: uppercase; color: var(--muted); background: var(--hover); border-bottom: 1px solid var(--border); }
.day { min-height: 108px; padding: 0.3rem; border-right: 1px solid var(--border); border-bottom: 1px solid var(--border); display: flex; flex-direction: column; gap: 0.2rem; cursor: pointer; min-width: 0; }
.day:nth-child(7n + 7) { border-right: none; }
.day.out { background: var(--hover); opacity: 0.6; }
.day.today .num { background: var(--primary); color: #fff; }
.num { align-self: flex-start; font-size: 0.8rem; font-weight: 600; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; border-radius: 50%; }
.event { text-align: left; border: none; border-radius: 5px; padding: 0.15rem 0.35rem; font: inherit; font-size: 0.72rem; cursor: pointer; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #fff; }
.event.appointment { background: var(--primary); }
.event.project_start, .event.project_due { background: var(--warning); }
.event.invoice_due { background: var(--danger); }
.time { font-weight: 700; margin-right: 0.3rem; }
@media (max-width: 700px) { .day { min-height: 70px; } .event { font-size: 0.65rem; } }
</style>
