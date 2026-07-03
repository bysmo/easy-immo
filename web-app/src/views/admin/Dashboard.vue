<template>
  <div class="dashboard">
    <!-- En-tête -->
    <div class="page-header">
      <h1>{{ $t('nav.dashboard') }}</h1>
      <span class="text-muted">{{ $t('common.loading') !== currentDate ? currentDate : '' }}</span>
    </div>

    <!-- Stats cards -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon blue">
          <i class="pi pi-building"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalAgencies }}</div>
          <div class="stat-label">{{ $t('dashboard.total_agencies') }}</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon green">
          <i class="pi pi-check-circle"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.activeAgencies }}</div>
          <div class="stat-label">{{ $t('dashboard.active_agencies') }}</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon yellow">
          <i class="pi pi-clock"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.trialAgencies }}</div>
          <div class="stat-label">{{ $t('dashboard.trial_agencies') }}</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon red">
          <i class="pi pi-ban"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.suspendedAgencies }}</div>
          <div class="stat-label">{{ $t('dashboard.suspended_agencies') }}</div>
        </div>
      </div>
    </div>

    <!-- Tableau agences récentes -->
    <div class="card">
      <div class="card-header">
        <h3 class="card-title">Agences récentes</h3>
        <RouterLink to="/admin/agencies" class="btn btn-outline btn-sm">
          Voir toutes
        </RouterLink>
      </div>

      <DataTable
        :value="recentAgencies"
        :loading="loading"
        striped-rows
        :rows="10"
        responsive-layout="scroll"
      >
        <Column field="name" header="Nom">
          <template #body="{ data }">
            <div class="agency-name-cell">
              <div class="agency-avatar">{{ data.name[0] }}</div>
              <div>
                <div class="font-semibold">{{ data.name }}</div>
                <div class="text-muted text-sm">{{ data.city }}, {{ $t(`countries.${data.country}`) }}</div>
              </div>
            </div>
          </template>
        </Column>

        <Column field="email" header="Email" />

        <Column field="status" header="Statut">
          <template #body="{ data }">
            <span :class="statusBadgeClass(data.status)">
              {{ $t(`agency.statuses.${data.status}`) }}
            </span>
          </template>
        </Column>

        <Column field="subscriptionPlanName" header="Plan">
          <template #body="{ data }">
            <span class="badge badge-purple">{{ data.subscriptionPlanName || 'Aucun' }}</span>
          </template>
        </Column>

        <Column field="createdAt" header="Créée le">
          <template #body="{ data }">
            {{ formatDate(data.createdAt) }}
          </template>
        </Column>

        <Column header="Actions">
          <template #body="{ data }">
            <div class="flex gap-1">
              <RouterLink :to="`/admin/agencies/${data.id}`" class="btn btn-outline btn-sm">
                <i class="pi pi-eye"></i>
              </RouterLink>
            </div>
          </template>
        </Column>
      </DataTable>
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
const stats = ref({
  totalAgencies: 0,
  activeAgencies: 0,
  trialAgencies: 0,
  suspendedAgencies: 0
})
const recentAgencies = ref([])
const currentDate = dayjs().format('DD MMMM YYYY')

const loadData = async () => {
  loading.value = true
  try {
    const [statsRes, agenciesRes] = await Promise.all([
      api.get('/tenants/dashboard/stats'),
      api.get('/tenants/agencies?size=10&sortBy=createdAt&sortDir=desc')
    ])
    stats.value = statsRes.data
    recentAgencies.value = agenciesRes.data.content || []
  } catch (error) {
    console.error('Erreur chargement dashboard:', error)
  } finally {
    loading.value = false
  }
}

const formatDate = (date) => dayjs(date).format('DD/MM/YYYY')

const statusBadgeClass = (status) => {
  const map = {
    ACTIVE: 'badge badge-success',
    TRIAL: 'badge badge-warning',
    SUSPENDED: 'badge badge-danger',
    CANCELLED: 'badge badge-info'
  }
  return map[status] || 'badge'
}

onMounted(loadData)
</script>

<style scoped>
.agency-name-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.agency-avatar {
  width: 36px;
  height: 36px;
  background: var(--primary-light);
  color: var(--primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1rem;
  flex-shrink: 0;
}

.font-semibold { font-weight: 600; }
.text-muted { color: var(--text-muted); }
.text-sm { font-size: 0.8rem; }
</style>
