import http from './http'

export const listStations = (keyword) =>
  http.get('/stations', { params: keyword ? { keyword } : {} })

export const listSeatTypes = () => http.get('/seat-types')

export const searchTrains = (params) => http.get('/trains/search', { params })
