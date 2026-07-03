import axios from 'axios'
import { getRefreshedToken } from './keycloak'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: { 'Content-Type': 'application/json' }
})

// Intercepteur : ajouter le JWT à chaque requête
api.interceptors.request.use(async (config) => {
  const token = await getRefreshedToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Intercepteur : gérer les erreurs globalement
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      window.location.href = '/'
    }
    return Promise.reject(error)
  }
)

export default api
