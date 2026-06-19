import request from '@/utils/request'

export function getSalesOrderList(params) {
  return request({
    url: '/sales/order/list',
    method: 'get',
    params
  })
}

export function getSalesOrderDetail(id) {
  return request({
    url: `/sales/order/${id}`,
    method: 'get'
  })
}

export function generateSalesOrderNo() {
  return request({
    url: '/sales/order/generate-no',
    method: 'get'
  })
}

export function addSalesOrder(data) {
  return request({
    url: '/sales/order',
    method: 'post',
    data
  })
}

export function updateSalesOrder(data) {
  return request({
    url: '/sales/order',
    method: 'put',
    data
  })
}

export function deleteSalesOrder(id) {
  return request({
    url: `/sales/order/${id}`,
    method: 'delete'
  })
}

export function confirmSalesOrder(id) {
  return request({
    url: `/sales/order/confirm/${id}`,
    method: 'post'
  })
}

export function checkCredit(params) {
  return request({
    url: '/sales/order/check-credit',
    method: 'get',
    params
  })
}

export function getAvailableStock(params) {
  return request({
    url: '/sales/stock/available',
    method: 'get',
    params
  })
}

export function getAvailableStockTree(params) {
  return request({
    url: '/sales/stock/available-tree',
    method: 'get',
    params
  })
}

export function getSalesOutboundList(params) {
  return request({
    url: '/sales-outbound/list',
    method: 'get',
    params
  })
}

export function getSalesOutboundDetail(id) {
  return request({
    url: `/sales-outbound/${id}`,
    method: 'get'
  })
}

export function generateSalesOutboundNo() {
  return request({
    url: '/sales-outbound/generate-no',
    method: 'get'
  })
}

export function generateOutboundFromOrder(orderId) {
  return request({
    url: `/sales-outbound/generate-from-order/${orderId}`,
    method: 'get'
  })
}

export function addSalesOutbound(data) {
  return request({
    url: '/sales-outbound',
    method: 'post',
    data
  })
}

export function updateSalesOutbound(data) {
  return request({
    url: '/sales-outbound',
    method: 'put',
    data
  })
}

export function deleteSalesOutbound(id) {
  return request({
    url: `/sales-outbound/${id}`,
    method: 'delete'
  })
}

export function auditSalesOutbound(id) {
  return request({
    url: `/sales-outbound/audit/${id}`,
    method: 'post'
  })
}
