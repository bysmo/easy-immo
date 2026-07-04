<template>
  <div class="lease-detail-page">
    <div class="page-header" v-if="lease">
      <div class="flex items-center gap-2">
        <h1>Contrat {{ lease.reference }}</h1>
        <span :class="statusBadgeClass(lease.status)">
          {{ lease.status }}
        </span>
      </div>
      <button class="btn btn-outline" @click="$router.push('/agency/leases')">
        <i class="pi pi-arrow-left"></i>
        Retour
      </button>
    </div>

    <div v-if="loading" class="flex items-center justify-center card" style="min-height: 200px">
      <i class="pi pi-spin pi-spinner" style="font-size: 2rem; color: var(--primary)"></i>
    </div>

    <div v-else-if="lease" class="flex flex-col gap-3">
      <!-- Fiche Résumé -->
      <div class="grid-3">
        <!-- Informations Parties -->
        <div class="card flex flex-col gap-2">
          <h2 class="card-title"><i class="pi pi-users"></i> Acteurs du contrat</h2>
          
          <div class="info-block">
            <span class="info-block-title">Locataire :</span>
            <div class="font-bold">{{ lease.tenantName }}</div>
          </div>
          
          <div class="info-block mt-1">
            <span class="info-block-title">Bien Loué :</span>
            <RouterLink :to="'/agency/properties/' + lease.propertyId" class="link-bold">
              {{ lease.propertyTitle }}
            </RouterLink>
          </div>
        </div>

        <!-- Termes financiers -->
        <div class="card flex flex-col gap-2">
          <h2 class="card-title"><i class="pi pi-wallet"></i> Termes financiers</h2>
          <div class="info-row">
            <span class="info-label">Loyer Mensuel :</span>
            <span class="info-val currency">{{ formatCurrency(lease.rentAmount) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Caution versée :</span>
            <span class="info-val currency">{{ formatCurrency(lease.depositAmount) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Fréquence de paiement :</span>
            <span class="info-val">Mensuel</span>
          </div>
        </div>

        <!-- Échéance & Durée -->
        <div class="card flex flex-col gap-2 justify-between">
          <div>
            <h2 class="card-title"><i class="pi pi-calendar"></i> Calendrier</h2>
            <div class="info-row mt-1">
              <span class="info-label">Date de début :</span>
              <span class="info-val">{{ formatDate(lease.startDate) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Date de fin :</span>
              <span class="info-val">{{ formatDate(lease.endDate) }}</span>
            </div>
          </div>
          <div class="flex gap-1 mt-2">
            <Button label="Enregistrer Loyer" icon="pi pi-check" style="flex:1" @click="registerPayment" />
          </div>
        </div>
      </div>

      <!-- Historique des quittances -->
      <div class="card flex flex-col gap-2">
        <h2 class="card-title"><i class="pi pi-history"></i> Historique des loyers</h2>
        <div class="table-container mt-1">
          <DataTable :value="payments" striped-rows responsive-layout="scroll">
            <template #empty>
              <div style="text-align:center; padding:1.5rem; color:var(--text-muted)">Aucun loyer enregistré pour ce contrat.</div>
            </template>
            <Column field="period" header="Période"></Column>
            <Column field="amount" header="Montant">
              <template #body="{ data }">
                <span class="currency">{{ formatCurrency(data.amount) }}</span>
              </template>
            </Column>
            <Column field="paymentDate" header="Payé le"></Column>
            <Column field="method" header="Mode"></Column>
            <Column field="status" header="Statut">
              <template #body="{ data }">
                <span :class="data.status === 'Payé' ? 'badge badge-success' : 'badge badge-warning'">
                  {{ data.status }}
                </span>
              </template>
            </Column>
            <Column header="Reçu">
              <template #body>
                <Button icon="pi pi-file-pdf" size="small" severity="secondary" outlined v-tooltip="'Télécharger le reçu'" />
              </template>
            </Column>
          </DataTable>
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
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Toast from 'primevue/toast'
import api from '@/services/api'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const lease = ref(null)
const loading = ref(false)
const payments = ref([])

const loadLease = async () => {
  loading.value = true
  try {
    const res = await api.get(`/leases/leases/${route.params.id}`)
    lease.value = res.data
  } catch (e) {
    // Fallback Mock Data
    lease.value = {
      id: route.params.id,
      reference: 'CON-2026-0004',
      propertyId: '1',
      propertyTitle: 'Appartement F3 Zone 4',
      tenantName: 'Koffi Mensah',
      rentAmount: 350000,
      depositAmount: 700000,
      startDate: '2026-01-01',
      endDate: '2026-12-31',
      status: 'ACTIVE'
    }
    payments.value = [
      { id: 1, period: 'Juin 2026', amount: 350000, paymentDate: '02/06/2026', method: 'Wave', status: 'Payé' },
      { id: 2, period: 'Mai 2026', amount: 350000, paymentDate: '05/05/2026', method: 'Orange Money', status: 'Payé' },
      { id: 3, period: 'Avril 2026', amount: 350000, paymentDate: '01/04/2026', method: 'MTN MoMo', status: 'Payé' }
    ]
  } finally {
    loading.value = false
  }
}

const formatCurrency = (val) => new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(val)
const formatDate = (date) => dayjs(date).format('DD/MM/YYYY')

const statusBadgeClass = (status) => ({
  ACTIVE: 'badge badge-success',
  TERMINATED: 'badge badge-danger',
  EXPIRED: 'badge badge-warning'
}[status] || 'badge')

const registerPayment = () => {
  toast.add({ severity: 'success', summary: 'Paiement', detail: 'Enregistrement initié', life: 3000 })
}

onMounted(loadLease)
</script>

<style scoped>
.card-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--primary);
  font-size: 1.1rem;
}
.info-block {
  padding: 0.25rem 0;
}
.info-block-title {
  font-size: 0.85rem;
  color: var(--text-muted);
  display: block;
}
.link-bold {
  font-weight: 600;
  color: var(--primary);
  text-decoration: none;
}
.link-bold:hover {
  text-decoration: underline;
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
</style>
