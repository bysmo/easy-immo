<template>
  <div class="tenants-page">
    <div class="page-header">
      <div>
        <h1>Locataires</h1>
        <p class="text-muted">Gérez les dossiers et coordonnées d'urgence des locataires logés ou inscrits sur mobile</p>
      </div>
      <Button label="Ajouter un Locataire" icon="pi pi-plus" class="btn btn-primary" @click="openNew" />
    </div>

    <!-- Filtres -->
    <div class="card mb-3">
      <div class="flex gap-2 items-center">
        <div style="flex:1">
          <InputText v-model="searchQuery" placeholder="Rechercher par nom, téléphone, profession..." @input="debouncedSearch" style="width: 100%" />
        </div>
        <Button icon="pi pi-refresh" severity="secondary" outlined @click="loadTenants" />
      </div>
    </div>

    <!-- Table -->
    <div class="table-container">
      <DataTable :value="tenants" :loading="loading" :paginator="true" :rows="10" stripedRows responsiveLayout="scroll">
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-user" style="font-size: 3rem; color: var(--text-muted)"></i>
            <p class="mt-2">Aucun locataire trouvé</p>
          </div>
        </template>

        <Column field="fullName" header="Locataire" sortable>
          <template #body="{ data }">
            <div>
              <div class="font-semibold">{{ data.fullName }}</div>
              <small class="text-muted" v-if="data.keycloakUserId">📲 Inscrit sur mobile</small>
            </div>
          </template>
        </Column>

        <Column field="phone" header="Téléphone" />
        <Column field="email" header="Email" />
        <Column field="profession" header="Profession" />

        <Column header="Bail Actif">
          <template #body="{ data }">
            <span :class="data.hasActiveLease ? 'badge badge-success' : 'badge badge-danger'">
              {{ data.hasActiveLease ? 'Oui' : 'Non' }}
            </span>
          </template>
        </Column>

        <Column header="Contact d'Urgence">
          <template #body="{ data }">
            <div v-if="data.emergencyContactName">
              <div>{{ data.emergencyContactName }}</div>
              <small class="text-muted">{{ data.emergencyContactPhone }}</small>
            </div>
            <span v-else class="text-muted">—</span>
          </template>
        </Column>

        <Column header="Actions" style="min-width: 100px">
          <template #body="{ data }">
            <div class="flex gap-1">
              <Button icon="pi pi-pencil" class="p-button-sm p-button-outlined" @click="editTenant(data)" />
              <Button icon="pi pi-trash" severity="danger" class="p-button-sm p-button-outlined" @click="confirmDeleteTenant(data)" />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Modal Formulaire -->
    <Dialog v-model:visible="tenantDialog" modal :header="dialogTitle" style="width: 600px" class="p-fluid">
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
          <label class="form-label">Profession / Employeur</label>
          <InputText v-model="form.profession" />
        </div>
        <div class="form-group">
          <label class="form-label">N° Pièce d'identité</label>
          <InputText v-model="form.nationalId" />
        </div>
      </div>

      <div class="menu-category" style="padding-left:0; margin-top:1rem">Contact à contacter en cas d'urgence</div>

      <div class="grid-2 mb-2">
        <div class="form-group">
          <label class="form-label">Nom Complet du contact</label>
          <InputText v-model="form.emergencyContactName" placeholder="Ex: Parent, Conjoint..." />
        </div>
        <div class="form-group">
          <label class="form-label">Téléphone du contact</label>
          <InputText v-model="form.emergencyContactPhone" />
        </div>
      </div>

      <template #footer>
        <Button label="Annuler" severity="secondary" outlined @click="tenantDialog = false" />
        <Button label="Enregistrer" severity="primary" @click="saveTenant" :loading="saving" />
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
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import api from '@/services/api'

const toast = useToast()
const confirm = useConfirm()

const tenants = ref([])
const loading = ref(false)
const saving = ref(false)
const searchQuery = ref('')

const tenantDialog = ref(false)
const form = ref({
  id: null,
  firstName: '',
  lastName: '',
  phone: '',
  email: '',
  nationalId: '',
  profession: '',
  emergencyContactName: '',
  emergencyContactPhone: ''
})

const dialogTitle = computed(() => form.value.id ? 'Modifier le locataire' : 'Nouveau locataire')

let searchTimeout = null
const debouncedSearch = () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(loadTenants, 400)
}

const loadTenants = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (searchQuery.value) params.append('search', searchQuery.value)

    const res = await api.get(`/leases/tenants?${params}`)
    tenants.value = res.data.content || []
  } catch (e) {
    // Fallback Offline Mock
    tenants.value = [
      { id: '1', fullName: 'Mamadou Diallo', phone: '+22997001122', email: 'mamadou@momo.com', profession: 'Comptable', hasActiveLease: true, emergencyContactName: 'Awa Diallo', emergencyContactPhone: '+22997001123' },
      { id: '2', fullName: 'Koffi Mensah', phone: '+22890123456', email: 'koffi@mensah.org', profession: 'Ingénieur', hasActiveLease: false, keycloakUserId: 'user-02' }
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
    profession: '',
    emergencyContactName: '',
    emergencyContactPhone: ''
  }
  tenantDialog.value = true
}

const editTenant = (tenant) => {
  form.value = { ...tenant }
  const names = tenant.fullName ? tenant.fullName.split(' ') : ['', '']
  form.value.firstName = tenant.firstName || names[0]
  form.value.lastName = tenant.lastName || names.slice(1).join(' ')
  tenantDialog.value = true
}

const saveTenant = async () => {
  if (!form.value.firstName || !form.value.lastName || !form.value.phone) {
    toast.add({ severity: 'error', summary: 'Champs requis', detail: 'Veuillez remplir les champs obligatoires', life: 3000 })
    return
  }

  saving.value = true
  try {
    if (form.value.id) {
      await api.put(`/leases/tenants/${form.value.id}`, form.value)
      toast.add({ severity: 'success', summary: 'Locataire modifié', life: 3000 })
    } else {
      await api.post('/leases/tenants', form.value)
      toast.add({ severity: 'success', summary: 'Locataire créé', life: 3000 })
    }
    tenantDialog.value = false
    loadTenants()
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Erreur', detail: e.response?.data?.message || 'Erreur d\'enregistrement', life: 3000 })
  } finally {
    saving.value = false
  }
}

const confirmDeleteTenant = (tenant) => {
  confirm.require({
    message: `Voulez-vous supprimer le locataire ${tenant.fullName} ?`,
    header: 'Confirmation de suppression',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: async () => {
      try {
        await api.delete(`/leases/tenants/${tenant.id}`)
        toast.add({ severity: 'success', summary: 'Locataire supprimé', life: 3000 })
        loadTenants()
      } catch (e) {
        toast.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de supprimer', life: 3000 })
      }
    }
  })
}

onMounted(loadTenants)
</script>

<style scoped>
.font-semibold { font-weight: 600; }
.empty-state { text-align: center; padding: 2rem; color: var(--text-muted); }
</style>
