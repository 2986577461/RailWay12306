import http from './http'

export const createPayment = (orderNo) => http.post(`/payments/${orderNo}`)

export const mockNotify = (orderNo, payload) =>
  http.post(`/payments/${orderNo}/mock-notify`, payload)

export const refundPayment = (orderNo) => http.post(`/payments/${orderNo}/refund`)
