<template>
  <div class="settings-page">
    <div class="page-header">
      <h1>Configuration de l'agence</h1>
      <Button label="Sauvegarder tout" icon="pi pi-save" @click="saveAllSettings" />
    </div>

    <div class="grid-3">
      <!-- Navigation gauche -->
      <div class="card flex flex-col gap-1" style="height: fit-content;">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          class="tab-btn"
          :class="{ 'active': activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          <i :class="tab.icon"></i>
          {{ tab.label }}
        </button>
      </div>

      <!-- Contenu principal -->
      <div class="card flex flex-col gap-3" style="grid-column: span 2;">
        
        <!-- Tab: Profile -->
        <div v-if="activeTab === 'profile'" class="flex flex-col gap-2">
          <h2 class="tab-title"><i class="pi pi-building"></i> Profil de l'agence</h2>
          <div class="form-group">
            <label class="form-label">Nom de l'agence</label>
            <InputText v-model="profile.name" style="width:100%" />
          </div>
          <div class="grid-2">
            <div class="form-group">
              <label class="form-label">Téléphone</label>
              <InputText v-model="profile.phone" style="width:100%" />
            </div>
            <div class="form-group">
              <label class="form-label">Email de contact</label>
              <InputText v-model="profile.email" style="width:100%" />
            </div>
          </div>
          <div class="grid-2">
            <div class="form-group">
              <label class="form-label">Ville</label>
              <InputText v-model="profile.city" style="width:100%" />
            </div>
            <div class="form-group">
              <label class="form-label">Numéro IFU (Identifiant Fiscal Unique)</label>
              <InputText v-model="profile.ifu" style="width:100%" placeholder="12020XXXXXXXX" />
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">Adresse physique</label>
            <InputText v-model="profile.address" style="width:100%" />
          </div>
        </div>

        <!-- Tab: Payments -->
        <div v-if="activeTab === 'payments'" class="flex flex-col gap-2">
          <h2 class="tab-title"><i class="pi pi-credit-card"></i> Paramètres & Passerelles de Paiement</h2>
          <div class="info-box mb-2">
            La devise de l'agence est configurée sur le <strong>Franc CFA (XOF)</strong>.
          </div>
          
          <div class="form-group">
            <label class="form-label">Passerelle de paiement mobile (Mobile Money)</label>
            <div class="flex flex-col gap-1 mt-1">
              <label class="flex items-center gap-1">
                <input type="checkbox" v-model="payments.wave" />
                <span>Activer les paiements via <strong>Wave</strong></span>
              </label>
              <label class="flex items-center gap-1">
                <input type="checkbox" v-model="payments.orange" />
                <span>Activer les paiements via <strong>Orange Money</strong></span>
              </label>
              <label class="flex items-center gap-1">
                <input type="checkbox" v-model="payments.mtn" />
                <span>Activer les paiements via <strong>MTN MoMo</strong></span>
              </label>
            </div>
          </div>

          <div class="form-group mt-1">
            <label class="form-label">Frais de service (refacturation locataire)</label>
            <Select
              v-model="payments.feeMode"
              :options="feeOptions"
              optionLabel="label"
              optionValue="value"
              style="width: 100%"
            />
          </div>
        </div>

        <!-- Tab: Contracts -->
        <div v-if="activeTab === 'contracts'" class="flex flex-col gap-2">
          <h2 class="tab-title"><i class="pi pi-file"></i> Préférences de location & SMS</h2>
          <div class="form-group">
            <label class="form-label">Délai d'envoi des rappels de loyer</label>
            <Select
              v-model="contracts.reminderDays"
              :options="reminderOptions"
              optionLabel="label"
              optionValue="value"
              style="width:100%"
            />
          </div>
          <div class="form-group">
            <label class="form-label">Notifications automatiques</label>
            <div class="flex flex-col gap-1 mt-1">
              <label class="flex items-center gap-1">
                <input type="checkbox" v-model="contracts.notifySms" />
                <span>Envoyer un reçu SMS automatique lors des encaissements</span>
              </label>
              <label class="flex items-center gap-1">
                <input type="checkbox" v-model="contracts.notifyEmail" />
                <span>Envoyer la quittance de loyer par email au locataire</span>
              </label>
            </div>
          </div>
        </div>

        <!-- Tab: Collaborators -->
        <div v-if="activeTab === 'team'" class="flex flex-col gap-2">
          <div class="flex justify-between items-center">
            <h2 class="tab-title"><i class="pi pi-users"></i> Agents & Collaborateurs</h2>
            <Button label="Inviter un agent" icon="pi pi-plus" size="small" outlined @click="inviteAgent" />
          </div>
          
          <div class="table-container mt-2">
            <DataTable :value="team" striped-rows responsive-layout="scroll">
              <Column field="fullName" header="Nom complet"></Column>
              <Column field="role" header="Rôle">
                <template #body="{ data }">
                  <span class="badge badge-purple">{{ data.role }}</span>
                </template>
              </Column>
              <Column field="email" header="Email"></Column>
              <Column field="status" header="Statut">
                <template #body="{ data }">
                  <span :class="data.status === 'Actif' ? 'badge badge-success' : 'badge badge-warning'">
                    {{ data.status }}
                  </span>
                </template>
              </Column>
            </DataTable>
          </div>
        </div>
      </div>
    </div>

    <!-- Dialog pour inviter un agent -->
    <Dialog v-model:visible="agentDialog" header="Inviter un collaborateur" :modal="true" style="width: 500px">
      <div class="grid-2 mb-2">
        <div class="form-group">
          <label class="form-label">Prénom *</label>
          <InputText v-model="agentForm.firstName" required style="width: 100%" />
        </div>
        <div class="form-group">
          <label class="form-label">Nom *</label>
          <InputText v-model="agentForm.lastName" required style="width: 100%" />
        </div>
      </div>

      <div class="form-group mb-2">
        <label class="form-label">Email de l'agent *</label>
        <InputText type="email" v-model="agentForm.email" required style="width: 100%" />
      </div>

      <div class="grid-2 mb-2">
        <div class="form-group">
          <label class="form-label">Téléphone</label>
          <InputText v-model="agentForm.phone" style="width: 100%" placeholder="+225 01020304" />
        </div>
        <div class="form-group">
          <label class="form-label">Rôle *</label>
          <Select
            v-model="agentForm.role"
            :options="roleOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Sélectionner le rôle"
            style="width: 100%"
            required
          />
        </div>
      </div>

      <div class="form-group mb-2">
        <label class="form-label">Identifiant (Username) *</label>
        <InputText v-model="agentForm.username" required style="width: 100%" />
      </div>

      <div class="form-group mb-3">
        <label class="form-label">Mot de passe provisoire *</label>
        <InputText type="password" v-model="agentForm.password" required style="width: 100%" placeholder="••••••••" />
        <small class="text-muted">L'agent sera invité à changer ce mot de passe à sa première connexion.</small>
      </div>

      <template #footer>
        <Button label="Annuler" severity="secondary" outlined @click="agentDialog = false" />
        <Button label="Créer l'agent" severity="primary" @click="saveAgent" :loading="savingAgent" />
      </template>
    </Dialog>

    <Toast />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import Toast from 'primevue/toast'
import api from '@/services/api'
import { getUserInfo } from '@/services/keycloak'

const toast = useToast()
const activeTab = ref('profile')

const tabs = [
  { id: 'profile', label: 'Profil de l\'agence', icon: 'pi pi-building' },
  { id: 'payments', label: 'Passerelles de Paiement', icon: 'pi pi-credit-card' },
  { id: 'contracts', label: 'Bail & Notifications', icon: 'pi pi-file' },
  { id: 'team', label: 'Agents & Collaborateurs', icon: 'pi pi-users' }
]

const userInfo = getUserInfo()
const agencyId = userInfo.agencyId

const profile = ref({
  name: '',
  phone: '',
  email: '',
  city: '',
  address: '',
  ifu: '',
  country: ''
})

const payments = ref({
  wave: true,
  orange: true,
  mtn: false,
  feeMode: 'ABSORBED'
})

const feeOptions = [
  { label: 'Frais de transaction absorbés par l\'agence', value: 'ABSORBED' },
  { label: 'Frais de transaction facturés en sus au locataire (1%)', value: 'LOCATAIRE_PERCENT' },
  { label: 'Partage des frais (0.5% agence / 0.5% locataire)', value: 'SHARED' }
]

const contracts = ref({
  reminderDays: 5,
  notifySms: true,
  notifyEmail: true
})

const reminderOptions = [
  { label: '5 jours avant l\'échéance', value: 5 },
  { label: '3 jours avant l\'échéance', value: 3 },
  { label: 'Le jour de l\'échéance', value: 0 },
  { label: 'Pas de rappel automatique', value: -1 }
]

const team = ref([])
const agentDialog = ref(false)
const savingAgent = ref(false)
const agentForm = ref({
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  username: '',
  password: '',
  role: 'AGENCY_AGENT'
})

const roleOptions = [
  { label: 'Agent Immobilier', value: 'AGENCY_AGENT' },
  { label: 'Administrateur d\'Agence', value: 'AGENCY_ADMIN' }
]

const fetchCollaborators = async () => {
  try {
    const res = await api.get('/tenants/agencies/collaborators')
    team.value = res.data.map(c => ({
      id: c.id,
      fullName: c.fullName,
      role: c.role === 'AGENCY_ADMIN' ? 'Administrateur' : 'Agent',
      email: c.email,
      status: c.status === 'ACTIVE' ? 'Actif' : c.status === 'INVITED' ? 'Invité' : 'Inactif'
    }))
  } catch (e) {
    console.error('Erreur chargement collaborateurs', e)
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de charger la liste des collaborateurs',
      life: 3000
    })
  }
}

const fetchAgencyProfile = async () => {
  if (!agencyId) return
  try {
    const res = await api.get(`/tenants/agencies/${agencyId}`)
    profile.value = {
      name: res.data.name,
      phone: res.data.phone,
      email: res.data.email,
      city: res.data.city,
      address: res.data.address || '',
      ifu: '',
      country: res.data.country || 'BF'
    }
  } catch (e) {
    console.error('Erreur chargement profil agence', e)
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Impossible de charger le profil de l\'agence',
      life: 3000
    })
  }
}

onMounted(() => {
  fetchAgencyProfile()
  fetchCollaborators()
})

const saveAllSettings = async () => {
  if (!agencyId) return
  try {
    await api.put(`/tenants/agencies/${agencyId}`, {
      name: profile.value.name,
      phone: profile.value.phone,
      address: profile.value.address,
      city: profile.value.city,
      country: profile.value.country || 'BF'
    })
    toast.add({ severity: 'success', summary: 'Configuration sauvée', detail: 'Modifications enregistrées avec succès', life: 3000 })
  } catch (e) {
    console.error('Erreur sauvegarde profil agence', e)
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: e.response?.data?.message || 'Impossible de sauvegarder le profil de l\'agence',
      life: 3000
    })
  }
}

const inviteAgent = () => {
  agentDialog.value = true
}

const saveAgent = async () => {
  if (!agentForm.value.firstName || !agentForm.value.lastName || !agentForm.value.email || !agentForm.value.username || !agentForm.value.password || !agentForm.value.role) {
    toast.add({ severity: 'warn', summary: 'Attention', detail: 'Veuillez remplir tous les champs obligatoires', life: 3000 })
    return
  }
  savingAgent.value = true
  try {
    await api.post('/tenants/agencies/collaborators', agentForm.value)
    toast.add({ severity: 'success', summary: 'Succès', detail: 'Collaborateur invité avec succès', life: 3000 })
    agentDialog.value = false
    fetchCollaborators()
    // Reset form
    agentForm.value = {
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      username: '',
      password: '',
      role: 'AGENCY_AGENT'
    }
  } catch (e) {
    console.error('Erreur invitation collaborateur', e)
    toast.add({
      severity: 'error',
      summary: 'Erreur',
      detail: e.response?.data?.message || 'Impossible d\'inviter le collaborateur',
      life: 3000
    })
  } finally {
    savingAgent.value = false
  }
}
</script>

<style scoped>
.tab-btn {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  padding: 0.85rem 1.25rem;
  background: transparent;
  border: none;
  border-radius: var(--radius);
  text-align: left;
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition);
}
.tab-btn:hover {
  background: var(--hover-bg);
  color: var(--text-primary);
}
.tab-btn.active {
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 600;
}
.tab-title {
  font-size: 1.2rem;
  color: var(--primary);
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}
.info-box {
  background: var(--primary-light);
  padding: 0.75rem;
  border-radius: var(--radius);
  font-size: 0.9rem;
  color: var(--text-secondary);
}
.flex-col {
  display: flex;
  flex-direction: column;
}
</style>
