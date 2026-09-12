import http from './http'

export const listAdminStations = (keyword) =>
  http.get('/admin/stations', { params: keyword ? { keyword } : {} })
export const createStation = (payload) => http.post('/admin/stations', payload)
export const updateStation = (id, payload) => http.put(`/admin/stations/${id}`, payload)
export const deleteStation = (id) => http.delete(`/admin/stations/${id}`)

export const listAdminTrains = () => http.get('/admin/trains')
export const getAdminTrain = (id) => http.get(`/admin/trains/${id}`)
export const createTrain = (payload) => http.post('/admin/trains', payload)
export const updateTrain = (id, payload) => http.put(`/admin/trains/${id}`, payload)
export const deleteTrain = (id) => http.delete(`/admin/trains/${id}`)

export const listFares = () => http.get('/admin/fares')
export const saveFare = (payload) => http.post('/admin/fares', payload)

export const createTrainRun = (payload) => http.post('/admin/train-runs', payload)
export const reloadInventory = () => http.post('/admin/inventory/reload')
export const adminOverview = () => http.get('/admin/overview')
