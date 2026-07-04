<template>
  <div class="billing-page">
    <div class="page-header">
      <h1>Facturation & Revenus SaaS</h1>
      <button class="btn btn-outline" @click="exportData">
        <i class="pi pi-download"></i> Exporter
      </button>
    </div>

    <!-- Stats -->
    <div class="stats-grid mb-3">
      <div class="stat-card">
        <div class="stat-icon green"><i class="pi pi-chart-line"></i></div>
        <div class="stat-info">
          <div class="stat-value currency">{{ formatCurrency(2120000) }}</div>
          <div class="stat-label">Revenus Mensuels (MRR)</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon blue"><i class="pi pi-wallet"></i></div>
        <div class="stat-info">
          <div class="stat-value currency">{{ formatCurrency(14350000) }}</div>
          <div class="stat-label">Revenus Totaux Perçus</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple"><i class="pi pi-credit-card"></i></div>
        <div class="stat-info">
          <div class="stat-value">40</div>
          <div class="stat-label">Abonnements Actifs Payants</div>
        </div>
      </div>
    </div>

    <!-- Invoices List -->
    <div class="card flex flex-col gap-2">
      <h2 class="card-title"><i class="pi pi-list"></i> Historique des Factures Agences</h2>

      <div class="table-container mt-2">
        <DataTable :value="invoices" striped-rows responsive-layout="scroll" :row-hover="true">
          <Column field="invoiceNumber" header="N° Facture"></Column>
          <Column field="agencyName" header="Agence"></Column>
          <Column field="planName" header="Plan"></Column>
          <Column field="amount" header="Montant">
            <template #body="{ data }">
              <span class="currency">{{ formatCurrency(data.amount) }}</span>
            </template>
          </Column>
          <Column field="paymentMethod" header="Moyen de paiement">
            <template #body="{ data }">
              <div class="flex items-center gap-1">
                <i :class="getPaymentIcon(data.paymentMethod)"></i>
                <span>{{ data.paymentMethod }}</span>
              </div>
            </template>
          </Column>
          <Column field="paymentDate" header="Date"></Column>
          <Column field="status" header="Statut">
            <template #body="{ data }">
              <span :class="statusBadgeClass(data.status)">{{ data.status }}</span>
            </template>
          </Column>
          <Column header="Action">
            <template #body>
              <Button icon="pi pi-file-pdf" size="small" severity="secondary" outlined v-tooltip="'Télécharger PDF'" />
            </template>
          </Column>
        </DataTable>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useToast } from 'primevue/usetoast'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'

const toast = useToast()

const invoices = ref([
  { id: 1, invoiceNumber: 'FAC-2026-0045', agencyName: 'Immo Prestige', planName: 'Plan Premium', amount: 65000, paymentMethod: 'Orange Money', paymentDate: '02/07/2026', status: 'Payé' },
  { id: 2, invoiceNumber: 'FAC-2026-0044', agencyName: 'Demeures d\'Afrique', planName: 'Plan Basique', amount: 25000, paymentMethod: 'Wave', paymentDate: '01/07/2026', status: 'Payé' },
  { id: 3, invoiceNumber: 'FAC-2026-0043', agencyName: 'Sénégal Immo', planName: 'Plan Premium', amount: 65000, paymentMethod: 'Carte Bancaire', paymentDate: '28/06/2026', status: 'Payé' },
  { id: 4, invoiceNumber: 'FAC-2026-0042', agencyName: 'Togo Logement', planName: 'Plan Basique', amount: 25000, paymentMethod: 'MTN MoMo', paymentDate: '25/06/2026', status: 'Payé' },
  { id: 5, invoiceNumber: 'FAC-2026-0041', agencyName: 'Abidjan Rentals', planName: 'Plan Premium', amount: 65000, paymentMethod: 'Orange Money', paymentDate: '24/06/2026', status: 'Échoué' }
])

const formatCurrency = (val) => new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(val)

const getPaymentIcon = (method) => {
  if (method.includes('Money') || method.includes('MoMo') || method === 'Wave') return 'pi pi-mobile text-warning'
  return 'pi pi-credit-card text-info'
}

const statusBadgeClass = (status) => {
  if (status === 'Payé') return 'badge badge-success'
  if (status === 'Échoué') return 'badge badge-danger'
  return 'badge badge-warning'
}

const exportData = () => {
  toast.add({ severity: 'success', summary: 'Succès', detail: 'Export des factures initié', life: 3000 })
}
</script>

<style scoped>
.card-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--primary);
  font-size: 1.1rem;
}
.text-warning { color: var(--warning); }
.text-info { color: var(--info); }
</style>
