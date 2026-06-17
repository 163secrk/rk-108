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
