<script setup lang="ts">
import { computed } from 'vue'
import { money } from '@/utils/format'

const props = defineProps<{ data: { label: string; value: number }[] }>()

const W = 640
const H = 230
const PAD_TOP = 14
const PAD_BOTTOM = 28

const max = computed(() => Math.max(...props.data.map((d) => d.value), 1))
const slot = computed(() => W / Math.max(props.data.length, 1))
const bars = computed(() =>
  props.data.map((d, i) => {
    const h = (d.value / max.value) * (H - PAD_TOP - PAD_BOTTOM)
    return { ...d, x: i * slot.value + slot.value * 0.18, w: slot.value * 0.64, y: H - PAD_BOTTOM - h, h }
  })
)
</script>

<template>
  <svg :viewBox="`0 0 ${W} ${H}`" role="img" aria-label="Graphique en barres" class="chart">
    <line :x1="0" :x2="W" :y1="H - PAD_BOTTOM" :y2="H - PAD_BOTTOM" class="axis" />
    <g v-for="b in bars" :key="b.label">
      <rect :x="b.x" :y="b.y" :width="b.w" :height="Math.max(b.h, 0)" rx="4" class="bar">
        <title>{{ b.label }} : {{ money(b.value) }}</title>
      </rect>
      <text :x="b.x + b.w / 2" :y="H - 10" text-anchor="middle" class="label">{{ b.label }}</text>
    </g>
  </svg>
</template>

<style scoped>
.chart { width: 100%; height: auto; }
.axis { stroke: var(--border); stroke-width: 1; }
.bar { fill: var(--primary); }
.bar:hover { filter: brightness(1.2); }
.label { fill: var(--muted); font-size: 11px; }
</style>
