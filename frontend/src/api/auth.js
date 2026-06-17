import request from '@/utils/request'

export function loginApi(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function getUserInfoApi() {
  return request({
    url: '/auth/userInfo',
    method: 'get'
  })
}

export function getMenusApi() {
  return request({
    url: '/auth/menus',
    method: 'get'
  })
}

export function logoutApi() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}
