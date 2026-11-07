<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppShell from '@/components/AppShell.vue'
import BaseModal from '@/components/BaseModal.vue'
import { errorMessage } from '@/services/api'
import { usersApi } from '@/services/resources'
import { useAuthStore } from '@/stores/auth'
import type { ManagedUser, UserPayload } from '@/types/domain'
import { roleLabels } from '@/utils/format'

const authStore = useAuthStore()
const users = ref<ManagedUser[]>([])
const error = ref('')
const modalOpen = ref(false)
const saving = ref(false)
const formError = ref('')
const form = ref<UserPayload>(emptyForm())

function emptyForm(): UserPayload {
  return { firstName: '', lastName: '', email: '', password: '', role: 'EMPLOYEE' }
}

async function load() {
  try {
    users.value = await usersApi.list()
  } catch (e) {
    error.value = errorMessage(e, 'Impossible de charger les utilisateurs.')
  }
}

function openCreate() {
  form.value = emptyForm()
  formError.value = ''
  modalOpen.value = true
}

async function save() {
  saving.value = true
  formError.value = ''
  try {
    await usersApi.create(form.value)
    modalOpen.value = false
    await load()
  } catch (e) {
    formError.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}

async function toggle(u: ManagedUser) {
  error.value = ''
  try {
    await usersApi.setEnabled(u.id, !u.enabled)
    await load()
  } catch (e) {
    error.value = errorMessage(e)
  }
}

onMounted(load)
</script>

<template>
  <AppShell>
    <div class="container">
      <h1 class="page-title">Utilisateurs</h1>
      <div class="toolbar">
        <p class="muted">Seuls les administrateurs peuvent créer des comptes : il n'existe pas d'inscription publique.</p>
        <button class="btn btn-primary" @click="openCreate">+ Nouvel utilisateur</button>
      </div>
      <p v-if="error" class="error-text">{{ error }}</p>

      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>Nom</th><th>Email</th><th>Rôle</th><th>Statut</th><th /></tr></thead>
          <tbody>
            <tr v-for="u in users" :key="u.id">
              <td><strong>{{ u.firstName }} {{ u.lastName }}</strong></td>
              <td>{{ u.email }}</td>
              <td>{{ roleLabels[u.role] }}</td>
              <td><span class="badge" :class="u.enabled ? 'badge-success' : 'badge-neutral'">{{ u.enabled ? 'Actif' : 'Désactivé' }}</span></td>
              <td>
                <div class="actions">
                  <button class="btn btn-sm" :disabled="u.id === authStore.user?.id" @click="toggle(u)">
                    {{ u.enabled ? 'Désactiver' : 'Réactiver' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <BaseModal v-if="modalOpen" title="Nouvel utilisateur" @close="modalOpen = false">
      <form id="user-form" class="form-grid" @submit.prevent="save">
        <label class="field">Prénom<input v-model="form.firstName" required /></label>
        <label class="field">Nom<input v-model="form.lastName" required /></label>
        <label class="field span-2">Email<input v-model="form.email" type="email" required /></label>
        <label class="field">Mot de passe (8 car. min.)<input v-model="form.password" type="password" minlength="8" required autocomplete="new-password" /></label>
        <label class="field">
          Rôle
          <select v-model="form.role">
            <option v-for="(label, key) in roleLabels" :key="key" :value="key">{{ label }}</option>
          </select>
        </label>
        <p v-if="formError" class="error-text span-2">{{ formError }}</p>
      </form>
      <template #footer>
        <button class="btn" type="button" @click="modalOpen = false">Annuler</button>
        <button class="btn btn-primary" type="submit" form="user-form" :disabled="saving">Créer</button>
      </template>
    </BaseModal>
  </AppShell>
</template>
