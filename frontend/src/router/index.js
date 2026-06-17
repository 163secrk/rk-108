import { createRouter, createWebHashHistory } from 'vue-router'
import Layout from '@/layouts/Layout.vue'

export const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    name: 'Layout',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据看板', icon: 'icon-dashboard' }
      },
      {
        path: '/system',
        name: 'System',
        component: () => import('@/views/System.vue'),
        meta: { title: '系统设置', icon: 'icon-settings' }
      }
    ]
  }
]

export const asyncRoutes = [
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '数据看板', icon: 'icon-dashboard' }
  },
  {
    path: '/basic',
    name: 'Basic',
    meta: { title: '基础资料', icon: 'icon-storage' },
    children: [
      {
        path: '/basic/steel-param',
        name: 'SteelParam',
        component: () => import('@/views/basic/SteelParam.vue'),
        meta: { title: '钢材参数', icon: 'icon-file' }
      },
      {
        path: '/basic/product',
        name: 'Product',
        component: () => import('@/views/basic/Product.vue'),
        meta: { title: '商品档案', icon: 'icon-sku' }
      },
      {
        path: '/basic/warehouse',
        name: 'Warehouse',
        component: () => import('@/views/basic/Warehouse.vue'),
        meta: { title: '仓库设置', icon: 'icon-building' }
      },
      {
        path: '/basic/partner',
        name: 'Partner',
        component: () => import('@/views/basic/Partner.vue'),
        meta: { title: '往来单位', icon: 'icon-user-group' }
      }
    ]
  },
  {
    path: '/purchase',
    name: 'Purchase',
    meta: { title: '采购管理', icon: 'icon-cart' },
    children: [
      {
        path: '/purchase/contract',
        name: 'PurchaseContract',
        component: () => import('@/views/purchase/Contract.vue'),
        meta: { title: '采购合同', icon: 'icon-file-copy' }
      },
      {
        path: '/purchase/stock-in',
        name: 'StockIn',
        component: () => import('@/views/purchase/StockIn.vue'),
        meta: { title: '入库', icon: 'icon-import' }
      }
    ]
  },
  {
    path: '/inventory',
    name: 'Inventory',
    meta: { title: '库存管理', icon: 'icon-list' },
    children: [
      {
        path: '/inventory/query',
        name: 'InventoryQuery',
        component: () => import('@/views/inventory/Query.vue'),
        meta: { title: '库存查询', icon: 'icon-search' }
      },
      {
        path: '/inventory/transfer',
        name: 'InventoryTransfer',
        component: () => import('@/views/inventory/Transfer.vue'),
        meta: { title: '调拨', icon: 'icon-exchange' }
      },
      {
        path: '/inventory/check',
        name: 'InventoryCheck',
        component: () => import('@/views/inventory/Check.vue'),
        meta: { title: '盘点', icon: 'icon-thunderbolt' }
      }
    ]
  },
  {
    path: '/sales',
    name: 'Sales',
    meta: { title: '销售管理', icon: 'icon-tag' },
    children: [
      {
        path: '/sales/order',
        name: 'SalesOrder',
        component: () => import('@/views/sales/Order.vue'),
        meta: { title: '销售订单', icon: 'icon-file-document' }
      },
      {
        path: '/sales/stock-out',
        name: 'StockOut',
        component: () => import('@/views/sales/StockOut.vue'),
        meta: { title: '出库', icon: 'icon-export' }
      }
    ]
  },
  {
    path: '/finance',
    name: 'Finance',
    meta: { title: '财务管理', icon: 'icon-yen-circle' },
    children: [
      {
        path: '/finance/receivable-payable',
        name: 'ReceivablePayable',
        component: () => import('@/views/finance/ReceivablePayable.vue'),
        meta: { title: '应收应付', icon: 'icon-account' }
      },
      {
        path: '/finance/cost',
        name: 'Cost',
        component: () => import('@/views/finance/Cost.vue'),
        meta: { title: '成本核算', icon: 'icon-calculator' }
      }
    ]
  },
  {
    path: '/system',
    name: 'System',
    component: () => import('@/views/System.vue'),
    meta: { title: '系统设置', icon: 'icon-settings' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes: constantRoutes
})

export default router
