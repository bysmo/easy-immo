<template>
  <div class="agency-create-page">
    <div class="page-header">
      <h1>{{ $t('agency.create') || 'Créer une agence' }}</h1>
      <button class="btn btn-outline" @click="$router.push('/admin/agencies')">
        <i class="pi pi-arrow-left"></i>
        Retour
      </button>
    </div>

    <form @submit.prevent="saveAgency" class="flex flex-col gap-3">
      <div class="grid-2">
        <!-- Informations Agence -->
        <div class="card flex flex-col gap-2">
          <h2 class="card-title mb-2"><i class="pi pi-building"></i> Profil de l'agence</h2>
          
          <div class="form-group">
            <label class="form-label">Nom de l'agence *</label>
            <InputText v-model="form.name" required style="width: 100%" />
          </div>

          <div class="form-group">
            <label class="form-label">Adresse email *</label>
            <InputText type="email" v-model="form.email" required style="width: 100%" />
          </div>

          <div class="grid-2">
            <div class="form-group">
              <label class="form-label">Téléphone *</label>
              <InputText v-model="form.phone" required style="width: 100%" placeholder="+225 01020304" />
            </div>

            <div class="form-group">
              <label class="form-label">Ville *</label>
              <InputText v-model="form.city" required style="width: 100%" />
            </div>
          </div>

          <div class="grid-2">
            <div class="form-group">
              <label class="form-label">Pays *</label>
              <Select
                v-model="form.country"
                :options="countries"
                optionLabel="label"
                optionValue="value"
                placeholder="Sélectionner le pays"
                style="width: 100%"
                required
              />
            </div>

            <div class="form-group">
              <label class="form-label">Abonnement *</label>
              <Select
                v-model="form.subscriptionPlanId"
                :options="plans"
                optionLabel="label"
                optionValue="value"
                placeholder="Sélectionner un plan"
                style="width: 100%"
                required
              />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Adresse physique</label>
            <InputText v-model="form.address" style="width: 100%" />
          </div>
        </div>

        <!-- Informations Administrateur -->
        <div class="card flex flex-col gap-2">
          <h2 class="card-title mb-2"><i class="pi pi-user"></i> Administrateur principal</h2>

          <div class="grid-2">
            <div class="form-group">
              <label class="form-label">Prénom *</label>
              <InputText v-model="form.adminFirstName" required style="width: 100%" />
            </div>

            <div class="form-group">
              <label class="form-label">Nom *</label>
              <InputText v-model="form.adminLastName" required style="width: 100%" />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Email de l'administrateur *</label>
            <InputText type="email" v-model="form.adminEmail" required style="width: 100%" />
          </div>

          <div class="form-group">
            <label class="form-label">Identifiant (Username) *</label>
            <InputText v-model="form.adminUsername" required style="width: 100%" />
          </div>

          <div class="form-group">
            <label class="form-label">Mot de passe provisoire *</label>
            <InputText type="password" v-model="form.adminPassword" required style="width: 100%" placeholder="••••••••" />
            <small class="text-muted">L'administrateur sera invité à changer ce mot de passe à sa première connexion.</small>
          </div>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="flex justify-between items-center card">
        <span class="text-muted">* Champs obligatoires</span>
        <div class="flex gap-1">
          <Button label="Annuler" severity="secondary" outlined @click="$router.push('/admin/agencies')" />
          <Button type="submit" label="Enregistrer l'agence" icon="pi pi-save" :loading="loading" />
        </div>
      </div>
    </form>

    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Button from 'primevue/button'
import Toast from 'primevue/toast'
import api from '@/services/api'

const router = useRouter()
const toast = useToast()
const loading = ref(false)

const form = ref({
  name: '',
  email: '',
  phone: '',
  address: '',
  city: '',
  country: '',
  subscriptionPlanId: '',
  adminFirstName: '',
  adminLastName: '',
  adminEmail: '',
  adminUsername: '',
  adminPassword: ''
})

const countries = [
  { label: 'Bénin', value: 'BJ' },
  { label: 'Burkina Faso', value: 'BF' },
  { label: 'Côte d\'Ivoire', value: 'CI' },
  { label: 'Guinée-Bissau', value: 'GW' },
  { label: 'Mali', value: 'ML' },
  { label: 'Niger', value: 'NE' },
  { label: 'Sénégal', value: 'SN' },
  { label: 'Togo', value: 'TG' }
]

const plans = ref([])

const formatPrice = (price) => {
  if (!price || price === 0) return 'Gratuit'
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(price)
}

onMounted(async () => {
  try {
    const res = await api.get('/tenants/plans')
    plans.value = res.data.map(p => ({
      label: `${p.displayName} (${formatPrice(p.priceMonthly)}/mois)`,
      value: p.id
    }))
    if (plans.value.length > 0) {
      form.value.subscriptionPlanId = plans.value[0].value
    }
  } catch (e) {
    console.error('Erreur chargement des plans', e)
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de charger les plans d\'abonnement',
      life: 3000
    })
  }
})

const saveAgency = async () => {
  loading.value = true
  try {
    const payload = {
      ...form.value,
      adminPhone: form.value.phone
    }
    await api.post('/tenants/agencies', payload)
    toast.add({ severity: 'success', summary: 'Succès', detail: 'Agence créée avec succès', life: 3000 })
    setTimeout(() => {
      router.push('/admin/agencies')
    }, 1500)
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: e.response?.data?.message || 'Impossible de créer l\'agence',
      life: 3000
    })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.card-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--primary);
}
.text-muted {
  font-size: 0.8rem;
  color: var(--text-muted);
}
</style>
