import http from './http'

export const createOrder = (payload) => http.post('/orders/requests', payload, {
  headers: { 'Idempotency-Key': crypto.randomUUID() }
})
export const listOrders = () => http.get('/orders')
export const getOrder = (orderNo) => http.get(`/orders/${orderNo}`)
