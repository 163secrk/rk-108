<script setup>
import { ref, computed, onMounted } from 'vue'
import { useBasicDataStore } from '@/stores/basicData'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconPlus,
  IconEdit,
  IconDelete,
  IconSearch,
  IconRefresh,
  IconUserGroup,
  IconInfoCircle
} from '@arco-design/web-vue/es/icon'

const store = useBasicDataStore()

const typeFilter = ref('ALL')
const searchText = ref('')
const submitting = ref(false)

const formRef = ref(null)

const modalVisible = ref(false)
const modalMode = ref('add')
const currentEditingId = ref(null)
const form = ref({
  name: '',
  code: '',
  type: 'SUPPLIER',
  contactPerson: '',
  phone: '',
  creditLimit: 0,
  paymentDays: 0,
  address: '',
  remark: '',
  status: 1
})

const formRules = {
  name: [
    { required: true, message: '请输入单位名称' },
    { maxLength: 100, message: '单位名称不能超过100个字符' }
  ],
  code: [
    { required: true, message: '请输入单位编码' },
    { maxLength: 50, message: '单位编码不能超过50个字符' }
  ],
  type: [
    { required: true, message: '请选择单位类型' }
  ],
  phone: [
    { match: /^[\d\-+\s()]*$/, message: '电话格式不正确' }
  ],
  creditLimit: [
    { validator: (v, cb) => {
      if (v !== null && v !== undefined && v < 0) {
        cb('信用额度不能为负数')
      } else {
        cb()
      }
    }}
  ],
  paymentDays: [
    { validator: (v, cb) => {
      if (v !== null && v !== undefined && v < 0) {
        cb('账期天数不能为负数')
      } else {
        cb()
      }
    }}
  ]
}

const typeOptions = [
  { label: '全部', value: 'ALL' },
  { label: '供应商', value: 'SUPPLIER' },
  { label: '客户', value: 'CUSTOMER' }
]

const typeLabelMap = {
  SUPPLIER: '供应商',
  CUSTOMER: '客户'
}

const typeColorMap = {
  SUPPLIER: 'arcoblue',
  CUSTOMER: 'green'
}

const filteredPartners = computed(() => {
  let list = store.partners
  if (typeFilter.value !== 'ALL') {
    list = list.filter(p => p.type === typeFilter.value)
  }
  if (searchText.value) {
    const keyword = searchText.value.toLowerCase()
    list = list.filter(p =>
      (p.name && p.name.toLowerCase().includes(keyword)) ||
      (p.code && p.code.toLowerCase().includes(keyword))
    )
  }
  return list
})

const summaryByType = computed(() => {
  const base = typeFilter.value === 'ALL' ? store.partners : store.partners.filter(p => p.type === typeFilter.value)
  const total = base.length
  const suppliers = base.filter(p => p.type === 'SUPPLIER').length
  const customers = base.filter(p => p.type === 'CUSTOMER').length
  const totalCredit = base.reduce((sum, p) => sum + (parseFloat(p.creditLimit) || 0), 0)
  return { total, suppliers, customers, totalCredit }
})

function formatCredit(val) {
  const num = parseFloat(val)
  if (isNaN(num) || num === 0) return '¥0.00'
  return '¥' + num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function openAdd() {
  modalMode.value = 'add'
  currentEditingId.value = null
  form.value = {
    name: '',
    code: '',
    type: typeFilter.value === 'CUSTOMER' ? 'CUSTOMER' : 'SUPPLIER',
    contactPerson: '',
    phone: '',
    creditLimit: 0,
    paymentDays: 0,
    address: '',
    remark: '',
    status: 1
  }
  modalVisible.value = true
  setTimeout(() => formRef.value?.clearValidate(), 0)
}

function openEdit(record) {
  modalMode.value = 'edit'
  currentEditingId.value = record.id
  form.value = {
    name: record.name || '',
    code: record.code || '',
    type: record.type || 'SUPPLIER',
    contactPerson: record.contactPerson || '',
    phone: record.phone || '',
    creditLimit: parseFloat(record.creditLimit) || 0,
    paymentDays: record.paymentDays || 0,
    address: record.address || '',
    remark: record.remark || '',
    status: record.status ?? 1
  }
  modalVisible.value = true
  setTimeout(() => formRef.value?.clearValidate(), 0)
}

async function handleSubmit() {
  submitting.value = true
  try {
    const valid = await formRef.value?.validate().catch(() => false)
    if (valid === false) {
      submitting.value = false
      return
    }
    if (modalMode.value === 'add') {
      const result = await store.addPartner(form.value)
      if (result) {
        Message.success('新增往来单位成功')
        modalVisible.value = false
      } else {
        Message.error('新增失败')
      }
    } else {
      const result = await store.updatePartner(currentEditingId.value, form.value)
      if (result) {
        Message.success('更新往来单位成功')
        modalVisible.value = false
      } else {
        Message.error('更新失败')
      }
    }
  } catch (e) {
    Message.error('操作失败：' + (e.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

function handleDelete(record) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除往来单位「${record.name}」吗？删除后不可恢复。`,
    okText: '确认删除',
    cancelText: '取消',
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      const result = await store.deletePartner(record.id)
      if (result) {
        Message.success('删除成功')
      } else {
        Message.error('删除失败')
      }
    }
  })
}

const columns = [
  { title: '编码', dataIndex: 'code', width: 120 },
  { title: '名称', dataIndex: 'name', width: 180 },
  { title: '类型', slotName: 'type', width: 100, align: 'center' },
  { title: '联系人', dataIndex: 'contactPerson', width: 100 },
  { title: '电话', dataIndex: 'phone', width: 140 },
  { title: '信用额度', slotName: 'creditLimit', width: 150, align: 'right' },
  { title: '账期天数', dataIndex: 'paymentDays', width: 100, align: 'center' },
  { title: '状态', slotName: 'status', width: 80, align: 'center' },
  { title: '操作', slotName: 'actions', width: 120, align: 'center', fixed: 'right' }
]

async function loadData() {
  try {
    await store.fetchPartners()
  } catch (e) {
    console.error('加载往来单位数据失败', e)
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="page-wrapper">
    <div class="page-header">
      <h2 class="page-title">往来单位</h2>
      <p class="page-desc">统一管理供应商与客户信息，包含信用额度与账期控制</p>
    </div>

    <div class="page-body">
      <div class="toolbar">
        <div class="toolbar-left">
          <a-radio-group v-model="typeFilter" type="button" size="medium">
            <a-radio v-for="opt in typeOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </a-radio>
          </a-radio-group>
          <a-input
            v-model="searchText"
            placeholder="搜索名称或编码..."
            style="width: 220px"
            allow-clear
          >
            <template #prefix>
              <icon-search />
            </template>
          </a-input>
        </div>
        <div class="toolbar-right">
          <a-button @click="searchText = ''">
            <template #icon><icon-refresh /></template>
            重置
          </a-button>
          <a-button type="primary" @click="openAdd">
            <template #icon><icon-plus /></template>
            新增单位
          </a-button>
        </div>
      </div>

      <a-alert type="info" :show-icon="true" class="credit-alert">
        <template #icon><icon-info-circle /></template>
        <template #content>
          <span class="credit-alert-text">
            信用额度是销售订单的关键校验依据：当客户的未结余额超过信用额度时，系统将在下单时发出预警提示，帮助控制应收风险。
          </span>
        </template>
      </a-alert>

      <a-table
        :data="filteredPartners"
        :columns="columns"
        :pagination="{ pageSize: 10, showTotal: true }"
        :scroll="{ x: 1100 }"
        class="data-table"
        row-key="id"
      >
        <template #type="{ record }">
          <a-tag :color="typeColorMap[record.type]" size="small">
            {{ typeLabelMap[record.type] || record.type }}
          </a-tag>
        </template>
        <template #creditLimit="{ record }">
          <span :class="['credit-value', { 'credit-positive': parseFloat(record.creditLimit) > 0 }]">
            {{ formatCredit(record.creditLimit) }}
          </span>
        </template>
        <template #status="{ record }">
          <a-tag :color="record.status === 1 ? 'green' : 'red'" size="small">
            {{ record.status === 1 ? '启用' : '停用' }}
          </a-tag>
        </template>
        <template #actions="{ record }">
          <a-space>
            <a-button type="text" size="small" @click="openEdit(record)">
              <template #icon><icon-edit /></template>
            </a-button>
            <a-button type="text" size="small" status="danger" @click="handleDelete(record)">
              <template #icon><icon-delete /></template>
            </a-button>
          </a-space>
        </template>
      </a-table>

      <div class="summary-bar" v-if="summaryByType.total > 0">
        <div class="summary-item">
          <span class="summary-label">单位总数：</span>
          <span class="summary-value">{{ summaryByType.total }} 家</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">供应商：</span>
          <span class="summary-value supplier">{{ summaryByType.suppliers }} 家</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">客户：</span>
          <span class="summary-value customer">{{ summaryByType.customers }} 家</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">信用额度合计：</span>
          <span class="summary-value credit">{{ formatCredit(summaryByType.totalCredit) }}</span>
        </div>
      </div>
    </div>

    <a-modal
      v-model:visible="modalVisible"
      :title="modalMode === 'add' ? '新增往来单位' : '编辑往来单位'"
      :ok-loading="submitting"
      @ok="handleSubmit"
      @cancel="modalVisible = false"
      width="620px"
    >
      <a-form ref="formRef" :model="form" :rules="formRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="name" label="单位名称" required>
              <a-input v-model="form.name" placeholder="请输入单位名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="code" label="单位编码" required>
              <a-input v-model="form.code" placeholder="请输入单位编码" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item field="type" label="单位类型" required>
          <a-radio-group v-model="form.type">
            <a-radio value="SUPPLIER">供应商</a-radio>
            <a-radio value="CUSTOMER">客户</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="contactPerson" label="联系人">
              <a-input v-model="form.contactPerson" placeholder="请输入联系人" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="phone" label="电话">
              <a-input v-model="form.phone" placeholder="请输入联系电话" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="creditLimit" label="信用额度（元）">
              <a-input-number
                v-model="form.creditLimit"
                :min="0"
                :step="1000"
                :precision="2"
                style="width: 100%"
                placeholder="0.00"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="paymentDays" label="账期天数">
              <a-input-number
                v-model="form.paymentDays"
                :min="0"
                :step="1"
                :precision="0"
                style="width: 100%"
                placeholder="0"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-alert type="warning" :show-icon="true" class="form-credit-alert">
          <template #content>
            信用额度是销售订单的核心风控参数，当客户未结余额超过此额度时，系统将在销售下单时发出预警提示。
          </template>
        </a-alert>
        <a-form-item field="address" label="地址">
          <a-input v-model="form.address" placeholder="请输入地址" />
        </a-form-item>
        <a-form-item field="remark" label="备注">
          <a-textarea v-model="form.remark" placeholder="请输入备注信息" :max-length="200" show-word-limit />
        </a-form-item>
        <a-form-item field="status" label="状态">
          <a-switch v-model="form.status" :checked-value="1" :unchecked-value="0">
            <template #checked>启用</template>
            <template #unchecked>停用</template>
          </a-switch>
        </a-form-item>
      </a-form>
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
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.page-title {
  margin: 0 0 6px;
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
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.credit-alert {
  margin-bottom: 16px;
}

.credit-alert-text {
  font-size: 13px;
  color: #4a5568;
}

.data-table {
  flex: 1;
}

.credit-value {
  font-weight: 600;
  font-family: 'Roboto Mono', monospace;
  color: #4a5568;
}

.credit-value.credit-positive {
  color: #00b42a;
  font-weight: 700;
}

.summary-bar {
  display: flex;
  gap: 40px;
  padding: 16px 20px;
  background: #f7f9fc;
  border-top: 1px solid #e2e8f0;
  border-radius: 0 0 8px 8px;
  margin-top: 12px;
}

.summary-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.summary-label {
  font-size: 13px;
  color: #718096;
}

.summary-value {
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
}

.summary-value.supplier {
  color: #165dff;
}

.summary-value.customer {
  color: #00b42a;
}

.summary-value.credit {
  color: #00b42a;
  font-size: 18px;
  font-family: 'Roboto Mono', monospace;
}

.form-credit-alert {
  margin-bottom: 16px;
}
</style>
