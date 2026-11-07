<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ data: { label: string; value: number; color: string }[] }>()

const R = 40
const C = 2 * Math.PI * R

const total = computed(() => props.data.reduce((sum, d) => sum + d.value, 0))
const segments = computed(() => {
  let offset = 0
  return props.data.map((d) => {
    const length = total.value === 0 ? 0 : (d.value / total.value) * C
    const segment = { ...d, dash: `${length} ${C - length}`, offset: -offset }
    offset += length
    return segment
  })
})
</script>

<template>
  <div class="donut">
    <svg viewBox="0 0 100 100" role="img" aria-label="Répartition" class="ring">
      <circle cx="50" cy="50" :r="R" class="track" />
      <circle v-for="s in segments" :key="s.label" cx="50" cy="50" :r="R" fill="none" :stroke="s.color"
        stroke-width="14" :stroke-dasharray="s.dash" :stroke-dashoffset="s.offset" transform="rotate(-90 50 50)" />
      <text x="50" y="54" text-anchor="middle" class="center">{{ total }}</text>
    </svg>
    <ul>
      <li v-for="d in data" :key="d.label">
        <span class="dot" :style="{ background: d.color }" />{{ d.label }}
        <strong>{{ d.value }}</strong>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.donut { display: flex; align-items: center; gap: 1.5rem; flex-wrap: wrap; }
.ring { width: 150px; height: 150px; }
.track { fill: none; stroke: var(--border); stroke-width: 14; }
.center { fill: var(--text); font-size: 16px; font-weight: 700; }
ul { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.45rem; font-size: 0.9rem; }
li { display: flex; align-items: center; gap: 0.5rem; }
li strong { margin-left: auto; padding-left: 1rem; }
.dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
</style>
