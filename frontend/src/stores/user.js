import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi, getUserInfoApi, getMenusApi, logoutApi } from '@/api/auth'
import router from '@/router'
import { asyncRoutes, constantRoutes } from '@/router'

function filterAsyncRoutes(routes, menuNames) {
  const res = []
  routes.forEach((route) => {
    const tmp = { ...route }
    if (menuNames.includes(tmp.meta?.title)) {
      if (tmp.children) {
        tmp.children = filterAsyncRoutes(tmp.children, menuNames)
      }
      res.push(tmp)
    } else if (tmp.children) {
      const child = filterAsyncRoutes(tmp.children, menuNames)
      if (child.length > 0) {
        tmp.children = child
        res.push(tmp)
      }
    }
  })
  return res
}

function flattenMenuNames(menus) {
  const names = []
  menus.forEach((m) => {
    names.push(m.name)
    if (m.children && m.children.length) {
      names.push(...flattenMenuNames(m.children))
    }
  })
  return names
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref({})
  const menus = ref([])
  const routesAdded = ref(false)

  function setToken(val) {
    token.value = val
    localStorage.setItem('token', val)
  }

  function clearToken() {
    token.value = ''
    localStorage.removeItem('token')
  }

  async function login(loginForm) {
    const res = await loginApi(loginForm)
    if (res?.data?.token) {
      setToken(res.data.token)
    }
    return res
  }

  async function getUserInfo() {
    const res = await getUserInfoApi()
    if (res?.data) {
      userInfo.value = res.data
    }
    return res
  }

  async function getMenus() {
    const res = await getMenusApi()
    if (res?.data) {
      menus.value = res.data
    }
    return res
  }

  function generateRoutes() {
    let accessedRoutes
    if (menus.value.length === 0) {
      accessedRoutes = asyncRoutes
    } else {
      const menuNames = flattenMenuNames(menus.value)
      accessedRoutes = filterAsyncRoutes(asyncRoutes, menuNames)
    }
    const layoutRoute = constantRoutes.find((r) => r.path === '/')
    if (layoutRoute) {
      layoutRoute.children = accessedRoutes
      router.addRoute(layoutRoute)
    }
    routesAdded.value = true
    return accessedRoutes
  }

  async function logout() {
    try {
      await logoutApi()
    } catch (e) {
      console.warn('logout api error', e)
    }
    clearToken()
    userInfo.value = {}
    menus.value = []
    routesAdded.value = false
    router.push('/login')
  }

  function resetState() {
    clearToken()
    userInfo.value = {}
    menus.value = []
    routesAdded.value = false
  }

  return {
    token,
    userInfo,
    menus,
    routesAdded,
    login,
    getUserInfo,
    getMenus,
    generateRoutes,
    logout,
    resetState,
    setToken
  }
})
