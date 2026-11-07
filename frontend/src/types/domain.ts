export type QuoteStatus = 'DRAFT' | 'SENT' | 'ACCEPTED' | 'REJECTED' | 'INVOICED'
export type InvoiceStatus = 'DRAFT' | 'SENT' | 'PAID' | 'OVERDUE' | 'CANCELLED'
export type ProjectStatus = 'PLANNED' | 'IN_PROGRESS' | 'ON_HOLD' | 'COMPLETED'
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH'
export type DocumentCategory = 'CONTRACT' | 'SIGNED_QUOTE' | 'INVOICE' | 'OTHER'

/** Ligne saisie dans les formulaires de devis / facture. */
export interface LineItem {
  description: string
  quantity: number
  unitPrice: number
  vatRate: number
  discountPercent: number
}

export interface LineItemView extends LineItem {
  totalHt: number
  totalVat: number
  totalTtc: number
}

export interface Quote {
  id: string
  number: string
  customerId: string
  customerName: string
  status: QuoteStatus
  issueDate: string
  validUntil: string | null
  notes: string | null
  lines: LineItemView[]
  totalHt: number
  totalVat: number
  totalTtc: number
  createdAt: string
}

export interface QuotePayload {
  customerId: string
  issueDate: string
  validUntil: string | null
  notes: string
  lines: LineItem[]
}

export interface Payment {
  id: string
  amount: number
  paidAt: string
  method: string | null
  reference: string | null
}

export interface PaymentPayload {
  amount: number
  paidAt: string
  method: string
  reference: string
}

export interface Invoice {
  id: string
  number: string
  customerId: string
  customerName: string
  quoteId: string | null
  quoteNumber: string | null
  status: InvoiceStatus
  issueDate: string
  dueDate: string
  notes: string | null
  lines: LineItemView[]
  totalHt: number
  totalVat: number
  totalTtc: number
  amountPaid: number
  balance: number
  payments: Payment[]
  createdAt: string
}

export interface InvoicePayload {
  customerId: string
  issueDate: string
  dueDate: string
  notes: string
  lines: LineItem[]
}

export interface Project {
  id: string
  name: string
  description: string | null
  customerId: string
  customerName: string
  budget: number | null
  startDate: string | null
  dueDate: string | null
  progress: number
  status: ProjectStatus
  createdAt: string
}

export interface ProjectPayload {
  name: string
  description: string
  customerId: string
  budget: number | null
  startDate: string | null
  dueDate: string | null
  status: ProjectStatus
}

export interface ProjectComment {
  id: string
  authorName: string
  content: string
  createdAt: string
}

export interface Task {
  id: string
  projectId: string
  projectName: string
  title: string
  description: string | null
  priority: TaskPriority
  status: TaskStatus
  estimateHours: number | null
  spentHours: number
  assigneeId: string | null
  assigneeName: string | null
  createdAt: string
}

export interface TaskPayload {
  projectId: string
  title: string
  description: string
  priority: TaskPriority
  status: TaskStatus
  estimateHours: number | null
  spentHours: number
  assigneeId: string | null
}

export interface Appointment {
  id: string
  title: string
  startAt: string
  endAt: string | null
  customerId: string | null
  customerName: string | null
  location: string | null
  notes: string | null
}

export interface AppointmentPayload {
  title: string
  startAt: string
  endAt: string | null
  customerId: string | null
  location: string
  notes: string
}

export interface AppNotification {
  id: string
  type: string
  message: string
  link: string | null
  read: boolean
  createdAt: string
}

export interface NotificationsPayload {
  items: AppNotification[]
  unreadCount: number
}

export interface DocumentItem {
  id: string
  fileName: string
  contentType: string | null
  size: number
  category: DocumentCategory
  customerId: string | null
  customerName: string | null
  uploadedBy: string | null
  createdAt: string
}

export interface MonthlyRevenue {
  month: string
  amount: number
}

export interface DashboardSummary {
  revenue: number
  pendingAmount: number
  quotesCount: number
  pendingInvoices: number
  paidInvoices: number
  newCustomers: number
  activeCustomers: number
  quotesConverted: number
  conversionRate: number
  monthlyRevenue: MonthlyRevenue[]
  projectDistribution: Record<string, number>
  recentActivity: AppNotification[]
}

export interface SearchResult {
  type: 'CUSTOMER' | 'QUOTE' | 'INVOICE' | 'PROJECT'
  id: string
  title: string
  subtitle: string
  link: string
}

export interface CalendarEvent {
  type: 'APPOINTMENT' | 'PROJECT_START' | 'PROJECT_DUE' | 'INVOICE_DUE'
  id: string
  title: string
  date: string
  time: string | null
}

export interface HistoryEntry {
  type: 'QUOTE' | 'INVOICE' | 'PROJECT'
  id: string
  label: string
  date: string
  status: string
  amount: number | null
}

export interface Assignee {
  id: string
  name: string
}

export interface ManagedUser {
  id: string
  firstName: string
  lastName: string
  email: string
  role: 'ADMIN' | 'EMPLOYEE' | 'ACCOUNTING'
  enabled: boolean
}

export interface UserPayload {
  firstName: string
  lastName: string
  email: string
  password: string
  role: 'ADMIN' | 'EMPLOYEE' | 'ACCOUNTING'
}

/** Valeur émise par le formulaire commun devis / facture. */
export interface DocumentFormValue {
  customerId: string
  issueDate: string
  /** "valable jusqu'au" pour un devis, échéance pour une facture */
  secondDate: string
  notes: string
  lines: LineItem[]
}
