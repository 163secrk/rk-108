<script setup>
import { ref, computed, watch, onMounted, markRaw } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Message } from '@arco-design/web-vue'
import {
  IconDashboard,
  IconStorage,
  IconFile,
  IconApps,
  IconHome,
  IconUser,
  IconUserGroup,
  IconCopy,
  IconDownload,
  IconImport,
  IconExport,
  IconList,
  IconSearch,
  IconSwap,
  IconThunderbolt,
  IconTag,
  IconUpload,
  IconBook,
  IconSafe,
  IconBarChart,
  IconSettings,
  IconMenuFold,
  IconMenuUnfold,
  IconPoweroff,
  IconEdit
} from '@arco-design/web-vue/es/icon'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const selectedKeys = ref([])
const openKeys = ref([])

const iconMap = {
  'icon-dashboard': markRaw(IconDashboard),
  'icon-storage': markRaw(IconStorage),
  'icon-file': markRaw(IconFile),
  'icon-sku': markRaw(IconApps),
  'icon-building': markRaw(IconHome),
  'icon-user-group': markRaw(IconUserGroup),
  'icon-cart': markRaw(IconSafe),
  'icon-file-copy': markRaw(IconBook),
  'icon-import': markRaw(IconImport),
  'icon-list': markRaw(IconList),
  'icon-search': markRaw(IconSearch),
  'icon-exchange': markRaw(IconSwap),
  'icon-thunderbolt': markRaw(IconThunderbolt),
  'icon-tag': markRaw(IconTag),
  'icon-file-document': markRaw(IconFile),
  'icon-export': markRaw(IconExport),
  'icon-yen-circle': markRaw(IconSafe),
  'icon-account': markRaw(IconBook),
  'icon-calculator': markRaw(IconBarChart),
  'icon-settings': markRaw(IconSettings)
}

const roleMap = {
  ADMIN: '管理员',
  PURCHASER: '采购员',
  SALER: '销售员',
  FINANCE: '财务'
}

const roleConfig = {
  管理员: { color: '#f53f3f' },
  采购员: { color: '#3491FA' },
  销售员: { color: '#00B42A' },
  财务:   { color: '#FF7D00' }
}

const roleCode = computed(() => userStore.userInfo?.role || 'ADMIN')
const roleName = computed(() => roleMap[roleCode.value] || '管理员')
const roleColor = computed(() => roleConfig[roleName.value]?.color || '#3491FA')
const userName = computed(() => userStore.userInfo?.nickname || '管理员')

const breadcrumbItems = computed(() => {
  const items = [{ title: '首页' }]
  if (route.matched && route.matched.length) {
    route.matched.forEach((m) => {
      if (m.meta?.title) items.push({ title: m.meta.title })
    })
  }
  return items
})

function menuPathToRoutePath(parentPath, childPath) {
  const fullPath = childPath || parentPath
  if (fullPath.startsWith('/')) return fullPath
  if (fullPath === 'dashboard') return '/dashboard'
  if (fullPath === 'system') return '/system'
  const parent = parentPath || ''
  const child = childPath || ''
  function toKebab(str) {
    return str.replace(/([A-Z])/g, '-$1').toLowerCase()
  }
  if (child) {
    return '/' + toKebab(parent) + '/' + toKebab(child)
  }
  return '/' + toKebab(parent)
}

function buildMenuTree() {
  const menus = userStore.menus || []
  return menus.map((m) => mapBackendMenuToMenuItem(m, null))
}

function mapBackendMenuToMenuItem(menu, parentPath) {
  const routePath = menuPathToRoutePath(parentPath, menu.path)
  const item = {
    key: routePath,
    title: menu.name,
    icon: iconMap[menu.icon]
  }
  if (menu.children && menu.children.length) {
    item.children = menu.children.map((c) => mapBackendMenuToMenuItem(c, menu.path))
  }
  return item
}

const menuData = ref([])

function refreshMenu() {
  menuData.value = buildMenuTree()
}

function handleMenuSelect(key) {
  router.push(key)
}

function toggleCollapsed() {
  collapsed.value = !collapsed.value
}

function handleLogout() {
  userStore.logout()
}

function handleProfile() {
  Message.info('个人设置功能开发中')
}

watch(
  () => route.path,
  (path) => {
    selectedKeys.value = [path]
    const parent = menuData.value.find(
      (m) => m.children && m.children.some((c) => c.key === path)
    )
    if (parent) {
      openKeys.value = [parent.key]
    }
  },
  { immediate: true }
)

watch(
  () => userStore.menus,
  () => {
    refreshMenu()
  },
  { deep: true }
)

onMounted(() => {
  setTimeout(() => refreshMenu(), 200)
})
</script>

<template>
  <a-layout class="layout-wrapper">
    <a-layout-sider
      :collapsed="collapsed"
      :width="220"
      :collapsed-width="60"
      collapsible
      hide-trigger
      class="layout-sider"
    >
      <div class="sider-header">
        <div class="logo-icon">
          <span class="mini-bar" v-for="i in 3" :key="i"></span>
        </div>
        <transition name="fade">
          <span v-if="!collapsed" class="logo-title">钢e通</span>
        </transition>
      </div>

      <a-menu
        v-model:selected-keys="selectedKeys"
        v-model:open-keys="openKeys"
        :selected-style="{ backgroundColor: '#ff6b35' }"
        class="layout-menu"
        @menu-item-click="handleMenuSelect"
      >
        <template v-for="item in menuData" :key="item.key">
          <a-sub-menu v-if="item.children && item.children.length" :key="item.key">
            <template #icon>
              <component :is="item.icon" v-if="item.icon" />
            </template>
            <template #title>{{ item.title }}</template>
            <a-menu-item
              v-for="child in item.children"
              :key="child.key"
            >
              <template #icon>
                <component :is="child.icon" v-if="child.icon" />
              </template>
              {{ child.title }}
            </a-menu-item>
          </a-sub-menu>
          <a-menu-item v-else :key="item.key">
            <template #icon>
              <component :is="item.icon" v-if="item.icon" />
            </template>
            {{ item.title }}
          </a-menu-item>
        </template>
      </a-menu>
    </a-layout-sider>

    <a-layout class="layout-right">
      <a-layout-header class="layout-header">
        <div class="header-left">
          <a-button type="outline" size="small" class="toggle-btn" @click="toggleCollapsed">
            <icon-menu-fold v-if="!collapsed" />
            <icon-menu-unfold v-else />
          </a-button>
          <a-breadcrumb class="header-breadcrumb">
            <a-breadcrumb-item v-for="(bc, idx) in breadcrumbItems" :key="idx">
              {{ bc.title }}
            </a-breadcrumb-item>
          </a-breadcrumb>
        </div>

        <div class="header-right">
          <a-dropdown>
            <div class="user-info">
              <a-avatar :size="32" class="user-avatar">
                <template #icon>
                  <icon-user />
                </template>
              </a-avatar>
              <span class="user-name">{{ userName }}</span>
              <a-tag :color="roleColor" bordered class="role-tag">
                {{ roleName }}
              </a-tag>
            </div>
            <template #content>
              <a-dmenu>
                <a-dmenu-item @click="handleProfile">
                  <template #icon>
                    <icon-edit />
                  </template>
                  个人设置
                </a-dmenu-item>
                <a-dmenu-item @click="handleLogout">
                  <template #icon>
                    <icon-poweroff />
                  </template>
                  退出登录
                </a-dmenu-item>
              </a-dmenu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content class="layout-content">
        <div class="content-card">
          <router-view v-slot="{ Component, route: routeSlot }">
            <transition name="fade" mode="out-in">
              <component :is="Component" :key="routeSlot.path" />
            </transition>
          </router-view>
        </div>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.layout-wrapper {
  height: 100vh;
  overflow: hidden;
}

.layout-sider {
  background: #2d3748;
  transition: all 0.3s ease;
  overflow: hidden;
}

.sider-header {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  background: #1a202c;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  gap: 12px;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #ff6b35 0%, #f7931e 100%);
  border-radius: 6px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 3px;
  padding: 6px 4px;
  flex-shrink: 0;
}

.mini-bar {
  width: 4px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 1px;
}

.mini-bar:nth-child(1) { height: 40%; }
.mini-bar:nth-child(2) { height: 70%; }
.mini-bar:nth-child(3) { height: 100%; }

.logo-title {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 2px;
  white-space: nowrap;
}

.layout-menu {
  background: #2d3748;
  border: none;
  width: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  height: calc(100vh - 60px);
}

:deep(.layout-menu .arco-menu-item),
:deep(.layout-menu .arco-menu-inline-header) {
  color: #cbd5e0;
  margin: 2px 0;
  border-radius: 4px;
}

:deep(.layout-menu .arco-menu-item:hover),
:deep(.layout-menu .arco-menu-inline-header:hover) {
  color: #fff;
  background: rgba(255, 255, 255, 0.06);
}

:deep(.layout-menu .arco-menu-selected) {
  color: #fff !important;
  background: #ff6b35 !important;
}

:deep(.layout-menu .arco-menu-inline .arco-menu-item) {
  padding-left: 52px !important;
}

:deep(.layout-sider[class*="-collapsed"] .arco-menu-pop-header) {
  color: #cbd5e0;
}

.layout-right {
  background: #f5f6fa;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.layout-header {
  height: 60px;
  background: #1a202c;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.toggle-btn {
  color: #cbd5e0;
  border-color: rgba(255, 255, 255, 0.15);
  background: transparent;
}

.toggle-btn:hover {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.05);
}

:deep(.header-breadcrumb .arco-breadcrumb-item) {
  color: #cbd5e0;
}

:deep(.header-breadcrumb .arco-breadcrumb-item:last-child) {
  color: #fff;
  font-weight: 500;
}

:deep(.header-breadcrumb .arco-breadcrumb-separator) {
  color: rgba(255, 255, 255, 0.3);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 6px;
  transition: background 0.2s;
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.08);
}

.user-avatar {
  background: linear-gradient(135deg, #ff6b35, #f7931e);
  border: 2px solid rgba(255, 255, 255, 0.2);
}

.user-name {
  color: #fff;
  font-size: 14px;
  font-weight: 500;
}

.role-tag {
  border-radius: 4px;
  font-size: 12px;
  padding: 2px 8px;
  color: #fff !important;
  border-color: transparent !important;
}

.layout-content {
  flex: 1;
  padding: 20px;
  overflow: auto;
}

.content-card {
  min-height: calc(100vh - 140px);
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
