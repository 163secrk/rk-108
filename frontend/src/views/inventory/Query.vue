<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { Message } from '@arco-design/web-vue'
import {
  IconSearch,
  IconRefresh,
  IconDown,
  IconRight
} from '@arco-design/web-vue/es/icon'
import { useBasicDataStore } from '@/stores/basicData'
import { queryStockTree } from '@/api/inventory'

const basicDataStore = useBasicDataStore()

const loading = ref(false)
const tableData = ref([])

const filterForm = ref({
  warehouseId: null,
  materialId: null,
  specId: null
})

const warehouseOptions = computed(() => {
  return basicDataStore.warehouses
    .filter(w => w.level === 1)
    .map(w => ({ value: w.id, label: w.name }))
})

const materialOptions = computed(() => {
  return basicDataStore.materials.map(m => ({ value: m.id, label: m.name }))
})

const specOptions = computed(() => {
  let list = basicDataStore.specs
  if (filterForm.value.materialId) {
    list = list.filter(s => s.materialId === filterForm.value.materialId)
  }
  return list.map(s => ({
    value: s.id,
    label: 'φ' + s.diameter + '×' + s.wallThickness
  }))
})

const expandedKeys = ref([])

const columns = [
  {
    title: '仓库',
    dataIndex: 'warehouseName',
    width: 120,
    fixed: 'left'
  },
  {
    title: '材质',
    dataIndex: 'materialName',
    width: 100
  },
  {
    title: '规格',
    dataIndex: 'specText',
    width: 140
  },
  {
    title: '商品名称',
    dataIndex: 'productName',
    width: 220
  },
  {
    title: '炉批号',
    dataIndex: 'furnaceNo',
    width: 140
  },
  {
    title: '入库日期',
    dataIndex: 'stockInDate',
    width: 120
  },
  {
    title: '剩余支数',
    dataIndex: 'remainingQuantity',
    width: 110,
    align: 'right',
    render: ({ record }) => {
      const val = record.totalQuantity != null ? record.totalQuantity : record.remainingQuantity
      return val != null ? val.toLocaleString() : '-'
    }
  },
  {
    title: '剩余吨位(吨)',
    dataIndex: 'remainingWeight',
    width: 130,
    align: 'right',
    render: ({ record }) => {
      const val = record.totalWeight != null ? record.totalWeight : record.remainingWeight
      if (val == null) return '-'
      const tons = Number(val) / 1000
      return tons.toFixed(3)
    }
  },
  {
    title: '入库单价(元/支)',
    dataIndex: 'unitPrice',
    width: 140,
    align: 'right',
    render: ({ record }) => {
      const val = record.unitPrice
      if (val == null) {
        return record.totalQuantity != null ? '' : '-'
      }
      return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    }
  }
]

const pagination = computed(() => {
  const totalSummary = tableData.value.reduce((sum, row) => sum + (row.totalQuantity || 0), 0)
  const totalWeight = tableData.value.reduce((sum, row) => {
    return sum + (row.totalWeight ? Number(row.totalWeight) : 0)
  }, 0)
  return {
    showTotal: true,
    total: tableData.value.length,
    simple: true,
    pageSize: 9999,
    showPageSize: false,
    showJumper: false
  }
})

const statistics = computed(() => {
  const totalBatches = tableData.value.reduce((sum, row) => {
    return sum + (row.children ? row.children.length : 0)
  }, 0)
  const totalQuantity = tableData.value.reduce((sum, row) => sum + (row.totalQuantity || 0), 0)
  const totalWeight = tableData.value.reduce((sum, row) => {
    return sum + (row.totalWeight ? Number(row.totalWeight) : 0)
  }, 0)
  return {
    specs: tableData.value.length,
    batches: totalBatches,
    quantity: totalQuantity,
    weight: (totalWeight / 1000).toFixed(3)
  }
})

async function fetchData() {
  loading.value = true
  try {
    const res = await queryStockTree({
      warehouseId: filterForm.value.warehouseId || undefined,
      materialId: filterForm.value.materialId || undefined,
      specId: filterForm.value.specId || undefined
    })
    tableData.value = res.data || []
  } catch (e) {
    console.error('查询库存失败', e)
    Message.error('查询库存失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchData()
}

function handleReset() {
  filterForm.value = {
    warehouseId: null,
    materialId: null,
    specId: null
  }
  fetchData()
}

function handleMaterialChange(val) {
  if (!val) {
    filterForm.value.specId = null
  }
}

function rowClassName(record, index) {
  if (record.children || record.totalQuantity != null) {
    return 'summary-row'
  }
  return 'batch-row'
}

async function initBasicData() {
  try {
    await Promise.all([
      basicDataStore.fetchWarehouses(),
      basicDataStore.fetchMaterials(),
      basicDataStore.fetchSpecs()
    ])
  } catch (e) {
    console.error('加载基础数据失败', e)
  }
}

onMounted(async () => {
  await initBasicData()
  fetchData()
})
</script>

<template>
  <div class="page-wrapper">
    <div class="page-header">
      <div class="header-title-box">
        <h2 class="page-title">库存查询</h2>
        <p class="page-desc">按仓库+规格汇总，支持按批次查看炉批号、入库日期、剩余数量等详情</p>
      </div>
      <div class="header-stats">
        <div class="stat-card">
          <span class="stat-label">规格种类</span>
          <span class="stat-value stat-blue">{{ statistics.specs }}</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">批次数</span>
          <span class="stat-value stat-purple">{{ statistics.batches }}</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">总数量(支)</span>
          <span class="stat-value stat-orange">{{ statistics.quantity.toLocaleString() }}</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">总吨位(吨)</span>
          <span class="stat-value stat-green">{{ statistics.weight }}</span>
        </div>
      </div>
    </div>

    <a-card class="filter-card" bordered>
      <a-form layout="inline" :model="filterForm">
        <a-form-item field="warehouseId" label="仓库">
          <a-select
            v-model="filterForm.warehouseId"
            placeholder="请选择仓库"
            allow-clear
            style="width: 160px"
          >
            <a-option
              v-for="opt in warehouseOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="materialId" label="材质">
          <a-select
            v-model="filterForm.materialId"
            placeholder="请选择材质"
            allow-clear
            style="width: 160px"
            @change="handleMaterialChange"
          >
            <a-option
              v-for="opt in materialOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="specId" label="规格">
          <a-select
            v-model="filterForm.specId"
            placeholder="请选择规格"
            allow-clear
            style="width: 200px"
          >
            <a-option
              v-for="opt in specOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </a-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">
              <template #icon>
                <icon-search />
              </template>
              查询
            </a-button>
            <a-button @click="handleReset">
              <template #icon>
                <icon-refresh />
              </template>
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card class="table-card" bordered>
      <a-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-class-name="rowClassName"
        :expanded-row-keys="expandedKeys"
        :default-expand-all="false"
        :scroll="{ x: 1200 }"
        row-key="id"
        border
        size="small"
        :indents="24"
      >
        <template #expand-icon="{ column, record, expanded }">
          <span v-if="record.children && record.children.length" class="expand-icon">
            <icon-down v-if="expanded" />
            <icon-right v-else />
          </span>
          <span v-else class="expand-icon-placeholder"></span>
        </template>
        <template #empty>
          <a-empty description="暂无库存数据" />
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<style scoped>
.page-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 20px;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 4px;
}

.header-title-box {
  flex: 1;
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

.header-stats {
  display: flex;
  gap: 16px;
  flex-shrink: 0;
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 110px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.stat-label {
  font-size: 12px;
  color: #718096;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
}

.stat-blue { color: #3491fa; }
.stat-purple { color: #722ed1; }
.stat-orange { color: #ff7d00; }
.stat-green { color: #00b42a; }

.filter-card {
  background: #fff;
  border-radius: 8px;
}

.table-card {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  min-height: 400px;
}

:deep(.summary-row) {
  background-color: #f7fafc !important;
}

:deep(.summary-row:hover > td) {
  background-color: #edf2f7 !important;
}

:deep(.summary-row td) {
  font-weight: 600;
  color: #2d3748;
  background-color: #f7fafc !important;
}

:deep(.batch-row td) {
  background-color: #ffffff !important;
  font-weight: 400;
  color: #4a5568;
}

:deep(.batch-row:hover > td) {
  background-color: #fff8f0 !important;
}

.expand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: #ff6b35;
  font-size: 12px;
  cursor: pointer;
}

.expand-icon-placeholder {
  display: inline-block;
  width: 20px;
  height: 20px;
}

:deep(.arco-table-indents) {
  display: inline-flex;
}

:deep(.arco-table-cell-expand-icon) {
  margin-right: 0;
}
</style>
