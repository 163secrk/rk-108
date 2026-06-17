<script setup>
import { ref, computed } from 'vue'
import { useBasicDataStore } from '@/stores/basicData'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconPlus,
  IconEdit,
  IconDelete,
  IconSearch,
  IconRefresh,
  IconTag,
  IconList,
  IconApps
} from '@arco-design/web-vue/es/icon'

const store = useBasicDataStore()

const activeTab = ref('spec')

const materialModalVisible = ref(false)
const materialModalMode = ref('add')
const materialForm = ref({
  name: '',
  description: '',
  standard: ''
})

const specModalVisible = ref(false)
const specModalMode = ref('add')
const specForm = ref({
  materialId: null,
  diameter: '',
  wallThickness: '',
  length: '',
  weightPerMeter: 0
})

const productModalVisible = ref(false)
const productModalMode = ref('add')
const productForm = ref({
  productCode: '',
  productName: '',
  materialId: null,
  specId: null,
  quantity: 0,
  unit: '支',
  unitPrice: 0
})

const materialSearch = ref('')
const specSearch = ref('')
const productSearch = ref('')

const currentEditingMaterialId = ref(null)
const currentEditingSpecId = ref(null)
const currentEditingProductId = ref(null)

const filteredMaterials = computed(() => {
  if (!materialSearch.value) return store.materials
  return store.materials.filter(m =>
    m.name.toLowerCase().includes(materialSearch.value.toLowerCase()) ||
    m.description.toLowerCase().includes(materialSearch.value.toLowerCase())
  )
})

const filteredSpecs = computed(() => {
  let list = store.filteredSpecs
  if (specSearch.value) {
    list = list.filter(s =>
      s.diameter.includes(specSearch.value) ||
      s.materialName.toLowerCase().includes(specSearch.value.toLowerCase())
    )
  }
  return list
})

const filteredProducts = computed(() => {
  let list = store.filteredProducts
  if (productSearch.value) {
    list = list.filter(p =>
      p.productName.toLowerCase().includes(productSearch.value.toLowerCase()) ||
      p.productCode.toLowerCase().includes(productSearch.value.toLowerCase())
    )
  }
  return list
})

const selectedMaterialName = computed(() => {
  if (!store.selectedMaterialId) return '全部材质'
  const m = store.getMaterialById(store.selectedMaterialId)
  return m ? m.name : '全部材质'
})

const specOptionsForProduct = computed(() => {
  if (!productForm.value.materialId) return []
  return store.getSpecsByMaterialId(productForm.value.materialId).map(s => ({
    value: s.id,
    label: `φ${s.diameter}×${s.wallThickness} (${parseFloat(s.length)/1000}m, ${s.weightPerMeter}kg/m)`
  }))
})

function handleSelectMaterial(id) {
  store.selectMaterial(store.selectedMaterialId === id ? null : id)
}

function openAddMaterial() {
  materialModalMode.value = 'add'
  materialForm.value = { name: '', description: '', standard: '' }
  materialModalVisible.value = true
}

function openEditMaterial(item) {
  materialModalMode.value = 'edit'
  currentEditingMaterialId.value = item.id
  materialForm.value = {
    name: item.name,
    description: item.description,
    standard: item.standard
  }
  materialModalVisible.value = true
}

function handleMaterialSubmit() {
  if (!materialForm.value.name.trim()) {
    Message.warning('请输入材质名称')
    return
  }
  if (materialModalMode.value === 'add') {
    store.addMaterial({ ...materialForm.value })
    Message.success('添加成功')
  } else {
    store.updateMaterial(currentEditingMaterialId.value, { ...materialForm.value })
    Message.success('修改成功')
  }
  materialModalVisible.value = false
}

function handleDeleteMaterial(item) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除材质「${item.name}」吗？删除后相关的规格和商品也会被移除。`,
    okButtonProps: { status: 'danger' },
    onOk: () => {
      store.deleteMaterial(item.id)
      Message.success('删除成功')
    }
  })
}

function openAddSpec() {
  if (!store.selectedMaterialId) {
    Message.warning('请先在左侧选择一个材质')
    return
  }
  specModalMode.value = 'add'
  specForm.value = {
    materialId: store.selectedMaterialId,
    diameter: '',
    wallThickness: '',
    length: '',
    weightPerMeter: 0
  }
  specModalVisible.value = true
}

function openEditSpec(item) {
  specModalMode.value = 'edit'
  currentEditingSpecId.value = item.id
  specForm.value = {
    materialId: item.materialId,
    diameter: item.diameter,
    wallThickness: item.wallThickness,
    length: item.length,
    weightPerMeter: item.weightPerMeter
  }
  specModalVisible.value = true
}

function handleSpecSubmit() {
  if (!specForm.value.diameter.trim()) {
    Message.warning('请输入直径')
    return
  }
  if (!specForm.value.wallThickness.trim()) {
    Message.warning('请输入壁厚')
    return
  }
  if (!specForm.value.weightPerMeter || specForm.value.weightPerMeter <= 0) {
    Message.warning('请输入有效的每米理论重量')
    return
  }
  if (specModalMode.value === 'add') {
    store.addSpec({ ...specForm.value })
    Message.success('添加成功')
  } else {
    store.updateSpec(currentEditingSpecId.value, { ...specForm.value })
    Message.success('修改成功')
  }
  specModalVisible.value = false
}

function handleDeleteSpec(item) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除规格 φ${item.diameter}×${item.wallThickness} 吗？`,
    okButtonProps: { status: 'danger' },
    onOk: () => {
      store.deleteSpec(item.id)
      Message.success('删除成功')
    }
  })
}

function openAddProduct() {
  if (!store.selectedMaterialId) {
    Message.warning('请先在左侧选择一个材质')
    return
  }
  productModalMode.value = 'add'
  productForm.value = {
    productCode: '',
    productName: '',
    materialId: store.selectedMaterialId,
    specId: null,
    quantity: 0,
    unit: '支',
    unitPrice: 0
  }
  productModalVisible.value = true
}

function openEditProduct(item) {
  productModalMode.value = 'edit'
  currentEditingProductId.value = item.id
  productForm.value = {
    productCode: item.productCode,
    productName: item.productName,
    materialId: item.materialId,
    specId: item.specId,
    quantity: item.quantity,
    unit: item.unit,
    unitPrice: item.unitPrice
  }
  productModalVisible.value = true
}

function handleProductSubmit() {
  if (!productForm.value.productCode.trim()) {
    Message.warning('请输入商品编号')
    return
  }
  if (!productForm.value.productName.trim()) {
    Message.warning('请输入商品名称')
    return
  }
  if (!productForm.value.specId) {
    Message.warning('请选择规格')
    return
  }
  if (productModalMode.value === 'add') {
    store.addProduct({ ...productForm.value })
    Message.success('添加成功')
  } else {
    store.updateProduct(currentEditingProductId.value, { ...productForm.value })
    Message.success('修改成功')
  }
  productModalVisible.value = false
}

function handleDeleteProduct(item) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除商品「${item.productName}」吗？`,
    okButtonProps: { status: 'danger' },
    onOk: () => {
      store.deleteProduct(item.id)
      Message.success('删除成功')
    }
  })
}

function getTotalWeight(product) {
  return store.calculateTotalWeight(product)
}

function getTotalAmount(product) {
  return store.calculateTotalAmount(product)
}

const specColumns = [
  { title: '材质', dataIndex: 'materialName', width: 100 },
  { title: '直径(mm)', dataIndex: 'diameter', width: 100 },
  { title: '壁厚(mm)', dataIndex: 'wallThickness', width: 100 },
  { title: '长度(mm)', dataIndex: 'length', width: 110 },
  { title: '每米理论重量(kg/m)', dataIndex: 'weightPerMeter', width: 150 },
  { title: '创建时间', dataIndex: 'createTime', width: 120 },
  { title: '操作', dataIndex: 'actions', width: 140 }
]

const productColumns = [
  { title: '商品编号', dataIndex: 'productCode', width: 140 },
  { title: '商品名称', dataIndex: 'productName', width: 240 },
  { title: '材质', dataIndex: 'materialName', width: 90 },
  { title: '规格(直径×壁厚)', dataIndex: 'spec', width: 130 },
  { title: '单支长度(m)', dataIndex: 'lengthM', width: 110 },
  { title: '每米重量(kg)', dataIndex: 'weightPerMeter', width: 120 },
  { title: '支数', dataIndex: 'quantity', width: 90, editable: true },
  { title: '单位', dataIndex: 'unit', width: 70 },
  { title: '理论总吨位(t)', dataIndex: 'totalWeight', width: 130 },
  { title: '单价(元/吨)', dataIndex: 'unitPrice', width: 120 },
  { title: '总金额(元)', dataIndex: 'totalAmount', width: 130 },
  { title: '操作', dataIndex: 'actions', width: 140 }
]
</script>

<template>
  <div class="page-wrapper">
    <div class="page-header">
      <h2 class="page-title">基础资料</h2>
      <p class="page-desc">维护钢材材质、规格参数及商品档案，支持理论重量自动换算</p>
    </div>

    <div class="page-body">
      <div class="layout-container">
        <div class="left-panel">
          <div class="panel-header">
            <span class="panel-title">
              <icon-list style="margin-right: 6px;" />
              材质列表
            </span>
            <a-button type="primary" size="small" @click="openAddMaterial">
              <template #icon>
                <icon-plus />
              </template>
              新增
            </a-button>
          </div>

          <a-input
            v-model="materialSearch"
            placeholder="搜索材质..."
            class="material-search"
            allow-clear
          >
            <template #prefix>
              <icon-search />
            </template>
          </a-input>

          <div class="material-list">
            <div
              class="material-item"
              :class="{ active: store.selectedMaterialId === null }"
              @click="handleSelectMaterial(null)"
            >
              <div class="material-icon all">
                <icon-apps />
              </div>
              <div class="material-info">
                <div class="material-name">全部材质</div>
                <div class="material-desc">共 {{ store.materials.length }} 种材质</div>
              </div>
            </div>

            <div
              v-for="item in filteredMaterials"
              :key="item.id"
              class="material-item"
              :class="{ active: store.selectedMaterialId === item.id }"
              @click="handleSelectMaterial(item.id)"
            >
              <div class="material-icon">
                <icon-tag />
              </div>
              <div class="material-info">
                <div class="material-name">{{ item.name }}</div>
                <div class="material-desc">{{ item.description }}</div>
              </div>
              <div class="material-actions" @click.stop>
                <a-button
                  type="outline"
                  size="mini"
                  @click="openEditMaterial(item)"
                >
                  <template #icon>
                    <icon-edit />
                  </template>
                </a-button>
                <a-button
                  type="outline"
                  size="mini"
                  status="danger"
                  @click="handleDeleteMaterial(item)"
                >
                  <template #icon>
                    <icon-delete />
                  </template>
                </a-button>
              </div>
            </div>
          </div>
        </div>

        <div class="right-panel">
          <a-tabs v-model:active-key="activeTab" type="card" class="data-tabs">
            <a-tab-pane key="spec" title="规格配置">
              <div class="tab-toolbar">
                <div class="toolbar-left">
                  <span class="filter-tip">
                    当前筛选：
                    <a-tag color="orange">{{ selectedMaterialName }}</a-tag>
                  </span>
                  <a-input
                    v-model="specSearch"
                    placeholder="搜索规格..."
                    style="width: 200px"
                    allow-clear
                  >
                    <template #prefix>
                      <icon-search />
                    </template>
                  </a-input>
                </div>
                <div class="toolbar-right">
                  <a-button @click="specSearch = ''">
                    <template #icon>
                      <icon-refresh />
                    </template>
                    重置
                  </a-button>
                  <a-button type="primary" @click="openAddSpec">
                    <template #icon>
                      <icon-plus />
                    </template>
                    新增规格
                  </a-button>
                </div>
              </div>

              <a-table
                :data="filteredSpecs"
                :columns="specColumns"
                :pagination="{ pageSize: 8 }"
                class="data-table"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.dataIndex === 'actions'">
                    <a-button type="outline" size="small" @click="openEditSpec(record)">
                      <template #icon>
                        <icon-edit />
                      </template>
                      编辑
                    </a-button>
                    <a-button
                      type="outline"
                      size="small"
                      status="danger"
                      style="margin-left: 8px"
                      @click="handleDeleteSpec(record)"
                    >
                      <template #icon>
                        <icon-delete />
                      </template>
                      删除
                    </a-button>
                  </template>
                  <template v-else-if="column.dataIndex === 'weightPerMeter'">
                    <span class="weight-value">{{ record.weightPerMeter.toFixed(2) }}</span>
                  </template>
                </template>
              </a-table>
            </a-tab-pane>

            <a-tab-pane key="product" title="商品档案">
              <div class="tab-toolbar">
                <div class="toolbar-left">
                  <span class="filter-tip">
                    当前筛选：
                    <a-tag color="orange">{{ selectedMaterialName }}</a-tag>
                  </span>
                  <a-input
                    v-model="productSearch"
                    placeholder="搜索商品..."
                    style="width: 200px"
                    allow-clear
                  >
                    <template #prefix>
                      <icon-search />
                    </template>
                  </a-input>
                </div>
                <div class="toolbar-right">
                  <a-button @click="productSearch = ''">
                    <template #icon>
                      <icon-refresh />
                    </template>
                    重置
                  </a-button>
                  <a-button type="primary" @click="openAddProduct">
                    <template #icon>
                      <icon-plus />
                    </template>
                    新增商品
                  </a-button>
                </div>
              </div>

              <a-table
                :data="filteredProducts"
                :columns="productColumns"
                :pagination="{ pageSize: 8 }"
                class="data-table product-table"
                :scroll="{ x: 1400 }"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.dataIndex === 'spec'">
                    φ{{ record.diameter }}×{{ record.wallThickness }}
                  </template>
                  <template v-else-if="column.dataIndex === 'lengthM'">
                    {{ (parseFloat(record.length) / 1000).toFixed(2) }}
                  </template>
                  <template v-else-if="column.dataIndex === 'weightPerMeter'">
                    {{ record.weightPerMeter.toFixed(3) }}
                  </template>
                  <template v-else-if="column.dataIndex === 'totalWeight'">
                    <span class="weight-value highlight">
                      {{ getTotalWeight(record).toFixed(4) }}
                    </span>
                  </template>
                  <template v-else-if="column.dataIndex === 'totalAmount'">
                    <span class="amount-value">
                      {{ getTotalAmount(record).toFixed(2) }}
                    </span>
                  </template>
                  <template v-else-if="column.dataIndex === 'unitPrice'">
                    {{ record.unitPrice.toFixed(2) }}
                  </template>
                  <template v-else-if="column.dataIndex === 'actions'">
                    <a-button type="outline" size="small" @click="openEditProduct(record)">
                      <template #icon>
                        <icon-edit />
                      </template>
                      编辑
                    </a-button>
                    <a-button
                      type="outline"
                      size="small"
                      status="danger"
                      style="margin-left: 8px"
                      @click="handleDeleteProduct(record)"
                    >
                      <template #icon>
                        <icon-delete />
                      </template>
                      删除
                    </a-button>
                  </template>
                </template>
              </a-table>

              <div class="product-summary" v-if="filteredProducts.length > 0">
                <div class="summary-item">
                  <span class="summary-label">商品总数：</span>
                  <span class="summary-value">{{ filteredProducts.length }} 种</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">总吨位：</span>
                  <span class="summary-value highlight">
                    {{ filteredProducts.reduce((sum, p) => sum + getTotalWeight(p), 0).toFixed(4) }} 吨
                  </span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">总金额：</span>
                  <span class="summary-value amount">
                    ¥{{ filteredProducts.reduce((sum, p) => sum + getTotalAmount(p), 0).toFixed(2) }}
                  </span>
                </div>
              </div>
            </a-tab-pane>
          </a-tabs>
        </div>
      </div>
    </div>

    <a-modal
      v-model:visible="materialModalVisible"
      :title="materialModalMode === 'add' ? '新增材质' : '编辑材质'"
      @ok="handleMaterialSubmit"
      @cancel="materialModalVisible = false"
    >
      <a-form :model="materialForm" layout="vertical">
        <a-form-item label="材质名称" required>
          <a-input v-model="materialForm.name" placeholder="如：Q235B、Q355D" />
        </a-form-item>
        <a-form-item label="材质说明">
          <a-input v-model="materialForm.description" placeholder="如：普通碳素结构钢" />
        </a-form-item>
        <a-form-item label="执行标准">
          <a-input v-model="materialForm.standard" placeholder="如：GB/T 700" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:visible="specModalVisible"
      :title="specModalMode === 'add' ? '新增规格' : '编辑规格'"
      @ok="handleSpecSubmit"
      @cancel="specModalVisible = false"
    >
      <a-form :model="specForm" layout="vertical">
        <a-form-item label="材质">
          <a-select v-model="specForm.materialId" :options="store.materialOptions" disabled />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="直径(mm)" required>
              <a-input v-model="specForm.diameter" placeholder="如：57" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="壁厚(mm)" required>
              <a-input v-model="specForm.wallThickness" placeholder="如：3.5" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="长度(mm)">
              <a-input v-model="specForm.length" placeholder="如：6000" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="每米理论重量(kg/m)" required>
          <a-input-number
            v-model="specForm.weightPerMeter"
            :min="0"
            :step="0.01"
            :precision="3"
            style="width: 100%"
            placeholder="此数据是计算吨位的基础，请务必准确"
          />
        </a-form-item>
        <a-alert type="warning" :show-icon="true">
          <template #content>
            每米理论重量是整个系统的核心基础数据，采购、销售、库存的吨位计算都依赖此数据，请务必确保准确！
          </template>
        </a-alert>
      </a-form>
    </a-modal>

    <a-modal
      v-model:visible="productModalVisible"
      :title="productModalMode === 'add' ? '新增商品' : '编辑商品'"
      @ok="handleProductSubmit"
      @cancel="productModalVisible = false"
      width="600px"
    >
      <a-form :model="productForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="商品编号" required>
              <a-input v-model="productForm.productCode" placeholder="如：P202403001" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="商品名称" required>
              <a-input v-model="productForm.productName" placeholder="如：无缝钢管 Q235B φ57×3.5" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="材质">
              <a-select v-model="productForm.materialId" :options="store.materialOptions" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="规格" required>
              <a-select v-model="productForm.specId" :options="specOptionsForProduct" placeholder="请选择规格" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="数量(支数)">
              <a-input-number
                v-model="productForm.quantity"
                :min="0"
                :precision="0"
                style="width: 100%"
                placeholder="输入支数"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="单位">
              <a-input v-model="productForm.unit" placeholder="支" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="单价(元/吨)">
              <a-input-number
                v-model="productForm.unitPrice"
                :min="0"
                :precision="2"
                style="width: 100%"
                placeholder="每吨价格"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <div class="preview-box" v-if="productForm.specId">
          <div class="preview-title">理论重量预览（自动计算）</div>
          <div class="preview-row">
            <span>总吨位 = 长度 × 支数 × 每米重量 ÷ 1000</span>
          </div>
          <div class="preview-row highlight">
            <span>预计总吨位：</span>
            <strong>{{ getTotalWeight(productForm).toFixed(4) }} 吨</strong>
          </div>
          <div class="preview-row amount" v-if="productForm.unitPrice > 0">
            <span>预计总金额：</span>
            <strong>¥{{ getTotalAmount(productForm).toFixed(2) }}</strong>
          </div>
        </div>
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
}

.layout-container {
  display: flex;
  gap: 20px;
  height: 100%;
  min-height: 560px;
}

.left-panel {
  width: 280px;
  flex-shrink: 0;
  background: #f7f9fc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a202c;
  display: flex;
  align-items: center;
}

.material-search {
  margin: 12px;
  width: calc(100% - 24px);
}

.material-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 12px;
}

.material-item {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 6px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.material-item:hover {
  background: #edf2f7;
}

.material-item.active {
  background: #fff1e6;
  border-left: 3px solid #ff6b35;
  padding-left: 9px;
}

.material-icon {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  background: #ff6b35;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
  flex-shrink: 0;
  font-size: 16px;
}

.material-icon.all {
  background: #4a5568;
}

.material-info {
  flex: 1;
  min-width: 0;
}

.material-name {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 2px;
}

.material-desc {
  font-size: 12px;
  color: #718096;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.material-actions {
  display: none;
  gap: 4px;
}

.material-item:hover .material-actions {
  display: flex;
}

.right-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.data-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
}

:deep(.data-tabs .arco-tabs-content) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:deep(.data-tabs .arco-tabs-pane) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tab-toolbar {
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

.filter-tip {
  font-size: 13px;
  color: #4a5568;
}

.data-table {
  flex: 1;
}

.weight-value {
  font-weight: 600;
  color: #2d3748;
  font-family: 'Roboto Mono', monospace;
}

.weight-value.highlight {
  color: #ff6b35;
  font-weight: 700;
}

.amount-value {
  font-weight: 600;
  color: #00b42a;
  font-family: 'Roboto Mono', monospace;
}

.product-table {
  flex: 1;
  overflow: auto;
}

.product-summary {
  display: flex;
  gap: 40px;
  padding: 16px 20px;
  background: #f7f9fc;
  border-top: 1px solid #e2e8f0;
  border-radius: 0 0 8px 8px;
  margin-top: auto;
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

.summary-value.highlight {
  color: #ff6b35;
  font-size: 18px;
}

.summary-value.amount {
  color: #00b42a;
  font-size: 18px;
}

.preview-box {
  margin-top: 16px;
  padding: 16px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 6px;
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #166534;
  margin-bottom: 8px;
}

.preview-row {
  font-size: 13px;
  color: #15803d;
  margin-bottom: 4px;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.preview-row.highlight strong {
  font-size: 18px;
  color: #ff6b35;
}

.preview-row.amount strong {
  font-size: 16px;
  color: #00b42a;
}
</style>
