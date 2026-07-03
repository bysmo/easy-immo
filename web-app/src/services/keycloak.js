import Keycloak from 'keycloak-js'

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: 'easy-immo',
  clientId: 'easy-immo-web'
})

export const initKeycloak = () => {
  return keycloak.init({
    onLoad: 'login-required',
    checkLoginIframe: false,
    pkceMethod: 'S256',
    responseMode: 'fragment'
  })
}

export const getToken = () => keycloak.token

export const getRefreshedToken = async () => {
  try {
    await keycloak.updateToken(60) // Rafraîchir si expire dans moins de 60s
    return keycloak.token
  } catch {
    keycloak.login()
  }
}

export const getUserInfo = () => ({
  id: keycloak.subject,
  username: keycloak.tokenParsed?.preferred_username,
  email: keycloak.tokenParsed?.email,
  firstName: keycloak.tokenParsed?.given_name,
  lastName: keycloak.tokenParsed?.family_name,
  roles: keycloak.tokenParsed?.realm_access?.roles || [],
  agencyId: keycloak.tokenParsed?.agency_id
})

export const hasRole = (role) => keycloak.hasRealmRole(role)

export const isSaasAdmin = () => keycloak.hasRealmRole('SAAS_ADMIN')
export const isAgencyAdmin = () => keycloak.hasRealmRole('AGENCY_ADMIN')
export const isAgencyAgent = () => keycloak.hasRealmRole('AGENCY_AGENT')
export const isTenant = () => keycloak.hasRealmRole('TENANT')

export const logout = () => keycloak.logout({
  redirectUri: window.location.origin
})

export default keycloak
