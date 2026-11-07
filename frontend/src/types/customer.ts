export interface Customer {
  id: string
  name: string
  company: string | null
  email: string
  phone: string | null
  address: string | null
  notes: string | null
  archived: boolean
  createdAt: string
  updatedAt: string
}

export interface CustomerPayload {
  name: string
  company: string
  email: string
  phone: string
  address: string
  notes: string
}

/** Page renvoyée par Spring Data. */
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
