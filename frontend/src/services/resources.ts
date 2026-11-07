import api from './api'
import type { Customer, CustomerPayload, Page } from '@/types/customer'
import type {
  Appointment, AppointmentPayload, Assignee, CalendarEvent, DashboardSummary, DocumentItem,
  HistoryEntry, Invoice, InvoicePayload, ManagedUser, NotificationsPayload, PaymentPayload,
  Project, ProjectComment, ProjectPayload, Quote, QuotePayload, SearchResult, Task, TaskPayload,
  TaskStatus, UserPayload
} from '@/types/domain'

type Params = Record<string, unknown>

function crud<T, P>(path: string) {
  return {
    list: (params?: Params) => api.get<Page<T>>(path, { params }).then((r) => r.data),
    get: (id: string) => api.get<T>(`${path}/${id}`).then((r) => r.data),
    create: (payload: P) => api.post<T>(path, payload).then((r) => r.data),
    update: (id: string, payload: P) => api.put<T>(`${path}/${id}`, payload).then((r) => r.data),
    remove: (id: string) => api.delete(`${path}/${id}`).then(() => undefined)
  }
}

export const customersApi = {
  ...crud<Customer, CustomerPayload>('/customers'),
  archive: (id: string) => api.patch(`/customers/${id}/archive`).then(() => undefined),
  unarchive: (id: string) => api.patch(`/customers/${id}/unarchive`).then(() => undefined),
  history: (id: string) => api.get<HistoryEntry[]>(`/customers/${id}/history`).then((r) => r.data)
}

export const quotesApi = {
  ...crud<Quote, QuotePayload>('/quotes'),
  send: (id: string) => api.post<Quote>(`/quotes/${id}/send`).then((r) => r.data),
  accept: (id: string) => api.post<Quote>(`/quotes/${id}/accept`).then((r) => r.data),
  reject: (id: string) => api.post<Quote>(`/quotes/${id}/reject`).then((r) => r.data),
  duplicate: (id: string) => api.post<Quote>(`/quotes/${id}/duplicate`).then((r) => r.data)
}

export const invoicesApi = {
  ...crud<Invoice, InvoicePayload>('/invoices'),
  fromQuote: (quoteId: string) => api.post<Invoice>(`/invoices/from-quote/${quoteId}`).then((r) => r.data),
  send: (id: string) => api.post<Invoice>(`/invoices/${id}/send`).then((r) => r.data),
  pay: (id: string, payload: PaymentPayload) => api.post<Invoice>(`/invoices/${id}/payments`, payload).then((r) => r.data),
  cancel: (id: string) => api.post<Invoice>(`/invoices/${id}/cancel`).then((r) => r.data)
}

export const projectsApi = {
  ...crud<Project, ProjectPayload>('/projects'),
  tasks: (id: string) => api.get<Task[]>(`/projects/${id}/tasks`).then((r) => r.data),
  comments: (id: string) => api.get<ProjectComment[]>(`/projects/${id}/comments`).then((r) => r.data),
  addComment: (id: string, content: string) =>
    api.post<ProjectComment>(`/projects/${id}/comments`, { content }).then((r) => r.data)
}

export const tasksApi = {
  mine: () => api.get<Task[]>('/tasks/mine').then((r) => r.data),
  create: (payload: TaskPayload) => api.post<Task>('/tasks', payload).then((r) => r.data),
  update: (id: string, payload: TaskPayload) => api.put<Task>(`/tasks/${id}`, payload).then((r) => r.data),
  updateStatus: (id: string, status: TaskStatus) =>
    api.patch<Task>(`/tasks/${id}/status`, { status }).then((r) => r.data),
  remove: (id: string) => api.delete(`/tasks/${id}`).then(() => undefined)
}

export const appointmentsApi = {
  list: (from: string, to: string) => api.get<Appointment[]>('/appointments', { params: { from, to } }).then((r) => r.data),
  create: (payload: AppointmentPayload) => api.post<Appointment>('/appointments', payload).then((r) => r.data),
  update: (id: string, payload: AppointmentPayload) => api.put<Appointment>(`/appointments/${id}`, payload).then((r) => r.data),
  remove: (id: string) => api.delete(`/appointments/${id}`).then(() => undefined)
}

export const documentsApi = {
  list: (params?: Params) => api.get<Page<DocumentItem>>('/documents', { params }).then((r) => r.data),
  upload: (form: FormData) => api.post<DocumentItem>('/documents', form).then((r) => r.data),
  remove: (id: string) => api.delete(`/documents/${id}`).then(() => undefined)
}

export const statsApi = {
  dashboard: () => api.get<DashboardSummary>('/stats/dashboard').then((r) => r.data)
}

export const searchApi = {
  search: (q: string) => api.get<SearchResult[]>('/search', { params: { q } }).then((r) => r.data)
}

export const calendarApi = {
  events: (from: string, to: string) => api.get<CalendarEvent[]>('/calendar', { params: { from, to } }).then((r) => r.data)
}

export const notificationsApi = {
  list: () => api.get<NotificationsPayload>('/notifications').then((r) => r.data),
  markRead: (id: string) => api.patch(`/notifications/${id}/read`).then(() => undefined),
  markAllRead: () => api.post('/notifications/read-all').then(() => undefined)
}

export const usersApi = {
  list: () => api.get<ManagedUser[]>('/users').then((r) => r.data),
  assignable: () => api.get<Assignee[]>('/users/assignable').then((r) => r.data),
  create: (payload: UserPayload) => api.post<ManagedUser>('/users', payload).then((r) => r.data),
  setEnabled: (id: string, value: boolean) =>
    api.patch<ManagedUser>(`/users/${id}/enabled`, null, { params: { value } }).then((r) => r.data),
  changePassword: (currentPassword: string, newPassword: string) =>
    api.post('/auth/change-password', { currentPassword, newPassword }).then(() => undefined)
}
