<template>
  <div class="disbursements-page">
    <div class="page-header">
      <div>
        <h1>Reversements Propriétaires</h1>
        <p class="text-muted">Consultez et validez les montants nets à reverser aux bailleurs après déduction des commissions agence et des réparations</p>
      </div>
    </div>

    <!-- Filtres -->
    <div class="card mb-3">
      <div class="flex gap-2 items-center">
        <div style="flex:1">
          <InputText v-model="searchQuery" placeholder="Rechercher par propriétaire..." @input="debouncedSearch" style="width: 100%" />
        </div>
        <Select v-model="statusFilter" :options="statusOptions" optionLabel="label" optionValue="value" placeholder="Tous les statuts" style="width: 200px" @change="loadDisbursements" />
        <Button icon="pi pi-refresh" severity="secondary" outlined @click="loadDisbursements" />
      </div>
    </div>

    <!-- Table -->
    <div class="table-container">
      <DataTable :value="disbursements" :loading="loading" :paginator="true" :rows="10" stripedRows responsiveLayout="scroll">
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-send" style="font-size: 3rem; color: var(--text-muted)"></i>
            <p class="mt-2">Aucun reversement propriétaire en attente</p>
          </div>
        </template>

        <Column field="ownerName" header="Bailleur" sortable>
          <template #body="{ data }">
            <div class="font-semibold">{{ data.ownerName }}</div>
          </template>
        </Column>

        <Column field="propertyRef" header="Bien géré" />

        <Column header="Période">
          <template #body="{ data }">
            <span class="font-semibold">{{ formatPeriod(data.periodMonth, data.periodYear) }}</span>
          </template>
        </Column>

        <Column field="totalRentCollected" header="Collecté ce mois">
          <template #body="{ data }">
            <span class="currency">{{ formatCurrency(data.totalRentCollected) }}</span>
          </template>
        </Column>

        <Column field="agencyCommission" header="Com. Agence">
          <template #body="{ data }">
            <span class="currency text-muted">{{ formatCurrency(data.agencyCommission) }}</span>
          </template>
        </Column>

        <Column field="repairsDeducted" header="Travaux Déduits">
          <template #body="{ data }">
            <span class="currency text-danger font-semibold" v-if="data.repairsDeducted > 0">- {{ formatCurrency(data.repairsDeducted) }}</span>
            <span v-else>—</span>
          </template>
        </Column>

        <Column field="netOwnerAmount" header="Net à reverser" sortable>
          <template #body="{ data }">
            <span class="currency text-success font-semibold" style="font-size:1.1rem">{{ formatCurrency(data.netOwnerAmount) }}</span>
          </template>
        </Column>

        <Column field="status" header="Statut" sortable>
          <template #body="{ data }">
            <span :class="statusBadgeClass(data.status)">
              {{ data.status === 'PAID' ? 'Transféré' : 'En attente' }}
            </span>
          </template>
        </Column>

        <Column header="Actions" style="min-width: 140px">
          <template #body="{ data }">
            <Button v-if="data.status === 'PENDING'" label="Payer le Bailleur" icon="pi pi-send" class="p-button-sm p-button-success p-button-outlined" @click="openDisburseDialog(data)" />
            <div v-else>
              <div class="text-sm text-success font-semibold"><i class="pi pi-check"></i> Transféré</div>
              <small class="text-muted">{{ formatDate(data.paidAt) }} ({{ data.paymentMethod }})</small>
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Modal Validation de Payout/Reversement -->
    <Dialog v-model:visible="disburseDialog" modal header="Valider le reversement bailleur" style="width: 420px" class="p-fluid">
      <p class="mb-3">Vous confirmez le reversement net de <strong class="text-success">{{ formatCurrency(selectedDisbursement?.netOwnerAmount) }}</strong> à <strong>{{ selectedDisbursement?.ownerName }}</strong>.</p>
      
      <div class="form-group mb-3">
        <label class="form-label">Mode de transfert choisi *</label>
        <Select v-model="payoutMethod" :options="disburseMethods" optionLabel="label" optionValue="value" />
      </div>

      <template #footer>
        <Button label="Annuler" severity="secondary" outlined @click="disburseDialog = false" />
        <Button label="Confirmer le transfert" severity="success" icon="pi pi-check" @click="executeDisbursement" :loading="processing" />
      </template>
    </Dialog>

    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Dialog from 'primevue/dialog'
import Toast from 'primevue/toast'
import { useToast } from 'primevue/usetoast'
import api from '@/services/api'
import dayjs from 'dayjs'

const toast = useToast()

const disbursements = ref([])
const loading = ref(false)
const processing = ref(false)
const searchQuery = ref('')
const statusFilter = ref(null)

const disburseDialog = ref(false)
const selectedDisbursement = ref(null)
const payoutMethod = ref('CASH')

const statusOptions = [
  { label: 'Tous', value: null },
  { label: 'En attente', value: 'PENDING' },
  { label: 'Transférés', value: 'PAID' }
]

const disburseMethods = [
  { label: 'Espèces (Caisse Agence)', value: 'CASH' },
  { label: 'Virement bancaire', value: 'BANK_TRANSFER' },
  { label: 'MTN Mobile Money', value: 'MTN_MOMO' },
  { label: 'Orange Money', value: 'ORANGE_MONEY' }
]

let searchTimeout = null
const debouncedSearch = () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(loadDisbursements, 400)
}

const loadDisbursements = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (statusFilter.value) params.append('status', statusFilter.value)
    const res = await api.get(`/payments/disbursements?${params}`)
    disbursements.value = res.data.content || []
  } catch (e) {
    // Offline Mock
    disbursements.value = [
      { id: '1', ownerName: 'Alain Koffi', propertyRef: 'VIL-002', periodMonth: 6, periodYear: 2026, totalRentCollected: 250000, ownerSharePct: 80, ownerAmount: 200000, agencyCommission: 50000, repairsDeducted: 35000, netOwnerAmount: 165000, status: 'PENDING' },
      { id: '2', ownerName: 'Fatou Diop', propertyRef: 'APP-105', periodMonth: 6, periodYear: 2026, totalRentCollected: 300000, ownerSharePct: 85, ownerAmount: 255000, agencyCommission: 45000, repairsDeducted: 0, netOwnerAmount: 255000, status: 'PAID', paidAt: '2026-06-10T14:30:00Z', paymentMethod: 'ORANGE_MONEY' }
    ]
  } finally {
    loading.value = false
  }
}

const openDisburseDialog = (disbursement) => {
  selectedDisbursement.value = disbursement
  payoutMethod.value = 'CASH'
  disburseDialog.value = true
}

const executeDisbursement = async () => {
  processing.value = true
  try {
    await api.post(`/payments/disbursements/${selectedDisbursement.value.id}/pay`, {
      paymentMethod: payoutMethod.value
    })
    toast.add({ severity: 'success', summary: 'Reversement validé avec succès', life: 3000 })
    disburseDialog.value = false
    loadDisbursements()
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de valider le reversement', life: 3000 })
  } finally {
    processing.value = false
  }
}

const formatCurrency = (value) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(value)
}

const formatDate = (date) => dayjs(date).format('DD/MM/YYYY [à] HH:mm')

const formatPeriod = (month, year) => {
  const months = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre']
  return `${months[month - 1]} ${year}`
}

const statusBadgeClass = (status) => ({
  PAID: 'badge badge-success',
  PENDING: 'badge badge-warning'
}[status] || 'badge')

onMounted(loadDisbursements)
</script>

<style scoped>
.font-semibold { font-weight: 600; }
.empty-state { text-align: center; padding: 2rem; color: var(--text-muted); }
.text-success { color: var(--success); }
.text-danger { color: var(--danger); }
.text-muted { color: var(--text-muted); }
.text-sm { font-size: 0.8rem; }
</style>
