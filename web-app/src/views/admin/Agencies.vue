<template>
  <div class="agencies-page">
    <div class="page-header">
      <h1>{{ $t('agency.title') }}</h1>
      <RouterLink to="/admin/agencies/create" class="btn btn-primary">
        <i class="pi pi-plus"></i>
        {{ $t('agency.create') }}
      </RouterLink>
    </div>

    <!-- Filtres -->
    <div class="card mb-3">
      <div class="filters flex gap-2 items-center">
        <div style="flex:1">
          <InputText
            v-model="searchQuery"
            :placeholder="$t('common.search') + '...'"
            @input="debouncedSearch"
            style="width: 100%"
          >
            <template #prefix><i class="pi pi-search" /></template>
          </InputText>
        </div>

        <Select
          v-model="statusFilter"
          :options="statusOptions"
          optionLabel="label"
          optionValue="value"
          :placeholder="$t('common.all')"
          style="width: 180px"
          @change="loadAgencies"
        />

        <Button
          icon="pi pi-refresh"
          severity="secondary"
          outlined
          @click="loadAgencies"
        />
      </div>
    </div>

    <!-- Tableau -->
    <div class="table-container">
      <DataTable
        :value="agencies"
        :loading="loading"
        :paginator="true"
        :rows="pageSize"
        :total-records="totalRecords"
        :lazy="true"
        @page="onPage"
        striped-rows
        responsive-layout="scroll"
        :row-hover="true"
      >
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-building" style="font-size:3rem; color: var(--text-muted)"></i>
            <p>Aucune agence trouvée</p>
          </div>
        </template>

        <Column field="name" header="Agence" sortable>
          <template #body="{ data }">
            <div class="agency-cell">
              <div class="agency-avatar-lg">{{ data.name[0] }}</div>
              <div>
                <div class="font-semibold">{{ data.name }}</div>
                <div class="text-sm text-muted">{{ data.email }}</div>
              </div>
            </div>
          </template>
        </Column>

        <Column header="Localisation">
          <template #body="{ data }">
            <div>
              <div>{{ data.city }}</div>
              <div class="text-sm">{{ $t(`countries.${data.country}`) }}</div>
            </div>
          </template>
        </Column>

        <Column field="subscriptionPlanName" header="Plan">
          <template #body="{ data }">
            <span v-if="data.subscriptionPlanName" class="badge badge-purple">
              {{ data.subscriptionPlanName }}
            </span>
            <span v-else class="text-muted">—</span>
          </template>
        </Column>

        <Column field="status" header="Statut" sortable>
          <template #body="{ data }">
            <span :class="statusBadgeClass(data.status)">
              {{ $t(`agency.statuses.${data.status}`) }}
            </span>
          </template>
        </Column>

        <Column field="createdAt" header="Créée le" sortable>
          <template #body="{ data }">{{ formatDate(data.createdAt) }}</template>
        </Column>

        <Column header="Actions" :exportable="false" style="min-width: 140px">
          <template #body="{ data }">
            <div class="flex gap-1">
              <Button
                icon="pi pi-eye"
                size="small"
                outlined
                @click="viewAgency(data.id)"
                v-tooltip="'Voir détail'"
              />
              <Button
                v-if="data.status !== 'SUSPENDED'"
                icon="pi pi-ban"
                size="small"
                severity="danger"
                outlined
                @click="confirmSuspend(data)"
                v-tooltip="'Suspendre'"
              />
              <Button
                v-if="data.status === 'SUSPENDED'"
                icon="pi pi-check"
                size="small"
                severity="success"
                outlined
                @click="activateAgency(data.id)"
                v-tooltip="'Activer'"
              />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Dialog suspension -->
    <Dialog v-model:visible="suspendDialog" modal header="Suspendre l'agence" style="width: 400px">
      <p class="mb-3">Vous allez suspendre <strong>{{ selectedAgency?.name }}</strong>.</p>
      <div class="form-group">
        <label class="form-label">Raison de la suspension *</label>
        <Textarea v-model="suspendReason" rows="3" style="width:100%" autoResize />
      </div>
      <template #footer>
        <Button label="Annuler" severity="secondary" outlined @click="suspendDialog = false" />
        <Button label="Suspendre" severity="danger" @click="executeSuspend" :disabled="!suspendReason" />
      </template>
    </Dialog>

    <Toast />
    <ConfirmDialog />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useI18n } from 'vue-i18n'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Textarea from 'primevue/textarea'
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'
import api from '@/services/api'
import dayjs from 'dayjs'

const router = useRouter()
const toast = useToast()
const { t } = useI18n()

const agencies = ref([])
const loading = ref(false)
const searchQuery = ref('')
const statusFilter = ref(null)
const currentPage = ref(0)
const pageSize = ref(20)
const totalRecords = ref(0)

const suspendDialog = ref(false)
const selectedAgency = ref(null)
const suspendReason = ref('')

const statusOptions = [
  { label: 'Tous', value: null },
  { label: 'Actif', value: 'ACTIVE' },
  { label: 'Essai', value: 'TRIAL' },
  { label: 'Suspendu', value: 'SUSPENDED' },
  { label: 'Annulé', value: 'CANCELLED' }
]

let searchTimeout = null
const debouncedSearch = () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(loadAgencies, 400)
}

const loadAgencies = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams({
      page: currentPage.value,
      size: pageSize.value,
      sortBy: 'createdAt',
      sortDir: 'desc'
    })
    if (searchQuery.value) params.append('search', searchQuery.value)
    if (statusFilter.value) params.append('status', statusFilter.value)

    const res = await api.get(`/tenants/agencies?${params}`)
    agencies.value = res.data.content || []
    totalRecords.value = res.data.totalElements || 0
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les agences', life: 3000 })
  } finally {
    loading.value = false
  }
}

const onPage = (event) => {
  currentPage.value = event.page
  loadAgencies()
}

const viewAgency = (id) => router.push(`/admin/agencies/${id}`)

const confirmSuspend = (agency) => {
  selectedAgency.value = agency
  suspendReason.value = ''
  suspendDialog.value = true
}

const executeSuspend = async () => {
  try {
    await api.post(`/tenants/agencies/${selectedAgency.value.id}/suspend`, {
      reason: suspendReason.value
    })
    toast.add({ severity: 'warn', summary: 'Agence suspendue', life: 3000 })
    suspendDialog.value = false
    loadAgencies()
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de suspendre', life: 3000 })
  }
}

const activateAgency = async (id) => {
  try {
    await api.post(`/tenants/agencies/${id}/activate`)
    toast.add({ severity: 'success', summary: 'Agence activée', life: 3000 })
    loadAgencies()
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Erreur', life: 3000 })
  }
}

const formatDate = (date) => dayjs(date).format('DD/MM/YYYY')

const statusBadgeClass = (status) => ({
  ACTIVE: 'badge badge-success',
  TRIAL: 'badge badge-warning',
  SUSPENDED: 'badge badge-danger',
  CANCELLED: 'badge badge-info'
}[status] || 'badge')

onMounted(loadAgencies)
</script>

<style scoped>
.agency-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.agency-avatar-lg {
  width: 40px;
  height: 40px;
  background: var(--primary-light);
  color: var(--primary);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.1rem;
  flex-shrink: 0;
}

.empty-state {
  text-align: center;
  padding: 3rem;
  color: var(--text-muted);
}

.font-semibold { font-weight: 600; }
.text-sm { font-size: 0.8rem; }
.text-muted { color: var(--text-muted); }
.filters { flex-wrap: wrap; }
</style>
