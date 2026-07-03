<template>
  <div class="dashboard">
    <!-- En-tête -->
    <div class="page-header">
      <div>
        <h1>Tableau de Bord</h1>
        <p class="text-muted">Vue d'ensemble de votre portefeuille immobilier</p>
      </div>
      <span class="date-badge">{{ currentDate }}</span>
    </div>

    <!-- Stats -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon blue">
          <i class="pi pi-home"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalProperties }}</div>
          <div class="stat-label">Biens gérés</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon green">
          <i class="pi pi-user-check"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.occupiedProperties }}</div>
          <div class="stat-label">Biens occupés</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon yellow">
          <i class="pi pi-spin pi-cog" v-if="loading"></i>
          <div class="stat-value" v-else>{{ stats.availableProperties }}</div>
          <div class="stat-label">Disponibles</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon purple">
          <div class="stat-value currency">{{ formatCurrency(stats.collectedRentMonth) }}</div>
          <div class="stat-label">Loyers perçus ce mois</div>
        </div>
      </div>
    </div>

    <!-- Deuxième rangée: Biens récents et alertes de baux/paiements -->
    <div class="grid-2 mb-3">
      <!-- Biens récents -->
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">Derniers biens ajoutés</h3>
          <RouterLink to="/agency/properties" class="btn btn-outline btn-sm">Gérer</RouterLink>
        </div>
        
        <DataTable :value="recentProperties" :loading="loading" class="p-datatable-sm" responsiveLayout="scroll">
          <Column field="reference" header="Réf" />
          <Column field="type" header="Type">
            <template #body="{ data }">
              {{ $t(`property.types.${data.type.toLowerCase()}`) }}
            </template>
          </Column>
          <Column field="city" header="Ville" />
          <Column field="currentRent" header="Loyer">
            <template #body="{ data }">
              <span class="currency">{{ formatCurrency(data.currentRent) }}</span>
            </template>
          </Column>
          <Column field="status" header="Statut">
            <template #body="{ data }">
              <span :class="statusBadgeClass(data.status)">
                {{ $t(`property.statuses.${data.status.toLowerCase()}`) }}
              </span>
            </template>
          </Column>
        </DataTable>
      </div>

      <!-- Relances / retards de paiement -->
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">Paiements en attente</h3>
          <RouterLink to="/agency/payments/rents" class="btn btn-outline btn-sm">Détail</RouterLink>
        </div>
        
        <DataTable :value="pendingPayments" :loading="loading" class="p-datatable-sm" responsiveLayout="scroll">
          <Column header="Locataire">
            <template #body="{ data }">
              {{ data.tenantName }}
            </template>
          </Column>
          <Column field="dueDate" header="Échéance">
            <template #body="{ data }">
              {{ formatDate(data.dueDate) }}
            </template>
          </Column>
          <Column field="amount" header="Montant">
            <template #body="{ data }">
              <span class="currency text-danger font-semibold">{{ formatCurrency(data.amount) }}</span>
            </template>
          </Column>
          <Column header="Statut">
            <template #body>
              <span class="badge badge-danger">En retard</span>
            </template>
          </Column>
        </DataTable>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import api from '@/services/api'
import dayjs from 'dayjs'

const loading = ref(false)
const currentDate = dayjs().format('DD MMMM YYYY')

const stats = ref({
  totalProperties: 12,
  occupiedProperties: 9,
  availableProperties: 3,
  collectedRentMonth: 840000
})

const recentProperties = ref([
  { id: '1', reference: 'VIL-002', type: 'VILLA', city: 'Cotonou', currentRent: 250000, status: 'AVAILABLE' },
  { id: '2', reference: 'APP-105', type: 'APPARTEMENT', city: 'Ouagadougou', currentRent: 120000, status: 'OCCUPIED' },
  { id: '3', reference: 'STU-010', type: 'STUDIO', city: 'Dakar', currentRent: 85000, status: 'MAINTENANCE' }
])

const pendingPayments = ref([
  { tenantName: 'Mamadou Diallo', dueDate: '2026-07-05', amount: 120000 },
  { tenantName: 'Koffi Mensah', dueDate: '2026-07-05', amount: 85000 },
  { tenantName: 'Awa Diop', dueDate: '2026-06-05', amount: 150000 }
])

const loadDashboardStats = async () => {
  loading.value = true
  try {
    // Dans l'idéal, appeler les endpoints réels via API Gateway
    // Ex: /api/properties (pour les stats de biens), /api/payments (pour loyers perçus)
    // Ici nous initialisons avec des fausses données au cas où les microservices ne tournent pas
  } catch (error) {
    console.error('Erreur chargement dashboard agence:', error)
  } finally {
    loading.value = false
  }
}

const formatCurrency = (value) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(value)
}

const formatDate = (date) => dayjs(date).format('DD/MM/YYYY')

const statusBadgeClass = (status) => {
  const map = {
    AVAILABLE: 'badge badge-success',
    OCCUPIED: 'badge badge-info',
    MAINTENANCE: 'badge badge-danger',
    RESERVED: 'badge badge-warning'
  }
  return map[status.toUpperCase()] || 'badge'
}

onMounted(loadDashboardStats)
</script>

<style scoped>
.date-badge {
  background: var(--primary-light);
  color: var(--primary);
  padding: 0.4rem 0.8rem;
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-size: 0.85rem;
}
.font-semibold { font-weight: 600; }
.text-danger { color: var(--danger); }
</style>
