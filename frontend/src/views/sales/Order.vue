<script setup>
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import {
  Message,
  Modal,
  Notification
} from '@arco-design/web-vue'
import {
  IconPlus,
  IconEdit,
  IconDelete,
  IconCheck,
  IconSearch,
  IconRefresh,
  IconPlusCircle,
  IconMinusCircle,
  IconStorage,
  IconExclamationCircle,
  IconCheckCircle
} from '@arco-design/web-vue/es/icon'
import {
  getSalesOrderList,
  getSalesOrderDetail,
  generateSalesOrderNo,
  addSalesOrder,
  updateSalesOrder,
  deleteSalesOrder,
  confirmSalesOrder,
  checkCredit,
  getAvailableStockTree
} from '@/api/sales'
import {
  getPartnerList,
  getProductList,
  getWarehouseList,
  getMaterialList
} from '@/api/basicData'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const confirming = ref(false)
const searchForm = reactive({
  status: '',
  orderNo: '',
  customerId: undefined
})

const tableData = ref([])
const customerOptions = ref([])
const productOptions = ref([])
const warehouseOptions = ref([])
const materialOptions = ref([])

const statusOptions = [
  { label: '全部', value: '' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

const statusTagMap = {
  DRAFT: { color: 'gray', text: '草稿' },
  CONFIRMED: { color: 'blue', text: '已确认' },
  COMPLETED: { color: 'green', text: '已完成' },
  CANCELLED: { color: 'red', text: '已取消' }
}

const modalVisible = ref(false)
const modalTitle = ref('新建销售订单')
const editMode = ref(false)
const formRef = ref(null)

const orderForm = reactive({
  id: undefined,
  orderNo: '',
  customerId: undefined,
  orderDate: '',
  status: 'DRAFT',
  totalAmount: 0,
  totalWeight: 0,
  isOverCredit: 0,
  remark: '',
  createBy: undefined,
  items: [],
  deletedItemIds: [],
  locks: []
})

const formRules = {
  customerId: [{ required: true, message: '请选择客户' }],
  orderDate: [{ required: true, message: '请选择订单日期' }]
}

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const pageSizeOptions = [10, 20, 50, 100]

const creditInfo = reactive({
  show: false,
  isOverCredit: false,
  creditLimit: 0,
  currentDebt: 0,
  orderAmount: 0,
  totalAfter: 0,
  message: ''
})

const stockModalVisible = ref(false)
const stockModalLoading = ref(false)
const currentItemIndex = ref(-1)
const stockSearchForm = reactive({
  warehouseId: undefined,
  materialId: undefined,
  furnaceNo: ''
})
const stockTreeData = ref([])
const selectedStockMap = reactive({})

function initSelectedStockMap() {
  for (const key in selectedStockMap) {
    delete selectedStockMap[key]
  }
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      status: searchForm.status
    }
    const res = await getSalesOrderList(params)
    if (res.code === 200) {
      let data = res.data
      if (searchForm.orderNo) {
        data = data.filter(item => item.orderNo && item.orderNo.includes(searchForm.orderNo))
      }
      if (searchForm.customerId) {
        data = data.filter(item => item.customerId === searchForm.customerId)
      }
      tableData.value = data
      pagination.total = data.length
    }
  } finally {
    loading.value = false
  }
}

async function loadCustomers() {
  try {
    const res = await getPartnerList({ type: 'CUSTOMER' })
    if (res.code === 200) {
      customerOptions.value = res.data.map(item => ({
        label: item.name,
        value: item.id,
        creditLimit: item.creditLimit
      }))
    }
  } catch (e) {
    console.error('加载客户失败', e)
    Message.error('加载客户数据失败')
  }
}

async function loadProducts() {
  try {
    const res = await getProductList()
    if (res.code === 200) {
      productOptions.value = res.data.map(item => ({
        label: `${item.productCode} - ${item.productName}`,
        value: item.id,
        ...item
      }))
    }
  } catch (e) {
    console.error('加载商品失败', e)
    Message.error('加载商品数据失败')
  }
}

async function loadWarehouses() {
  try {
    const res = await getWarehouseList()
    if (res.code === 200) {
      warehouseOptions.value = res.data
        .filter(w => w.level === 1)
        .map(item => ({
          label: item.name,
          value: item.id
        }))
    }
  } catch (e) {
    console.error('加载仓库失败', e)
  }
}

async function loadMaterials() {
  try {
    const res = await getMaterialList()
    if (res.code === 200) {
      materialOptions.value = res.data.map(item => ({
        label: item.name,
        value: item.id
      }))
    }
  } catch (e) {
    console.error('加载材质失败', e)
  }
}

async function handleSearch() {
  pagination.current = 1
  await loadData()
}

async function handleReset() {
  searchForm.status = ''
  searchForm.orderNo = ''
  searchForm.customerId = undefined
  pagination.current = 1
  await loadData()
}

function handlePageChange(page) {
  pagination.current = page
}

function handlePageSizeChange(size) {
  pagination.pageSize = size
  pagination.current = 1
}

const paginatedData = computed(() => {
  const start = (pagination.current - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return tableData.value.slice(start, end)
})

function rowClassName(record) {
  if (record.isOverCredit === 1 || record.isOverCredit === true) {
    return 'over-credit-row'
  }
  return ''
}

async function handleAdd() {
  editMode.value = false
  modalTitle.value = '新建销售订单'
  resetOrderForm()
  try {
    const res = await generateSalesOrderNo()
    if (res.code === 200) {
      orderForm.orderNo = res.data
    }
  } catch (e) {
    console.error('生成订单编号失败', e)
  }
  orderForm.createBy = userStore.userInfo?.id
  addEmptyItem()
  creditInfo.show = false
  creditInfo.isOverCredit = false
  modalVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

async function handleEdit(record) {
  editMode.value = true
  modalTitle.value = '编辑销售订单'
  try {
    const res = await getSalesOrderDetail(record.id)
    if (res.code === 200) {
      const order = res.data
      orderForm.id = order.id
      orderForm.orderNo = order.orderNo
      orderForm.customerId = order.customerId
      orderForm.orderDate = order.orderDate
      orderForm.status = order.status
      orderForm.totalAmount = Number(order.totalAmount) || 0
      orderForm.totalWeight = Number(order.totalWeight) || 0
      orderForm.isOverCredit = order.isOverCredit || 0
      orderForm.remark = order.remark || ''
      orderForm.createBy = order.createBy
      orderForm.items = (order.items || []).map(item => ({
        ...item,
        quantity: Number(item.quantity) || 0,
        unitPrice: Number(item.unitPrice) || 0,
        amount: Number(item.amount) || 0,
        weight: Number(item.weight) || 0,
        weightPerMeter: Number(item.weightPerMeter) || 0,
        length: item.length || '',
        outStockQuantity: Number(item.outStockQuantity) || 0,
        remainingQuantity: Number(item.remainingQuantity) || 0,
        stockLocks: (item.stockLocks || []).map(l => ({
          ...l,
          lockQuantity: Number(l.lockQuantity) || 0,
          lockWeight: Number(l.lockWeight) || 0
        }))
      }))
      orderForm.deletedItemIds = []
      creditInfo.show = false
      if (order.isOverCredit === 1) {
        creditInfo.show = true
        creditInfo.isOverCredit = true
      }
      modalVisible.value = true
      nextTick(() => {
        formRef.value?.clearValidate()
      })
    }
  } catch (e) {
    console.error('加载订单详情失败', e)
    Message.error('加载订单详情失败')
  }
}

function handleView(record) {
  handleEdit(record)
}

async function handleDelete(record) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除订单「${record.orderNo}」吗？删除后无法恢复。`,
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      try {
        const res = await deleteSalesOrder(record.id)
        if (res.code === 200) {
          Message.success('删除成功')
          await loadData()
        } else {
          Message.error(res.message || '删除失败')
        }
      } catch (e) {
        console.error('删除失败', e)
        Message.error('删除失败')
      }
    }
  })
}

async function handleConfirm(record) {
  Modal.confirm({
    title: '确认订单',
    content: `确定要确认订单「${record.orderNo}」吗？确认后将锁定对应库存，且不能再修改订单内容。`,
    okButtonProps: { status: 'success' },
    onOk: async () => {
      confirming.value = true
      try {
        const res = await confirmSalesOrder(record.id)
        if (res.code === 200) {
          const data = res.data || {}
          if (data.isOverCredit) {
            const creditInfo = data.creditInfo || {}
            Notification.warning({
              title: '信用额度超额警告',
              content: creditInfo.message || '该客户信用额度已超额，财务可特批处理。',
              duration: 8000
            })
          }
          Message.success(res.message || '确认成功')
          await loadData()
        } else {
          Message.error(res.message || '确认失败')
        }
      } catch (e) {
        console.error('确认失败', e)
        Message.error('确认失败')
      } finally {
        confirming.value = false
      }
    }
  })
}

function resetOrderForm() {
  orderForm.id = undefined
  orderForm.orderNo = ''
  orderForm.customerId = undefined
  orderForm.orderDate = ''
  orderForm.status = 'DRAFT'
  orderForm.totalAmount = 0
  orderForm.totalWeight = 0
  orderForm.isOverCredit = 0
  orderForm.remark = ''
  orderForm.createBy = undefined
  orderForm.items = []
  orderForm.deletedItemIds = []
  orderForm.locks = []
}

function addEmptyItem() {
  orderForm.items.push({
    id: undefined,
    orderId: undefined,
    productId: undefined,
    productCode: '',
    productName: '',
    materialId: undefined,
    materialName: '',
    specId: undefined,
    diameter: '',
    wallThickness: '',
    length: '',
    weightPerMeter: 0,
    quantity: 0,
    unitPrice: 0,
    amount: 0,
    weight: 0,
    outStockQuantity: 0,
    remainingQuantity: 0,
    stockLocks: [],
    sortNo: orderForm.items.length + 1,
    createTime: ''
  })
}

function handleAddItem() {
  addEmptyItem()
}

function handleRemoveItem(index) {
  const item = orderForm.items[index]
  if (item && item.id) {
    orderForm.deletedItemIds.push(item.id)
  }
  orderForm.items.splice(index, 1)
  recalculateSortNo()
  recalculateTotals()
}

function recalculateSortNo() {
  orderForm.items.forEach((item, idx) => {
    item.sortNo = idx + 1
  })
}

function handleProductChange(item, productId) {
  if (!productId) {
    item.productId = undefined
    item.productCode = ''
    item.productName = ''
    item.materialId = undefined
    item.materialName = ''
    item.specId = undefined
    item.diameter = ''
    item.wallThickness = ''
    item.length = ''
    item.weightPerMeter = 0
    item.stockLocks = []
    calculateRow(item)
    return
  }

  const product = productOptions.value.find(p => p.value === productId)
  if (product) {
    item.productId = productId
    item.productCode = product.productCode
    item.productName = product.productName
    item.materialId = product.materialId
    item.materialName = product.materialName
    item.specId = product.specId
    item.diameter = product.diameter
    item.wallThickness = product.wallThickness
    item.length = product.length
    item.weightPerMeter = Number(product.weightPerMeter) || 0
    item.stockLocks = []
    calculateRow(item)
  }
}

function calculateRow(item) {
  const quantity = Number(item.quantity) || 0
  const unitPrice = Number(item.unitPrice) || 0
  item.amount = quantity * unitPrice

  const weightPerMeter = Number(item.weightPerMeter) || 0
  const length = Number(item.length) || 0
  if (weightPerMeter > 0 && length > 0) {
    const lengthM = length / 1000
    item.weight = weightPerMeter * lengthM * quantity
  } else {
    item.weight = 0
  }

  item.remainingQuantity = quantity - (Number(item.outStockQuantity) || 0)
  recalculateTotals()
}

function recalculateTotals() {
  let totalAmount = 0
  let totalWeight = 0
  orderForm.items.forEach(item => {
    totalAmount += Number(item.amount) || 0
    totalWeight += Number(item.weight) || 0
  })
  orderForm.totalAmount = totalAmount
  orderForm.totalWeight = totalWeight
}

function handleQuantityChange(item) {
  calculateRow(item)
  if (item.quantity > 0) {
    checkStockForItem(item)
  }
}

function handlePriceChange(item) {
  calculateRow(item)
}

async function checkStockForItem(item) {
  // 简化版，不强制校验，在打开库存选择器时提示
}

async function handleSelectStock(index) {
  const item = orderForm.items[index]
  if (!item.productId) {
    Message.warning('请先选择商品规格')
    return
  }
  currentItemIndex.value = index
  stockSearchForm.warehouseId = undefined
  stockSearchForm.materialId = undefined
  stockSearchForm.furnaceNo = ''
  initSelectedStockMap()

  // 预填充已有的锁定批次
  if (item.stockLocks && item.stockLocks.length > 0) {
    item.stockLocks.forEach(lock => {
      if (lock.stockId) {
        selectedStockMap[lock.stockId] = {
          id: lock.stockId,
          lockQuantity: Number(lock.lockQuantity) || 0,
          warehouseId: lock.warehouseId,
          furnaceNo: lock.furnaceNo,
          checked: true
        }
      }
    })
  }

  stockModalVisible.value = true
  await loadStockTree()
}

async function loadStockTree() {
  const item = orderForm.items[currentItemIndex.value]
  if (!item) return
  stockModalLoading.value = true
  try {
    const params = {
      productId: item.productId,
      warehouseId: stockSearchForm.warehouseId || undefined,
      materialId: stockSearchForm.materialId || undefined,
      excludeOrderId: orderForm.id || undefined
    }
    const res = await getAvailableStockTree(params)
    if (res.code === 200) {
      let data = res.data || []
      if (stockSearchForm.furnaceNo) {
        data = data.map(summary => {
          if (summary.children) {
            const filtered = summary.children.filter(b =>
              b.furnaceNo && b.furnaceNo.includes(stockSearchForm.furnaceNo)
            )
            if (filtered.length > 0) {
              return {
                ...summary,
                children: filtered,
                totalQuantity: filtered.reduce((sum, b) => sum + (Number(b.remainingQuantity) || 0), 0),
                totalWeight: filtered.reduce((sum, b) => {
                  const w = Number(b.remainingWeight) || 0
                  return sum + w
                }, 0)
              }
            }
          }
          return null
        }).filter(s => s !== null)
      }
      stockTreeData.value = data
    }
  } catch (e) {
    console.error('加载库存失败', e)
    Message.error('加载库存数据失败')
  } finally {
    stockModalLoading.value = false
  }
}

async function handleStockSearch() {
  await loadStockTree()
}

async function handleStockReset() {
  stockSearchForm.warehouseId = undefined
  stockSearchForm.materialId = undefined
  stockSearchForm.furnaceNo = ''
  await loadStockTree()
}

function getSelectedQty() {
  let total = 0
  for (const key in selectedStockMap) {
    total += Number(selectedStockMap[key].lockQuantity) || 0
  }
  return total
}

function handleBatchCheckChange(batch, checked) {
  if (checked) {
    if (!selectedStockMap[batch.id]) {
      selectedStockMap[batch.id] = {
        id: batch.id,
        lockQuantity: 0,
        warehouseId: getWarehouseIdByBatch(batch.id),
        furnaceNo: batch.furnaceNo,
        checked: true
      }
    }
    selectedStockMap[batch.id].checked = true
    if (!selectedStockMap[batch.id].lockQuantity) {
      selectedStockMap[batch.id].lockQuantity = Math.min(
        Number(batch.remainingQuantity) || 0,
        1
      )
    }
  } else {
    if (selectedStockMap[batch.id]) {
      selectedStockMap[batch.id].checked = false
    }
  }
}

function getWarehouseIdByBatch(batchId) {
  for (const summary of stockTreeData.value) {
    if (summary.children) {
      const found = summary.children.find(b => b.id === batchId)
      if (found) return summary.warehouseId
    }
  }
  return undefined
}

function getSummaryChecked(summary) {
  if (!summary.children || summary.children.length === 0) return false
  const checkedCount = summary.children.filter(b =>
    selectedStockMap[b.id] && selectedStockMap[b.id].checked
  ).length
  return checkedCount === summary.children.length
}

function getSummaryIndeterminate(summary) {
  if (!summary.children || summary.children.length === 0) return false
  const checkedCount = summary.children.filter(b =>
    selectedStockMap[b.id] && selectedStockMap[b.id].checked
  ).length
  return checkedCount > 0 && checkedCount < summary.children.length
}

function handleSummaryCheckChange(summary, checked) {
  if (!summary.children) return
  summary.children.forEach(batch => {
    handleBatchCheckChange(batch, checked)
  })
}

function getBatchMaxQty(batch) {
  return Number(batch.remainingQuantity) || 0
}

function confirmStockSelection() {
  const item = orderForm.items[currentItemIndex.value]
  if (!item) return

  const selectedQty = getSelectedQty()
  if (selectedQty <= 0) {
    Message.warning('请至少选择一个批次并填写数量')
    return
  }

  const newLocks = []
  for (const key in selectedStockMap) {
    const s = selectedStockMap[key]
    if (s.checked && Number(s.lockQuantity) > 0) {
      let warehouseName = ''
      let furnaceNo = s.furnaceNo || ''
      for (const summary of stockTreeData.value) {
        if (summary.children) {
          const batch = summary.children.find(b => b.id === s.id)
          if (batch) {
            warehouseName = summary.warehouseName || ''
            furnaceNo = batch.furnaceNo || furnaceNo
            break
          }
        }
      }
      newLocks.push({
        id: undefined,
        orderId: undefined,
        orderItemId: undefined,
        stockId: s.id,
        productId: item.productId,
        warehouseId: s.warehouseId,
        warehouseName: warehouseName,
        furnaceNo: furnaceNo,
        lockQuantity: Number(s.lockQuantity),
        lockWeight: 0,
        status: 'LOCKED',
        createTime: '',
        releaseTime: ''
      })
    }
  }

  item.stockLocks = newLocks
  if (selectedQty !== Number(item.quantity)) {
    item.quantity = selectedQty
    calculateRow(item)
  }
  stockModalVisible.value = false
  Message.success('库存选择成功')
}

function cancelStockSelection() {
  stockModalVisible.value = false
}

function formatLocksText(locks) {
  if (!locks || locks.length === 0) return '-'
  return locks.map(l =>
    `${l.warehouseName || ''}/${l.furnaceNo || ''}×${l.lockQuantity}支`
  ).join('；')
}

function getLockTotalQty(item) {
  if (!item.stockLocks) return 0
  return item.stockLocks.reduce((sum, l) => sum + (Number(l.lockQuantity) || 0), 0)
}

async function doCheckCredit() {
  if (!orderForm.customerId || !orderForm.totalAmount) {
    creditInfo.show = false
    return
  }
  try {
    const res = await checkCredit({
      customerId: orderForm.customerId,
      orderAmount: orderForm.totalAmount
    })
    if (res.code === 200) {
      const info = res.data || {}
      creditInfo.show = true
      creditInfo.isOverCredit = info.isOverCredit || false
      creditInfo.creditLimit = Number(info.creditLimit) || 0
      creditInfo.currentDebt = Number(info.currentDebt) || 0
      creditInfo.orderAmount = Number(info.orderAmount) || 0
      creditInfo.totalAfter = Number(info.totalAfter) || 0
      creditInfo.message = info.message || ''
    }
  } catch (e) {
    console.error('信用检查失败', e)
  }
}

watch(() => orderForm.customerId, () => {
  doCheckCredit()
})
watch(() => orderForm.totalAmount, () => {
  doCheckCredit()
})

async function handleSubmit() {
  submitting.value = true
  try {
    const valid = await formRef.value?.validate().catch(() => false)
    if (valid === false) {
      submitting.value = false
      return
    }

    if (!orderForm.customerId) {
      Message.error('请选择客户')
      submitting.value = false
      return
    }
    if (!orderForm.orderDate) {
      Message.error('请选择订单日期')
      submitting.value = false
      return
    }

    const validItems = orderForm.items.filter(item => item.productId)
    if (validItems.length === 0) {
      Message.error('请至少添加一条明细')
      submitting.value = false
      return
    }

    const hasInvalidItem = validItems.some(item => {
      const qty = Number(item.quantity) || 0
      const price = Number(item.unitPrice) || 0
      return qty <= 0 || price <= 0
    })

    if (hasInvalidItem) {
      Message.error('请填写有效的数量和单价')
      submitting.value = false
      return
    }

    // 信用额度检查（只警告不拦截）
    if (creditInfo.isOverCredit) {
      Notification.warning({
        title: '信用额度超额警告',
        content: creditInfo.message || '该客户信用额度已超额，财务可特批处理。',
        duration: 5000
      })
    }

    const submitData = {
      ...orderForm,
      items: orderForm.items.filter(item => item.productId)
    }

    let res
    if (editMode.value) {
      res = await updateSalesOrder(submitData)
    } else {
      res = await addSalesOrder(submitData)
    }

    if (res.code === 200) {
      Message.success(editMode.value ? '修改成功' : '创建成功')
      modalVisible.value = false
      await loadData()
    } else {
      Message.error(res.message || (editMode.value ? '修改失败' : '创建失败'))
    }
  } catch (e) {
    console.error('提交失败', e)
    Message.error('提交失败')
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  modalVisible.value = false
}

onMounted(() => {
  loadData()
  loadCustomers()
  loadProducts()
  loadWarehouses()
  loadMaterials()
})
</script>

<template>
  <div class="page-wrapper">
    <div class="page-header">
      <h2 class="page-title">销售订单</h2>
      <p class="page-desc">销售订单的创建、确认、库存锁定与信用额度管理</p>
    </div>

    <div class="page-body">
      <div class="search-bar">
        <a-form :model="searchForm" layout="inline">
          <a-form-item label="订单状态">
            <a-select
              v-model="searchForm.status"
              placeholder="全部"
              style="width: 150px"
              :options="statusOptions"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="订单编号">
            <a-input
              v-model="searchForm.orderNo"
              placeholder="请输入订单编号"
              style="width: 200px"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="客户">
            <a-select
              v-model="searchForm.customerId"
              placeholder="请选择客户"
              style="width: 200px"
              :options="customerOptions"
              allow-clear
            />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="handleSearch">
              <template #icon>
                <icon-search />
              </template>
              搜索
            </a-button>
            <a-button @click="handleReset" style="margin-left: 8px">
              <template #icon>
                <icon-refresh />
              </template>
              重置
            </a-button>
          </a-form-item>
        </a-form>
      </div>

      <div class="toolbar">
        <a-button type="primary" @click="handleAdd">
          <template #icon>
            <icon-plus />
          </template>
          新建订单
        </a-button>
      </div>

      <a-table
        :loading="loading"
        :data="paginatedData"
        :pagination="{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          showTotal: true,
          pageSizeOptions: pageSizeOptions,
          showJumper: true,
          showPageSize: true
        }"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
        :bordered="false"
        stripe
        :row-class-name="rowClassName"
      >
        <template #columns>
          <a-table-column title="订单编号" data-index="orderNo" :width="180" fixed="left">
            <template #cell="{ record }">
              <span>
                {{ record.orderNo }}
                <a-tag v-if="record.isOverCredit === 1 || record.isOverCredit === true" color="red" size="small" style="margin-left: 4px">
                  超额
                </a-tag>
              </span>
            </template>
          </a-table-column>
          <a-table-column title="客户" data-index="customerName" min-width="200" />
          <a-table-column title="订单日期" data-index="orderDate" :width="120" />
          <a-table-column title="订单状态" :width="100">
            <template #cell="{ record }">
              <a-tag :color="statusTagMap[record.status]?.color">
                {{ statusTagMap[record.status]?.text }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="订单金额(元)" :width="140" align="right">
            <template #cell="{ record }">
              <span class="amount-text">{{ Number(record.totalAmount).toFixed(2) }}</span>
            </template>
          </a-table-column>
          <a-table-column title="总吨位(吨)" :width="120" align="right">
            <template #cell="{ record }">
              <span class="weight-text">{{ Number(record.totalWeight).toFixed(3) }}</span>
            </template>
          </a-table-column>
          <a-table-column title="创建人" data-index="createByName" :width="100" />
          <a-table-column title="创建时间" data-index="createTime" :width="170" />
          <a-table-column title="操作" :width="260" fixed="right">
            <template #cell="{ record }">
              <a-button type="outline" size="small" @click="handleView(record)">
                查看
              </a-button>
              <a-button
                v-if="record.status === 'DRAFT'"
                type="outline"
                size="small"
                status="primary"
                @click="handleEdit(record)"
                style="margin-left: 4px"
              >
                编辑
              </a-button>
              <a-button
                v-if="record.status === 'DRAFT'"
                type="outline"
                size="small"
                status="success"
                @click="handleConfirm(record)"
                :loading="confirming"
                style="margin-left: 4px"
              >
                <template #icon>
                  <icon-check />
                </template>
                确认
              </a-button>
              <a-button
                v-if="record.status === 'DRAFT'"
                type="outline"
                size="small"
                status="danger"
                @click="handleDelete(record)"
                style="margin-left: 4px"
              >
                <template #icon>
                  <icon-delete />
                </template>
                删除
              </a-button>
            </template>
          </a-table-column>
        </template>
        <template #empty>
          <a-empty description="暂无数据" />
        </template>
      </a-table>
    </div>

    <a-modal
      v-model:visible="modalVisible"
      :title="modalTitle"
      :mask-closable="false"
      width="1300px"
      :ok-loading="submitting"
      @ok="handleSubmit"
      @cancel="handleCancel"
      :ok-text="editMode ? '保存修改' : '创建订单'"
      :cancel-text="'取消'"
    >
      <div class="contract-modal">
        <a-form
          ref="formRef"
          :model="orderForm"
          :rules="formRules"
          layout="vertical"
        >
          <div v-if="creditInfo.show"
               :class="['credit-warning-box', creditInfo.isOverCredit ? 'credit-over' : 'credit-ok']">
            <template v-if="creditInfo.isOverCredit">
              <icon-exclamation-circle />
              <span class="credit-warn-text">{{ creditInfo.message }}</span>
            </template>
            <template v-else>
              <icon-check-circle />
              <span class="credit-ok-text">
                信用额度：{{ creditInfo.creditLimit.toFixed(2) }}元，
                当前欠款：{{ creditInfo.currentDebt.toFixed(2) }}元，
                本订单：{{ creditInfo.orderAmount.toFixed(2) }}元，
                合计：{{ creditInfo.totalAfter.toFixed(2) }}元，额度正常。
              </span>
            </template>
          </div>

          <div class="form-section">
            <h3 class="section-title">订单基本信息</h3>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="订单编号" field="orderNo">
                  <a-input v-model="orderForm.orderNo" readonly />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="客户" field="customerId" required>
                  <a-select
                    v-model="orderForm.customerId"
                    placeholder="请选择客户"
                    :options="customerOptions"
                    :disabled="orderForm.status !== 'DRAFT'"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="订单日期" field="orderDate" required>
                  <a-date-picker
                    v-model="orderForm.orderDate"
                    placeholder="请选择订单日期"
                    style="width: 100%"
                    :disabled="orderForm.status !== 'DRAFT'"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="订单状态">
                  <a-tag :color="statusTagMap[orderForm.status]?.color">
                    {{ statusTagMap[orderForm.status]?.text }}
                  </a-tag>
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="备注">
              <a-textarea
                v-model="orderForm.remark"
                placeholder="请输入备注"
                :rows="2"
                :disabled="orderForm.status !== 'DRAFT'"
              />
            </a-form-item>
          </div>

          <div class="form-section">
            <div class="section-header">
              <h3 class="section-title">钢材明细</h3>
              <a-button
                v-if="orderForm.status === 'DRAFT'"
                type="outline"
                size="small"
                @click="handleAddItem"
              >
                <template #icon>
                  <icon-plus-circle />
                </template>
                添加明细
              </a-button>
            </div>

            <div class="detail-table-wrapper">
              <table class="detail-table">
                <thead>
                  <tr>
                    <th style="width: 50px">序号</th>
                    <th style="width: 240px">商品信息</th>
                    <th style="width: 80px">材质</th>
                    <th style="width: 120px">规格(直径×壁厚)</th>
                    <th style="width: 70px">长度(mm)</th>
                    <th style="width: 90px">每米重量(kg)</th>
                    <th style="width: 90px">数量(支)</th>
                    <th style="width: 100px">单价(元/支)</th>
                    <th style="width: 110px">金额(元)</th>
                    <th style="width: 90px">重量(吨)</th>
                    <th style="width: 180px">库存锁定</th>
                    <th style="width: 90px" v-if="orderForm.status === 'DRAFT'">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, index) in orderForm.items" :key="index">
                    <td class="text-center">{{ index + 1 }}</td>
                    <td>
                      <a-select
                        v-if="orderForm.status === 'DRAFT'"
                        v-model="item.productId"
                        placeholder="请选择商品"
                        :options="productOptions"
                        style="width: 100%"
                        allow-clear
                        @change="(val) => handleProductChange(item, val)"
                      />
                      <span v-else>{{ item.productName }}</span>
                    </td>
                    <td>{{ item.materialName || '-' }}</td>
                    <td>
                      <span v-if="item.diameter && item.wallThickness">
                        φ{{ item.diameter }}×{{ item.wallThickness }}
                      </span>
                      <span v-else>-</span>
                    </td>
                    <td class="text-right">{{ item.length || '-' }}</td>
                    <td class="text-right">{{ Number(item.weightPerMeter).toFixed(3) || '-' }}</td>
                    <td>
                      <a-input-number
                        v-if="orderForm.status === 'DRAFT'"
                        v-model="item.quantity"
                        :min="0"
                        :precision="0"
                        style="width: 100%"
                        @change="() => handleQuantityChange(item)"
                      />
                      <span v-else class="text-right">{{ item.quantity }}</span>
                    </td>
                    <td>
                      <a-input-number
                        v-if="orderForm.status === 'DRAFT'"
                        v-model="item.unitPrice"
                        :min="0"
                        :precision="2"
                        style="width: 100%"
                        @change="() => handlePriceChange(item)"
                      />
                      <span v-else class="text-right">{{ Number(item.unitPrice).toFixed(2) }}</span>
                    </td>
                    <td class="text-right amount-cell">
                      {{ Number(item.amount).toFixed(2) }}
                    </td>
                    <td class="text-right weight-cell">
                      {{ Number(item.weight).toFixed(3) }}
                    </td>
                    <td class="text-left">
                      <div v-if="orderForm.status === 'DRAFT'" class="lock-cell">
                        <span v-if="getLockTotalQty(item) > 0" class="lock-summary">
                          已选{{ getLockTotalQty(item) }}支
                        </span>
                        <span v-else class="lock-empty">未选</span>
                        <a-button
                          type="outline"
                          size="mini"
                          status="primary"
                          @click="handleSelectStock(index)"
                        >
                          <template #icon>
                            <icon-storage />
                          </template>
                          选择库存
                        </a-button>
                      </div>
                      <div v-else class="lock-cell">
                        <span class="lock-summary">
                          {{ getLockTotalQty(item) }}支
                        </span>
                        <div class="lock-detail-text">
                          {{ formatLocksText(item.stockLocks) }}
                        </div>
                      </div>
                    </td>
                    <td v-if="orderForm.status === 'DRAFT'" class="text-center">
                      <a-button
                        type="text"
                        size="small"
                        status="danger"
                        @click="handleRemoveItem(index)"
                      >
                        <template #icon>
                          <icon-minus-circle />
                        </template>
                      </a-button>
                    </td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr class="total-row">
                    <td colspan="8" class="text-right"><strong>合计：</strong></td>
                    <td class="text-right amount-cell">
                      <strong>{{ Number(orderForm.totalAmount).toFixed(2) }}</strong>
                    </td>
                    <td class="text-right weight-cell">
                      <strong>{{ Number(orderForm.totalWeight).toFixed(3) }}</strong>
                    </td>
                    <td colspan="2"></td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        </a-form>
      </div>
    </a-modal>

    <a-modal
      v-model:visible="stockModalVisible"
      title="选择库存"
      :mask-closable="false"
      width="1100px"
      @ok="confirmStockSelection"
      @cancel="cancelStockSelection"
      ok-text="确认选择"
      cancel-text="取消"
    >
      <div class="stock-modal">
        <div class="stock-search-bar">
          <a-form :model="stockSearchForm" layout="inline">
            <a-form-item label="仓库">
              <a-select
                v-model="stockSearchForm.warehouseId"
                placeholder="全部仓库"
                style="width: 180px"
                :options="warehouseOptions"
                allow-clear
              />
            </a-form-item>
            <a-form-item label="材质">
              <a-select
                v-model="stockSearchForm.materialId"
                placeholder="全部材质"
                style="width: 180px"
                :options="materialOptions"
                allow-clear
              />
            </a-form-item>
            <a-form-item label="炉批号">
              <a-input
                v-model="stockSearchForm.furnaceNo"
                placeholder="请输入炉批号"
                style="width: 200px"
                allow-clear
              />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" size="small" @click="handleStockSearch">
                <template #icon>
                  <icon-search />
                </template>
                查询
              </a-button>
              <a-button size="small" style="margin-left: 8px" @click="handleStockReset">
                <template #icon>
                  <icon-refresh />
                </template>
                重置
              </a-button>
            </a-form-item>
          </a-form>
        </div>

        <div class="selected-summary">
          已选择数量：<span class="selected-qty">{{ getSelectedQty() }}</span> 支
        </div>

        <a-spin :loading="stockModalLoading" class="stock-tree-spin">
          <div class="stock-tree-wrapper" v-if="stockTreeData.length > 0">
            <table class="stock-tree-table">
              <thead>
                <tr>
                  <th style="width: 50px">
                    <a-checkbox disabled> </a-checkbox>
                  </th>
                  <th>仓库 / 材质 / 规格</th>
                  <th style="width: 120px" class="text-right">库存总数(支)</th>
                  <th style="width: 120px" class="text-right">库存总重(吨)</th>
                </tr>
              </thead>
              <tbody>
                <template v-for="summary in stockTreeData" :key="'sum-' + summary.id">
                  <tr class="summary-row">
                    <td class="text-center">
                      <a-checkbox
                        :model-value="getSummaryChecked(summary)"
                        :indeterminate="getSummaryIndeterminate(summary)"
                        @change="(v) => handleSummaryCheckChange(summary, v)"
                      />
                    </td>
                    <td>
                      <div class="summary-text">
                        <strong>【{{ summary.warehouseName }}】</strong>
                        <span>{{ summary.productName }}</span>
                        <span class="sub-text">({{ summary.materialName }} / {{ summary.specText }})</span>
                      </div>
                    </td>
                    <td class="text-right qty-cell">{{ summary.totalQuantity }}</td>
                    <td class="text-right weight-cell">{{ Number(summary.totalWeight).toFixed(3) }}</td>
                  </tr>
                  <template v-if="summary.children && summary.children.length > 0">
                    <tr v-for="batch in summary.children" :key="'batch-' + batch.id" class="batch-row">
                      <td class="text-center">
                        <a-checkbox
                          :model-value="selectedStockMap[batch.id]?.checked || false"
                          @change="(v) => handleBatchCheckChange(batch, v)"
                        />
                      </td>
                      <td class="batch-info">
                        <div class="batch-main">
                          <span class="batch-furnace">炉批号：{{ batch.furnaceNo }}</span>
                        </div>
                        <div class="batch-sub">
                          <span>入库日期：{{ batch.stockInDate || '-' }}</span>
                          <span style="margin-left: 16px">成本单价：{{ Number(batch.unitPrice).toFixed(2) }}元/支</span>
                        </div>
                      </td>
                      <td class="text-right">
                        <div class="batch-qty-info">
                          可用：<span class="avail-qty">{{ batch.remainingQuantity }}</span> 支
                        </div>
                        <div v-if="selectedStockMap[batch.id]?.checked" class="batch-qty-input">
                          <a-input-number
                            v-model="selectedStockMap[batch.id].lockQuantity"
                            :min="1"
                            :max="getBatchMaxQty(batch)"
                            :precision="0"
                            size="mini"
                            style="width: 120px"
                          />
                          <span style="margin-left: 4px">支</span>
                        </div>
                      </td>
                      <td class="text-right">
                        <div class="batch-weight-info">
                          {{ Number(batch.remainingWeight).toFixed(3) }} 吨
                        </div>
                      </td>
                    </tr>
                  </template>
                </template>
              </tbody>
            </table>
          </div>
          <div v-else class="stock-empty">
            <a-empty description="该商品暂无可用库存" />
          </div>
        </a-spin>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.page-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.page-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: #1a202c;
  letter-spacing: 0.5px;
}

.page-desc {
  margin: 0;
  font-size: 13px;
  color: #718096;
}

.page-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.search-bar {
  margin-bottom: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

.amount-text {
  color: #ff6b35;
  font-weight: 600;
}

.weight-text {
  color: #3491fa;
  font-weight: 600;
}

:deep(.over-credit-row .arco-table-td) {
  background-color: #fff2f0 !important;
}

.contract-modal {
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 8px;
}

.credit-warning-box {
  padding: 12px 16px;
  border-radius: 6px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.credit-warning-box.credit-over {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  color: #f53f3f;
}

.credit-warning-box.credit-ok {
  background: #f2fff7;
  border: 1px solid #d9f7be;
  color: #00b42a;
}

.credit-warn-text {
  font-weight: 500;
}

.credit-ok-text {
  color: #00b42a;
}

.form-section {
  margin-bottom: 24px;
}

.section-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1a202c;
  padding-left: 10px;
  border-left: 3px solid #ff6b35;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-header .section-title {
  margin-bottom: 0;
}

.detail-table-wrapper {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  min-width: 1400px;
}

.detail-table th,
.detail-table td {
  padding: 10px 8px;
  border-bottom: 1px solid #e2e8f0;
  border-right: 1px solid #e2e8f0;
}

.detail-table th:last-child,
.detail-table td:last-child {
  border-right: none;
}

.detail-table thead th {
  background: #f7f8fa;
  font-weight: 600;
  color: #4a5568;
  text-align: center;
}

.detail-table tbody tr:hover {
  background: #f8fafc;
}

.detail-table tbody tr:last-child td {
  border-bottom: none;
}

.detail-table .text-center {
  text-align: center;
}

.detail-table .text-right {
  text-align: right;
}

.detail-table .text-left {
  text-align: left;
}

.detail-table .amount-cell {
  color: #ff6b35;
  font-weight: 500;
}

.detail-table .weight-cell {
  color: #3491fa;
  font-weight: 500;
}

.detail-table tfoot .total-row {
  background: #f7f8fa;
}

.detail-table tfoot td {
  border-top: 2px solid #e2e8f0;
  border-bottom: none;
  font-size: 14px;
}

.lock-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}

.lock-summary {
  font-size: 12px;
  color: #165dff;
  font-weight: 500;
}

.lock-empty {
  font-size: 12px;
  color: #86909c;
}

.lock-detail-text {
  font-size: 11px;
  color: #86909c;
  word-break: break-all;
  line-height: 1.4;
}

:deep(.arco-modal-body) {
  padding: 20px 24px;
}

:deep(.arco-form-item-label) {
  font-weight: 500;
}

:deep(.arco-input-number) {
  width: 100%;
}

:deep(.arco-table) {
  font-size: 13px;
}

.stock-modal {
  max-height: 65vh;
  display: flex;
  flex-direction: column;
}

.stock-search-bar {
  padding: 12px;
  background: #f8fafc;
  border-radius: 6px;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.selected-summary {
  margin-bottom: 12px;
  font-size: 13px;
  color: #4a5568;
  flex-shrink: 0;
}

.selected-qty {
  color: #ff6b35;
  font-weight: 700;
  font-size: 15px;
  margin: 0 4px;
}

.stock-tree-spin {
  flex: 1;
  overflow: auto;
  min-height: 200px;
}

.stock-tree-wrapper {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.stock-tree-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  min-width: 800px;
}

.stock-tree-table th,
.stock-tree-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #e2e8f0;
  border-right: 1px solid #e2e8f0;
}

.stock-tree-table th:last-child,
.stock-tree-table td:last-child {
  border-right: none;
}

.stock-tree-table thead th {
  background: #f7f8fa;
  font-weight: 600;
  color: #4a5568;
  text-align: center;
}

.stock-tree-table .summary-row {
  background: #f0f5ff;
}

.stock-tree-table .summary-row:hover {
  background: #e8f0ff;
}

.stock-tree-table .batch-row:hover {
  background: #fafcff;
}

.stock-tree-table .summary-row:last-child td {
  border-bottom: none;
}

.summary-text {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.summary-text .sub-text {
  font-size: 12px;
  color: #86909c;
}

.batch-info {
  padding-left: 32px;
}

.batch-main {
  margin-bottom: 4px;
}

.batch-furnace {
  font-weight: 500;
  color: #1a202c;
}

.batch-sub {
  font-size: 12px;
  color: #86909c;
  display: flex;
  flex-wrap: wrap;
}

.batch-qty-info {
  margin-bottom: 4px;
}

.avail-qty {
  color: #00b42a;
  font-weight: 600;
}

.batch-qty-input {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.batch-qty-info,
.batch-weight-info {
  font-size: 13px;
}

.batch-weight-info {
  color: #3491fa;
}

.stock-tree-table .text-right {
  text-align: right;
}

.stock-tree-table .qty-cell {
  color: #ff6b35;
  font-weight: 600;
}

.stock-tree-table .weight-cell {
  color: #3491fa;
  font-weight: 600;
}

.stock-empty {
  padding: 40px 0;
}

:deep(.arco-modal-footer) {
  padding-top: 16px;
}
</style>
