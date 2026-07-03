import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import Aura from '@primevue/themes/aura'

import App from './App.vue'
import router from './router'
import i18n from './i18n'
import { initKeycloak } from './services/keycloak'

import 'primeicons/primeicons.css'
import './assets/main.css'

// Initialiser Keycloak avant de monter l'app
initKeycloak().then(() => {
  const app = createApp(App)

  app.use(createPinia())
  app.use(router)
  app.use(i18n)
  app.use(PrimeVue, {
    theme: {
      preset: Aura,
      options: {
        prefix: 'p',
        darkModeSelector: '.dark-mode',
        cssLayer: false
      }
    }
  })
  app.use(ToastService)
  app.use(ConfirmationService)

  app.mount('#app')
}).catch(err => {
  console.error('Erreur Keycloak:', err)
})
