<template>
  <div class="app-layout">
    <!-- Sidebar Admin SaaS -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <div class="logo">
          <span class="logo-icon">🏠</span>
          <span class="logo-text" v-if="!sidebarCollapsed">Easy-Immo</span>
        </div>
        <button class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">
          <i :class="sidebarCollapsed ? 'pi pi-chevron-right' : 'pi pi-chevron-left'"></i>
        </button>
      </div>

      <nav class="sidebar-nav">
        <RouterLink to="/admin/dashboard" class="nav-item" active-class="active">
          <i class="pi pi-chart-bar"></i>
          <span v-if="!sidebarCollapsed">{{ $t('nav.dashboard') }}</span>
        </RouterLink>
        <RouterLink to="/admin/agencies" class="nav-item" active-class="active">
          <i class="pi pi-building"></i>
          <span v-if="!sidebarCollapsed">{{ $t('nav.agencies') }}</span>
        </RouterLink>
        <RouterLink to="/admin/plans" class="nav-item" active-class="active">
          <i class="pi pi-tags"></i>
          <span v-if="!sidebarCollapsed">{{ $t('nav.plans') }}</span>
        </RouterLink>
        <RouterLink to="/admin/billing" class="nav-item" active-class="active">
          <i class="pi pi-credit-card"></i>
          <span v-if="!sidebarCollapsed">{{ $t('nav.billing') }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-footer" v-if="!sidebarCollapsed">
        <div class="user-info">
          <div class="user-avatar">{{ userInitials }}</div>
          <div class="user-details">
            <span class="user-name">{{ userName }}</span>
            <span class="user-role">Super Admin</span>
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
          <!-- Sélecteur de langue -->
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

const userName = computed(() => {
  const first = userInfo.firstName || ''
  const last = userInfo.lastName || ''
  return `${first} ${last}`.trim() || userInfo.username || 'Admin'
})

const userInitials = computed(() => {
  const first = userInfo.firstName?.[0] || ''
  const last = userInfo.lastName?.[0] || ''
  const initials = `${first}${last}`.toUpperCase()
  return initials || userInfo.username?.[0]?.toUpperCase() || 'A'
})

const currentPageTitle = computed(() => {
  const name = route.name || ''
  if (name.includes('dashboard')) return t('nav.dashboard')
  if (name.includes('agencies')) return t('nav.agencies')
  if (name.includes('plans')) return t('nav.plans')
  if (name.includes('billing')) return t('nav.billing')
  return 'Easy-Immo'
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
  background: var(--primary);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.85rem;
  flex-shrink: 0;
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
