import http from './http'

export const sendSmsCode = (phone) => http.post('/users/sms-code', { phone })
export const register = (payload) => http.post('/users/register', payload)
export const login = (payload) => http.post('/users/login', payload)
export const loginByCode = (payload) => http.post('/users/login-by-code', payload)
export const fetchMe = () => http.get('/users/me')
