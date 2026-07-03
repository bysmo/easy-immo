import { createRouter, createWebHistory } from 'vue-router'
import { isSaasAdmin, isAgencyAdmin, isAgencyAgent } from '@/services/keycloak'

// Layouts
const AdminLayout = () => import('@/layouts/AdminLayout.vue')
const AgencyLayout = () => import('@/layouts/AgencyLayout.vue')

// Admin SaaS views
const AdminDashboard = () => import('@/views/admin/Dashboard.vue')
const AdminAgencies = () => import('@/views/admin/Agencies.vue')
const AdminAgencyCreate = () => import('@/views/admin/AgencyCreate.vue')
const AdminAgencyDetail = () => import('@/views/admin/AgencyDetail.vue')
const AdminPlans = () => import('@/views/admin/Plans.vue')
const AdminBilling = () => import('@/views/admin/Billing.vue')

// Agency views
const AgencyDashboard = () => import('@/views/agency/Dashboard.vue')
const AgencyOwners = () => import('@/views/agency/Owners.vue')
const AgencyProperties = () => import('@/views/agency/Properties.vue')
const AgencyPropertyDetail = () => import('@/views/agency/PropertyDetail.vue')
const AgencyTenants = () => import('@/views/agency/Tenants.vue')
const AgencyLeases = () => import('@/views/agency/Leases.vue')
const AgencyLeaseCreate = () => import('@/views/agency/LeaseCreate.vue')
const AgencyLeaseDetail = () => import('@/views/agency/LeaseDetail.vue')
const AgencyDeposits = () => import('@/views/agency/Deposits.vue')
const AgencyRents = () => import('@/views/agency/Rents.vue')
const AgencyDisbursements = () => import('@/views/agency/Disbursements.vue')
const AgencyReports = () => import('@/views/agency/Reports.vue')
const AgencySettings = () => import('@/views/agency/Settings.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ── Admin SaaS ───────────────────────────────────────
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requiresSaasAdmin: true },
      children: [
        { path: '', redirect: '/admin/dashboard' },
        { path: 'dashboard', name: 'admin-dashboard', component: AdminDashboard },
        { path: 'agencies', name: 'admin-agencies', component: AdminAgencies },
        { path: 'agencies/create', name: 'admin-agency-create', component: AdminAgencyCreate },
        { path: 'agencies/:id', name: 'admin-agency-detail', component: AdminAgencyDetail },
        { path: 'plans', name: 'admin-plans', component: AdminPlans },
        { path: 'billing', name: 'admin-billing', component: AdminBilling }
      ]
    },

    // ── Agence Immobilière ────────────────────────────────
    {
      path: '/agency',
      component: AgencyLayout,
      meta: { requiresAgency: true },
      children: [
        { path: '', redirect: '/agency/dashboard' },
        { path: 'dashboard', name: 'agency-dashboard', component: AgencyDashboard },
        { path: 'owners', name: 'agency-owners', component: AgencyOwners },
        { path: 'properties', name: 'agency-properties', component: AgencyProperties },
        { path: 'properties/:id', name: 'agency-property-detail', component: AgencyPropertyDetail },
        { path: 'tenants', name: 'agency-tenants', component: AgencyTenants },
        { path: 'leases', name: 'agency-leases', component: AgencyLeases },
        { path: 'leases/create', name: 'agency-lease-create', component: AgencyLeaseCreate },
        { path: 'leases/:id', name: 'agency-lease-detail', component: AgencyLeaseDetail },
        { path: 'payments/deposits', name: 'agency-deposits', component: AgencyDeposits },
        { path: 'payments/rents', name: 'agency-rents', component: AgencyRents },
        { path: 'payments/disbursements', name: 'agency-disbursements', component: AgencyDisbursements },
        { path: 'reports', name: 'agency-reports', component: AgencyReports },
        { path: 'settings', name: 'agency-settings', component: AgencySettings }
      ]
    },

    // ── Redirection automatique selon rôle ────────────────
    {
      path: '/',
      redirect: () => {
        if (isSaasAdmin()) return '/admin/dashboard'
        if (isAgencyAdmin() || isAgencyAgent()) return '/agency/dashboard'
        return '/unauthorized'
      }
    },

    {
      path: '/unauthorized',
      name: 'unauthorized',
      component: () => import('@/views/Unauthorized.vue')
    },

    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFound.vue')
    }
  ]
})

// Guards de navigation
router.beforeEach((to) => {
  if (to.meta.requiresSaasAdmin && !isSaasAdmin()) {
    return '/unauthorized'
  }
  if (to.meta.requiresAgency && !isAgencyAdmin() && !isAgencyAgent()) {
    return '/unauthorized'
  }
})

export default router
