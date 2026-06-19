<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconPlus,
  IconDelete,
  IconCheck,
  IconSearch,
  IconRefresh,
  IconEye
} from '@arco-design/web-vue/es/icon'
import {
  getStockInOrderList,
  getStockInOrderDetail,
  generateStockInOrderNo,
  addStockInOrder,
  deleteStockInOrder,
  auditStockInOrder,
  getContractDetail,
  getContractList
} from '@/api/purchase'
import { getWarehouseList } from '@/api/basicData'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const searchForm = reactive({
  status: '',
  orderNo: ''
})

const tableData = ref([])
const contractOptions = ref([])
const warehouseOptions = ref([])

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'DRAFT' },
  { label: '已审核', value: 'AUDITED' }
]

const statusTagMap = {
  DRAFT: { color: 'gray', text: '待审核' },
  AUDITED: { color: 'green', text: '已审核' }
}

const modalVisible = ref(false)
const modalTitle = ref('新建入库单')
const viewMode = ref(false)
const formRef = ref(null)

const orderForm = reactive({
  id: undefined,
  orderNo: '',
  contractId: undefined,
  contractNo: '',
  supplierName: '',
  warehouseId: undefined,
  status: 'DRAFT',
  totalQuantity: 0,
  totalTheoreticalWeight: 0,
  totalActualWeight: 0,
  totalAmount: 0,
  remark: '',
  createBy: undefined,
  items: []
})

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
    const res = await getStockInOrderList(params)
    if (res.code === 200) {
      let data = res.data
      if (searchForm.orderNo) {
        data = data.filter(item => item.orderNo.includes(searchForm.orderNo))
      }
      tableData.value = data
      pagination.total = data.length
    }
  } finally {
    loading.value = false
  }
}

async function loadAuditedContracts() {
  try {
    const res = await getContractList({ status: 'AUDITED' })
    if (res.code === 200) {
      contractOptions.value = res.data.map(item => ({
        label: `${item.contractNo} - ${item.supplierName || ''}`,
        value: item.id,
        ...item
      }))
    }
  } catch (e) {
    console.error('加载合同失败', e)
    Message.error('加载采购合同失败')
  }
}

async function loadWarehouses() {
  try {
    const res = await getWarehouseList()
    if (res.code === 200) {
      warehouseOptions.value = res.data.map(item => ({
        label: `${item.name}${item.code ? ' (' + item.code + ')' : ''}`,
        value: item.id
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
  searchForm.orderNo = ''
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

function resetOrderForm() {
  orderForm.id = undefined
  orderForm.orderNo = ''
  orderForm.contractId = undefined
  orderForm.contractNo = ''
  orderForm.supplierName = ''
  orderForm.warehouseId = undefined
  orderForm.status = 'DRAFT'
  orderForm.totalQuantity = 0
  orderForm.totalTheoreticalWeight = 0
  orderForm.totalActualWeight = 0
  orderForm.totalAmount = 0
  orderForm.remark = ''
  orderForm.createBy = undefined
  orderForm.items = []
}

async function handleAdd() {
  viewMode.value = false
  modalTitle.value = '新建入库单'
  resetOrderForm()
  try {
    const res = await generateStockInOrderNo()
    if (res.code === 200) {
      orderForm.orderNo = res.data
    }
  } catch (e) {
    console.error('生成入库单号失败', e)
  }
  orderForm.createBy = userStore.userInfo?.id
  await loadAuditedContracts()
  modalVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

async function handleContractChange(contractId) {
  if (!contractId) {
    orderForm.contractNo = ''
    orderForm.supplierName = ''
    orderForm.items = []
    recalculateTotals()
    return
  }
  try {
    const res = await getContractDetail(contractId)
    if (res.code === 200) {
      const contract = res.data
      orderForm.contractNo = contract.contractNo
      orderForm.supplierName = contract.supplierName
      orderForm.items = (contract.items || [])
        .filter(item => (item.remainingQuantity || 0) > 0)
        .map(item => ({
          contractItemId: item.id,
          productId: item.productId,
          productCode: item.productCode,
          productName: item.productName,
          materialId: item.materialId,
          materialName: item.materialName,
          specId: item.specId,
          diameter: item.diameter,
          wallThickness: item.wallThickness,
          length: item.length,
          weightPerMeter: Number(item.weightPerMeter) || 0,
          contractQuantity: Number(item.quantity) || 0,
          inStockQuantity: Number(item.inStockQuantity) || 0,
          remainingQuantity: Number(item.remainingQuantity) || 0,
          contractWeight: Number(item.weight) || 0,
          quantity: Number(item.remainingQuantity) || 0,
          unitPrice: Number(item.unitPrice) || 0,
          furnaceNo: '',
          actualWeight: 0,
          theoreticalWeight: 0,
          deviationRate: 0,
          amount: 0
        }))
      recalculateTotals()
    }
  } catch (e) {
    console.error('加载合同详情失败', e)
    Message.error('加载合同详情失败')
  }
}

function calculateItem(item) {
  const qty = Number(item.quantity) || 0
  const contractQty = Number(item.contractQuantity) || 0
  const contractWeight = Number(item.contractWeight) || 0
  let theoretical = 0
  if (contractQty > 0 && contractWeight > 0) {
    theoretical = (contractWeight / contractQty) * qty
  }
  item.theoreticalWeight = Number(theoretical.toFixed(3))
  const actual = Number(item.actualWeight) || 0
  if (theoretical > 0 && actual > 0) {
    item.deviationRate = Number((((actual - theoretical) / theoretical) * 100).toFixed(2))
  } else {
    item.deviationRate = 0
  }
  const price = Number(item.unitPrice) || 0
  item.amount = Number((qty * price).toFixed(2))
  recalculateTotals()
}

function recalculateTotals() {
  let totalQty = 0
  let totalTheoretical = 0
  let totalActual = 0
  let totalAmount = 0
  orderForm.items.forEach(item => {
    totalQty += Number(item.quantity) || 0
    totalTheoretical += Number(item.theoreticalWeight) || 0
    totalActual += Number(item.actualWeight) || 0
    totalAmount += Number(item.amount) || 0
  })
  orderForm.totalQuantity = totalQty
  orderForm.totalTheoreticalWeight = Number(totalTheoretical.toFixed(3))
  orderForm.totalActualWeight = Number(totalActual.toFixed(3))
  orderForm.totalAmount = Number(totalAmount.toFixed(2))
}

function handleQuantityChange(item) {
  if (Number(item.quantity) > Number(item.remainingQuantity)) {
    Message.warning(`入库数量不能超过剩余可入库数量（${item.remainingQuantity}）`)
    item.quantity = item.remainingQuantity
  }
  calculateItem(item)
}

function handleActualWeightChange(item) {
  calculateItem(item)
}

function deviationColor(rate) {
  const v = Number(rate) || 0
  if (v > 0) return '#f53f3f'
  if (v < 0) return '#00b42a'
  return '#86909c'
}

async function handleView(record) {
  viewMode.value = true
  modalTitle.value = '入库单详情'
  try {
    const res = await getStockInOrderDetail(record.id)
    if (res.code === 200) {
      const order = res.data
      fillOrderForm(order)
      modalVisible.value = true
    }
  } catch (e) {
    console.error('加载入库单详情失败', e)
    Message.error('加载入库单详情失败')
  }
}

function fillOrderForm(order) {
  orderForm.id = order.id
  orderForm.orderNo = order.orderNo
  orderForm.contractId = order.contractId
  orderForm.contractNo = order.contractNo
  orderForm.supplierName = order.supplierName
  orderForm.warehouseId = order.warehouseId
  orderForm.status = order.status
  orderForm.totalQuantity = Number(order.totalQuantity) || 0
  orderForm.totalTheoreticalWeight = Number(order.totalTheoreticalWeight) || 0
  orderForm.totalActualWeight = Number(order.totalActualWeight) || 0
  orderForm.totalAmount = Number(order.totalAmount) || 0
  orderForm.remark = order.remark || ''
  orderForm.createBy = order.createBy
  orderForm.items = (order.items || []).map(item => ({
    ...item,
    quantity: Number(item.quantity) || 0,
    unitPrice: Number(item.unitPrice) || 0,
    theoreticalWeight: Number(item.theoreticalWeight) || 0,
    actualWeight: Number(item.actualWeight) || 0,
    deviationRate: Number(item.deviationRate) || 0,
    amount: Number(item.amount) || 0,
    contractQuantity: Number(item.contractQuantity) || 0,
    inStockQuantity: Number(item.inStockQuantity) || 0,
    remainingQuantity: Number(item.remainingQuantity) || 0,
    contractWeight: Number(item.contractWeight) || 0
  }))
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (!orderForm.contractId) {
      Message.error('请选择采购合同')
      submitting.value = false
      return
    }
    if (!orderForm.warehouseId) {
      Message.error('请选择入库仓库')
      submitting.value = false
      return
    }

    const validItems = orderForm.items.filter(item => Number(item.quantity) > 0)
    if (validItems.length === 0) {
      Message.error('请至少录入一条入库明细')
      submitting.value = false
      return
    }

    const invalidFurnace = validItems.find(item => !item.furnaceNo || !item.furnaceNo.trim())
    if (invalidFurnace) {
      Message.error('每条入库明细必须录入炉批号')
      submitting.value = false
      return
    }

    const invalidWeight = validItems.find(item => !(Number(item.actualWeight) > 0))
    if (invalidWeight) {
      Message.error('每条入库明细必须录入过磅重量')
      submitting.value = false
      return
    }

    const overQty = validItems.find(item => Number(item.quantity) > Number(item.remainingQuantity))
    if (overQty) {
      Message.error(`商品「${overQty.productName}」入库数量超过合同剩余可入库数量`)
      submitting.value = false
      return
    }

    const submitData = {
      orderNo: orderForm.orderNo,
      contractId: orderForm.contractId,
      warehouseId: orderForm.warehouseId,
      remark: orderForm.remark,
      createBy: orderForm.createBy,
      items: validItems.map(item => ({
        contractItemId: item.contractItemId,
        productId: item.productId,
        materialId: item.materialId,
        specId: item.specId,
        furnaceNo: item.furnaceNo.trim(),
        quantity: Number(item.quantity),
        unitPrice: Number(item.unitPrice),
        actualWeight: Number(item.actualWeight)
      }))
    }

    const res = await addStockInOrder(submitData)
    if (res.code === 200) {
      Message.success('创建成功')
      modalVisible.value = false
      await loadData()
    } else {
      Message.error(res.message || '创建失败')
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

function handleOk() {
  if (viewMode.value) {
    modalVisible.value = false
    return
  }
  handleSubmit()
}

async function handleDelete(record) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除入库单「${record.orderNo}」吗？删除后无法恢复。`,
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      try {
        const res = await deleteStockInOrder(record.id)
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
    content: `确定要审核入库单「${record.orderNo}」吗？审核后将：1) 对应仓库库存增加并按炉批号记录；2) 更新合同入库数量与状态；3) 记录入库成本单价。`,
    okButtonProps: { status: 'success' },
    onOk: async () => {
      try {
        const res = await auditStockInOrder(record.id)
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

onMounted(() => {
  loadData()
  loadWarehouses()
})
</script>

<template>
  <div class="page-wrapper">
    <div class="page-header">
      <h2 class="page-title">采购入库</h2>
      <p class="page-desc">仓库管理员收货后录入入库单，强制录入炉批号用于质量追溯，自动核算过磅重量与理算重量偏差率</p>
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
          <a-form-item label="入库单号">
            <a-input
              v-model="searchForm.orderNo"
              placeholder="请输入入库单号"
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
          新建入库单
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
          <a-table-column title="入库单号" data-index="orderNo" :width="180" fixed="left" />
          <a-table-column title="采购合同" data-index="contractNo" :width="180" />
          <a-table-column title="供应商" data-index="supplierName" min-width="180" />
          <a-table-column title="入库仓库" data-index="warehouseName" :width="140" />
          <a-table-column title="状态" :width="100">
            <template #cell="{ record }">
              <a-tag :color="statusTagMap[record.status]?.color">
                {{ statusTagMap[record.status]?.text }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="入库数量" :width="100" align="right">
            <template #cell="{ record }">
              {{ Number(record.totalQuantity) || 0 }}
            </template>
          </a-table-column>
          <a-table-column title="理算重量(吨)" :width="130" align="right">
            <template #cell="{ record }">
              <span class="weight-text">{{ Number(record.totalTheoreticalWeight).toFixed(3) }}</span>
            </template>
          </a-table-column>
          <a-table-column title="过磅重量(吨)" :width="130" align="right">
            <template #cell="{ record }">
              <span class="amount-text">{{ Number(record.totalActualWeight).toFixed(3) }}</span>
            </template>
          </a-table-column>
          <a-table-column title="金额(元)" :width="130" align="right">
            <template #cell="{ record }">
              {{ Number(record.totalAmount).toFixed(2) }}
            </template>
          </a-table-column>
          <a-table-column title="创建人" data-index="createByName" :width="100" />
          <a-table-column title="创建时间" data-index="createTime" :width="170" />
          <a-table-column title="操作" :width="220" fixed="right">
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
      :ok-button-props="{ status: 'success' }"
      @ok="handleOk"
      @cancel="handleCancel"
      :ok-text="viewMode ? '关闭' : '创建入库单'"
      :cancel-text="viewMode ? '' : '取消'"
      :hide-cancel="viewMode"
    >
      <div class="order-modal">
        <a-form ref="formRef" :model="orderForm" layout="vertical">
          <div class="form-section">
            <h3 class="section-title">入库单基本信息</h3>
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item label="入库单号" field="orderNo">
                  <a-input v-model="orderForm.orderNo" readonly />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="采购合同" field="contractId" required>
                  <a-select
                    v-model="orderForm.contractId"
                    placeholder="请选择已审核的采购合同"
                    :options="contractOptions"
                    :disabled="viewMode"
                    allow-search
                    allow-clear
                    @change="handleContractChange"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="入库仓库" field="warehouseId" required>
                  <a-select
                    v-model="orderForm.warehouseId"
                    placeholder="请选择入库仓库/库位"
                    :options="warehouseOptions"
                    :disabled="viewMode"
                    allow-search
                    allow-clear
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item label="供应商">
                  <a-input v-model="orderForm.supplierName" readonly placeholder="选择合同后自动带入" />
                </a-form-item>
              </a-col>
              <a-col :span="16">
                <a-form-item label="备注">
                  <a-input v-model="orderForm.remark" placeholder="请输入备注" :disabled="viewMode" />
                </a-form-item>
              </a-col>
            </a-row>
          </div>

          <div class="form-section">
            <div class="section-header">
              <h3 class="section-title">入库明细</h3>
              <span class="section-tip">合同明细自动带入，请录入炉批号与过磅重量</span>
            </div>

            <div class="detail-table-wrapper">
              <table class="detail-table">
                <thead>
                  <tr>
                    <th style="width: 50px">序号</th>
                    <th style="width: 180px">商品</th>
                    <th style="width: 90px">材质</th>
                    <th style="width: 120px">规格</th>
                    <th style="width: 90px">合同数量</th>
                    <th style="width: 90px">剩余可入</th>
                    <th style="width: 90px">入库数量</th>
                    <th style="width: 140px">炉批号</th>
                    <th style="width: 110px">单价(元/支)</th>
                    <th style="width: 110px">理算重量(吨)</th>
                    <th style="width: 120px">过磅重量(吨)</th>
                    <th style="width: 100px">偏差率</th>
                    <th style="width: 120px">金额(元)</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, index) in orderForm.items" :key="index">
                    <td class="text-center">{{ index + 1 }}</td>
                    <td>{{ item.productName || '-' }}</td>
                    <td>{{ item.materialName || '-' }}</td>
                    <td>
                      <span v-if="item.diameter && item.wallThickness">
                        φ{{ item.diameter }}×{{ item.wallThickness }}
                      </span>
                      <span v-else>-</span>
                    </td>
                    <td class="text-right">{{ item.contractQuantity || 0 }}</td>
                    <td class="text-right">{{ item.remainingQuantity || 0 }}</td>
                    <td>
                      <a-input-number
                        v-if="!viewMode"
                        v-model="item.quantity"
                        :min="0"
                        :max="item.remainingQuantity"
                        :precision="0"
                        style="width: 100%"
                        @change="() => handleQuantityChange(item)"
                      />
                      <span v-else class="text-right">{{ item.quantity }}</span>
                    </td>
                    <td>
                      <a-input
                        v-if="!viewMode"
                        v-model="item.furnaceNo"
                        placeholder="必填"
                        :max-length="50"
                      />
                      <span v-else>{{ item.furnaceNo }}</span>
                    </td>
                    <td class="text-right">{{ Number(item.unitPrice).toFixed(2) }}</td>
                    <td class="text-right weight-cell">{{ Number(item.theoreticalWeight).toFixed(3) }}</td>
                    <td>
                      <a-input-number
                        v-if="!viewMode"
                        v-model="item.actualWeight"
                        :min="0"
                        :precision="3"
                        style="width: 100%"
                        @change="() => handleActualWeightChange(item)"
                      />
                      <span v-else class="text-right amount-cell">{{ Number(item.actualWeight).toFixed(3) }}</span>
                    </td>
                    <td class="text-right">
                      <span :style="{ color: deviationColor(item.deviationRate), fontWeight: 600 }">
                        {{ Number(item.deviationRate) > 0 ? '+' : '' }}{{ Number(item.deviationRate).toFixed(2) }}%
                      </span>
                    </td>
                    <td class="text-right amount-cell">{{ Number(item.amount).toFixed(2) }}</td>
                  </tr>
                  <tr v-if="orderForm.items.length === 0">
                    <td colspan="13" class="text-center empty-tip">
                      {{ viewMode ? '暂无明细' : '请先选择采购合同，合同明细将自动带入' }}
                    </td>
                  </tr>
                </tbody>
                <tfoot v-if="orderForm.items.length > 0">
                  <tr class="total-row">
                    <td colspan="6" class="text-right"><strong>合计：</strong></td>
                    <td class="text-right"><strong>{{ orderForm.totalQuantity }}</strong></td>
                    <td colspan="2"></td>
                    <td class="text-right weight-cell">
                      <strong>{{ Number(orderForm.totalTheoreticalWeight).toFixed(3) }}</strong>
                    </td>
                    <td class="text-right amount-cell">
                      <strong>{{ Number(orderForm.totalActualWeight).toFixed(3) }}</strong>
                    </td>
                    <td colspan="2"></td>
                    <td class="text-right amount-cell">
                      <strong>{{ Number(orderForm.totalAmount).toFixed(2) }}</strong>
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
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
  min-width: 1500px;
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
