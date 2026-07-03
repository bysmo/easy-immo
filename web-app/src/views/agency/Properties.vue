<template>
  <div class="properties-page">
    <div class="page-header">
      <div>
        <h1>Biens Immobiliers</h1>
        <p class="text-muted">Gérez les logements (appartements, villas, bureaux) disponibles à la location</p>
      </div>
      <Button label="Ajouter un Bien" icon="pi pi-plus" class="btn btn-primary" @click="openNew" />
    </div>

    <!-- Filtres -->
    <div class="card mb-3">
      <div class="flex gap-2 items-center">
        <div style="flex:1">
          <InputText v-model="searchQuery" placeholder="Rechercher par adresse, ville, référence..." @input="debouncedSearch" style="width: 100%" />
        </div>
        <Select v-model="statusFilter" :options="statusOptions" optionLabel="label" optionValue="value" placeholder="Tous les statuts" style="width: 200px" @change="loadProperties" />
        <Button icon="pi pi-refresh" severity="secondary" outlined @click="loadProperties" />
      </div>
    </div>

    <!-- Table -->
    <div class="table-container">
      <DataTable :value="properties" :loading="loading" :paginator="true" :rows="10" stripedRows responsiveLayout="scroll">
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-home" style="font-size: 3rem; color: var(--text-muted)"></i>
            <p class="mt-2">Aucun logement trouvé</p>
          </div>
        </template>

        <Column header="Visuel">
          <template #body="{ data }">
            <div class="property-img-container">
              <img :src="data.coverPhotoUrl || 'https://placehold.co/80x60?text=Pas+d%27image'" class="property-thumbnail" alt="Visuel" />
            </div>
          </template>
        </Column>

        <Column field="reference" header="Référence" sortable>
          <template #body="{ data }">
            <span class="font-semibold">{{ data.reference || 'N/A' }}</span>
          </template>
        </Column>

        <Column field="type" header="Type">
          <template #body="{ data }">
            {{ $t(`property.types.${data.type.toLowerCase()}`) }}
          </template>
        </Column>

        <Column header="Adresse">
          <template #body="{ data }">
            <div>
              <div>{{ data.address }}</div>
              <small class="text-muted">{{ data.city }}, {{ $t(`countries.${data.country}`) }}</small>
            </div>
          </template>
        </Column>

        <Column field="currentRent" header="Loyer Mensuel" sortable>
          <template #body="{ data }">
            <span class="currency">{{ formatCurrency(data.currentRent) }}</span>
          </small></template>
        </Column>

        <Column field="ownerFullName" header="Propriétaire">
          <template #body="{ data }">
            {{ data.ownerFullName || 'Aucun lié' }}
          </template>
        </Column>

        <Column field="status" header="Statut" sortable>
          <template #body="{ data }">
            <span :class="statusBadgeClass(data.status)">
              {{ $t(`property.statuses.${data.status.toLowerCase()}`) }}
            </span>
          </template>
        </Column>

        <Column header="Actions" style="min-width: 140px">
          <template #body="{ data }">
            <div class="flex gap-1">
              <Button icon="pi pi-eye" class="p-button-sm p-button-outlined" @click="viewDetail(data.id)" />
              <Button icon="pi pi-pencil" class="p-button-sm p-button-outlined" @click="editProperty(data)" />
              <Button icon="pi pi-trash" severity="danger" class="p-button-sm p-button-outlined" @click="confirmDeleteProperty(data)" />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Modal Formulaire -->
    <Dialog v-model:visible="propertyDialog" modal :header="dialogTitle" style="width: 700px" class="p-fluid">
      <div class="grid-2 mb-2">
        <div class="form-group">
          <label class="form-label">Référence interne *</label>
          <InputText v-model="form.reference" placeholder="Ex: VIL-005" required />
        </div>
        <div class="form-group">
          <label class="form-label">Type de bien *</label>
          <Select v-model="form.type" :options="typeOptions" optionLabel="label" optionValue="value" placeholder="Sélectionner" />
        </div>
      </div>

      <div class="form-group mb-2">
        <label class="form-label">Adresse physique *</label>
        <InputText v-model="form.address" required />
      </div>

      <div class="grid-3 mb-2">
        <div class="form-group">
          <label class="form-label">Ville *</label>
          <InputText v-model="form.city" required />
        </div>
        <div class="form-group">
          <label class="form-label">Pays *</label>
          <Select v-model="form.country" :options="countryOptions" optionLabel="label" optionValue="value" />
        </div>
        <div class="form-group">
          <label class="form-label">Propriétaire *</label>
          <Select v-model="form.ownerId" :options="ownersList" optionLabel="fullName" optionValue="id" placeholder="Choisir" />
        </div>
      </div>

      <div class="grid-3 mb-2">
        <div class="form-group">
          <label class="form-label">Loyer Mensuel (XOF) *</label>
          <InputNumber v-model="form.currentRent" mode="decimal" required />
        </div>
        <div class="form-group">
          <label class="form-label">Mois de Caution *</label>
          <InputNumber v-model="form.depositMonths" :min="1" :max="12" />
        </div>
        <div class="form-group">
          <label class="form-label">Surface (m²)</label>
          <InputNumber v-model="form.areaSqm" :min="0" />
        </div>
      </div>

      <div class="grid-2 mb-2">
        <div class="form-group">
          <label class="form-label">Nombre de Chambres</label>
          <InputNumber v-model="form.rooms" :min="0" />
        </div>
        <div class="form-group">
          <label class="form-label">Nombre de douches</label>
          <InputNumber v-model="form.bathrooms" :min="0" />
        </div>
      </div>

      <div class="form-group mb-3">
        <label class="form-label">Description du logement</label>
        <Textarea v-model="form.description" rows="3" autoResize />
      </div>

      <template #footer>
        <Button label="Annuler" severity="secondary" outlined @click="propertyDialog = false" />
        <Button label="Enregistrer" severity="primary" @click="saveProperty" :loading="saving" />
      </template>
    </Dialog>

    <Toast />
    <ConfirmDialog />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
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

const router = useRouter()
const toast = useToast()
const confirm = useConfirm()

const properties = ref([])
const ownersList = ref([])
const loading = ref(false)
const saving = ref(false)
const searchQuery = ref('')
const statusFilter = ref(null)

const propertyDialog = ref(false)
const form = ref({
  id: null,
  reference: '',
  type: 'APPARTEMENT',
  address: '',
  city: '',
  country: 'BJ',
  rooms: 1,
  bathrooms: 1,
  areaSqm: 50,
  currentRent: 100000,
  depositMonths: 2,
  ownerId: null,
  description: '',
  amenities: ''
})

const statusOptions = [
  { label: 'Tous', value: null },
  { label: 'Disponible', value: 'AVAILABLE' },
  { label: 'Occupé', value: 'OCCUPIED' },
  { label: 'En réparation', value: 'MAINTENANCE' }
]

const typeOptions = [
  { label: 'Appartement', value: 'APPARTEMENT' },
  { label: 'Villa', value: 'VILLA' },
  { label: 'Studio', value: 'STUDIO' },
  { label: 'Boutique', value: 'BOUTIQUE' },
  { label: 'Bureau', value: 'BUREAU' },
  { label: 'Entrepôt', value: 'ENTREPOT' }
]

const countryOptions = [
  { label: 'Bénin', value: 'BJ' },
  { label: 'Burkina Faso', value: 'BF' },
  { label: 'Côte d\'Ivoire', value: 'CI' },
  { label: 'Sénégal', value: 'SN' },
  { label: 'Mali', value: 'ML' },
  { label: 'Niger', value: 'NE' },
  { label: 'Togo', value: 'TG' }
]

const dialogTitle = computed(() => form.value.id ? 'Modifier le logement' : 'Nouveau logement')

let searchTimeout = null
const debouncedSearch = () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(loadProperties, 400)
}

const loadProperties = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (searchQuery.value) params.append('search', searchQuery.value)
    if (statusFilter.value) params.append('status', statusFilter.value)

    const res = await api.get(`/properties?${params}`)
    properties.value = res.data.content || []
  } catch (e) {
    // Fallback Offline Mock
    properties.value = [
      { id: '1', reference: 'VIL-002', type: 'VILLA', address: 'Fidjrossé Cotonou', city: 'Cotonou', country: 'BJ', currentRent: 250000, depositMonths: 3, ownerFullName: 'Alain Koffi', status: 'AVAILABLE' },
      { id: '2', reference: 'APP-105', type: 'APPARTEMENT', address: 'Cocody Riviera', city: 'Abidjan', country: 'CI', currentRent: 300000, depositMonths: 2, ownerFullName: 'Fatou Diop', status: 'OCCUPIED' }
    ]
  } finally {
    loading.value = false
  }
}

const loadOwners = async () => {
  try {
    const res = await api.get('/properties/owners?size=100')
    ownersList.value = res.data.content || []
  } catch (e) {
    ownersList.value = [
      { id: '1', fullName: 'Alain Koffi' },
      { id: '2', fullName: 'Fatou Diop' }
    ]
  }
}

const openNew = () => {
  form.value = {
    id: null,
    reference: '',
    type: 'APPARTEMENT',
    address: '',
    city: '',
    country: 'BJ',
    rooms: 2,
    bathrooms: 1,
    areaSqm: 65,
    currentRent: 150000,
    depositMonths: 2,
    ownerId: ownersList.value[0]?.id || null,
    description: '',
    amenities: ''
  }
  propertyDialog.value = true
}

const editProperty = (property) => {
  form.value = { ...property }
  propertyDialog.value = true
}

const saveProperty = async () => {
  if (!form.value.reference || !form.value.address || !form.value.city) {
    toast.add({ severity: 'error', summary: 'Champs requis', life: 3000 })
    return
  }

  saving.value = true
  try {
    if (form.value.id) {
      await api.put(`/properties/${form.value.id}`, form.value)
      toast.add({ severity: 'success', summary: 'Logement modifié', life: 3000 })
    } else {
      await api.post('/properties', form.value)
      toast.add({ severity: 'success', summary: 'Logement créé', life: 3000 })
    }
    propertyDialog.value = false
    loadProperties()
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible d\'enregistrer', life: 3000 })
  } finally {
    saving.value = false
  }
}

const viewDetail = (id) => router.push(`/agency/properties/${id}`)

const confirmDeleteProperty = (property) => {
  confirm.require({
    message: `Voulez-vous vraiment supprimer le bien référencé ${property.reference} ?`,
    header: 'Confirmation de suppression',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: async () => {
      try {
        await api.delete(`/properties/${property.id}`)
        toast.add({ severity: 'success', summary: 'Logement supprimé', life: 3000 })
        loadProperties()
      } catch (e) {
        toast.add({ severity: 'error', summary: 'Erreur', detail: e.response?.data?.message || 'Impossible de supprimer', life: 3000 })
      }
    }
  })
}

const formatCurrency = (value) => {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'XOF', maximumFractionDigits: 0 }).format(value)
}

const statusBadgeClass = (status) => {
  const map = {
    AVAILABLE: 'badge badge-success',
    OCCUPIED: 'badge badge-info',
    MAINTENANCE: 'badge badge-danger',
    RESERVED: 'badge badge-warning'
  }
  return map[status.toUpperCase()] || 'badge'
}

onMounted(() => {
  loadProperties()
  loadOwners()
})
</script>

<style scoped>
.property-thumbnail {
  width: 70px;
  height: 52px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
}
.empty-state { text-align: center; padding: 2rem; color: var(--text-muted); }
.font-semibold { font-weight: 600; }
</style>
