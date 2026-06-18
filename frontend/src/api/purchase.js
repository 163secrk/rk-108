import request from '@/utils/request'

export function getContractList(params) {
  return request({
    url: '/purchase/contract/list',
    method: 'get',
    params
  })
}

export function getContractDetail(id) {
  return request({
    url: `/purchase/contract/${id}`,
    method: 'get'
  })
}

export function generateContractNo() {
  return request({
    url: '/purchase/contract/generate-no',
    method: 'get'
  })
}

export function addContract(data) {
  return request({
    url: '/purchase/contract',
    method: 'post',
    data
  })
}

export function updateContract(data) {
  return request({
    url: '/purchase/contract',
    method: 'put',
    data
  })
}

export function deleteContract(id) {
  return request({
    url: `/purchase/contract/${id}`,
    method: 'delete'
  })
}

export function auditContract(id) {
  return request({
    url: `/purchase/contract/audit/${id}`,
    method: 'post'
  })
}

export function completeContract(id) {
  return request({
    url: `/purchase/contract/complete/${id}`,
    method: 'post'
  })
}

export function getInTransitStockList(params) {
  return request({
    url: '/purchase/in-transit/list',
    method: 'get',
    params
  })
}
