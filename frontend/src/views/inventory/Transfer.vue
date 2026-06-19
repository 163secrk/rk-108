<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconPlus,
  IconDelete,
  IconCheck,
  IconSearch,
  IconRefresh,
  IconEye,
  IconEdit,
  IconDownload
} from '@arco-design/web-vue/es/icon'
import {
  getTransferList,
  getTransferDetail,
  generateTransferNo,
  addTransfer,
  updateTransfer,
  deleteTransfer,
  auditTransfer,
  receiveTransfer
} from '@/api/transfer'
import { queryStockDetail } from '@/api/inventory'
import { getWarehouseList } from '@/api/basicData'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const searchForm = reactive({
  status: '',
  transferNo: ''
})

const tableData = ref([])
const warehouseOptions = ref([])
const fromWarehouseOptions = ref([])
const toWarehouseOptions = ref([])
const stockOptions = ref([])

const statusOptions = [
  { label: '全部', value: '' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已审核(在途)', value: 'AUDITED' },
  { label: '已收货', value: 'RECEIVED' }
]

const statusTagMap = {
  DRAFT: { color: 'gray', text: '草稿' },
  AUDITED: { color: 'blue', text: '已审核(在途)' },
  RECEIVED: { color: 'green', text: '已收货' },
  CANCELLED: { color: 'red', text: '已取消' }
}

const modalVisible = ref(false)
const modalTitle = ref('新建调拨单')
const viewMode = ref(false)
const editMode = ref(false)
const receiveMode = ref(false)
const formRef = ref(null)

const transferForm = reactive({
  id: undefined,
  transferNo: '',
  fromWarehouseId: undefined,
  toWarehouseId: undefined,
  status: 'DRAFT',
  totalQuantity: 0,
  totalWeight: 0,
  receivedQuantity: 0,
  receivedWeight: 0,
  remark: '',
  createBy: undefined,
  items: []
})

const stockSelectVisible = ref(false)
const stockSelectLoading = ref(false)
const selectedStockForEdit = ref(null)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const pageSizeOptions = [10, 20, 50, 100]

async function loadData() {
  loading.value = true
  try {
    const params = { status: searchForm.status }
    const res = await getTransferList(params)
    if (res.code === 200) {
      let data = res.data
      if (searchForm.transferNo) {
        data = data.filter(item => item.transferNo.includes(searchForm.transferNo))
      }
      tableData.value = data
      pagination.total = data.length
    }
  } finally {
    loading.value = false
  }
}

async function loadWarehouses() {
  try {
    const res = await getWarehouseList()
    if (res.code === 200) {
      const level1List = res.data.filter(item => item.level === 1)
      warehouseOptions.value = level1List.map(item => ({
        label: `${item.name}${item.code ? ' (' + item.code + ')' : ''}`,
        value: item.id,
        ...item
      }))
    }
  } catch (e) {
    console.error('加载仓库失败', e)
    Message.error('加载仓库数据失败')
  }
}

async function handleSearch() {
  pagination.current = 1
  await loadData()
}

async function handleReset() {
  searchForm.status = ''
  searchForm.transferNo = ''
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

function resetTransferForm() {
  transferForm.id = undefined
  transferForm.transferNo = ''
  transferForm.fromWarehouseId = undefined
  transferForm.toWarehouseId = undefined
  transferForm.status = 'DRAFT'
  transferForm.totalQuantity = 0
  transferForm.totalWeight = 0
  transferForm.receivedQuantity = 0
  transferForm.receivedWeight = 0
  transferForm.remark = ''
  transferForm.createBy = undefined
  transferForm.items = []
}

async function handleAdd() {
  viewMode.value = false
  editMode.value = false
  receiveMode.value = false
  modalTitle.value = '新建调拨单'
  resetTransferForm()
  try {
    const res = await generateTransferNo()
    if (res.code === 200) {
      transferForm.transferNo = res.data
    }
  } catch (e) {
    console.error('生成调拨单号失败', e)
  }
  transferForm.createBy = userStore.userInfo?.id
  modalVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

async function handleEdit(record) {
  viewMode.value = false
  editMode.value = true
  receiveMode.value = false
  modalTitle.value = '编辑调拨单'
  try {
    const res = await getTransferDetail(record.id)
    if (res.code === 200) {
      const transfer = res.data
      fillTransferForm(transfer)
      modalVisible.value = true
      nextTick(() => {
        formRef.value?.clearValidate()
      })
    }
  } catch (e) {
    console.error('加载调拨单详情失败', e)
    Message.error('加载调拨单详情失败')
  }
}

async function handleView(record) {
  viewMode.value = true
  editMode.value = false
  receiveMode.value = false
  modalTitle.value = '调拨单详情'
  try {
    const res = await getTransferDetail(record.id)
    if (res.code === 200) {
      const transfer = res.data
      fillTransferForm(transfer)
      modalVisible.value = true
    }
  } catch (e) {
    console.error('加载调拨单详情失败', e)
    Message.error('加载调拨单详情失败')
  }
}

function fillTransferForm(transfer) {
  transferForm.id = transfer.id
  transferForm.transferNo = transfer.transferNo
  transferForm.fromWarehouseId = transfer.fromWarehouseId
  transferForm.toWarehouseId = transfer.toWarehouseId
  transferForm.status = transfer.status
  transferForm.totalQuantity = Number(transfer.totalQuantity) || 0
  transferForm.totalWeight = Number(transfer.totalWeight) || 0
  transferForm.receivedQuantity = Number(transfer.receivedQuantity) || 0
  transferForm.receivedWeight = Number(transfer.receivedWeight) || 0
  transferForm.remark = transfer.remark || ''
  transferForm.createBy = transfer.createBy
  transferForm.items = (transfer.items || []).map(item => ({
    ...item,
    planQuantity: Number(item.planQuantity) || 0,
    planWeight: Number(item.planWeight) || 0,
    actualQuantity: Number(item.actualQuantity) || 0,
    actualWeight: Number(item.actualWeight) || 0,
    diffQuantity: Number(item.diffQuantity) || 0,
    diffWeight: Number(item.diffWeight) || 0,
    costUnitPrice: Number(item.costUnitPrice) || 0,
    costAmount: Number(item.costAmount) || 0
  }))
}

async function handleFromWarehouseChange(warehouseId) {
  if (!warehouseId) {
    transferForm.items = []
    recalculateTotals()
    return
  }
  toWarehouseOptions.value = warehouseOptions.value.filter(w => w.value !== warehouseId)
}

async function handleToWarehouseChange() {
}

function openStockSelect(index) {
  if (!transferForm.fromWarehouseId) {
    Message.warning('请先选择调出仓库')
    return
  }
  selectedStockForEdit.value = index
  loadStockList()
  stockSelectVisible.value = true
}

async function loadStockList() {
  if (!transferForm.fromWarehouseId) return
  stockSelectLoading.value = true
  try {
    const res = await queryStockDetail({ warehouseId: transferForm.fromWarehouseId })
    if (res.code === 200) {
      stockOptions.value = res.data.filter(item => Number(item.quantity) > 0).map(item => ({
        ...item,
        label: `${item.productName || ''} - ${item.furnaceNo || ''} - ${item.quantity}支`,
        value: item.id
      }))
    }
  } catch (e) {
    console.error('加载库存列表失败', e)
    Message.error('加载库存列表失败')
  } finally {
    stockSelectLoading.value = false
  }
}

function handleStockSelect(stockId) {
  const stock = stockOptions.value.find(s => s.id === stockId)
  const index = selectedStockForEdit.value
  if (stock && index !== null) {
    if (transferForm.items[index]) {
      transferForm.items[index].stockId = stock.id
      transferForm.items[index].productId = stock.productId
      transferForm.items[index].productName = stock.productName
      transferForm.items[index].materialId = stock.materialId
      transferForm.items[index].materialName = stock.materialName
      transferForm.items[index].specId = stock.specId
      transferForm.items[index].specText = stock.specText
      transferForm.items[index].furnaceNo = stock.furnaceNo
      transferForm.items[index].planQuantity = 0
      transferForm.items[index].planWeight = 0
      transferForm.items[index].costUnitPrice = Number(stock.costUnitPrice) || 0
      transferForm.items[index].costAmount = 0
    }
  }
  stockSelectVisible.value = false
}

function handleAddItem() {
  if (!transferForm.fromWarehouseId) {
    Message.warning('请先选择调出仓库')
    return
  }
  transferForm.items.push({
    id: undefined,
    stockId: undefined,
    productId: undefined,
    productName: '',
    materialId: undefined,
    materialName: '',
    specId: undefined,
    specText: '',
    furnaceNo: '',
    planQuantity: 0,
    planWeight: 0,
    actualQuantity: 0,
    actualWeight: 0,
    diffQuantity: 0,
    diffWeight: 0,
    costUnitPrice: 0,
    costAmount: 0,
    sortNo: transferForm.items.length + 1
  })
}

function handleRemoveItem(index) {
  transferForm.items.splice(index, 1)
  recalculateTotals()
}

function handleQuantityChange(item) {
  const stock = stockOptions.value.find(s => s.id === item.stockId)
  const qty = Number(item.planQuantity) || 0
  if (stock && qty > Number(stock.quantity)) {
    Message.warning(`调拨数量不能超过库存数量（${stock.quantity}）`)
    item.planQuantity = Number(stock.quantity)
  }
  calculateItemWeight(item)
  recalculateTotals()
}

function calculateItemWeight(item) {
  const stock = stockOptions.value.find(s => s.id === item.stockId)
  const qty = Number(item.planQuantity) || 0
  if (stock && stock.quantity > 0 && stock.weight) {
    const unitWeight = Number(stock.weight) / Number(stock.quantity)
    item.planWeight = Number((unitWeight * qty).toFixed(3))
  }
  if (item.costUnitPrice) {
    item.costAmount = Number((item.costUnitPrice * qty).toFixed(2))
  }
}

function recalculateTotals() {
  let totalQty = 0
  let totalWeight = 0
  let totalReceivedQty = 0
  let totalReceivedWeight = 0
  transferForm.items.forEach(item => {
    totalQty += Number(item.planQuantity) || 0
    totalWeight += Number(item.planWeight) || 0
    totalReceivedQty += Number(item.actualQuantity) || 0
    totalReceivedWeight += Number(item.actualWeight) || 0
  })
  transferForm.totalQuantity = totalQty
  transferForm.totalWeight = Number(totalWeight.toFixed(3))
  transferForm.receivedQuantity = totalReceivedQty
  transferForm.receivedWeight = Number(totalReceivedWeight.toFixed(3))
}

function diffColor(diff) {
  const v = Number(diff) || 0
  if (v > 0) return '#00b42a'
  if (v < 0) return '#f53f3f'
  return '#86909c'
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (!transferForm.fromWarehouseId) {
      Message.error('请选择调出仓库')
      return
    }
    if (!transferForm.toWarehouseId) {
      Message.error('请选择目标仓库')
      return
    }
    if (transferForm.fromWarehouseId === transferForm.toWarehouseId) {
      Message.error('调出仓库和目标仓库不能相同')
      return
    }

    const validItems = transferForm.items.filter(item => Number(item.planQuantity) > 0 && item.stockId)
    if (validItems.length === 0) {
      Message.error('请至少添加一条调拨明细')
      return
    }

    const submitData = {
      id: editMode.value ? transferForm.id : undefined,
      transferNo: transferForm.transferNo,
      fromWarehouseId: transferForm.fromWarehouseId,
      toWarehouseId: transferForm.toWarehouseId,
      remark: transferForm.remark,
      createBy: transferForm.createBy,
      items: validItems.map(item => ({
        stockId: item.stockId,
        planQuantity: Number(item.planQuantity)
      }))
    }

    let res
    if (editMode.value) {
      res = await updateTransfer(submitData)
    } else {
      res = await addTransfer(submitData)
    }

    if (res.code === 200) {
      Message.success(editMode.value ? '更新成功' : '创建成功')
      modalVisible.value = false
      await loadData()
    } else {
      Message.error(res.message || (editMode.value ? '更新失败' : '创建失败'))
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
  stockSelectVisible.value = false
}

function handleOk() {
  if (viewMode.value) {
    modalVisible.value = false
    return
  }
  if (receiveMode.value) {
    handleReceiveSubmit()
    return
  }
  handleSubmit()
}

async function handleDelete(record) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除调拨单「${record.transferNo}」吗？删除后无法恢复。`,
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      try {
        const res = await deleteTransfer(record.id)
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

async function handleAudit(record) {
  Modal.confirm({
    title: '确认审核',
    content: `确定要审核调拨单「${record.transferNo}」吗？审核后将：1) 扣减调出仓库库存；2) 货物进入"在途库存"状态，目标仓库库存暂不增加。`,
    okButtonProps: { status: 'success' },
    onOk: async () => {
      try {
        const res = await auditTransfer(record.id)
        if (res.code === 200) {
          Message.success('审核成功')
          await loadData()
        } else {
          Message.error(res.message || '审核失败')
        }
      } catch (e) {
        console.error('审核失败', e)
        Message.error('审核失败')
      }
    }
  })
}

async function handleReceive(record) {
  viewMode.value = false
  editMode.value = false
  receiveMode.value = true
  modalTitle.value = '确认收货'
  try {
    const res = await getTransferDetail(record.id)
    if (res.code === 200) {
      const transfer = res.data
      fillTransferForm(transfer)
      transferForm.items.forEach(item => {
        item.actualQuantity = item.planQuantity
        item.actualWeight = item.planWeight
      })
      recalculateTotals()
      modalVisible.value = true
    }
  } catch (e) {
    console.error('加载调拨单详情失败', e)
    Message.error('加载调拨单详情失败')
  }
}

function handleActualQtyChange(item) {
  const actualQty = Number(item.actualQuantity) || 0
  const planQty = Number(item.planQuantity) || 0
  if (item.planWeight && planQty > 0) {
    const unitWeight = Number(item.planWeight) / planQty
    item.actualWeight = Number((unitWeight * actualQty).toFixed(3))
  }
  item.diffQuantity = actualQty - planQty
  item.diffWeight = Number((item.actualWeight - (item.planWeight || 0)).toFixed(3))
  recalculateTotals()
}

function handleActualWeightChange(item) {
  item.diffWeight = Number(((item.actualWeight || 0) - (item.planWeight || 0)).toFixed(3))
  recalculateTotals()
}

async function handleReceiveSubmit() {
  submitting.value = true
  try {
    const validItems = transferForm.items.filter(item => Number(item.actualQuantity) > 0)
    if (validItems.length === 0) {
      Message.error('请填写实收数量')
      return
    }

    const submitItems = transferForm.items.map(item => ({
      id: item.id,
      actualQuantity: Number(item.actualQuantity) || 0,
      actualWeight: item.actualWeight ? Number(item.actualWeight) : undefined
    }))

    const res = await receiveTransfer(transferForm.id, submitItems)
    if (res.code === 200) {
      Message.success('收货成功')
      modalVisible.value = false
      await loadData()
    } else {
      Message.error(res.message || '收货失败')
    }
  } catch (e) {
    console.error('收货失败', e)
    Message.error('收货失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
  loadWarehouses()
})
</script>

<template>
  <div class="page-wrapper">
    <div class="page-header">
      <h2 class="page-title">库存调拨</h2>
      <p class="page-desc">仓库间调拨采用两步法：调出仓库发起调拨单，审核后扣减库存进入在途状态；目标仓库确认收货后正式入库</p>
    </div>

    <div class="page-body">
      <div class="search-bar">
        <a-form :model="searchForm" layout="inline">
          <a-form-item label="单据状态">
            <a-select
              v-model="searchForm.status"
              placeholder="全部"
              style="width: 150px"
              :options="statusOptions"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="调拨单号">
            <a-input
              v-model="searchForm.transferNo"
              placeholder="请输入调拨单号"
              style="width: 200px"
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
          新建调拨单
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
      >
        <template #columns>
          <a-table-column title="调拨单号" data-index="transferNo" :width="180" fixed="left" />
          <a-table-column title="调出仓库" data-index="fromWarehouseName" :width="140" />
          <a-table-column title="目标仓库" data-index="toWarehouseName" :width="140" />
          <a-table-column title="状态" :width="120">
            <template #cell="{ record }">
              <a-tag :color="statusTagMap[record.status]?.color">
                {{ statusTagMap[record.status]?.text }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="调拨数量" :width="100" align="right">
            <template #cell="{ record }">
              {{ Number(record.totalQuantity) || 0 }}
            </template>
          </a-table-column>
          <a-table-column title="调拨重量(吨)" :width="130" align="right">
            <template #cell="{ record }">
              <span class="weight-text">{{ Number(record.totalWeight).toFixed(3) }}</span>
            </template>
          </a-table-column>
          <a-table-column title="实收数量" :width="100" align="right">
            <template #cell="{ record }">
              {{ Number(record.receivedQuantity) || 0 }}
            </template>
          </a-table-column>
          <a-table-column title="创建人" data-index="createByName" :width="100" />
          <a-table-column title="创建时间" data-index="createTime" :width="170" />
          <a-table-column title="操作" :width="280" fixed="right">
            <template #cell="{ record }">
              <a-button type="outline" size="small" @click="handleView(record)">
                <template #icon>
                  <icon-eye />
                </template>
                查看
              </a-button>
              <a-button
                v-if="record.status === 'DRAFT'"
                type="outline"
                size="small"
                @click="handleEdit(record)"
                style="margin-left: 4px"
              >
                <template #icon>
                  <icon-edit />
                </template>
                编辑
              </a-button>
              <a-button
                v-if="record.status === 'DRAFT'"
                type="outline"
                size="small"
                status="success"
                @click="handleAudit(record)"
                style="margin-left: 4px"
              >
                <template #icon>
                  <icon-check />
                </template>
                审核
              </a-button>
              <a-button
                v-if="record.status === 'AUDITED'"
                type="outline"
                size="small"
                status="primary"
                @click="handleReceive(record)"
                style="margin-left: 4px"
              >
                <template #icon>
                  <icon-download />
                </template>
                收货
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
      width="1280px"
      :ok-loading="submitting"
      :ok-button-props="{ status: receiveMode ? 'primary' : 'success' }"
      @ok="handleOk"
      @cancel="handleCancel"
      :ok-text="viewMode ? '关闭' : (receiveMode ? '确认收货' : (editMode ? '保存修改' : '创建调拨单'))"
      :cancel-text="viewMode ? '' : '取消'"
      :hide-cancel="viewMode"
    >
      <div class="order-modal">
        <a-form ref="formRef" :model="transferForm" layout="vertical">
          <div class="form-section">
            <h3 class="section-title">调拨单基本信息</h3>
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item label="调拨单号" field="transferNo">
                  <a-input v-model="transferForm.transferNo" readonly />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="调出仓库" field="fromWarehouseId" required>
                  <a-select
                    v-model="transferForm.fromWarehouseId"
                    placeholder="请选择调出仓库"
                    :options="warehouseOptions"
                    :disabled="viewMode || receiveMode || transferForm.status !== 'DRAFT'"
                    allow-search
                    allow-clear
                    @change="handleFromWarehouseChange"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="目标仓库" field="toWarehouseId" required>
                  <a-select
                    v-model="transferForm.toWarehouseId"
                    placeholder="请选择目标仓库"
                    :options="toWarehouseOptions"
                    :disabled="viewMode || receiveMode || transferForm.status !== 'DRAFT'"
                    allow-search
                    allow-clear
                    @change="handleToWarehouseChange"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="16">
                <a-form-item label="备注">
                  <a-input
                    v-model="transferForm.remark"
                    placeholder="请输入备注"
                    :disabled="viewMode || receiveMode || transferForm.status !== 'DRAFT'"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8" v-if="receiveMode || transferForm.status === 'RECEIVED'">
                <a-form-item label="实收数量">
                  <a-input :value="transferForm.receivedQuantity" readonly />
                </a-form-item>
              </a-col>
            </a-row>
          </div>

          <div class="form-section">
            <div class="section-header">
              <h3 class="section-title">调拨明细</h3>
              <div>
                <a-button
                  v-if="!viewMode && !receiveMode && transferForm.status === 'DRAFT'"
                  type="primary"
                  size="small"
                  @click="handleAddItem"
                >
                  <template #icon>
                    <icon-plus />
                  </template>
                  添加明细
                </a-button>
              </div>
            </div>

            <div class="detail-table-wrapper">
              <table class="detail-table">
                <thead>
                  <tr>
                    <th style="width: 50px">序号</th>
                    <th style="width: 200px">商品</th>
                    <th style="width: 100px">材质</th>
                    <th style="width: 100px">规格</th>
                    <th style="width: 120px">炉批号</th>
                    <th style="width: 100px">计划数量</th>
                    <th style="width: 120px">计划重量(吨)</th>
                    <th v-if="receiveMode || transferForm.status === 'RECEIVED'" style="width: 100px">实收数量</th>
                    <th v-if="receiveMode || transferForm.status === 'RECEIVED'" style="width: 120px">实收重量(吨)</th>
                    <th v-if="receiveMode || transferForm.status === 'RECEIVED'" style="width: 100px">数量差异</th>
                    <th style="width: 120px">成本单价(元)</th>
                    <th v-if="!viewMode && !receiveMode && transferForm.status === 'DRAFT'" style="width: 80px">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, index) in transferForm.items" :key="index">
                    <td class="text-center">{{ index + 1 }}</td>
                    <td>
                      <a-input
                        v-if="!viewMode && !receiveMode && transferForm.status === 'DRAFT'"
                        :value="item.productName || '点击选择库存'"
                        placeholder="点击选择库存"
                        readonly
                        style="cursor: pointer; width: 100%"
                        @click="openStockSelect(index)"
                      />
                      <span v-else>{{ item.productName || '-' }}</span>
                    </td>
                    <td>{{ item.materialName || '-' }}</td>
                    <td>{{ item.specText || '-' }}</td>
                    <td>{{ item.furnaceNo || '-' }}</td>
                    <td>
                      <a-input-number
                        v-if="!viewMode && !receiveMode && transferForm.status === 'DRAFT'"
                        v-model="item.planQuantity"
                        :min="0"
                        :precision="0"
                        style="width: 100%"
                        @change="() => handleQuantityChange(item)"
                      />
                      <span v-else class="text-right">{{ item.planQuantity }}</span>
                    </td>
                    <td class="text-right weight-cell">{{ Number(item.planWeight).toFixed(3) }}</td>
                    <td v-if="receiveMode || transferForm.status === 'RECEIVED'">
                      <a-input-number
                        v-if="receiveMode"
                        v-model="item.actualQuantity"
                        :min="0"
                        :precision="0"
                        style="width: 100%"
                        @change="() => handleActualQtyChange(item)"
                      />
                      <span v-else class="text-right">{{ item.actualQuantity }}</span>
                    </td>
                    <td v-if="receiveMode || transferForm.status === 'RECEIVED'">
                      <a-input-number
                        v-if="receiveMode"
                        v-model="item.actualWeight"
                        :min="0"
                        :precision="3"
                        style="width: 100%"
                        @change="() => handleActualWeightChange(item)"
                      />
                      <span v-else class="text-right">{{ Number(item.actualWeight).toFixed(3) }}</span>
                    </td>
                    <td v-if="receiveMode || transferForm.status === 'RECEIVED'" class="text-right">
                      <span :style="{ color: diffColor(item.diffQuantity), fontWeight: 600 }">
                        {{ Number(item.diffQuantity) > 0 ? '+' : '' }}{{ item.diffQuantity }}
                      </span>
                    </td>
                    <td class="text-right">{{ Number(item.costUnitPrice).toFixed(2) }}</td>
                    <td v-if="!viewMode && !receiveMode && transferForm.status === 'DRAFT'" class="text-center">
                      <a-button
                        type="text"
                        size="small"
                        status="danger"
                        @click="handleRemoveItem(index)"
                      >
                        <template #icon>
                          <icon-delete />
                        </template>
                      </a-button>
                    </td>
                  </tr>
                  <tr v-if="transferForm.items.length === 0">
                    <td :colspan="viewMode || transferForm.status !== 'DRAFT' ? 11 : 12" class="text-center empty-tip">
                      {{ viewMode ? '暂无明细' : '请先选择调出仓库，然后点击"添加明细"选择要调拨的库存' }}
                    </td>
                  </tr>
                </tbody>
                <tfoot v-if="transferForm.items.length > 0">
                  <tr class="total-row">
                    <td colspan="5" class="text-right"><strong>合计：</strong></td>
                    <td class="text-right"><strong>{{ transferForm.totalQuantity }}</strong></td>
                    <td class="text-right weight-cell">
                      <strong>{{ Number(transferForm.totalWeight).toFixed(3) }}</strong>
                    </td>
                    <td v-if="receiveMode || transferForm.status === 'RECEIVED'" class="text-right">
                      <strong>{{ transferForm.receivedQuantity }}</strong>
                    </td>
                    <td v-if="receiveMode || transferForm.status === 'RECEIVED'" class="text-right">
                      <strong>{{ Number(transferForm.receivedWeight).toFixed(3) }}</strong>
                    </td>
                    <td v-if="receiveMode || transferForm.status === 'RECEIVED'" class="text-right"></td>
                    <td></td>
                    <td v-if="!viewMode && !receiveMode && transferForm.status === 'DRAFT'"></td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        </a-form>
      </div>
    </a-modal>

    <a-modal
      v-model:visible="stockSelectVisible"
      title="选择库存批次"
      :mask-closable="false"
      width="1000px"
      :footer="false"
    >
      <div class="stock-select-modal">
        <div class="stock-select-header">
          <span class="stock-select-tip">请选择要调拨的库存批次（按炉批号）</span>
        </div>
        <a-table
          :loading="stockSelectLoading"
          :data="stockOptions"
          :pagination="false"
          :bordered="false"
          stripe
          size="small"
          @row-click="(record) => handleStockSelect(record.id)"
          :row-style="{ cursor: 'pointer' }"
        >
          <template #columns>
            <a-table-column title="商品名称" data-index="productName" :width="200" />
            <a-table-column title="材质" data-index="materialName" :width="100" />
            <a-table-column title="规格" data-index="specText" :width="120" />
            <a-table-column title="炉批号" data-index="furnaceNo" :width="140" />
            <a-table-column title="库存数量" data-index="quantity" :width="100" align="right" />
            <a-table-column title="库存重量(吨)" :width="120" align="right">
              <template #cell="{ record }">
                {{ Number(record.weight).toFixed(3) }}
              </template>
            </a-table-column>
            <a-table-column title="成本单价(元)" :width="120" align="right">
              <template #cell="{ record }">
                {{ Number(record.costUnitPrice).toFixed(2) }}
              </template>
            </a-table-column>
          </template>
          <template #empty>
            <a-empty description="暂无可用库存" />
          </template>
        </a-table>
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

.order-modal {
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 8px;
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

.section-tip {
  font-size: 12px;
  color: #f53f3f;
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
  min-width: 1200px;
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

.detail-table .amount-cell {
  color: #ff6b35;
  font-weight: 500;
}

.detail-table .weight-cell {
  color: #3491fa;
  font-weight: 500;
}

.detail-table .empty-tip {
  text-align: center;
  color: #c9cdd4;
  padding: 24px 8px;
}

.detail-table tfoot .total-row {
  background: #f7f8fa;
}

.detail-table tfoot td {
  border-top: 2px solid #e2e8f0;
  border-bottom: none;
  font-size: 14px;
}

.stock-select-modal {
  max-height: 60vh;
  overflow-y: auto;
}

.stock-select-header {
  margin-bottom: 12px;
  padding: 8px 0;
}

.stock-select-tip {
  font-size: 13px;
  color: #718096;
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
</style>
