<template>
  <div class="plans-page">
    <div class="page-header">
      <h1>Plans d'abonnement SaaS</h1>
      <button class="btn btn-primary" @click="openCreateDialog">
        <i class="pi pi-plus"></i> Nouveau Plan
      </button>
    </div>

    <!-- Plans Cards -->
    <div class="grid-3 mb-4">
      <div v-for="plan in plans" :key="plan.id" class="card plan-card" :class="{ 'featured': plan.id === 'PREMIUM' }">
        <div class="plan-badge-featured v-if" v-if="plan.id === 'PREMIUM'">Recommandé</div>
        
        <div class="plan-header flex flex-col items-center mb-3">
          <div class="plan-icon" :style="{ background: plan.color + '1a', color: plan.color }">
            <i :class="plan.icon"></i>
          </div>
          <h2 class="plan-name mt-1">{{ plan.name }}</h2>
          <div class="plan-price mt-1">
            <span class="price-val">{{ formatPrice(plan.price) }}</span>
            <span class="price-period" v-if="plan.price > 0">/ mois</span>
          </div>
        </div>

        <ul class="plan-features flex flex-col gap-1 mb-3">
          <li><i class="pi pi-check text-success"></i> Jusqu'à <strong>{{ plan.maxProperties }}</strong> biens</li>
          <li><i class="pi pi-check text-success"></i> Jusqu'à <strong>{{ plan.maxAgents }}</strong> agents</li>
          <li v-if="plan.features.support"><i class="pi pi-check text-success"></i> Support {{ plan.features.support }}</li>
          <li v-else class="disabled"><i class="pi pi-times text-danger"></i> Pas de support technique</li>
          <li :class="{ 'disabled': !plan.features.sms }">
            <i :class="plan.features.sms ? 'pi pi-check text-success' : 'pi pi-times text-danger'"></i>
            Notifications SMS
          </li>
          <li :class="{ 'disabled': !plan.features.reports }">
            <i :class="plan.features.reports ? 'pi pi-check text-success' : 'pi pi-times text-danger'"></i>
            Rapports avancés & exports
          </li>
          <li :class="{ 'disabled': !plan.features.branding }">
            <i :class="plan.features.branding ? 'pi pi-check text-success' : 'pi pi-times text-danger'"></i>
            Marque blanche
          </li>
        </ul>

        <div class="plan-footer flex flex-col gap-1">
          <div class="active-agencies-info mb-1">
            <strong>{{ plan.activeAgencies }}</strong> agence(s) active(s)
          </div>
          <Button label="Modifier le plan" size="small" outlined style="width:100%" @click="editPlan(plan)" />
        </div>
      </div>
    </div>

    <!-- Edit Plan Dialog -->
    <Dialog v-model:visible="editDialog" modal header="Modifier le plan d'abonnement" style="width: 400px">
      <div class="form-group" v-if="editingPlan">
        <label class="form-label">Nom du plan</label>
        <InputText v-model="editingPlan.name" style="width:100%" />
      </div>
      <div class="form-group" v-if="editingPlan">
        <label class="form-label">Prix mensuel (FCFA)</label>
        <InputNumber v-model="editingPlan.price" style="width:100%" />
      </div>
      <div class="grid-2" v-if="editingPlan">
        <div class="form-group">
          <label class="form-label">Biens Max</label>
          <InputNumber v-model="editingPlan.maxProperties" style="width:100%" />
        </div>
        <div class="form-group">
          <label class="form-label">Agents Max</label>
          <InputNumber v-model="editingPlan.maxAgents" style="width:100%" />
        </div>
      </div>
      <template #footer>
        <Button label="Annuler" severity="secondary" outlined @click="editDialog = false" />
        <Button label="Enregistrer" @click="savePlan" />
      </template>
    </Dialog>

    <Toast />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Toast from 'primevue/toast'

const toast = useToast()
const editDialog = ref(false)
const editingPlan = ref(null)

const plans = ref([
  {
    id: 'TRIAL',
    name: 'Essai Gratuit',
    price: 0,
    maxProperties: 5,
    maxAgents: 1,
    icon: 'pi pi-info-circle',
    color: '#64748b',
    activeAgencies: 4,
    features: { support: 'Email uniquement', sms: false, reports: false, branding: false }
  },
  {
    id: 'BASIC',
    name: 'Plan Basique',
    price: 25000,
    maxProperties: 30,
    maxAgents: 3,
    icon: 'pi pi-home',
    color: '#4f46e5',
    activeAgencies: 12,
    features: { support: 'Standard (Email/Ticket)', sms: true, reports: true, branding: false }
  },
  {
    id: 'PREMIUM',
    name: 'Plan Premium',
    price: 65000,
    maxProperties: 150,
    maxAgents: 10,
    icon: 'pi pi-star-fill',
    color: '#a855f7',
    activeAgencies: 28,
    features: { support: 'Prioritaire 24/7', sms: true, reports: true, branding: true }
  }
])

const formatPrice = (price) => {
  if (price === 0) return 'Gratuit'
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(price)
}

const editPlan = (plan) => {
  editingPlan.value = { ...plan }
  editDialog.value = true
}

const savePlan = () => {
  const index = plans.value.findIndex(p => p.id === editingPlan.value.id)
  if (index !== -1) {
    plans.value[index] = editingPlan.value
    toast.add({ severity: 'success', summary: 'Succès', detail: 'Plan d\'abonnement mis à jour', life: 3000 })
  }
  editDialog.value = false
}

const openCreateDialog = () => {
  toast.add({ severity: 'info', summary: 'Info', detail: 'Création désactivée en version locale', life: 3000 })
}
</script>

<style scoped>
.plan-card {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border-width: 2px;
}
.plan-card.featured {
  border-color: var(--primary);
  transform: scale(1.02);
}
.plan-badge-featured {
  position: absolute;
  top: -12px;
  right: 20px;
  background: var(--primary);
  color: white;
  padding: 0.2rem 0.75rem;
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  font-weight: 600;
  box-shadow: var(--shadow-sm);
}
.plan-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.6rem;
}
.plan-name {
  font-size: 1.25rem;
  font-weight: 700;
}
.price-val {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--text-primary);
}
.price-period {
  font-size: 0.85rem;
  color: var(--text-muted);
}
.plan-features {
  list-style: none;
}
.plan-features li {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  padding: 0.25rem 0;
}
.plan-features li.disabled {
  color: var(--text-muted);
  text-decoration: line-through;
}
.active-agencies-info {
  text-align: center;
  font-size: 0.85rem;
  color: var(--text-secondary);
}
.text-success { color: var(--success); }
.text-danger { color: var(--danger); }
</style>
