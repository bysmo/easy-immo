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
          <div class="stat-value">{{ loading ? '…' : stats.totalProperties }}</div>
          <div class="stat-label">Biens gérés</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon green">
          <i class="pi pi-user-check"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ loading ? '…' : stats.occupiedProperties }}</div>
          <div class="stat-label">Biens occupés</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon yellow">
          <i class="pi pi-check-circle"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ loading ? '…' : stats.availableProperties }}</div>
          <div class="stat-label">Disponibles</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon purple">
          <i class="pi pi-wallet"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value currency" style="font-size: 1.1rem">{{ loading ? '…' : formatCurrency(stats.collectedRentMonth) }}</div>
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
          <template #empty>
            <div class="empty-state"><i class="pi pi-home" style="font-size:2rem;color:var(--text-muted)"></i><p class="mt-2">Aucun bien enregistré</p></div>
          </template>
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

        <DataTable :value="pendingPayments" :loading="loadingPayments" class="p-datatable-sm" responsiveLayout="scroll">
          <template #empty>
            <div class="empty-state"><i class="pi pi-check" style="font-size:2rem;color:var(--success)"></i><p class="mt-2">Aucun paiement en retard</p></div>
          </template>
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
import 'dayjs/locale/fr'
dayjs.locale('fr')

const loading = ref(false)
const loadingPayments = ref(false)
const currentDate = dayjs().format('DD MMMM YYYY')

const stats = ref({
  totalProperties: 0,
  occupiedProperties: 0,
  availableProperties: 0,
  collectedRentMonth: 0
})

const recentProperties = ref([])
const pendingPayments = ref([])

const loadDashboardStats = async () => {
  loading.value = true
  try {
    // Charger les biens de l'agence (triés du plus récent)
    const res = await api.get('/properties?size=10&sortBy=createdAt&sortDir=desc')
    const data = res.data

    const items = data.content || []
    recentProperties.value = items.slice(0, 5)

    stats.value.totalProperties = data.totalElements || items.length
    stats.value.occupiedProperties = items.filter(p => p.status === 'OCCUPIED').length
    stats.value.availableProperties = items.filter(p => p.status === 'AVAILABLE').length

    // Calculer les loyers perçus sur la totalité (somme des logements OCCUPIED)
    stats.value.collectedRentMonth = items
      .filter(p => p.status === 'OCCUPIED')
      .reduce((sum, p) => sum + Number(p.currentRent || 0), 0)
  } catch (error) {
    console.error('Erreur chargement propriétés dashboard:', error)
    recentProperties.value = []
  } finally {
    loading.value = false
  }
}

const loadPendingPayments = async () => {
  loadingPayments.value = true
  try {
    // Tenter de charger les paiements en retard depuis le service leases
    const res = await api.get('/leases?status=ACTIVE&size=5')
    pendingPayments.value = res.data.content || []
  } catch (error) {
    console.error('Erreur chargement paiements dashboard:', error)
    pendingPayments.value = []
  } finally {
    loadingPayments.value = false
  }
}

const formatCurrency = (value) => {
  if (!value && value !== 0) return '—'
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(value)
}

const formatDate = (date) => date ? dayjs(date).format('DD/MM/YYYY') : '—'

const statusBadgeClass = (status) => {
  const map = {
    AVAILABLE: 'badge badge-success',
    OCCUPIED: 'badge badge-info',
    MAINTENANCE: 'badge badge-danger',
    RESERVED: 'badge badge-warning'
  }
  return map[status?.toUpperCase()] || 'badge'
}

onMounted(() => {
  loadDashboardStats()
  loadPendingPayments()
})
</script>

<style scoped>
.date-badge {
  background: var(--primary-light);
  color: var(--primary);
  padding: 0.4rem 0.8rem;
  border-radius: var(--radius-sm);
  font-weight: 600;
  font-size: 0.85rem;
  white-space: nowrap;
}
.font-semibold { font-weight: 600; }
.text-danger { color: var(--danger); }
.empty-state { text-align: center; padding: 2rem; color: var(--text-muted); }
.mt-2 { margin-top: 0.5rem; }
</style>
