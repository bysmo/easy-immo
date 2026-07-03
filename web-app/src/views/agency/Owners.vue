<template>
  <div class="owners-page">
    <div class="page-header">
      <div>
        <h1>Propriétaires</h1>
        <p class="text-muted">Gérez les fiches des bailleurs et leurs coordonnées de reversement bancaires/Momo</p>
      </div>
      <Button label="Nouveau Propriétaire" icon="pi pi-plus" class="btn btn-primary" @click="openNew" />
    </div>

    <!-- Filtres -->
    <div class="card mb-3">
      <div class="flex gap-2 items-center">
        <div style="flex:1">
          <InputText v-model="searchQuery" placeholder="Rechercher par nom, téléphone, email..." @input="debouncedSearch" style="width: 100%" />
        </div>
        <Button icon="pi pi-refresh" severity="secondary" outlined @click="loadOwners" />
      </div>
    </div>

    <!-- Table -->
    <div class="table-container">
      <DataTable :value="owners" :loading="loading" :paginator="true" :rows="10" stripedRows responsiveLayout="scroll">
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-users" style="font-size: 3rem; color: var(--text-muted)"></i>
            <p class="mt-2">Aucun propriétaire trouvé</p>
          </div>
        </template>

        <Column field="fullName" header="Nom Complet" sortable>
          <template #body="{ data }">
            <div class="font-semibold">{{ data.fullName }}</div>
          </template>
        </Column>

        <Column field="phone" header="Téléphone" />
        <Column field="email" header="Email" />
        
        <Column header="Part Reversée">
          <template #body="{ data }">
            <span class="badge badge-success">{{ data.sharePercentage }} %</span>
          </template>
        </Column>

        <Column header="Mode Reversement">
          <template #body="{ data }">
            <span v-if="data.bankName">🏦 {{ data.bankName }}</span>
            <span v-else-if="data.mobileMoneyPhone">📱 {{ data.mobileMoneyProvider.toUpperCase() }} ({{ data.mobileMoneyPhone }})</span>
            <span v-else class="text-muted">Non défini</span>
          </template>
        </Column>

        <Column field="propertyCount" header="Biens associés">
          <template #body="{ data }">
            <span class="badge badge-purple">{{ data.propertyCount }} biens</span>
          </template>
        </Column>

        <Column header="Actions" style="min-width: 100px">
          <template #body="{ data }">
            <div class="flex gap-1">
              <Button icon="pi pi-pencil" class="p-button-sm p-button-outlined" @click="editOwner(data)" />
              <Button icon="pi pi-trash" severity="danger" class="p-button-sm p-button-outlined" @click="confirmDeleteOwner(data)" />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Modal Formulaire -->
    <Dialog v-model:visible="ownerDialog" modal :header="dialogTitle" style="width: 600px" class="p-fluid">
      <div class="grid-2 mb-2">
        <div class="form-group">
          <label class="form-label">Prénom *</label>
          <InputText v-model="form.firstName" required autofocus />
        </div>
        <div class="form-group">
          <label class="form-label">Nom de famille *</label>
          <InputText v-model="form.lastName" required />
        </div>
      </div>

      <div class="grid-2 mb-2">
        <div class="form-group">
          <label class="form-label">Téléphone *</label>
          <InputText v-model="form.phone" required />
        </div>
        <div class="form-group">
          <label class="form-label">Email</label>
          <InputText v-model="form.email" />
        </div>
      </div>

      <div class="grid-2 mb-2">
        <div class="form-group">
          <label class="form-label">N° Identité (CNI / Passeport)</label>
          <InputText v-model="form.nationalId" />
        </div>
        <div class="form-group">
          <label class="form-label">Part reversée au propriétaire (%) *</label>
          <InputNumber v-model="form.sharePercentage" suffix=" %" :min="0" :max="100" />
        </div>
      </div>

      <div class="form-group mb-2">
        <label class="form-label">Adresse de résidence</label>
        <InputText v-model="form.address" />
      </div>

      <div class="menu-category" style="padding-left:0; margin-top:1rem">Coordonnées de Reversement Financier</div>

      <div class="grid-2 mb-2">
        <div class="form-group">
          <label class="form-label">Nom de la Banque</label>
          <InputText v-model="form.bankName" placeholder="Ex: BOA, SG, Ecobank" />
        </div>
        <div class="form-group">
          <label class="form-label">RIB / N° de Compte</label>
          <InputText v-model="form.bankAccount" />
        </div>
      </div>

      <div class="grid-2 mb-2">
        <div class="form-group">
          <label class="form-label">Téléphone Mobile Money</label>
          <InputText v-model="form.mobileMoneyPhone" placeholder="Ex: +22997000000" />
        </div>
        <div class="form-group">
          <label class="form-label">Opérateur Mobile Money</label>
          <Select v-model="form.mobileMoneyProvider" :options="momoProviders" optionLabel="label" optionValue="value" placeholder="Sélectionner" />
        </div>
      </div>

      <div class="form-group mb-3">
        <label class="form-label">Notes privées / Informations complémentaires</label>
        <Textarea v-model="form.notes" rows="2" autoResize />
      </div>

      <template #footer>
        <Button label="Annuler" severity="secondary" outlined @click="ownerDialog = false" />
        <Button label="Enregistrer" severity="primary" @click="saveOwner" :loading="saving" />
      </template>
    </Dialog>

    <Toast />
    <ConfirmDialog />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Button from 'primevue/button'
import Select from 'primevue/select'
import Dialog from 'primevue/dialog'
import Textarea from 'primevue/textarea'
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import api from '@/services/api'

const toast = useToast()
const confirm = useConfirm()

const owners = ref([])
const loading = ref(false)
const saving = ref(false)
const searchQuery = ref('')

const ownerDialog = ref(false)
const form = ref({
  id: null,
  firstName: '',
  lastName: '',
  phone: '',
  email: '',
  nationalId: '',
  address: '',
  bankName: '',
  bankAccount: '',
  mobileMoneyPhone: '',
  mobileMoneyProvider: '',
  sharePercentage: 80,
  notes: ''
})

const momoProviders = [
  { label: 'Aucun', value: '' },
  { label: 'MTN Mobile Money', value: 'mtn' },
  { label: 'Orange Money', value: 'orange' }
]

const dialogTitle = computed(() => form.value.id ? 'Modifier le propriétaire' : 'Nouveau propriétaire')

let searchTimeout = null
const debouncedSearch = () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(loadOwners, 400)
}

const loadOwners = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (searchQuery.value) params.append('search', searchQuery.value)
    
    // Appel direct au microservice properties via api-gateway
    const res = await api.get(`/properties/owners?${params}`)
    owners.value = res.data.content || []
  } catch (e) {
    // Mode offline/mock fallback s'il n'y a pas de connexion DB
    owners.value = [
      { id: '1', fullName: 'Alain Koffi', phone: '+22997000102', email: 'alain.koffi@yahoo.com', sharePercentage: 80, bankName: 'BOA Benin', propertyCount: 3 },
      { id: '2', fullName: 'Fatou Diop', phone: '+221771234567', email: 'fatou.diop@gmail.com', sharePercentage: 85, mobileMoneyPhone: '+221771234567', mobileMoneyProvider: 'orange', propertyCount: 2 }
    ]
  } finally {
    loading.value = false
  }
}

const openNew = () => {
  form.value = {
    id: null,
    firstName: '',
    lastName: '',
    phone: '',
    email: '',
    nationalId: '',
    address: '',
    bankName: '',
    bankAccount: '',
    mobileMoneyPhone: '',
    mobileMoneyProvider: '',
    sharePercentage: 80,
    notes: ''
  }
  ownerDialog.value = true
}

const editOwner = (owner) => {
  form.value = { ...owner }
  // Extraire les noms du fullname pour le formulaire
  const names = owner.fullName ? owner.fullName.split(' ') : ['', '']
  form.value.firstName = owner.firstName || names[0]
  form.value.lastName = owner.lastName || names.slice(1).join(' ')
  ownerDialog.value = true
}

const saveOwner = async () => {
  if (!form.value.firstName || !form.value.lastName || !form.value.phone) {
    toast.add({ severity: 'error', summary: 'Champs requis', detail: 'Veuillez remplir les informations obligatoires', life: 3000 })
    return
  }

  saving.value = true
  try {
    if (form.value.id) {
      await api.put(`/properties/owners/${form.value.id}`, form.value)
      toast.add({ severity: 'success', summary: 'Propriétaire modifié', life: 3000 })
    } else {
      await api.post('/properties/owners', form.value)
      toast.add({ severity: 'success', summary: 'Propriétaire créé', life: 3000 })
    }
    ownerDialog.value = false
    loadOwners()
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Erreur', detail: e.response?.data?.message || 'Erreur lors de la sauvegarde', life: 3000 })
  } finally {
    saving.value = false
  }
}

const confirmDeleteOwner = (owner) => {
  confirm.require({
    message: `Voulez-vous vraiment supprimer le propriétaire ${owner.fullName} ? Cette action est irréversible.`,
    header: 'Confirmation de suppression',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: async () => {
      try {
        await api.delete(`/properties/owners/${owner.id}`)
        toast.add({ severity: 'success', summary: 'Propriétaire supprimé', life: 3000 })
        loadOwners()
      } catch (e) {
        toast.add({ severity: 'error', summary: 'Erreur', detail: e.response?.data?.message || 'Impossible de supprimer', life: 3000 })
      }
    }
  })
}

onMounted(loadOwners)
</script>

<style scoped>
.font-semibold { font-weight: 600; }
.mt-2 { margin-top: 0.5rem; }
.empty-state { text-align: center; padding: 2rem; color: var(--text-muted); }
</style>
