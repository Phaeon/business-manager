import type { LineItem } from '@/types/domain'

/**
 * Miroir du calcul backend (PricingCalculator.java) : sert uniquement à
 * l'aperçu en direct dans les formulaires. Le backend reste la référence.
 */
const round2 = (n: number): number => Math.round((n + Number.EPSILON) * 100) / 100

export function lineHt(line: LineItem): number {
  return round2((line.quantity * line.unitPrice * (100 - line.discountPercent)) / 100)
}

export function lineVat(line: LineItem): number {
  return round2((lineHt(line) * line.vatRate) / 100)
}

export interface Totals {
  ht: number
  vat: number
  ttc: number
}

export function computeTotals(lines: LineItem[]): Totals {
  let ht = 0
  let vat = 0
  for (const line of lines) {
    ht += lineHt(line)
    vat += lineVat(line)
  }
  return { ht: round2(ht), vat: round2(vat), ttc: round2(ht + vat) }
}
