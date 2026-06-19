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
