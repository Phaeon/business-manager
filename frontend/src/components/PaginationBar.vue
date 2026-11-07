<script setup lang="ts">
defineProps<{ page: number; totalPages: number; totalElements: number }>()
const emit = defineEmits<{ change: [page: number] }>()
</script>

<template>
  <div v-if="totalPages > 1" class="pager">
    <span class="muted">{{ totalElements }} résultat(s)</span>
    <div class="buttons">
      <button class="btn btn-sm" :disabled="page === 0" @click="emit('change', page - 1)">‹ Précédent</button>
      <span class="muted">Page {{ page + 1 }} / {{ totalPages }}</span>
      <button class="btn btn-sm" :disabled="page + 1 >= totalPages" @click="emit('change', page + 1)">Suivant ›</button>
    </div>
  </div>
</template>

<style scoped>
.pager { display: flex; justify-content: space-between; align-items: center; margin-top: 1rem; flex-wrap: wrap; gap: 0.5rem; }
.buttons { display: flex; align-items: center; gap: 0.75rem; }
</style>
