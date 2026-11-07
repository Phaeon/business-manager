<script setup lang="ts">
import { toneOf } from '@/utils/format'

defineProps<{ status: string; labels: Record<string, string> }>()
</script>

<template>
  <span class="badge" :class="`badge-${toneOf(status)}`">{{ labels[status] ?? status }}</span>
</template>
