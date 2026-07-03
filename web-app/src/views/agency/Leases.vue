<template>
  <div class="leases-page">
    <div class="page-header">
      <div>
        <h1>Contrats de Baux</h1>
        <p class="text-muted">Consultez l'état d'occupation des logements, générez les contrats et résiliez les baux</p>
      </div>
      <Button label="Nouveau Bail" icon="pi pi-plus" class="btn btn-primary" @click="goToCreate" />
    </div>

    <!-- Filtres -->
    <div class="card mb-3">
      <div class="flex gap-2 items-center">
        <div style="flex:1">
          <InputText v-model="searchQuery" placeholder="Rechercher par locataire, bien..." @input="debouncedSearch" style="width: 100%" />
        </div>
        <Select v-model="statusFilter" :options="statusOptions" optionLabel="label" optionValue="value" placeholder="Tous les baux" style="width: 200px" @change="loadLeases" />
        <Button icon="pi pi-refresh" severity="secondary" outlined @click="loadLeases" />
      </div>
    </div>

    <!-- Table -->
    <div class="table-container">
      <DataTable :value="leases" :loading="loading" :paginator="true" :rows="10" stripedRows responsiveLayout="scroll">
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-file" style="font-size: 3rem; color: var(--text-muted)"></i>
            <p class="mt-2">Aucun contrat de bail trouvé</p>
          </div>
        </template>

        <Column field="tenantFullName" header="Locataire" sortable>
          <template #body="{ data }">
            <div>
              <div class="font-semibold">{{ data.tenantFullName }}</div>
              <small class="text-muted">{{ data.tenantPhone }}</small>
            </div>
          </template>
        </Column>

        <Column header="Logement Référence">
          <template #body="{ data }">
            <div>
              <span class="font-semibold">{{ data.propertyReference || 'N/A' }}</span>
              <div class="text-sm text-muted">{{ data.propertyAddress }}</div>
            </div>
          </template>
        </Column>

        <Column header="Durée">
          <template #body="{ data }">
            <div>Du {{ formatDate(data.startDate) }}</div>
            <div class="text-sm text-muted">au {{ data.endDate ? formatDate(data.endDate) : 'Indéterminé' }}</div>
          </template>
        </Column>

        <Column field="monthlyRent" header="Loyer Mensuel" sortable>
          <template #body="{ data }">
            <span class="currency">{{ formatCurrency(data.monthlyRent) }}</span>
          </template>
        </Column>

        <Column field="status" header="Statut" sortable>
          <template #body="{ data }">
            <span :class="statusBadgeClass(data.status)">
              {{ data.status === 'ACTIVE' ? 'En cours' : data.status === 'TERMINATED' ? 'Résilie' : 'Expiré' }}
            </span>
          </template>
        </Column>

        <Column header="Actions" style="min-width: 180px">
          <template #body="{ data }">
            <div class="flex gap-1">
              <Button v-if="data.contractPdfUrl" icon="pi pi-download" class="p-button-sm p-button-outlined" @click="downloadContract(data.contractPdfUrl)" v-tooltip="'Télécharger le contrat'" />
              
              <Button v-if="data.status === 'ACTIVE'" icon="pi pi-refresh" severity="success" class="p-button-sm p-button-outlined" @click="openRenewDialog(data)" v-tooltip="'Renouveler le bail'" />
              
              <Button v-if="data.status === 'ACTIVE'" icon="pi pi-times-circle" severity="danger" class="p-button-sm p-button-outlined" @click="openTerminateDialog(data)" v-tooltip="'Résilier le bail'" />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Modal Renouvellement -->
    <Dialog v-model:visible="renewDialog" modal header="Renouveler le contrat de bail" style="width: 450px" class="p-fluid">
      <div class="form-group mb-2">
        <label class="form-label">Nouvelle Date de Fin *</label>
        <DatePicker v-model="renewForm.newEndDate" dateFormat="dd/mm/yy" :minDate="minEndDate" required />
      </div>

      <div class="form-group mb-2">
        <label class="form-label">Nouveau Loyer Mensuel (XOF) *</label>
        <InputNumber v-model="renewForm.newMonthlyRent" mode="decimal" required />
      </div>

      <div class="form-group mb-3">
        <label class="form-label">Notes complémentaires (avenant)</label>
        <Textarea v-model="renewForm.notes" rows="3" autoResize />
      </div>

      <template #footer>
        <Button label="Annuler" severity="secondary" outlined @click="renewDialog = false" />
        <Button label="Confirmer Renouvellement" severity="success" @click="executeRenewal" :loading="processing" />
      </template>
    </Dialog>

    <!-- Modal Résiliation -->
    <Dialog v-model:visible="terminateDialog" modal header="Résilier le contrat de bail" style="width: 450px" class="p-fluid">
      <p class="mb-3">Vous résiliez le bail de <strong>{{ selectedLease?.tenantFullName }}</strong> pour le bien <strong>{{ selectedLease?.propertyReference }}</strong>.</p>
      
      <div class="form-group mb-2">
        <label class="form-label">Date de fin effective *</label>
        <DatePicker v-model="terminateForm.terminationDate" dateFormat="dd/mm/yy" required />
      </div>

      <div class="form-group mb-2">
        <label class="form-label">Montant de caution à restituer (XOF) *</label>
        <InputNumber v-model="terminateForm.depositRefunded" mode="decimal" required />
      </div>

      <div class="form-group mb-3">
        <label class="form-label">Motif de résiliation *</label>
        <Textarea v-model="terminateForm.reason" rows="2" placeholder="Ex: Départ volontaire, non-paiement..." required />
      </div>

      <template #footer>
        <Button label="Annuler" severity="secondary" outlined @click="terminateDialog = false" />
        <Button label="Valider Résiliation" severity="danger" @click="executeTermination" :loading="processing" :disabled="!terminateForm.reason" />
      </template>
    </Dialog>

    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Dialog from 'primevue/dialog'
import Textarea from 'primevue/textarea'
import DatePicker from 'primevue/datepicker'
import Toast from 'primevue/toast'
import { useToast } from 'primevue/usetoast'
import api from '@/services/api'
import dayjs from 'dayjs'

const router = useRouter()
const toast = useToast()

const leases = ref([])
const loading = ref(false)
const processing = ref(false)
const searchQuery = ref('')
const statusFilter = ref(null)

const renewDialog = ref(false)
const terminateDialog = ref(false)
const selectedLease = ref(null)

const renewForm = ref({
  newEndDate: null,
  newMonthlyRent: 0,
  notes: ''
})

const terminateForm = ref({
  terminationDate: new Date(),
  depositRefunded: 0,
  reason: '',
  notes: ''
})

const statusOptions = [
  { label: 'Tous', value: null },
  { label: 'En cours', value: 'ACTIVE' },
  { label: 'Résilie', value: 'TERMINATED' },
  { label: 'Expiré', value: 'EXPIRED' }
]

const minEndDate = ref(new Date())

let searchTimeout = null
const debouncedSearch = () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(loadLeases, 400)
}

const loadLeases = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (searchQuery.value) params.append('search', searchQuery.value)
    if (statusFilter.value) params.append('status', statusFilter.value)

    const res = await api.get(`/leases?${params}`)
    leases.value = res.data.content || []
  } catch (e) {
    // Fallback Offline Mock
    leases.value = [
      { id: '1', tenantFullName: 'Mamadou Diallo', tenantPhone: '+22997001122', propertyReference: 'VIL-002', propertyAddress: 'Fidjrossé Cotonou', startDate: '2026-01-01', endDate: '2026-12-31', monthlyRent: 250000, depositAmount: 500000, status: 'ACTIVE' }
    ]
  } finally {
    loading.value = false
  }
}

const goToCreate = () => router.push('/agency/leases/create')

const openRenewDialog = (lease) => {
  selectedLease.value = lease
  renewForm.value = {
    newEndDate: lease.endDate ? new Date(lease.endDate) : new Date(),
    newMonthlyRent: lease.monthlyRent,
    notes: ''
  }
  minEndDate.value = new Date(lease.startDate)
  renewDialog.value = true
}

const executeRenewal = async () => {
  processing.value = true
  try {
    const payload = {
      newEndDate: dayjs(renewForm.value.newEndDate).format('YYYY-MM-DD'),
      newMonthlyRent: renewForm.value.newMonthlyRent,
      notes: renewForm.value.notes
    }
    await api.post(`/leases/${selectedLease.value.id}/renew`, payload)
    toast.add({ severity: 'success', summary: 'Contrat renouvelé', life: 3000 })
    renewDialog.value = false
    loadLeases()
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de renouveler', life: 3000 })
  } finally {
    processing.value = false
  }
}

const openTerminateDialog = (lease) => {
  selectedLease.value = lease
  terminateForm.value = {
    terminationDate: new Date(),
    depositRefunded: lease.depositAmount,
    reason: '',
    notes: ''
  }
  terminateDialog.value = true
}

const executeTermination = async () => {
  processing.value = true
  try {
    const payload = {
      terminationDate: dayjs(terminateForm.value.terminationDate).format('YYYY-MM-DD'),
      depositRefunded: terminateForm.value.depositRefunded,
      reason: terminateForm.value.reason,
      notes: terminateForm.value.notes
    }
    await api.post(`/leases/${selectedLease.value.id}/terminate`, payload)
    toast.add({ severity: 'warn', summary: 'Contrat résilié', life: 3000 })
    terminateDialog.value = false
    loadLeases()
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de résilier', life: 3000 })
  } finally {
    processing.value = false
  }
}

const downloadContract = (url) => {
  window.open(url, '_blank')
}

const formatCurrency = (value) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(value)
}

const formatDate = (date) => dayjs(date).format('DD/MM/YYYY')

const statusBadgeClass = (status) => ({
  ACTIVE: 'badge badge-success',
  TERMINATED: 'badge badge-danger',
  EXPIRED: 'badge badge-warning'
}[status] || 'badge')

onMounted(loadLeases)
</script>

<style scoped>
.font-semibold { font-weight: 600; }
.empty-state { text-align: center; padding: 2rem; color: var(--text-muted); }
.text-sm { font-size: 0.8rem; }
</style>
