<template>
  <div class="lease-create-page">
    <div class="page-header">
      <div>
        <h1>Nouveau Contrat de Bail</h1>
        <p class="text-muted">Affecter un logement disponible à un locataire et fixer les conditions financières</p>
      </div>
      <Button label="Retour aux baux" icon="pi pi-arrow-left" class="btn btn-outline" @click="goBack" />
    </div>

    <div class="card" style="max-width: 800px; margin: 0 auto">
      <form @submit.prevent="submitForm">
        
        <div class="grid-2 mb-3">
          <div class="form-group">
            <label class="form-label">Bien Immobilier à louer *</label>
            <Select v-model="form.propertyId" :options="properties" optionLabel="label" optionValue="id" placeholder="Choisir un logement disponible" @change="onPropertySelect" required />
            <small class="text-muted" v-if="selectedProperty">
              Loyer de base: {{ formatCurrency(selectedProperty.currentRent) }} | Caution recommandée: {{ selectedProperty.depositMonths }} mois
            </small>
          </div>

          <div class="form-group">
            <label class="form-label">Locataire bénéficiaire *</label>
            <Select v-model="form.tenantId" :options="tenants" optionLabel="fullName" optionValue="id" filter placeholder="Rechercher / Choisir le locataire" required />
          </div>
        </div>

        <div class="grid-2 mb-3">
          <div class="form-group">
            <label class="form-label">Date d'effet (Début du bail) *</label>
            <DatePicker v-model="form.startDate" dateFormat="dd/mm/yy" required />
          </div>

          <div class="form-group">
            <label class="form-label">Date de fin de bail (Optionnel)</label>
            <DatePicker v-model="form.endDate" dateFormat="dd/mm/yy" />
          </div>
        </div>

        <div class="grid-3 mb-3">
          <div class="form-group">
            <label class="form-label">Loyer Mensuel Négocié (XOF) *</label>
            <InputNumber v-model="form.monthlyRent" mode="decimal" @input="calculateDeposit" required />
          </div>

          <div class="form-group">
            <label class="form-label">Montant de la Caution (XOF) *</label>
            <InputNumber v-model="form.depositAmount" mode="decimal" required />
          </div>

          <div class="form-group">
            <label class="form-label">Jour d'échéance mensuel *</label>
            <InputNumber v-model="form.paymentDay" :min="1" :max="28" placeholder="5" required />
            <small class="text-muted">Jour limite de paiement (1-28)</small>
          </div>
        </div>

        <div class="form-group mb-4">
          <label class="form-label">Clauses particulières ou notes additionnelles</label>
          <Textarea v-model="form.notes" rows="4" placeholder="Indiquez ici les conditions spécifiques (ex: travaux à la charge du locataire, aménagements...)" />
        </div>

        <div class="flex justify-between items-center border-t pt-3">
          <span class="text-muted">* Champs obligatoires</span>
          <div class="flex gap-2">
            <Button label="Annuler" severity="secondary" outlined @click="goBack" />
            <Button type="submit" label="Générer & Enregistrer le Bail" severity="primary" icon="pi pi-check" :loading="saving" />
          </div>
        </div>

      </form>
    </div>

    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Select from 'primevue/select'
import InputNumber from 'primevue/inputnumber'
import DatePicker from 'primevue/datepicker'
import Textarea from 'primevue/textarea'
import Button from 'primevue/button'
import Toast from 'primevue/toast'
import { useToast } from 'primevue/usetoast'
import api from '@/services/api'
import dayjs from 'dayjs'

const router = useRouter()
const toast = useToast()

const properties = ref([])
const tenants = ref([])
const selectedProperty = ref(null)
const saving = ref(false)

const form = ref({
  propertyId: null,
  tenantId: null,
  startDate: new Date(),
  endDate: null,
  monthlyRent: 0,
  depositAmount: 0,
  paymentDay: 5,
  notes: ''
})

const loadFormData = async () => {
  try {
    // 1. Récupérer les biens AVAILABLE
    const propRes = await api.get('/properties?status=AVAILABLE&size=100')
    properties.value = (propRes.data.content || []).map(p => ({
      id: p.id,
      label: `${p.reference} - ${p.address} (${p.city})`,
      currentRent: p.currentRent,
      depositMonths: p.depositMonths || 2
    }))
  } catch (e) {
    properties.value = [
      { id: '1', label: 'VIL-002 - Fidjrossé Cotonou (Cotonou)', currentRent: 250000, depositMonths: 3 }
    ]
  }

  try {
    // 2. Récupérer tous les locataires
    const tenantRes = await api.get('/leases/tenants?size=200')
    tenants.value = tenantRes.data.content || []
  } catch (e) {
    tenants.value = [
      { id: '1', fullName: 'Mamadou Diallo' }
    ]
  }
}

const onPropertySelect = (e) => {
  const prop = properties.value.find(p => p.id === e.value)
  if (prop) {
    selectedProperty.value = prop
    form.value.monthlyRent = prop.currentRent
    form.value.depositAmount = prop.currentRent * prop.depositMonths
  }
}

const calculateDeposit = (e) => {
  if (selectedProperty.value) {
    form.value.depositAmount = e.value * selectedProperty.value.depositMonths
  }
}

const submitForm = async () => {
  if (!form.value.propertyId || !form.value.tenantId || !form.value.startDate) {
    toast.add({ severity: 'error', summary: 'Champs requis', detail: 'Veuillez remplir les informations obligatoires', life: 3000 })
    return
  }

  saving.value = true
  try {
    const payload = {
      ...form.value,
      startDate: dayjs(form.value.startDate).format('YYYY-MM-DD'),
      endDate: form.value.endDate ? dayjs(form.value.endDate).format('YYYY-MM-DD') : null
    }

    await api.post('/leases', payload)
    toast.add({ severity: 'success', summary: 'Bail enregistré avec succès', life: 3000 })
    setTimeout(() => {
      router.push('/agency/leases')
    }, 1500)
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Erreur', detail: e.response?.data?.message || 'Erreur lors de la création du bail', life: 3000 })
  } finally {
    saving.value = false
  }
}

const goBack = () => router.push('/agency/leases')

const formatCurrency = (value) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(value)
}

onMounted(loadFormData)
</script>

<style scoped>
.border-t { border-top: 1px solid var(--border); }
.pt-3 { padding-top: 1rem; }
.mt-1 { margin-top: 0.25rem; }
.form-group small { display: block; margin-top: 0.25rem; }
</style>
