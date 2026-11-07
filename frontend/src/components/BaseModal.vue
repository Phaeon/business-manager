<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'

defineProps<{ title: string; wide?: boolean }>()
const emit = defineEmits<{ close: [] }>()

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') emit('close')
}
onMounted(() => document.addEventListener('keydown', onKey))
onUnmounted(() => document.removeEventListener('keydown', onKey))
</script>

<template>
  <div class="overlay" @mousedown.self="emit('close')">
    <div class="modal" :class="{ wide }" role="dialog" aria-modal="true" :aria-label="title">
      <header>
        <h2>{{ title }}</h2>
        <button class="close" type="button" aria-label="Fermer" @click="emit('close')">×</button>
      </header>
      <div class="body"><slot /></div>
      <footer v-if="$slots.footer"><slot name="footer" /></footer>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed; inset: 0; z-index: 50; background: rgba(15, 23, 42, 0.55);
  display: flex; align-items: flex-start; justify-content: center; padding: 3rem 1rem; overflow-y: auto;
}
.modal {
  width: 100%; max-width: 560px; background: var(--surface); color: var(--text);
  border: 1px solid var(--border); border-radius: 14px; box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25);
}
.modal.wide { max-width: 900px; }
header { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.25rem; border-bottom: 1px solid var(--border); }
h2 { margin: 0; font-size: 1.1rem; }
.close { background: none; border: none; font-size: 1.6rem; line-height: 1; color: var(--muted); cursor: pointer; }
.body { padding: 1.25rem; }
footer { display: flex; justify-content: flex-end; gap: 0.5rem; padding: 0.9rem 1.25rem; border-top: 1px solid var(--border); }
</style>
