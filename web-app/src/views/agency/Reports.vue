<template>
  <div class="reports-page">
    <div class="page-header">
      <h1>Rapports Financiers & Analyses</h1>
      <div class="flex gap-1">
        <Button label="Exporter Excel" icon="pi pi-file-excel" severity="success" outlined @click="exportExcel" />
        <Button label="Rapport Annuel PDF" icon="pi pi-file-pdf" severity="danger" @click="exportPDF" />
      </div>
    </div>

    <!-- Stats rapides -->
    <div class="stats-grid mb-3">
      <div class="stat-card">
        <div class="stat-icon green"><i class="pi pi-arrow-down-left"></i></div>
        <div class="stat-info">
          <div class="stat-value currency">{{ formatCurrency(4250000) }}</div>
          <div class="stat-label">Loyers Encaissés (ce mois)</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red"><i class="pi pi-arrow-up-right"></i></div>
        <div class="stat-info">
          <div class="stat-value currency">{{ formatCurrency(380000) }}</div>
          <div class="stat-label">Dépenses & Travaux</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon blue"><i class="pi pi-external-link"></i></div>
        <div class="stat-info">
          <div class="stat-value currency">{{ formatCurrency(3500000) }}</div>
          <div class="stat-label">Reversements Propriétaires</div>
        </div>
      </div>
    </div>

    <!-- Graphiques -->
    <div class="grid-2 mb-3">
      <div class="card flex flex-col gap-2">
        <h2 class="card-title"><i class="pi pi-chart-bar"></i> Flux de trésorerie mensuel (FCFA)</h2>
        <div class="chart-container" style="position: relative; height:250px;">
          <canvas ref="barChartCanvas"></canvas>
        </div>
      </div>

      <div class="card flex flex-col gap-2">
        <h2 class="card-title"><i class="pi pi-chart-pie"></i> Canaux de Paiement Préférés</h2>
        <div class="chart-container" style="position: relative; height:250px; display: flex; justify-content: center;">
          <canvas ref="pieChartCanvas"></canvas>
        </div>
      </div>
    </div>

    <!-- Tableau récapitulatif -->
    <div class="card flex flex-col gap-2">
      <h2 class="card-title"><i class="pi pi-list"></i> Résumé trimestriel</h2>
      <div class="table-container mt-1">
        <table class="report-table">
          <thead>
            <tr>
              <th>Trimestre</th>
              <th>Biens occupés</th>
              <th>Entrées (FCFA)</th>
              <th>Sorties (FCFA)</th>
              <th>Taux d'occupation</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Q2 2026</td>
              <td>14 / 15</td>
              <td class="currency">{{ formatCurrency(12450000) }}</td>
              <td class="currency">{{ formatCurrency(1150000) }}</td>
              <td><span class="badge badge-success">93.3%</span></td>
            </tr>
            <tr>
              <td>Q1 2026</td>
              <td>12 / 15</td>
              <td class="currency">{{ formatCurrency(10200000) }}</td>
              <td class="currency">{{ formatCurrency(890000) }}</td>
              <td><span class="badge badge-success">80.0%</span></td>
            </tr>
            <tr>
              <td>Q4 2025</td>
              <td>11 / 15</td>
              <td class="currency">{{ formatCurrency(9450000) }}</td>
              <td class="currency">{{ formatCurrency(1450000) }}</td>
              <td><span class="badge badge-warning">73.3%</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Toast from 'primevue/toast'
import { Chart } from 'chart.js/auto'

const toast = useToast()

const barChartCanvas = ref(null)
const pieChartCanvas = ref(null)

const formatCurrency = (val) => new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(val)

const exportExcel = () => {
  toast.add({ severity: 'success', summary: 'Export', detail: 'Export Excel généré', life: 3000 })
}

const exportPDF = () => {
  toast.add({ severity: 'info', summary: 'Rapport PDF', detail: 'Téléchargement du PDF démarré', life: 3000 })
}

onMounted(() => {
  // Bar Chart (Monthly Cash Flow)
  new Chart(barChartCanvas.value, {
    type: 'bar',
    data: {
      labels: ['Janv', 'Févr', 'Mars', 'Avril', 'Mai', 'Juin'],
      datasets: [
        {
          label: 'Encaissements',
          data: [3100000, 3200000, 3500000, 3900000, 4100000, 4250000],
          backgroundColor: '#4f46e5',
          borderRadius: 4
        },
        {
          label: 'Dépenses',
          data: [500000, 450000, 300000, 600000, 250000, 380000],
          backgroundColor: '#ef4444',
          borderRadius: 4
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        y: {
          beginAtZero: true,
          ticks: {
            callback: (value) => value / 1000 + 'k'
          }
        }
      }
    }
  })

  // Pie Chart (Payment Channels)
  new Chart(pieChartCanvas.value, {
    type: 'doughnut',
    data: {
      labels: ['Orange Money', 'Wave', 'MTN MoMo', 'Virement / Espèces'],
      datasets: [
        {
          data: [45, 30, 15, 10],
          backgroundColor: ['#f97316', '#3b82f6', '#eab308', '#10b981']
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'right'
        }
      }
    }
  })
})
</script>

<style scoped>
.card-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--primary);
  font-size: 1.1rem;
}
.report-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}
.report-table th, .report-table td {
  padding: 0.85rem 1.25rem;
  border-bottom: 1px solid var(--border);
  font-size: 0.9rem;
}
.report-table th {
  background: var(--bg-main);
  color: var(--text-secondary);
  font-weight: 600;
}
.report-table tbody tr:hover {
  background: var(--hover-bg);
}
</style>
