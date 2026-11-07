<script setup lang="ts">
import { ref } from 'vue'
import AppShell from '@/components/AppShell.vue'
import { errorMessage } from '@/services/api'
import { usersApi } from '@/services/resources'
import { useAuthStore } from '@/stores/auth'
import { roleLabels } from '@/utils/format'

const authStore = useAuthStore()
const current = ref('')
const next = ref('')
const confirm = ref('')
const message = ref('')
const error = ref('')
const saving = ref(false)

async function submit() {
  message.value = ''
  error.value = ''
  if (next.value !== confirm.value) {
    error.value = 'La confirmation ne correspond pas au nouveau mot de passe.'
    return
  }
  saving.value = true
  try {
    await usersApi.changePassword(current.value, next.value)
    message.value = 'Mot de passe modifié.'
    current.value = ''
    next.value = ''
    confirm.value = ''
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <AppShell>
    <div class="container narrow">
      <h1 class="page-title">Mon profil</h1>

      <section class="card">
        <h2>Informations</h2>
        <p><strong>{{ authStore.user?.firstName }} {{ authStore.user?.lastName }}</strong></p>
        <p class="muted">{{ authStore.user?.email }} · {{ authStore.user ? roleLabels[authStore.user.role] : '' }}</p>
      </section>

      <form class="card pwd" @submit.prevent="submit">
        <h2>Changer mon mot de passe</h2>
        <label class="field">Mot de passe actuel<input v-model="current" type="password" required autocomplete="current-password" /></label>
        <label class="field">Nouveau mot de passe (8 caractères min.)<input v-model="next" type="password" minlength="8" required autocomplete="new-password" /></label>
        <label class="field">Confirmation<input v-model="confirm" type="password" minlength="8" required autocomplete="new-password" /></label>
        <p v-if="error" class="error-text">{{ error }}</p>
        <p v-if="message" class="ok">{{ message }}</p>
        <button class="btn btn-primary" type="submit" :disabled="saving">Mettre à jour</button>
      </form>
    </div>
  </AppShell>
</template>

<style scoped>
.narrow { max-width: 620px; }
.pwd { display: flex; flex-direction: column; gap: 0.9rem; margin-top: 1rem; }
.card p { margin: 0 0 0.35rem; }
.ok { color: var(--success); font-size: 0.9rem; margin: 0; }
</style>
