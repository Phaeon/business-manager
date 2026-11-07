<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const email = ref('')
const password = ref('')
const errorMessage = ref('')
const loading = ref(false)

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

async function handleSubmit() {
  errorMessage.value = ''
  loading.value = true
  try {
    await authStore.login({ email: email.value, password: password.value })
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch {
    errorMessage.value = 'Email ou mot de passe incorrect'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <form class="card login-card" @submit.prevent="handleSubmit">
      <h1>Business Manager</h1>
      <p class="muted">Connectez-vous à votre espace</p>

      <label class="field">
        Email
        <input v-model="email" type="email" required autocomplete="email" />
      </label>

      <label class="field">
        Mot de passe
        <input v-model="password" type="password" required autocomplete="current-password" />
      </label>

      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <button type="submit" class="btn btn-primary" :disabled="loading">
        {{ loading ? 'Connexion…' : 'Se connecter' }}
      </button>
    </form>
  </div>
</template>

<style scoped>
.login-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 1rem; }
.login-card { width: 100%; max-width: 380px; display: flex; flex-direction: column; gap: 1rem; padding: 2rem; }
h1 { margin: 0; font-size: 1.5rem; }
p { margin: 0; }
</style>
