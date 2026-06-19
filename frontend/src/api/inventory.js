import request from '@/utils/request'

export function queryStockTree(params) {
  return request({
    url: '/inventory/query/tree',
    method: 'get',
    params
  })
}

export function queryStockDetail(params) {
  return request({
    url: '/inventory/query/detail',
    method: 'get',
    params
  })
}
