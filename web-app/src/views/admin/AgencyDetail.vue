<template>
  <div class="agency-detail-page">
    <div class="page-header" v-if="agency">
      <div class="flex items-center gap-2">
        <h1>{{ agency.name }}</h1>
        <span :class="statusBadgeClass(agency.status)">
          {{ agency.status }}
        </span>
      </div>
      <button class="btn btn-outline" @click="$router.push('/admin/agencies')">
        <i class="pi pi-arrow-left"></i>
        Retour
      </button>
    </div>

    <div v-if="loading" class="flex items-center justify-center card" style="min-height: 200px">
      <i class="pi pi-spin pi-spinner" style="font-size: 2rem; color: var(--primary)"></i>
    </div>

    <div v-else-if="agency" class="flex flex-col gap-3">
      <!-- Statistiques rapides -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon purple"><i class="pi pi-home"></i></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.properties || 0 }}</div>
            <div class="stat-label">Biens Immobiliers</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon blue"><i class="pi pi-file"></i></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.leases || 0 }}</div>
            <div class="stat-label">Contrats Actifs</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon green"><i class="pi pi-wallet"></i></div>
          <div class="stat-info">
            <div class="stat-value currency">{{ formatCurrency(stats.revenue || 0) }}</div>
            <div class="stat-label">Paiements Encaissés</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon yellow"><i class="pi pi-users"></i></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.agents || 0 }}</div>
            <div class="stat-label">Agents Actifs</div>
          </div>
        </div>
      </div>

      <!-- Corps principal -->
      <div class="grid-2">
        <!-- Infos agence & Actions -->
        <div class="flex flex-col gap-3">
          <div class="card flex flex-col gap-2">
            <h2 class="card-title"><i class="pi pi-info-circle"></i> Fiche d'information</h2>
            
            <div class="info-row">
              <span class="info-label">Pays :</span>
              <span class="info-val">{{ agency.country }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Ville :</span>
              <span class="info-val">{{ agency.city }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Adresse :</span>
              <span class="info-val">{{ agency.address || '—' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Email :</span>
              <span class="info-val">{{ agency.email }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Téléphone :</span>
              <span class="info-val">{{ agency.phone }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">Création :</span>
              <span class="info-val">{{ formatDate(agency.createdAt) }}</span>
            </div>
          </div>

          <div class="card flex flex-col gap-2">
            <h2 class="card-title"><i class="pi pi-shield"></i> Administration & Statut</h2>
            
            <div class="info-row">
              <span class="info-label">Plan d'abonnement :</span>
              <span class="badge badge-purple">{{ agency.subscriptionPlanName || '—' }}</span>
            </div>

            <div class="flex gap-1 mt-2">
              <Button
                v-if="agency.status !== 'SUSPENDED'"
                label="Suspendre l'agence"
                icon="pi pi-ban"
                severity="danger"
                style="flex: 1"
                @click="confirmSuspend"
              />
              <Button
                v-else
                label="Activer l'agence"
                icon="pi pi-check"
                severity="success"
                style="flex: 1"
                @click="activateAgency"
              />
            </div>
          </div>
        </div>

        <!-- Mocks managers & derniers événements -->
        <div class="card flex flex-col gap-2">
          <h2 class="card-title"><i class="pi pi-history"></i> Activité récente</h2>
          <div class="timeline flex flex-col gap-2">
            <div class="timeline-item flex gap-1 items-center">
              <i class="pi pi-check-circle text-success"></i>
              <div>
                <p class="timeline-title">Realm Keycloak configuré</p>
                <small class="text-muted">Il y a 10 minutes</small>
              </div>
            </div>
            <div class="timeline-item flex gap-1 items-center">
              <i class="pi pi-user text-blue"></i>
              <div>
                <p class="timeline-title">Administrateur créé</p>
                <small class="text-muted">Il y a 10 minutes</small>
              </div>
            </div>
            <div class="timeline-item flex gap-1 items-center">
              <i class="pi pi-plus-circle text-purple"></i>
              <div>
                <p class="timeline-title">Compte agence enregistré</p>
                <small class="text-muted">Il y a 11 minutes</small>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Dialog suspension -->
    <Dialog v-model:visible="suspendDialog" modal header="Suspendre l'agence" style="width: 400px">
      <p class="mb-3">Vous allez suspendre l'agence <strong>{{ agency?.name }}</strong>.</p>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Textarea from 'primevue/textarea'
import Toast from 'primevue/toast'
import api from '@/services/api'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const agency = ref(null)
const loading = ref(false)
const suspendDialog = ref(false)
const suspendReason = ref('')

const stats = ref({
  properties: 12,
  leases: 8,
  revenue: 850000,
  agents: 3
})

const loadAgency = async () => {
  loading.value = true
  try {
    const res = await api.get(`/tenants/agencies/${route.params.id}`)
    agency.value = res.data
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger l\'agence', life: 3000 })
  } finally {
    loading.value = false
  }
}

const confirmSuspend = () => {
  suspendReason.value = ''
  suspendDialog.value = true
}

const executeSuspend = async () => {
  try {
    await api.post(`/tenants/agencies/${agency.value.id}/suspend`, {
      reason: suspendReason.value
    })
    toast.add({ severity: 'warn', summary: 'Agence suspendue', life: 3000 })
    suspendDialog.value = false
    loadAgency()
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de suspendre', life: 3000 })
  }
}

const activateAgency = async () => {
  try {
    await api.post(`/tenants/agencies/${agency.value.id}/activate`)
    toast.add({ severity: 'success', summary: 'Agence activée', life: 3000 })
    loadAgency()
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Erreur', life: 3000 })
  }
}

const formatDate = (date) => dayjs(date).format('DD/MM/YYYY HH:mm')
const formatCurrency = (val) => new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(val)

const statusBadgeClass = (status) => ({
  ACTIVE: 'badge badge-success',
  TRIAL: 'badge badge-warning',
  SUSPENDED: 'badge badge-danger',
  CANCELLED: 'badge badge-info'
}[status] || 'badge')

onMounted(loadAgency)
</script>

<style scoped>
.card-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--primary);
  font-size: 1.1rem;
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
.timeline-item {
  padding: 0.5rem 0;
}
.timeline-title {
  font-size: 0.9rem;
  font-weight: 500;
}
.text-success { color: var(--success); }
.text-blue { color: var(--info); }
.text-purple { color: var(--primary); }
</style>
