import axios from 'axios'

// S13: instancia compartida por la consola. La cookie HttpOnly se envía en el mismo origen.
export const adminHttp = axios.create({
  baseURL: `${import.meta.env.VITE_ADMIN_API_BASE_URL || ''}/api/admin/v1/`,
  timeout: 15000,
  validateStatus: () => true,
})
