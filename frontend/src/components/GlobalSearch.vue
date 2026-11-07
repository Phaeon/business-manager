<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { searchApi } from '@/services/resources'
import type { SearchResult } from '@/types/domain'
import { searchTypeLabels } from '@/utils/format'

const router = useRouter()
const root = ref<HTMLElement | null>(null)
const query = ref('')
const results = ref<SearchResult[]>([])
const open = ref(false)
const searched = ref(false)
let timer: ReturnType<typeof setTimeout> | undefined

watch(query, (value) => {
  clearTimeout(timer)
  if (value.trim().length < 2) {
    results.value = []
    searched.value = false
    return
  }
  timer = setTimeout(async () => {
    try {
      results.value = await searchApi.search(value.trim())
    } catch {
      results.value = []
    }
    searched.value = true
    open.value = true
  }, 250)
})

function go(result: SearchResult) {
  open.value = false
  query.value = ''
  router.push(result.link)
}

function onDocumentClick(e: MouseEvent) {
  if (root.value && !root.value.contains(e.target as Node)) open.value = false
}
onMounted(() => document.addEventListener('click', onDocumentClick))
onUnmounted(() => {
  document.removeEventListener('click', onDocumentClick)
  clearTimeout(timer)
})
</script>

<template>
  <div ref="root" class="search">
    <input v-model="query" type="search" placeholder="Rechercher clients, devis, factures, projets…"
      aria-label="Recherche globale" @focus="open = true" />
    <ul v-if="open && searched" class="results">
      <li v-if="results.length === 0" class="empty">Aucun résultat</li>
      <li v-for="r in results" :key="r.type + r.id">
        <button type="button" @click="go(r)">
          <span class="type">{{ searchTypeLabels[r.type] }}</span>
          <span class="title">{{ r.title }}</span>
          <span class="sub">{{ r.subtitle }}</span>
        </button>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.search { position: relative; flex: 1; max-width: 460px; }
.results {
  position: absolute; top: calc(100% + 6px); left: 0; right: 0; z-index: 40; list-style: none; margin: 0; padding: 0.3rem;
  background: var(--surface); border: 1px solid var(--border); border-radius: 10px; box-shadow: 0 12px 30px rgba(0, 0, 0, 0.18);
  max-height: 380px; overflow-y: auto;
}
.results button {
  width: 100%; display: grid; grid-template-columns: 62px 1fr; gap: 0 0.5rem; text-align: left;
  background: none; border: none; color: var(--text); padding: 0.45rem 0.5rem; border-radius: 6px; cursor: pointer; font: inherit;
}
.results button:hover { background: var(--hover); }
.type { grid-row: span 2; font-size: 0.7rem; text-transform: uppercase; color: var(--primary); font-weight: 700; align-self: center; }
.title { font-weight: 600; font-size: 0.88rem; }
.sub { color: var(--muted); font-size: 0.78rem; }
.empty { padding: 0.7rem; color: var(--muted); font-size: 0.88rem; }
</style>
