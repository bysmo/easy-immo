<template>
  <div class="error-page">
    <div class="error-content">
      <span class="error-code">404</span>
      <h1>Page non trouvée</h1>
      <p class="text-muted">Désolé, la page que vous recherchez n'existe pas ou a été déplacée.</p>
      <RouterLink to="/" class="btn btn-primary mt-3">Retour à l'accueil</RouterLink>
    </div>
  </div>
</template>

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
.error-code {
  font-size: 6rem;
  font-weight: 800;
  color: var(--primary);
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
