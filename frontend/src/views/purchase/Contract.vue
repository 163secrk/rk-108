<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import {
  Message,
  Modal
} from '@arco-design/web-vue'
import {
  IconPlus,
  IconEdit,
  IconDelete,
  IconCheck,
  IconCheckCircle,
  IconSearch,
  IconRefresh,
  IconPlusCircle,
  IconMinusCircle
} from '@arco-design/web-vue/es/icon'
import {
  getContractList,
  getContractDetail,
  generateContractNo,
  addContract,
  updateContract,
  deleteContract,
  auditContract,
  completeContract
} from '@/api/purchase'
import {
  getPartnerList,
  getProductList
} from '@/api/basicData'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const searchForm = reactive({
  status: '',
  contractNo: '',
  supplierId: undefined
})

const tableData = ref([])
const supplierOptions = ref([])
const productOptions = ref([])

const statusOptions = [
  { label: '全部', value: '' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已审核', value: 'AUDITED' },
  { label: '已完成', value: 'COMPLETED' }
]

const statusTagMap = {
  DRAFT: { color: 'gray', text: '草稿' },
  AUDITED: { color: 'blue', text: '已审核' },
  COMPLETED: { color: 'green', text: '已完成' }
}

const modalVisible = ref(false)
const modalTitle = ref('新建采购合同')
const editMode = ref(false)
const formRef = ref(null)

const contractForm = reactive({
  id: undefined,
  contractNo: '',
  supplierId: undefined,
  signDate: '',
  deliveryDate: '',
  status: 'DRAFT',
  totalAmount: 0,
  totalWeight: 0,
  remark: '',
  createBy: undefined,
  items: [],
  deletedItemIds: []
})

const formRules = {
  supplierId: [{ required: true, message: '请选择供应商' }],
  signDate: [{ required: true, message: '请选择签订日期' }]
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
    const res = await getContractList(params)
    if (res.code === 200) {
      let data = res.data
      if (searchForm.contractNo) {
        data = data.filter(item => item.contractNo.includes(searchForm.contractNo))
      }
      if (searchForm.supplierId) {
        data = data.filter(item => item.supplierId === searchForm.supplierId)
      }
      tableData.value = data
      pagination.total = data.length
    }
  } finally {
    loading.value = false
  }
}

async function loadSuppliers() {
  try {
    const res = await getPartnerList({ type: 'SUPPLIER' })
    if (res.code === 200) {
      supplierOptions.value = res.data.map(item => ({
        label: item.name,
        value: item.id
      }))
    }
  } catch (e) {
    console.error('加载供应商失败', e)
    Message.error('加载供应商数据失败')
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

async function handleSearch() {
  pagination.current = 1
  await loadData()
}

async function handleReset() {
  searchForm.status = ''
  searchForm.contractNo = ''
  searchForm.supplierId = undefined
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

async function handleAdd() {
  editMode.value = false
  modalTitle.value = '新建采购合同'
  resetContractForm()
  try {
    const res = await generateContractNo()
    if (res.code === 200) {
      contractForm.contractNo = res.data
    }
  } catch (e) {
    console.error('生成合同编号失败', e)
  }
  contractForm.createBy = userStore.userInfo?.id
  addEmptyItem()
  modalVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

async function handleEdit(record) {
  editMode.value = true
  modalTitle.value = '编辑采购合同'
  try {
    const res = await getContractDetail(record.id)
    if (res.code === 200) {
      const contract = res.data
      contractForm.id = contract.id
      contractForm.contractNo = contract.contractNo
      contractForm.supplierId = contract.supplierId
      contractForm.signDate = contract.signDate
      contractForm.deliveryDate = contract.deliveryDate || ''
      contractForm.status = contract.status
      contractForm.totalAmount = Number(contract.totalAmount) || 0
      contractForm.totalWeight = Number(contract.totalWeight) || 0
      contractForm.remark = contract.remark || ''
      contractForm.createBy = contract.createBy
      contractForm.items = (contract.items || []).map(item => ({
        ...item,
        quantity: Number(item.quantity) || 0,
        unitPrice: Number(item.unitPrice) || 0,
        amount: Number(item.amount) || 0,
        weight: Number(item.weight) || 0,
        weightPerMeter: Number(item.weightPerMeter) || 0,
        length: item.length || ''
      }))
      contractForm.deletedItemIds = []
      modalVisible.value = true
      nextTick(() => {
        formRef.value?.clearValidate()
      })
    }
  } catch (e) {
    console.error('加载合同详情失败', e)
    Message.error('加载合同详情失败')
  }
}

function handleView(record) {
  handleEdit(record)
}

async function handleDelete(record) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除合同「${record.contractNo}」吗？删除后无法恢复。`,
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      try {
        const res = await deleteContract(record.id)
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
    content: `确定要审核合同「${record.contractNo}」吗？审核后将锁定不可修改，并将数量计入在途库存。`,
    okButtonProps: { status: 'success' },
    onOk: async () => {
      try {
        const res = await auditContract(record.id)
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

async function handleComplete(record) {
  Modal.confirm({
    title: '确认完成',
    content: `确定要将合同「${record.contractNo}」标记为完成吗？只有所有明细都已入库完成才能标记。`,
    okButtonProps: { status: 'success' },
    onOk: async () => {
      try {
        const res = await completeContract(record.id)
        if (res.code === 200) {
          Message.success('完成成功')
          await loadData()
        } else {
          Message.error(res.message || '完成失败')
        }
      } catch (e) {
        console.error('完成失败', e)
        Message.error('完成失败')
      }
    }
  })
}

function resetContractForm() {
  contractForm.id = undefined
  contractForm.contractNo = ''
  contractForm.supplierId = undefined
  contractForm.signDate = ''
  contractForm.deliveryDate = ''
  contractForm.status = 'DRAFT'
  contractForm.totalAmount = 0
  contractForm.totalWeight = 0
  contractForm.remark = ''
  contractForm.createBy = undefined
  contractForm.items = []
  contractForm.deletedItemIds = []
}

function addEmptyItem() {
  contractForm.items.push({
    id: undefined,
    contractId: undefined,
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
    inStockQuantity: 0,
    remainingQuantity: 0,
    sortNo: contractForm.items.length + 1,
    createTime: ''
  })
}

function handleAddItem() {
  addEmptyItem()
}

function handleRemoveItem(index) {
  const item = contractForm.items[index]
  if (item && item.id) {
    contractForm.deletedItemIds.push(item.id)
  }
  contractForm.items.splice(index, 1)
  recalculateSortNo()
  recalculateTotals()
}

function recalculateSortNo() {
  contractForm.items.forEach((item, idx) => {
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

  item.remainingQuantity = quantity - (Number(item.inStockQuantity) || 0)

  recalculateTotals()
}

function recalculateTotals() {
  let totalAmount = 0
  let totalWeight = 0
  contractForm.items.forEach(item => {
    totalAmount += Number(item.amount) || 0
    totalWeight += Number(item.weight) || 0
  })
  contractForm.totalAmount = totalAmount
  contractForm.totalWeight = totalWeight
}

function handleQuantityChange(item) {
  calculateRow(item)
}

function handlePriceChange(item) {
  calculateRow(item)
}

async function handleSubmit() {
  submitting.value = true
  try {
    const valid = await formRef.value?.validate().catch(() => false)
    if (valid === false) {
      submitting.value = false
      return
    }

    if (!contractForm.supplierId) {
      Message.error('请选择供应商')
      submitting.value = false
      return
    }
    if (!contractForm.signDate) {
      Message.error('请选择签订日期')
      submitting.value = false
      return
    }

    const validItems = contractForm.items.filter(item => item.productId)
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

    const submitData = {
      ...contractForm,
      items: contractForm.items.filter(item => item.productId)
    }

    let res
    if (editMode.value) {
      res = await updateContract(submitData)
    } else {
      res = await addContract(submitData)
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

watch(() => contractForm.signDate, (newVal) => {
  if (newVal && !contractForm.deliveryDate) {
    const signDate = new Date(newVal)
    signDate.setDate(signDate.getDate() + 30)
    const year = signDate.getFullYear()
    const month = String(signDate.getMonth() + 1).padStart(2, '0')
    const day = String(signDate.getDate()).padStart(2, '0')
    contractForm.deliveryDate = `${year}-${month}-${day}`
  }
})

onMounted(() => {
  loadData()
  loadSuppliers()
  loadProducts()
})
</script>

<template>
  <div class="page-wrapper">
    <div class="page-header">
      <h2 class="page-title">采购合同</h2>
      <p class="page-desc">采购合同的创建、审批、执行与归档管理</p>
    </div>

    <div class="page-body">
      <div class="search-bar">
        <a-form :model="searchForm" layout="inline">
          <a-form-item label="合同状态">
            <a-select
              v-model="searchForm.status"
              placeholder="全部"
              style="width: 150px"
              :options="statusOptions"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="合同编号">
            <a-input
              v-model="searchForm.contractNo"
              placeholder="请输入合同编号"
              style="width: 200px"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="供应商">
            <a-select
              v-model="searchForm.supplierId"
              placeholder="请选择供应商"
              style="width: 200px"
              :options="supplierOptions"
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
          新建合同
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
          <a-table-column title="合同编号" data-index="contractNo" :width="180" fixed="left" />
          <a-table-column title="供应商" data-index="supplierName" min-width="200" />
          <a-table-column title="签订日期" data-index="signDate" :width="120" />
          <a-table-column title="交货日期" data-index="deliveryDate" :width="120" />
          <a-table-column title="合同状态" :width="100">
            <template #cell="{ record }">
              <a-tag :color="statusTagMap[record.status]?.color">
                {{ statusTagMap[record.status]?.text }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="合同金额(元)" :width="140" align="right">
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
          <a-table-column title="操作" :width="240" fixed="right">
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
                status="success"
                @click="handleComplete(record)"
                style="margin-left: 4px"
              >
                <template #icon>
                  <icon-check-circle />
                </template>
                完成
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
      :ok-text="editMode ? '保存修改' : '创建合同'"
      :cancel-text="'取消'"
    >
      <div class="contract-modal">
        <a-form
          ref="formRef"
          :model="contractForm"
          :rules="formRules"
          layout="vertical"
        >
          <div class="form-section">
            <h3 class="section-title">合同基本信息</h3>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="合同编号" field="contractNo">
                  <a-input v-model="contractForm.contractNo" readonly />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="供应商" field="supplierId" required>
                  <a-select
                    v-model="contractForm.supplierId"
                    placeholder="请选择供应商"
                    :options="supplierOptions"
                    :disabled="contractForm.status !== 'DRAFT'"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="签订日期" field="signDate" required>
                  <a-date-picker
                    v-model="contractForm.signDate"
                    placeholder="请选择签订日期"
                    style="width: 100%"
                    :disabled="contractForm.status !== 'DRAFT'"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="交货日期" field="deliveryDate">
                  <a-date-picker
                    v-model="contractForm.deliveryDate"
                    placeholder="请选择交货日期"
                    style="width: 100%"
                    :disabled="contractForm.status !== 'DRAFT'"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="备注">
              <a-textarea
                v-model="contractForm.remark"
                placeholder="请输入备注"
                :rows="2"
                :disabled="contractForm.status !== 'DRAFT'"
              />
            </a-form-item>
          </div>

          <div class="form-section">
            <div class="section-header">
              <h3 class="section-title">合同明细</h3>
              <a-button
                v-if="contractForm.status === 'DRAFT'"
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
                    <th style="width: 280px">商品信息</th>
                    <th style="width: 100px">材质</th>
                    <th style="width: 120px">规格(直径×壁厚)</th>
                    <th style="width: 80px">长度(mm)</th>
                    <th style="width: 100px">每米重量(kg)</th>
                    <th style="width: 100px">数量(支)</th>
                    <th style="width: 100px">单价(元/支)</th>
                    <th style="width: 120px">金额(元)</th>
                    <th style="width: 100px">重量(吨)</th>
                    <th style="width: 100px">已入库</th>
                    <th style="width: 60px" v-if="contractForm.status === 'DRAFT'">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, index) in contractForm.items" :key="index">
                    <td class="text-center">{{ index + 1 }}</td>
                    <td>
                      <a-select
                        v-if="contractForm.status === 'DRAFT'"
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
                        v-if="contractForm.status === 'DRAFT'"
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
                        v-if="contractForm.status === 'DRAFT'"
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
                    <td class="text-right">{{ item.inStockQuantity || 0 }}</td>
                    <td v-if="contractForm.status === 'DRAFT'" class="text-center">
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
                      <strong>{{ Number(contractForm.totalAmount).toFixed(2) }}</strong>
                    </td>
                    <td class="text-right weight-cell">
                      <strong>{{ Number(contractForm.totalWeight).toFixed(3) }}</strong>
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

.contract-modal {
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

.detail-table-wrapper {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  min-width: 1300px;
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
