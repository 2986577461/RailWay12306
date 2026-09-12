import http from './http'

export const listPassengers = () => http.get('/passengers')
export const addPassenger = (payload) => http.post('/passengers', payload)
export const updatePassenger = (id, payload) => http.put(`/passengers/${id}`, payload)
export const deletePassenger = (id) => http.delete(`/passengers/${id}`)
