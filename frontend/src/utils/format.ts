const euro = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' })

export const money = (value: number | null | undefined): string => euro.format(value ?? 0)

function parseDate(value: string): Date {
  // Une date seule (YYYY-MM-DD) est lue en heure locale pour éviter tout décalage d'un jour
  return value.length === 10 ? new Date(`${value}T00:00:00`) : new Date(value)
}

export const dateFr = (value?: string | null): string =>
  value ? new Intl.DateTimeFormat('fr-FR').format(parseDate(value)) : '—'

export const dateTimeFr = (value?: string | null): string =>
  value ? new Intl.DateTimeFormat('fr-FR', { dateStyle: 'short', timeStyle: 'short' }).format(parseDate(value)) : '—'

export function fileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} o`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`
  return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`
}

const pad = (n: number): string => String(n).padStart(2, '0')

/** Format YYYY-MM-DD en heure locale. */
export const toIsoDate = (d: Date): string => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
export const todayIso = (): string => toIsoDate(new Date())

export function addDaysIso(days: number, from: Date = new Date()): string {
  const d = new Date(from)
  d.setDate(d.getDate() + days)
  return toIsoDate(d)
}

/** "2026-03" -> "mars 26" */
export function monthLabel(month: string): string {
  const [year, m] = month.split('-').map(Number)
  const label = new Date(year, m - 1, 1).toLocaleDateString('fr-FR', { month: 'short' })
  return `${label.replace('.', '')} ${String(year).slice(2)}`
}

export type Tone = 'neutral' | 'info' | 'success' | 'warning' | 'danger'

export const quoteLabels: Record<string, string> = {
  DRAFT: 'Brouillon', SENT: 'Envoyé', ACCEPTED: 'Accepté', REJECTED: 'Refusé', INVOICED: 'Facturé'
}
export const invoiceLabels: Record<string, string> = {
  DRAFT: 'Brouillon', SENT: 'Envoyée', PAID: 'Payée', OVERDUE: 'En retard', CANCELLED: 'Annulée'
}
export const projectLabels: Record<string, string> = {
  PLANNED: 'Planifié', IN_PROGRESS: 'En cours', ON_HOLD: 'En pause', COMPLETED: 'Terminé'
}
export const taskLabels: Record<string, string> = {
  TODO: 'À faire', IN_PROGRESS: 'En cours', IN_REVIEW: 'En revue', DONE: 'Terminée'
}
export const priorityLabels: Record<string, string> = { LOW: 'Basse', MEDIUM: 'Moyenne', HIGH: 'Haute' }
export const roleLabels: Record<string, string> = {
  ADMIN: 'Administrateur', EMPLOYEE: 'Employé', ACCOUNTING: 'Comptabilité'
}
export const categoryLabels: Record<string, string> = {
  CONTRACT: 'Contrat', SIGNED_QUOTE: 'Devis signé', INVOICE: 'Facture', OTHER: 'Autre'
}
export const searchTypeLabels: Record<string, string> = {
  CUSTOMER: 'Client', QUOTE: 'Devis', INVOICE: 'Facture', PROJECT: 'Projet'
}
export const historyTypeLabels: Record<string, string> = { QUOTE: 'Devis', INVOICE: 'Facture', PROJECT: 'Projet' }

const tones: Record<string, Tone> = {
  DRAFT: 'neutral', PLANNED: 'neutral', TODO: 'neutral', LOW: 'neutral',
  SENT: 'info', INVOICED: 'info', IN_PROGRESS: 'info',
  ACCEPTED: 'success', PAID: 'success', COMPLETED: 'success', DONE: 'success',
  IN_REVIEW: 'warning', ON_HOLD: 'warning', MEDIUM: 'warning',
  REJECTED: 'danger', CANCELLED: 'danger', OVERDUE: 'danger', HIGH: 'danger'
}
export const toneOf = (status: string): Tone => tones[status] ?? 'neutral'
