<template>
  <div class="error-page">
    <div class="error-content">
      <span class="error-icon">🚫</span>
      <h1>Accès non autorisé</h1>
      <p class="text-muted">Vous n'avez pas les droits nécessaires pour accéder à cette page. Veuillez vous connecter avec un compte approprié.</p>
      <button class="btn btn-primary mt-3" @click="handleLogout">Déconnexion / Se reconnecter</button>
    </div>
  </div>
</template>

<script setup>
import { logout } from '@/services/keycloak'
const handleLogout = () => logout()
</script>

<style scoped>
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: var(--bg-main);
  text-align: center;
}
.error-content {
  max-width: 450px;
  padding: 2rem;
}
.error-icon {
  font-size: 5rem;
  line-height: 1;
}
h1 {
  margin-top: 1rem;
  font-size: 1.75rem;
}
.mt-3 {
  margin-top: 1.5rem;
}
</style>
