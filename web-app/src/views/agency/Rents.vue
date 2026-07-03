<template>
  <div class="rents-page">
    <div class="page-header">
      <div>
        <h1>Suivi des Loyers Mensuels</h1>
        <p class="text-muted">Gérez les appels de loyers, enregistrez les règlements et suivez les arriérés de paiement</p>
      </div>
    </div>

    <!-- Filtres -->
    <div class="card mb-3">
      <div class="flex gap-2 items-center">
        <div style="flex:1">
          <InputText v-model="searchQuery" placeholder="Rechercher par locataire..." @input="debouncedSearch" style="width: 100%" />
        </div>
        <Select v-model="statusFilter" :options="statusOptions" optionLabel="label" optionValue="value" placeholder="Tous les statuts" style="width: 200px" @change="loadRents" />
        <Button icon="pi pi-refresh" severity="secondary" outlined @click="loadRents" />
      </div>
    </div>

    <!-- Table -->
    <div class="table-container">
      <DataTable :value="rents" :loading="loading" :paginator="true" :rows="10" stripedRows responsiveLayout="scroll">
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-money-bill" style="font-size: 3rem; color: var(--text-muted)"></i>
            <p class="mt-2">Aucun loyer en attente de traitement</p>
          </div>
        </template>

        <Column field="tenantName" header="Locataire" sortable>
          <template #body="{ data }">
            <div class="font-semibold">{{ data.tenantName }}</div>
          </template>
        </Column>

        <Column field="propertyRef" header="Bien" />
        
        <Column header="Période">
          <template #body="{ data }">
            <span class="font-semibold">{{ formatPeriod(data.periodMonth, data.periodYear) }}</span>
          </template>
        </Column>

        <Column field="expectedAmount" header="Loyer Attendu" sortable>
          <template #body="{ data }">
            <span class="currency">{{ formatCurrency(data.expectedAmount) }}</span>
          </template>
        </Column>

        <Column field="paidAmount" header="Montant Payé">
          <template #body="{ data }">
            <span class="currency text-success font-semibold">{{ formatCurrency(data.paidAmount) }}</span>
          </template>
        </Column>

        <Column header="Reste à payer">
          <template #body="{ data }">
            <span class="currency text-danger font-semibold">{{ formatCurrency(data.expectedAmount - data.paidAmount) }}</span>
          </template>
        </Column>

        <Column field="dueDate" header="Échéance" sortable>
          <template #body="{ data }">
            <span :class="isOverdue(data.dueDate) && data.status !== 'PAID' ? 'text-danger font-semibold' : ''">
              {{ formatDate(data.dueDate) }}
            </span>
          </template>
        </Column>

        <Column field="status" header="Statut" sortable>
          <template #body="{ data }">
            <span :class="statusBadgeClass(data.status)">
              {{ data.status === 'PAID' ? 'Réglé' : data.status === 'PARTIAL' ? 'Partiel' : isOverdue(data.dueDate) ? 'En retard' : 'En attente' }}
            </span>
          </template>
        </Column>

        <Column header="Actions" style="min-width: 150px">
          <template #body="{ data }">
            <div class="flex gap-1" v-if="data.status !== 'PAID'">
              <Button label="Encaisser" icon="pi pi-check" class="p-button-sm p-button-success p-button-outlined" @click="openPaymentDialog(data)" />
              <Button label="Mobile Money" icon="pi pi-phone" class="p-button-sm p-button-primary p-button-outlined" @click="openMomoDialog(data)" />
            </div>
            <span v-else class="text-muted text-sm"><i class="pi pi-check-circle text-success"></i> Reçu émis</span>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Modal Encaissement Manuel -->
    <Dialog v-model:visible="paymentDialog" modal header="Enregistrer un paiement de loyer" style="width: 400px" class="p-fluid">
      <div class="form-group mb-2">
        <label class="form-label">Montant perçu (XOF) *</label>
        <InputNumber v-model="paymentForm.amount" mode="decimal" :max="selectedRent?.expectedAmount - selectedRent?.paidAmount" required />
      </div>

      <div class="form-group mb-2">
        <label class="form-label">Mode de règlement *</label>
        <Select v-model="paymentForm.paymentMethod" :options="manualMethods" optionLabel="label" optionValue="value" />
      </div>

      <div class="form-group mb-3">
        <label class="form-label">Référence (N° Chèque / Reçu virement)</label>
        <InputText v-model="paymentForm.reference" placeholder="Ex: CHQ-889410" />
      </div>

      <template #footer>
        <Button label="Annuler" severity="secondary" outlined @click="paymentDialog = false" />
        <Button label="Confirmer Encaissement" severity="success" @click="executeManualPayment" :loading="processing" />
      </template>
    </Dialog>

    <!-- Modal Mobile Money Checkout -->
    <Dialog v-model:visible="momoDialog" modal header="Déclencher un paiement Mobile Money" style="width: 450px" class="p-fluid">
      <p class="mb-3">Une demande de paiement USSD (Push OTP) va être envoyée sur le téléphone du locataire.</p>

      <div class="form-group mb-2">
        <label class="form-label">Téléphone de facturation *</label>
        <InputText v-model="momoForm.phone" placeholder="Ex: +22997000000" required />
      </div>

      <div class="grid-2 mb-2">
        <div class="form-group">
          <label class="form-label">Opérateur UEMOA *</label>
          <Select v-model="momoForm.provider" :options="momoProviders" optionLabel="label" optionValue="value" />
        </div>
        <div class="form-group">
          <label class="form-label">Pays UEMOA *</label>
          <Select v-model="momoForm.countryCode" :options="countryOptions" optionLabel="label" optionValue="value" />
        </div>
      </div>

      <div class="form-group mb-3">
        <label class="form-label">Montant à débiter (XOF)</label>
        <InputNumber v-model="momoForm.amount" mode="decimal" readonly />
      </div>

      <template #footer>
        <Button label="Annuler" severity="secondary" outlined @click="momoDialog = false" />
        <Button label="Envoyer push SMS / USSD" severity="primary" icon="pi pi-phone" @click="executeMomoPayment" :loading="processing" />
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
import InputNumber from 'primevue/inputnumber'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Dialog from 'primevue/dialog'
import Toast from 'primevue/toast'
import { useToast } from 'primevue/usetoast'
import api from '@/services/api'
import dayjs from 'dayjs'

const toast = useToast()

const rents = ref([])
const loading = ref(false)
const processing = ref(false)
const searchQuery = ref('')
const statusFilter = ref(null)

const paymentDialog = ref(false)
const momoDialog = ref(false)
const selectedRent = ref(null)

const paymentForm = ref({
  amount: 0,
  paymentMethod: 'CASH',
  reference: ''
})

const momoForm = ref({
  phone: '',
  provider: 'MTN_MOMO',
  countryCode: 'BJ',
  amount: 0
})

const statusOptions = [
  { label: 'Tous', value: null },
  { label: 'Réglé', value: 'PAID' },
  { label: 'Partiel', value: 'PARTIAL' },
  { label: 'En attente', value: 'PENDING' }
]

const manualMethods = [
  { label: 'Espèces (Cash)', value: 'CASH' },
  { label: 'Virement Bancaire', value: 'BANK_TRANSFER' }
]

const momoProviders = [
  { label: 'MTN Mobile Money', value: 'MTN_MOMO' },
  { label: 'Orange Money', value: 'ORANGE_MONEY' },
  { label: 'Wave', value: 'WAVE' }
]

const countryOptions = [
  { label: '🇧🇯 Bénin', value: 'BJ' },
  { label: '🇨🇮 Côte d\'Ivoire', value: 'CI' },
  { label: '🇸🇳 Sénégal', value: 'SN' },
  { label: '🇧🇫 Burkina Faso', value: 'BF' },
  { label: '🇲🇱 Mali', value: 'ML' },
  { label: '🇳🇪 Niger', value: 'NE' },
  { label: '🇹🇬 Togo', value: 'TG' }
]

let searchTimeout = null
const debouncedSearch = () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(loadRents, 400)
}

const loadRents = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (statusFilter.value) params.append('status', statusFilter.value)
    const res = await api.get(`/payments/rents?${params}`)
    rents.value = res.data.content || []
  } catch (e) {
    // Offline Mock
    rents.value = [
      { id: '1', tenantName: 'Mamadou Diallo', propertyRef: 'VIL-002', periodMonth: 7, periodYear: 2026, expectedAmount: 250000, paidAmount: 0, dueDate: '2026-07-05', status: 'PENDING', tenantPhone: '+22997001122' },
      { id: '2', tenantName: 'Awa Diop', propertyRef: 'APP-105', periodMonth: 6, periodYear: 2026, expectedAmount: 150000, paidAmount: 150000, dueDate: '2026-06-05', status: 'PAID', tenantPhone: '+221771234567' }
    ]
  } finally {
    loading.value = false
  }
}

const openPaymentDialog = (rent) => {
  selectedRent.value = rent
  paymentForm.value = {
    amount: rent.expectedAmount - rent.paidAmount,
    paymentMethod: 'CASH',
    reference: ''
  }
  paymentDialog.value = true
}

const executeManualPayment = async () => {
  processing.value = true
  try {
    await api.post(`/payments/rents/${selectedRent.value.id}/record`, paymentForm.value)
    toast.add({ severity: 'success', summary: 'Encaissement de loyer enregistré', life: 3000 })
    paymentDialog.value = false
    loadRents()
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible d\'enregistrer le paiement', life: 3000 })
  } finally {
    processing.value = false
  }
}

const openMomoDialog = (rent) => {
  selectedRent.value = rent
  momoForm.value = {
    phone: rent.tenantPhone || '',
    provider: 'MTN_MOMO',
    countryCode: 'BJ',
    amount: rent.expectedAmount - rent.paidAmount
  }
  momoDialog.value = true
}

const executeMomoPayment = async () => {
  processing.value = true
  try {
    const payload = {
      paymentType: 'RENT',
      targetId: selectedRent.value.id,
      phone: momoForm.value.phone,
      amount: momoForm.value.amount,
      provider: momoForm.value.provider,
      countryCode: momoForm.value.countryCode
    }

    const res = await api.post('/payments/momo/initiate', payload)
    toast.add({ severity: 'info', summary: 'Paiement initié', detail: res.data.message, life: 5000 })
    momoDialog.value = false
    loadRents()
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Erreur', detail: 'Échec d\'initiation Mobile Money', life: 3000 })
  } finally {
    processing.value = false
  }
}

const formatCurrency = (value) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(value)
}

const formatDate = (date) => dayjs(date).format('DD/MM/YYYY')

const formatPeriod = (month, year) => {
  const months = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre']
  return `${months[month - 1]} ${year}`
}

const isOverdue = (dueDate) => {
  return dayjs(dueDate).isBefore(dayjs(), 'day')
}

const statusBadgeClass = (status) => ({
  PAID: 'badge badge-success',
  PARTIAL: 'badge badge-warning',
  PENDING: 'badge badge-danger'
}[status] || 'badge')

onMounted(loadRents)
</script>

<style scoped>
.font-semibold { font-weight: 600; }
.empty-state { text-align: center; padding: 2rem; color: var(--text-muted); }
.text-success { color: var(--success); }
.text-danger { color: var(--danger); }
.text-sm { font-size: 0.8rem; }
</style>
