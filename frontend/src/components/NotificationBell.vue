<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { notificationsApi } from '@/services/resources'
import type { AppNotification } from '@/types/domain'
import { dateTimeFr } from '@/utils/format'

const router = useRouter()
const root = ref<HTMLElement | null>(null)
const items = ref<AppNotification[]>([])
const unread = ref(0)
const open = ref(false)
let interval: ReturnType<typeof setInterval> | undefined

async function load() {
  try {
    const payload = await notificationsApi.list()
    items.value = payload.items
    unread.value = payload.unreadCount
  } catch {
    /* silencieux : la cloche ne doit jamais bloquer l'interface */
  }
}

function toggle() {
  open.value = !open.value
  if (open.value) load()
}

async function select(n: AppNotification) {
  open.value = false
  if (!n.read) {
    await notificationsApi.markRead(n.id).catch(() => undefined)
    await load()
  }
  if (n.link) router.push(n.link)
}

async function markAll() {
  await notificationsApi.markAllRead().catch(() => undefined)
  await load()
}

function onDocumentClick(e: MouseEvent) {
  if (root.value && !root.value.contains(e.target as Node)) open.value = false
}

onMounted(() => {
  load()
  interval = setInterval(load, 60_000)
  document.addEventListener('click', onDocumentClick)
})
onUnmounted(() => {
  clearInterval(interval)
  document.removeEventListener('click', onDocumentClick)
})
</script>

<template>
  <div ref="root" class="bell">
    <button type="button" class="btn icon" aria-label="Notifications" @click="toggle">
      🔔<span v-if="unread > 0" class="count">{{ unread > 99 ? '99+' : unread }}</span>
    </button>
    <div v-if="open" class="panel">
      <div class="panel-head">
        <strong>Notifications</strong>
        <button v-if="unread > 0" class="link" type="button" @click="markAll">Tout marquer comme lu</button>
      </div>
      <p v-if="items.length === 0" class="empty">Rien à signaler.</p>
      <ul>
        <li v-for="n in items" :key="n.id">
          <button type="button" :class="{ unread: !n.read }" @click="select(n)">
            <span>{{ n.message }}</span>
            <small>{{ dateTimeFr(n.createdAt) }}</small>
          </button>
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.bell { position: relative; }
.icon { position: relative; padding: 0.4rem 0.6rem; }
.count {
  position: absolute; top: -6px; right: -6px; min-width: 18px; height: 18px; padding: 0 4px; border-radius: 9px;
  background: var(--danger); color: #fff; font-size: 0.68rem; font-weight: 700; display: flex; align-items: center; justify-content: center;
}
.panel {
  position: absolute; right: 0; top: calc(100% + 8px); width: 340px; max-width: 90vw; z-index: 40;
  background: var(--surface); border: 1px solid var(--border); border-radius: 12px; box-shadow: 0 12px 30px rgba(0, 0, 0, 0.18);
}
.panel-head { display: flex; justify-content: space-between; align-items: center; padding: 0.7rem 0.9rem; border-bottom: 1px solid var(--border); }
.link { background: none; border: none; color: var(--primary); font-size: 0.78rem; cursor: pointer; }
ul { list-style: none; margin: 0; padding: 0.25rem; max-height: 360px; overflow-y: auto; }
li button {
  width: 100%; text-align: left; background: none; border: none; color: var(--text); padding: 0.55rem 0.65rem;
  border-radius: 8px; cursor: pointer; font: inherit; font-size: 0.85rem; display: flex; flex-direction: column; gap: 0.15rem;
}
li button:hover { background: var(--hover); }
li button.unread { font-weight: 600; }
li button.unread { border-left: 3px solid var(--primary); }
li small { color: var(--muted); font-weight: 400; }
.empty { padding: 1rem; color: var(--muted); font-size: 0.88rem; margin: 0; }
</style>
