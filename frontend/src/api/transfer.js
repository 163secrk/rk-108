import request from '@/utils/request'

export function getTransferList(params) {
  return request({
    url: '/transfer/list',
    method: 'get',
    params
  })
}

export function getTransferDetail(id) {
  return request({
    url: `/transfer/${id}`,
    method: 'get'
  })
}

export function generateTransferNo() {
  return request({
    url: '/transfer/generate-no',
    method: 'get'
  })
}

export function addTransfer(data) {
  return request({
    url: '/transfer',
    method: 'post',
    data
  })
}

export function updateTransfer(data) {
  return request({
    url: '/transfer',
    method: 'put',
    data
  })
}

export function deleteTransfer(id) {
  return request({
    url: `/transfer/${id}`,
    method: 'delete'
  })
}

export function auditTransfer(id) {
  return request({
    url: `/transfer/audit/${id}`,
    method: 'post'
  })
}

export function receiveTransfer(id, data) {
  return request({
    url: `/transfer/receive/${id}`,
    method: 'post',
    data
  })
}

export function getInTransitStockList(params) {
  return request({
    url: '/transfer/in-transit/list',
    method: 'get',
    params
  })
}

export function getAvailableStock(params) {
  return request({
    url: '/transfer/available-stock',
    method: 'get',
    params
  })
}
