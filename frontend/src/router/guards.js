import { useUserStore } from '@/stores/user'
import { Message } from '@arco-design/web-vue'

export function setupRouterGuards(router) {
  router.beforeEach(async (to, from, next) => {
    const userStore = useUserStore()
    const hasToken = !!userStore.token

    if (to.path === '/login') {
      if (hasToken) {
        next('/')
      } else {
        next()
      }
      return
    }

    if (!hasToken) {
      Message.warning('请先登录')
      next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
      return
    }

    if (!userStore.routesAdded) {
      try {
        if (!userStore.userInfo || Object.keys(userStore.userInfo).length === 0) {
          await userStore.getUserInfo()
        }
        if (userStore.menus.length === 0) {
          await userStore.getMenus()
        }
        userStore.generateRoutes()
        next({ ...to, replace: true })
      } catch (error) {
        console.error('guard error:', error)
        userStore.resetState()
        next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
      }
      return
    }

    next()
  })
}
