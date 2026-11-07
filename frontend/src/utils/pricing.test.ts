import { describe, expect, it } from 'vitest'
import { computeTotals, lineHt, lineVat } from './pricing'

const line = (quantity: number, unitPrice: number, vatRate: number, discountPercent = 0) => ({
  description: 'Prestation',
  quantity,
  unitPrice,
  vatRate,
  discountPercent
})

describe('pricing', () => {
  it('applique quantité et remise sur le HT', () => {
    expect(lineHt(line(3, 100, 20, 10))).toBe(270)
  })

  it('calcule la TVA sur le montant remisé', () => {
    expect(lineVat(line(3, 100, 20, 10))).toBe(54)
  })

  it('additionne des lignes à taux de TVA différents', () => {
    const totals = computeTotals([line(1, 1000, 20), line(2, 50, 5.5)])
    expect(totals).toEqual({ ht: 1100, vat: 205.5, ttc: 1305.5 })
  })

  it('arrondit au centime supérieur à 0,5', () => {
    expect(lineHt(line(3, 0.335, 0))).toBe(1.01)
  })

  it('renvoie zéro sans ligne', () => {
    expect(computeTotals([])).toEqual({ ht: 0, vat: 0, ttc: 0 })
  })
})
