<template>
  <div class="property-detail-page">
    <div class="page-header" v-if="property">
      <div class="flex items-center gap-2">
        <h1>{{ property.title }}</h1>
        <span :class="statusBadgeClass(property.status)">
          {{ property.status }}
        </span>
      </div>
      <button class="btn btn-outline" @click="$router.push('/agency/properties')">
        <i class="pi pi-arrow-left"></i>
        Retour
      </button>
    </div>

    <div v-if="loading" class="flex items-center justify-center card" style="min-height: 200px">
      <i class="pi pi-spin pi-spinner" style="font-size: 2rem; color: var(--primary)"></i>
    </div>

    <div v-else-if="property" class="flex flex-col gap-3">
      <!-- Image et Infos Clés -->
      <div class="grid-3">
        <div class="card flex flex-col items-center justify-center" style="grid-column: span 2; min-height: 250px; background: #eef2ff;">
          <i class="pi pi-image text-muted" style="font-size: 4rem;"></i>
          <p class="text-muted mt-1">Image du bien immobilier</p>
        </div>
        
        <div class="card flex flex-col gap-2 justify-between">
          <div>
            <h2 class="card-title"><i class="pi pi-money-bill"></i> Conditions financières</h2>
            <div class="price-box mt-2">
              <span class="price-val">{{ formatCurrency(property.rentAmount) }}</span>
              <span class="price-period">/ mois</span>
            </div>
            <div class="info-row mt-2">
              <span class="info-label">Charges :</span>
              <span class="info-val">{{ formatCurrency(property.chargesAmount || 0) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Dépôt de garantie :</span>
              <span class="info-val">{{ formatCurrency(property.securityDepositAmount || (property.rentAmount * 2)) }}</span>
            </div>
          </div>

          <div class="flex gap-1 mt-2">
            <Button label="Modifier" icon="pi pi-pencil" outlined style="flex:1" @click="editProperty" />
            <Button v-if="property.status === 'AVAILABLE'" label="Louer" icon="pi pi-file-plus" severity="success" style="flex:1.5" @click="createNewLease" />
          </div>
        </div>
      </div>

      <!-- Détails et Propriétaire -->
      <div class="grid-2">
        <div class="card flex flex-col gap-2">
          <h2 class="card-title"><i class="pi pi-sliders-h"></i> Caractéristiques</h2>
          
          <div class="info-row">
            <span class="info-label">Type de bien :</span>
            <span class="info-val">{{ property.type }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Nombre de pièces :</span>
            <span class="info-val">{{ property.rooms || '—' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Superficie :</span>
            <span class="info-val">{{ property.area ? property.area + ' m²' : '—' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Ville :</span>
            <span class="info-val">{{ property.city }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Adresse :</span>
            <span class="info-val">{{ property.address || '—' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Description :</span>
            <p class="desc-val">{{ property.description || 'Aucune description fournie.' }}</p>
          </div>
        </div>

        <div class="card flex flex-col gap-2">
          <h2 class="card-title"><i class="pi pi-user"></i> Propriétaire</h2>
          <div class="flex items-center gap-2 mt-2">
            <div class="avatar-lg">{{ property.ownerName ? property.ownerName[0] : 'P' }}</div>
            <div>
              <div class="font-bold">{{ property.ownerName || 'Propriétaire Non Assigné' }}</div>
              <div class="text-muted text-sm">{{ property.ownerPhone || '—' }}</div>
            </div>
          </div>
          <div class="divider mt-2"></div>
          <h2 class="card-title mt-2"><i class="pi pi-users"></i> Locataire Actuel</h2>
          <div class="flex items-center gap-2 mt-2" v-if="property.status === 'RENTED'">
            <div class="avatar-lg bg-green">{{ property.tenantName ? property.tenantName[0] : 'L' }}</div>
            <div>
              <div class="font-bold">{{ property.tenantName || 'Locataire' }}</div>
              <div class="text-muted text-sm">{{ property.tenantPhone || '—' }}</div>
            </div>
          </div>
          <p v-else class="text-muted mt-1 text-center">Aucun locataire actif pour ce bien.</p>
        </div>
      </div>
    </div>
    
    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Toast from 'primevue/toast'
import api from '@/services/api'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const property = ref(null)
const loading = ref(false)

const loadProperty = async () => {
  loading.value = true
  try {
    // API endpoint for properties detail
    const res = await api.get(`/properties/properties/${route.params.id}`)
    property.value = res.data
  } catch (e) {
    // Fallback Mock Data if API fails in local env
    property.value = {
      id: route.params.id,
      title: 'Appartement F3 Zone 4',
      type: 'APPARTEMENT',
      status: 'AVAILABLE',
      rentAmount: 350000,
      chargesAmount: 25000,
      securityDepositAmount: 700000,
      rooms: 3,
      area: 85,
      city: 'Abidjan',
      address: 'Rue des Majorettes, Marcory',
      description: 'Superbe appartement de 3 pièces dans une résidence sécurisée avec parking. Idéalement situé en Zone 4.',
      ownerName: 'Diallo Ousmane',
      ownerPhone: '+225 07070707',
      tenantName: '',
      tenantPhone: ''
    }
  } finally {
    loading.value = false
  }
}

const formatCurrency = (val) => new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(val)

const statusBadgeClass = (status) => ({
  AVAILABLE: 'badge badge-success',
  RENTED: 'badge badge-info',
  MAINTENANCE: 'badge badge-danger'
}[status] || 'badge')

const editProperty = () => {
  toast.add({ severity: 'info', summary: 'Edition', detail: 'Fonctionnalité en cours de développement', life: 3000 })
}

const createNewLease = () => {
  router.push({ path: '/agency/leases/create', query: { propertyId: property.value.id } })
}

onMounted(loadProperty)
</script>

<style scoped>
.card-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--primary);
  font-size: 1.1rem;
}
.price-box {
  background: var(--primary-light);
  padding: 1rem;
  border-radius: var(--radius);
  text-align: center;
}
.price-val {
  font-size: 1.8rem;
  font-weight: 800;
  color: var(--primary);
}
.price-period {
  font-size: 0.9rem;
  color: var(--text-secondary);
}
.info-row {
  display: flex;
  justify-content: space-between;
  padding: 0.65rem 0;
  border-bottom: 1px dashed var(--border);
}
.info-row:last-child {
  border-bottom: none;
}
.info-label {
  font-weight: 500;
  color: var(--text-secondary);
}
.info-val {
  font-weight: 600;
  color: var(--text-primary);
}
.desc-val {
  font-size: 0.9rem;
  color: var(--text-secondary);
  margin-top: 0.25rem;
  text-align: justify;
}
.avatar-lg {
  width: 44px;
  height: 44px;
  background: var(--primary-light);
  color: var(--primary);
  border-radius: var(--radius);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.2rem;
}
.avatar-lg.bg-green {
  background: var(--success-light);
  color: var(--success);
}
.font-bold { font-weight: 700; }
.divider {
  border-top: 1px solid var(--border);
  margin: 1rem 0;
}
</style>
