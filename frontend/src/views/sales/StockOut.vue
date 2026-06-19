<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import {
  Message,
  Modal
} from '@arco-design/web-vue'
import {
  IconPlus,
  IconEdit,
  IconDelete,
  IconCheck,
  IconSearch,
  IconRefresh,
  IconFile,
  IconEye
} from '@arco-design/web-vue/es/icon'
import {
  getSalesOutboundList,
  getSalesOutboundDetail,
  generateSalesOutboundNo,
  generateOutboundFromOrder,
  addSalesOutbound,
  updateSalesOutbound,
  deleteSalesOutbound,
  auditSalesOutbound,
  getSalesOrderList
} from '@/api/sales'
import { getPartnerList } from '@/api/basicData'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const auditing = ref(false)
const searchForm = reactive({
  status: '',
  outboundNo: '',
  orderNo: '',
  customerId: undefined
})

const tableData = ref([])
const customerOptions = ref([])
const confirmedOrders = ref([])

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'DRAFT' },
  { label: '已审核', value: 'AUDITED' }
]

const statusTagMap = {
  DRAFT: { color: 'orange', text: '待审核' },
  AUDITED: { color: 'green', text: '已审核' }
}

const modalVisible = ref(false)
const modalTitle = ref('新建出库单')
const editMode = ref(false)
const formRef = ref(null)

const orderSelectVisible = ref(false)

const outboundForm = reactive({
  id: undefined,
  outboundNo: '',
  orderId: undefined,
  orderNo: '',
  customerId: undefined,
  customerName: '',
  status: 'DRAFT',
  totalQuantity: 0,
  totalWeight: 0,
  plateNo: '',
  driverName: '',
  driverPhone: '',
  remark: '',
  createBy: undefined,
  items: [],
  deletedItemIds: []
})

const formRules = {
  orderId: [{ required: true, message: '请选择销售订单' }],
  plateNo: [{ required: true, message: '请输入车牌号' }],
  driverName: [{ required: true, message: '请输入司机姓名' }],
  driverPhone: [{ required: true, message: '请输入联系电话' }]
}

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const pageSizeOptions = [10, 20, 50, 100]

async function loadData() {
  loading.value = true
  try {
    const params = {
      status: searchForm.status
    }
    const res = await getSalesOutboundList(params)
    if (res.code === 200) {
      let data = res.data || []
      if (searchForm.outboundNo) {
        data = data.filter(item =>
          item.outboundNo && item.outboundNo.includes(searchForm.outboundNo)
        )
      }
      if (searchForm.orderNo) {
        data = data.filter(item =>
          item.orderNo && item.orderNo.includes(searchForm.orderNo)
        )
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
        value: item.id
      }))
    }
  } catch (e) {
    console.error('加载客户失败', e)
    Message.error('加载客户数据失败')
  }
}

async function loadConfirmedOrders() {
  try {
    const res = await getSalesOrderList({ status: 'CONFIRMED' })
    if (res.code === 200) {
      confirmedOrders.value = res.data || []
    }
  } catch (e) {
    console.error('加载销售订单失败', e)
    Message.error('加载销售订单失败')
  }
}

async function handleSearch() {
  pagination.current = 1
  await loadData()
}

async function handleReset() {
  searchForm.status = ''
  searchForm.outboundNo = ''
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

function resetOutboundForm() {
  outboundForm.id = undefined
  outboundForm.outboundNo = ''
  outboundForm.orderId = undefined
  outboundForm.orderNo = ''
  outboundForm.customerId = undefined
  outboundForm.customerName = ''
  outboundForm.status = 'DRAFT'
  outboundForm.totalQuantity = 0
  outboundForm.totalWeight = 0
  outboundForm.plateNo = ''
  outboundForm.driverName = ''
  outboundForm.driverPhone = ''
  outboundForm.remark = ''
  outboundForm.createBy = undefined
  outboundForm.items = []
  outboundForm.deletedItemIds = []
}

async function handleAdd() {
  editMode.value = false
  modalTitle.value = '新建出库单'
  resetOutboundForm()
  await loadConfirmedOrders()
  try {
    const res = await generateSalesOutboundNo()
    if (res.code === 200) {
      outboundForm.outboundNo = res.data
    }
  } catch (e) {
    console.error('生成出库单号失败', e)
  }
  outboundForm.createBy = userStore.userInfo?.id
  modalVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

async function handleEdit(record) {
  editMode.value = true
  modalTitle.value = '编辑出库单'
  try {
    const res = await getSalesOutboundDetail(record.id)
    if (res.code === 200) {
      const outbound = res.data
      outboundForm.id = outbound.id
      outboundForm.outboundNo = outbound.outboundNo
      outboundForm.orderId = outbound.orderId
      outboundForm.orderNo = outbound.orderNo
      outboundForm.customerId = outbound.customerId
      outboundForm.customerName = outbound.customerName
      outboundForm.status = outbound.status
      outboundForm.totalQuantity = Number(outbound.totalQuantity) || 0
      outboundForm.totalWeight = Number(outbound.totalWeight) || 0
      outboundForm.plateNo = outbound.plateNo || ''
      outboundForm.driverName = outbound.driverName || ''
      outboundForm.driverPhone = outbound.driverPhone || ''
      outboundForm.remark = outbound.remark || ''
      outboundForm.createBy = outbound.createBy
      outboundForm.items = (outbound.items || []).map(item => ({
        ...item,
        planQuantity: Number(item.planQuantity) || 0,
        actualQuantity: Number(item.actualQuantity) || 0,
        planWeight: Number(item.planWeight) || 0,
        actualWeight: Number(item.actualWeight) || 0,
        unitPrice: Number(item.unitPrice) || 0,
        amount: Number(item.amount) || 0
      }))
      outboundForm.deletedItemIds = []
      modalVisible.value = true
      nextTick(() => {
        formRef.value?.clearValidate()
      })
    }
  } catch (e) {
    console.error('加载出库单详情失败', e)
    Message.error('加载出库单详情失败')
  }
}

function handleView(record) {
  handleEdit(record)
}

async function handleDelete(record) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除出库单「${record.outboundNo}」吗？删除后无法恢复。`,
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      try {
        const res = await deleteSalesOutbound(record.id)
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
    title: '审核出库单',
    content: `确定要审核出库单「${record.outboundNo}」吗？审核后将释放锁定库存、扣减实际库存，且不能再修改。`,
    okButtonProps: { status: 'success' },
    onOk: async () => {
      auditing.value = true
      try {
        const res = await auditSalesOutbound(record.id)
        if (res.code === 200) {
          Message.success('审核成功')
          await loadData()
        } else {
          Message.error(res.message || '审核失败')
        }
      } catch (e) {
        console.error('审核失败', e)
        Message.error('审核失败')
      } finally {
        auditing.value = false
      }
    }
  })
}

async function handleOrderSelect(orderId) {
  if (!orderId) {
    outboundForm.orderId = undefined
    outboundForm.orderNo = ''
    outboundForm.customerId = undefined
    outboundForm.customerName = ''
    outboundForm.items = []
    recalculateTotals()
    return
  }
  try {
    const res = await generateOutboundFromOrder(orderId)
    if (res.code === 200) {
      const data = res.data
      outboundForm.orderId = orderId
      outboundForm.orderNo = data.orderNo || ''
      outboundForm.customerId = data.customerId
      outboundForm.customerName = data.customerName || ''
      outboundForm.totalQuantity = Number(data.totalQuantity) || 0
      outboundForm.totalWeight = Number(data.totalWeight) || 0
      outboundForm.items = (data.items || []).map((item, idx) => ({
        ...item,
        id: undefined,
        outboundId: undefined,
        planQuantity: Number(item.planQuantity) || 0,
        actualQuantity: Number(item.actualQuantity) || 0,
        planWeight: Number(item.planWeight) || 0,
        actualWeight: Number(item.actualWeight) || 0,
        unitPrice: Number(item.unitPrice) || 0,
        amount: Number(item.amount) || 0,
        sortNo: idx + 1
      }))
    }
  } catch (e) {
    console.error('加载订单出库数据失败', e)
    Message.error('加载订单出库数据失败')
  }
}

function handleActualQtyChange(item) {
  const planQty = Number(item.planQuantity) || 0
  let actualQty = Number(item.actualQuantity) || 0
  if (actualQty > planQty) {
    actualQty = planQty
    item.actualQuantity = actualQty
    Message.warning('实发数量不能超过锁定数量')
  }
  if (actualQty < 0) {
    actualQty = 0
    item.actualQuantity = 0
  }
  if (item.planWeight && planQty > 0) {
    const planWeight = Number(item.planWeight) || 0
    const perQtyWeight = planWeight / planQty
    item.actualWeight = Number((perQtyWeight * actualQty).toFixed(3))
  }
  calculateRow(item)
}

function handleActualWeightChange(item) {
  calculateRow(item)
}

function calculateRow(item) {
  const actualQty = Number(item.actualQuantity) || 0
  const unitPrice = Number(item.unitPrice) || 0
  item.amount = Number((actualQty * unitPrice).toFixed(2))
  recalculateTotals()
}

function recalculateTotals() {
  let totalQty = 0
  let totalWeight = 0
  outboundForm.items.forEach(item => {
    totalQty += Number(item.actualQuantity) || 0
    totalWeight += Number(item.actualWeight) || 0
  })
  outboundForm.totalQuantity = totalQty
  outboundForm.totalWeight = Number(totalWeight.toFixed(3))
}

function formatSpecText(item) {
  if (item.diameter && item.wallThickness) {
    return `φ${item.diameter}×${item.wallThickness}`
  }
  return '-'
}

async function handleSubmit() {
  submitting.value = true
  try {
    const valid = await formRef.value?.validate().catch(() => false)
    if (valid === false) {
      submitting.value = false
      return
    }

    if (!outboundForm.orderId) {
      Message.error('请选择销售订单')
      submitting.value = false
      return
    }
    if (!outboundForm.plateNo) {
      Message.error('请输入车牌号')
      submitting.value = false
      return
    }
    if (!outboundForm.driverName) {
      Message.error('请输入司机姓名')
      submitting.value = false
      return
    }
    if (!outboundForm.driverPhone) {
      Message.error('请输入联系电话')
      submitting.value = false
      return
    }

    const validItems = outboundForm.items.filter(item =>
      Number(item.actualQuantity) > 0
    )
    if (validItems.length === 0) {
      Message.error('请至少保留一条有数量的出库明细')
      submitting.value = false
      return
    }

    const submitData = {
      ...outboundForm,
      items: validItems
    }

    let res
    if (editMode.value) {
      res = await updateSalesOutbound(submitData)
    } else {
      res = await addSalesOutbound(submitData)
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
})
</script>

<template>
  <div class="page-wrapper">
    <div class="page-header">
      <h2 class="page-title">销售出库</h2>
      <p class="page-desc">基于销售订单的出库管理，库存锁定释放与实际扣减</p>
    </div>

    <div class="page-body">
      <div class="search-bar">
        <a-form :model="searchForm" layout="inline">
          <a-form-item label="出库状态">
            <a-select
              v-model="searchForm.status"
              placeholder="全部"
              style="width: 150px"
              :options="statusOptions"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="出库单号">
            <a-input
              v-model="searchForm.outboundNo"
              placeholder="请输入出库单号"
              style="width: 180px"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="订单号">
            <a-input
              v-model="searchForm.orderNo"
              placeholder="请输入订单号"
              style="width: 180px"
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
          新建出库单
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
          <a-table-column title="出库单号" data-index="outboundNo" :width="180" fixed="left">
            <template #cell="{ record }">
              <span class="outbound-no">{{ record.outboundNo }}</span>
            </template>
          </a-table-column>
          <a-table-column title="关联订单" data-index="orderNo" :width="180">
            <template #cell="{ record }">
              <span class="order-no-link">
                <icon-file style="margin-right: 4px; font-size: 12px" />
                {{ record.orderNo || '-' }}
              </span>
            </template>
          </a-table-column>
          <a-table-column title="客户" data-index="customerName" min-width="180" />
          <a-table-column title="状态" :width="100">
            <template #cell="{ record }">
              <a-tag :color="statusTagMap[record.status]?.color">
                {{ statusTagMap[record.status]?.text }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="总数量(支)" :width="110" align="right">
            <template #cell="{ record }">
              <span class="qty-text">{{ record.totalQuantity || 0 }}</span>
            </template>
          </a-table-column>
          <a-table-column title="总吨位(吨)" :width="110" align="right">
            <template #cell="{ record }">
              <span class="weight-text">{{ Number(record.totalWeight).toFixed(3) }}</span>
            </template>
          </a-table-column>
          <a-table-column title="车牌号" data-index="plateNo" :width="110" />
          <a-table-column title="司机" data-index="driverName" :width="100" />
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
                status="primary"
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
                :loading="auditing"
                style="margin-left: 4px"
              >
                <template #icon>
                  <icon-check />
                </template>
                审核
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
      width="1200px"
      :ok-loading="submitting"
      @ok="handleSubmit"
      @cancel="handleCancel"
      :ok-text="editMode ? '保存修改' : '创建出库单'"
      :cancel-text="'取消'"
      :footer="outboundForm.status === 'AUDITED' ? false : undefined"
    >
      <div class="outbound-modal">
        <a-form
          ref="formRef"
          :model="outboundForm"
          :rules="formRules"
          layout="vertical"
        >
          <div class="form-section">
            <h3 class="section-title">基本信息</h3>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="出库单号" field="outboundNo">
                  <a-input v-model="outboundForm.outboundNo" readonly />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="销售订单" field="orderId" required>
                  <a-select
                    v-model="outboundForm.orderId"
                    placeholder="请选择已确认的销售订单"
                    :options="confirmedOrders.map(o => ({
                      label: o.orderNo + ' - ' + (o.customerName || ''),
                      value: o.id
                    }))"
                    :disabled="outboundForm.status !== 'DRAFT'"
                    allow-clear
                    @change="handleOrderSelect"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item label="客户名称">
                  <a-input :value="outboundForm.customerName" readonly />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="出库状态">
                  <a-tag :color="statusTagMap[outboundForm.status]?.color">
                    {{ statusTagMap[outboundForm.status]?.text }}
                  </a-tag>
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="总数量(支)">
                  <a-input-number
                    :value="outboundForm.totalQuantity"
                    readonly
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </div>

          <div class="form-section">
            <h3 class="section-title">物流信息</h3>
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item label="车牌号" field="plateNo">
                  <a-input
                    v-model="outboundForm.plateNo"
                    placeholder="请输入车牌号"
                    :disabled="outboundForm.status !== 'DRAFT'"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="司机姓名" field="driverName">
                  <a-input
                    v-model="outboundForm.driverName"
                    placeholder="请输入司机姓名"
                    :disabled="outboundForm.status !== 'DRAFT'"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="联系电话" field="driverPhone">
                  <a-input
                    v-model="outboundForm.driverPhone"
                    placeholder="请输入联系电话"
                    :disabled="outboundForm.status !== 'DRAFT'"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </div>

          <div class="form-section">
            <h3 class="section-title">出库明细</h3>
            <div class="detail-table-wrapper">
              <table class="detail-table">
                <thead>
                  <tr>
                    <th style="width: 50px">序号</th>
                    <th style="width: 180px">商品信息</th>
                    <th style="width: 70px">材质</th>
                    <th style="width: 110px">规格</th>
                    <th style="width: 100px">仓库</th>
                    <th style="width: 110px">炉批号</th>
                    <th style="width: 90px">计划数量(支)</th>
                    <th style="width: 90px">实发数量(支)</th>
                    <th style="width: 90px">计划重量(吨)</th>
                    <th style="width: 90px">实发重量(吨)</th>
                    <th style="width: 100px">单价(元/支)</th>
                    <th style="width: 110px">金额(元)</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, index) in outboundForm.items" :key="index">
                    <td class="text-center">{{ index + 1 }}</td>
                    <td>{{ item.productName || '-' }}</td>
                    <td>{{ item.materialName || '-' }}</td>
                    <td>{{ formatSpecText(item) }}</td>
                    <td>{{ item.warehouseName || '-' }}</td>
                    <td>{{ item.furnaceNo || '-' }}</td>
                    <td class="text-right plan-cell">{{ item.planQuantity || 0 }}</td>
                    <td>
                      <a-input-number
                        v-if="outboundForm.status === 'DRAFT'"
                        v-model="item.actualQuantity"
                        :min="0"
                        :max="item.planQuantity"
                        :precision="0"
                        style="width: 100%"
                        @change="() => handleActualQtyChange(item)"
                      />
                      <span v-else class="text-right actual-cell">{{ item.actualQuantity || 0 }}</span>
                    </td>
                    <td class="text-right weight-cell">{{ Number(item.planWeight).toFixed(3) }}</td>
                    <td>
                      <a-input-number
                        v-if="outboundForm.status === 'DRAFT'"
                        v-model="item.actualWeight"
                        :min="0"
                        :precision="3"
                        :step="0.001"
                        style="width: 100%"
                        @change="() => handleActualWeightChange(item)"
                      />
                      <span v-else class="text-right weight-cell">{{ Number(item.actualWeight).toFixed(3) }}</span>
                    </td>
                    <td class="text-right">{{ Number(item.unitPrice).toFixed(2) }}</td>
                    <td class="text-right amount-cell">{{ Number(item.amount).toFixed(2) }}</td>
                  </tr>
                  <tr v-if="outboundForm.items.length === 0">
                    <td colspan="12" class="empty-row">
                      <a-empty description="暂无明细，请先选择销售订单" />
                    </td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr class="total-row">
                    <td colspan="6" class="text-right"><strong>合计：</strong></td>
                    <td class="text-right"><strong>{{ outboundForm.items.reduce((sum, i) => sum + (Number(i.planQuantity) || 0), 0) }}</strong></td>
                    <td class="text-right"><strong>{{ outboundForm.totalQuantity }}</strong></td>
                    <td class="text-right weight-cell">
                      <strong>{{ Number(outboundForm.items.reduce((sum, i) => sum + (Number(i.planWeight) || 0), 0)).toFixed(3) }}</strong>
                    </td>
                    <td class="text-right weight-cell">
                      <strong>{{ Number(outboundForm.totalWeight).toFixed(3) }}</strong>
                    </td>
                    <td></td>
                    <td class="text-right amount-cell">
                      <strong>{{ Number(outboundForm.items.reduce((sum, i) => sum + (Number(i.amount) || 0), 0)).toFixed(2) }}</strong>
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>

          <div class="form-section">
            <a-form-item label="备注">
              <a-textarea
                v-model="outboundForm.remark"
                placeholder="请输入备注"
                :rows="2"
                :disabled="outboundForm.status !== 'DRAFT'"
              />
            </a-form-item>
          </div>
        </a-form>
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

.outbound-no {
  color: #165dff;
  font-weight: 500;
}

.order-no-link {
  color: #165dff;
  font-size: 12px;
  display: flex;
  align-items: center;
}

.qty-text {
  color: #ff6b35;
  font-weight: 600;
}

.weight-text {
  color: #3491fa;
  font-weight: 600;
}

.outbound-modal {
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

.detail-table .text-left {
  text-align: left;
}

.detail-table .plan-cell {
  color: #86909c;
}

.detail-table .actual-cell {
  color: #ff6b35;
  font-weight: 500;
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

.detail-table .empty-row {
  padding: 40px 0;
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
