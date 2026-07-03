<template>
  <div class="app-layout">
    <!-- Sidebar Agence Immobilière -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <div class="logo">
          <span class="logo-icon">🔑</span>
          <span class="logo-text" v-if="!sidebarCollapsed">Easy-Immo</span>
        </div>
        <button class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">
          <i :class="sidebarCollapsed ? 'pi pi-chevron-right' : 'pi pi-chevron-left'"></i>
        </button>
      </div>

      <nav class="sidebar-nav">
        <RouterLink to="/agency/dashboard" class="nav-item" active-class="active">
          <i class="pi pi-chart-line"></i>
          <span v-if="!sidebarCollapsed">{{ $t('nav.dashboard') }}</span>
        </RouterLink>
        <RouterLink to="/agency/owners" class="nav-item" active-class="active">
          <i class="pi pi-users"></i>
          <span v-if="!sidebarCollapsed">{{ $t('nav.owners') }}</span>
        </RouterLink>
        <RouterLink to="/agency/properties" class="nav-item" active-class="active">
          <i class="pi pi-home"></i>
          <span v-if="!sidebarCollapsed">{{ $t('nav.properties') }}</span>
        </RouterLink>
        <RouterLink to="/agency/tenants" class="nav-item" active-class="active">
          <i class="pi pi-user"></i>
          <span v-if="!sidebarCollapsed">{{ $t('nav.tenants') }}</span>
        </RouterLink>
        <RouterLink to="/agency/leases" class="nav-item" active-class="active">
          <i class="pi pi-file-sign"></i>
          <span v-if="!sidebarCollapsed">{{ $t('nav.leases') }}</span>
        </RouterLink>
        
        <!-- Sous-menu Paiements -->
        <div class="menu-category" v-if="!sidebarCollapsed">Finances</div>
        
        <RouterLink to="/agency/payments/deposits" class="nav-item" active-class="active">
          <i class="pi pi-wallet"></i>
          <span v-if="!sidebarCollapsed">{{ $t('nav.deposits') }}</span>
        </RouterLink>
        <RouterLink to="/agency/payments/rents" class="nav-item" active-class="active">
          <i class="pi pi-money-bill"></i>
          <span v-if="!sidebarCollapsed">{{ $t('nav.rents') }}</span>
        </RouterLink>
        <RouterLink to="/agency/payments/disbursements" class="nav-item" active-class="active">
          <i class="pi pi-send"></i>
          <span v-if="!sidebarCollapsed">{{ $t('nav.disbursements') }}</span>
        </RouterLink>

        <div class="menu-category" v-if="!sidebarCollapsed">Analyses</div>

        <RouterLink to="/agency/reports" class="nav-item" active-class="active">
          <i class="pi pi-percentage"></i>
          <span v-if="!sidebarCollapsed">{{ $t('nav.reports') }}</span>
        </RouterLink>
        <RouterLink to="/agency/settings" class="nav-item" active-class="active">
          <i class="pi pi-cog"></i>
          <span v-if="!sidebarCollapsed">{{ $t('nav.settings') }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-footer" v-if="!sidebarCollapsed">
        <div class="user-info">
          <div class="user-avatar agency-avatar-color">{{ userInitials }}</div>
          <div class="user-details">
            <span class="user-name">{{ userName }}</span>
            <span class="user-role">{{ agencyName }}</span>
          </div>
        </div>
        <button class="logout-btn" @click="handleLogout">
          <i class="pi pi-sign-out"></i>
        </button>
      </div>
    </aside>

    <!-- Main content -->
    <main class="main-content">
      <header class="top-bar">
        <div class="breadcrumb">
          <span class="page-title">{{ currentPageTitle }}</span>
        </div>
        <div class="top-bar-actions">
          <Select v-model="currentLocale" :options="localeOptions"
                  optionLabel="label" optionValue="value"
                  @change="changeLocale" class="locale-select" />
        </div>
      </header>

      <div class="page-content">
        <RouterView />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { getUserInfo, logout } from '@/services/keycloak'
import Select from 'primevue/select'

const route = useRoute()
const { t, locale } = useI18n()

const sidebarCollapsed = ref(false)
const userInfo = getUserInfo()

const currentLocale = ref(locale.value)
const localeOptions = [
  { label: '🇫🇷 Français', value: 'fr' },
  { label: '🇬🇧 English', value: 'en' }
]

const changeLocale = (e) => {
  locale.value = e.value
  localStorage.setItem('locale', e.value)
}

const userName = computed(() =>
  `${userInfo.firstName} ${userInfo.lastName}`.trim() || userInfo.username
)

const userInitials = computed(() =>
  `${userInfo.firstName?.[0] || ''}${userInfo.lastName?.[0] || ''}`.toUpperCase() || 'U'
)

const agencyName = ref('Agence Immobilière') // Pourrait être extrait d'un store global Pinia

const currentPageTitle = computed(() => {
  const name = route.name || ''
  if (name.includes('dashboard')) return t('nav.dashboard')
  if (name.includes('owners')) return t('nav.owners')
  if (name.includes('properties')) return t('nav.properties')
  if (name.includes('tenants')) return t('nav.tenants')
  if (name.includes('leases')) return t('nav.leases')
  if (name.includes('deposits')) return t('nav.deposits')
  if (name.includes('rents')) return t('nav.rents')
  if (name.includes('disbursements')) return t('nav.disbursements')
  if (name.includes('reports')) return t('nav.reports')
  if (name.includes('settings')) return t('nav.settings')
  return 'Portail Agence'
})

const handleLogout = () => logout()
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  background: var(--bg-main);
}

.sidebar {
  width: 260px;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  overflow: hidden;
}

.sidebar.collapsed {
  width: 68px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1rem;
  border-bottom: 1px solid var(--border);
}

.logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-weight: 700;
  font-size: 1.1rem;
  color: var(--primary);
}

.logo-icon {
  font-size: 1.5rem;
}

.collapse-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  padding: 0.25rem;
  border-radius: 4px;
  transition: background 0.2s;
}

.collapse-btn:hover {
  background: var(--hover-bg);
}

.sidebar-nav {
  flex: 1;
  padding: 1rem 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  overflow-y: auto;
}

.menu-category {
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--text-muted);
  padding: 1rem 1rem 0.25rem;
  letter-spacing: 0.05em;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  text-decoration: none;
  color: var(--text-secondary);
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.2s;
  white-space: nowrap;
}

.nav-item:hover {
  background: var(--hover-bg);
  color: var(--primary);
}

.nav-item.active {
  background: var(--primary-light);
  color: var(--primary);
}

.nav-item i {
  font-size: 1rem;
  min-width: 20px;
  text-align: center;
}

.sidebar-footer {
  padding: 1rem;
  border-top: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-avatar {
  width: 36px;
  height: 36px;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.85rem;
  flex-shrink: 0;
}

.agency-avatar-color {
  background: var(--success);
}

.user-details {
  flex: 1;
  overflow: hidden;
}

.user-name {
  display: block;
  font-weight: 600;
  font-size: 0.85rem;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  display: block;
  font-size: 0.75rem;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.logout-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  padding: 0.5rem;
  border-radius: 8px;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: #fee2e2;
  color: #ef4444;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.top-bar {
  height: 64px;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1.5rem;
}

.page-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.top-bar-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.locale-select {
  width: 150px;
}

.page-content {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
  background: var(--bg-main);
}
</style>
