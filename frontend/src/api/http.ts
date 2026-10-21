import axios from 'axios'
/** Raw transport only. Business calls go through client.ts + their DTO boundary. */
export const http = axios.create({ baseURL: '/api', withCredentials: true, timeout: 15000 })
