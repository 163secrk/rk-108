import request from '@/utils/request'

export function getMaterialList() {
  return request({
    url: '/basic/material/list',
    method: 'get'
  })
}

export function getMaterialById(id) {
  return request({
    url: `/basic/material/${id}`,
    method: 'get'
  })
}

export function addMaterial(data) {
  return request({
    url: '/basic/material',
    method: 'post',
    data
  })
}

export function updateMaterial(data) {
  return request({
    url: '/basic/material',
    method: 'put',
    data
  })
}

export function deleteMaterial(id) {
  return request({
    url: `/basic/material/${id}`,
    method: 'delete'
  })
}

export function getSpecList(params) {
  return request({
    url: '/basic/spec/list',
    method: 'get',
    params
  })
}

export function getSpecById(id) {
  return request({
    url: `/basic/spec/${id}`,
    method: 'get'
  })
}

export function addSpec(data) {
  return request({
    url: '/basic/spec',
    method: 'post',
    data
  })
}

export function updateSpec(data) {
  return request({
    url: '/basic/spec',
    method: 'put',
    data
  })
}

export function deleteSpec(id) {
  return request({
    url: `/basic/spec/${id}`,
    method: 'delete'
  })
}

export function getProductList(params) {
  return request({
    url: '/basic/product/list',
    method: 'get',
    params
  })
}

export function getProductById(id) {
  return request({
    url: `/basic/product/${id}`,
    method: 'get'
  })
}

export function addProduct(data) {
  return request({
    url: '/basic/product',
    method: 'post',
    data
  })
}

export function updateProduct(data) {
  return request({
    url: '/basic/product',
    method: 'put',
    data
  })
}

export function deleteProduct(id) {
  return request({
    url: `/basic/product/${id}`,
    method: 'delete'
  })
}

export function getWarehouseList() {
  return request({
    url: '/basic/warehouse/list',
    method: 'get'
  })
}

export function getWarehouseTree() {
  return request({
    url: '/basic/warehouse/tree',
    method: 'get'
  })
}

export function getWarehouseById(id) {
  return request({
    url: `/basic/warehouse/${id}`,
    method: 'get'
  })
}

export function addWarehouse(data) {
  return request({
    url: '/basic/warehouse',
    method: 'post',
    data
  })
}

export function updateWarehouse(data) {
  return request({
    url: '/basic/warehouse',
    method: 'put',
    data
  })
}

export function deleteWarehouse(id) {
  return request({
    url: `/basic/warehouse/${id}`,
    method: 'delete'
  })
}

export function getPartnerList(params) {
  return request({
    url: '/basic/partner/list',
    method: 'get',
    params
  })
}

export function getPartnerById(id) {
  return request({
    url: `/basic/partner/${id}`,
    method: 'get'
  })
}

export function addPartner(data) {
  return request({
    url: '/basic/partner',
    method: 'post',
    data
  })
}

export function updatePartner(data) {
  return request({
    url: '/basic/partner',
    method: 'put',
    data
  })
}

export function deletePartner(id) {
  return request({
    url: `/basic/partner/${id}`,
    method: 'delete'
  })
}
