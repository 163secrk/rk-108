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
  IconHome,
  IconFolder,
  IconLocation,
  IconDragDotVertical
} from '@arco-design/web-vue/es/icon'

const store = useBasicDataStore()

const selectedKeys = ref([])
const expandedKeys = ref([])
const submitting = ref(false)
const searchValue = ref('')

const modalVisible = ref(false)
const modalMode = ref('add')
const modalForm = ref({
  name: '',
  code: '',
  parentId: null,
  level: 1,
  address: '',
  remark: '',
  status: 1,
  sortNo: 0
})
const currentEditId = ref(null)

const selectedNode = computed(() => {
  if (!selectedKeys.value.length) return null
  const id = selectedKeys.value[0]
  return findNodeById(store.warehouseTree, id)
})

const levelLabel = computed(() => {
  const map = { 1: '仓库', 2: '分区', 3: '货位' }
  return map
})

const fullPath = computed(() => {
  if (!selectedNode.value) return ''
  return buildPath(store.warehouseTree, selectedNode.value.id)
})

const canAddChild = computed(() => {
  if (!selectedNode.value) return true
  return selectedNode.value.level < 3
})

const childLevel = computed(() => {
  if (!selectedNode.value) return 1
  return selectedNode.value.level + 1
})

const childLevelText = computed(() => {
  return levelLabel.value[childLevel.value] || ''
})

const childrenList = computed(() => {
  if (!selectedNode.value) return []
  return selectedNode.value.children || []
})

const treeData = computed(() => {
  if (!searchValue.value) return store.warehouseTree
  return filterTree(store.warehouseTree, searchValue.value.toLowerCase())
})

function findNodeById(nodes, id) {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children && node.children.length) {
      const found = findNodeById(node.children, id)
      if (found) return found
    }
  }
  return null
}

function buildPath(nodes, targetId, path = []) {
  for (const node of nodes) {
    const currentPath = [...path, node.name]
    if (node.id === targetId) return currentPath.join(' - ')
    if (node.children && node.children.length) {
      const result = buildPath(node.children, targetId, currentPath)
      if (result) return result
    }
  }
  return null
}

function filterTree(nodes, keyword) {
  const result = []
  for (const node of nodes) {
    const nameMatch = node.name.toLowerCase().includes(keyword) || (node.code && node.code.toLowerCase().includes(keyword))
    const filteredChildren = node.children ? filterTree(node.children, keyword) : []
    if (nameMatch || filteredChildren.length) {
      result.push({ ...node, children: filteredChildren })
    }
  }
  return result
}

function handleSelect(keys) {
  selectedKeys.value = keys
}

function getLevelIcon(level) {
  if (level === 1) return IconHome
  if (level === 2) return IconFolder
  return IconLocation
}

function getLevelColor(level) {
  if (level === 1) return '#ff6b35'
  if (level === 2) return '#3491fa'
  return '#00b42a'
}

function openAddRoot() {
  modalMode.value = 'add'
  modalForm.value = {
    name: '',
    code: '',
    parentId: null,
    level: 1,
    address: '',
    remark: '',
    status: 1,
    sortNo: 0
  }
  modalVisible.value = true
}

function openAddChild() {
  if (!selectedNode.value && selectedKeys.value.length === 0) {
    Message.warning('请先选择一个父级节点')
    return
  }
  const parent = selectedNode.value
  if (parent && parent.level >= 3) {
    Message.warning('货位下不能再添加子节点')
    return
  }
  modalMode.value = 'add'
  modalForm.value = {
    name: '',
    code: '',
    parentId: parent ? parent.id : null,
    level: parent ? parent.level + 1 : 1,
    address: parent && parent.address ? parent.address : '',
    remark: '',
    status: 1,
    sortNo: 0
  }
  modalVisible.value = true
}

function openEdit(node) {
  modalMode.value = 'edit'
  currentEditId.value = node.id
  modalForm.value = {
    name: node.name,
    code: node.code || '',
    parentId: node.parentId,
    level: node.level,
    address: node.address || '',
    remark: node.remark || '',
    status: node.status ?? 1,
    sortNo: node.sortNo || 0
  }
  modalVisible.value = true
}

function openEditSelected() {
  if (selectedNode.value) {
    openEdit(selectedNode.value)
  }
}

async function handleSubmit() {
  if (!modalForm.value.name.trim()) {
    Message.warning('请输入名称')
    return
  }
  submitting.value = true
  try {
    if (modalMode.value === 'add') {
      const result = await store.addWarehouse({ ...modalForm.value })
      if (result) {
        Message.success('添加成功')
        modalVisible.value = false
        if (result.parentId) {
          if (!expandedKeys.value.includes(result.parentId)) {
            expandedKeys.value.push(result.parentId)
          }
        }
      }
    } else {
      const result = await store.updateWarehouse(currentEditId.value, { ...modalForm.value })
      if (result) {
        Message.success('修改成功')
        modalVisible.value = false
      }
    }
  } finally {
    submitting.value = false
  }
}

function handleDelete(node) {
  const childCount = node.children ? node.children.length : 0
  const levelText = levelLabel.value[node.level] || '节点'
  let content = `确定删除${levelText}「${node.name}」吗？`
  if (childCount > 0) {
    content = `${levelText}「${node.name}」下有 ${childCount} 个子节点，删除后子节点也会被移除。确定删除吗？`
  }
  Modal.confirm({
    title: '确认删除',
    content,
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      const success = await store.deleteWarehouse(node.id)
      if (success) {
        Message.success('删除成功')
        if (selectedKeys.value.includes(node.id)) {
          selectedKeys.value = []
        }
      }
    }
  })
}

function handleDeleteSelected() {
  if (selectedNode.value) {
    handleDelete(selectedNode.value)
  }
}

const childColumns = [
  { title: '名称', dataIndex: 'name', width: 160 },
  { title: '编码', dataIndex: 'code', width: 120 },
  { title: '状态', slotName: 'status', width: 80 },
  { title: '排序', dataIndex: 'sortNo', width: 80 },
  { title: '备注', dataIndex: 'remark', width: 160 },
  { title: '操作', slotName: 'actions', width: 120 }
]

async function loadData() {
  try {
    await store.fetchWarehouseTree()
    await store.fetchWarehouses()
  } catch (e) {
    console.error('加载仓库数据失败', e)
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="page-wrapper">
    <div class="page-header">
      <h2 class="page-title">仓库管理</h2>
      <p class="page-desc">维护仓库、分区、货位三级层级结构，支持多级库位管理</p>
    </div>

    <div class="page-body">
      <div class="layout-container">
        <div class="left-panel">
          <div class="panel-header">
            <span class="panel-title">
              <icon-home style="margin-right: 6px;" />
              仓库结构
            </span>
            <a-button type="primary" size="small" @click="openAddRoot">
              <template #icon>
                <icon-plus />
              </template>
              新增仓库
            </a-button>
          </div>

          <a-input
            v-model="searchValue"
            placeholder="搜索仓库/分区/货位..."
            class="tree-search"
            allow-clear
          >
            <template #prefix>
              <icon-search />
            </template>
          </a-input>

          <div class="tree-container">
            <a-tree
              :data="treeData"
              :selected-keys="selectedKeys"
              :expanded-keys="expandedKeys"
              show-line
              block-node
              @select="handleSelect"
              @expand="expandedKeys = $event"
            >
              <template #title="nodeData">
                <div class="tree-node" @contextmenu.prevent>
                  <component
                    :is="getLevelIcon(nodeData.level)"
                    :style="{ color: getLevelColor(nodeData.level), marginRight: '6px', fontSize: '14px' }"
                  />
                  <span class="tree-node-name">{{ nodeData.name }}</span>
                  <span v-if="nodeData.code" class="tree-node-code">{{ nodeData.code }}</span>
                  <span class="tree-node-actions" @click.stop>
                    <a-button
                      v-if="nodeData.level < 3"
                      type="text"
                      size="mini"
                      @click="selectedKeys = [nodeData.id]; openAddChild()"
                    >
                      <template #icon><icon-plus /></template>
                    </a-button>
                    <a-button type="text" size="mini" @click="openEdit(nodeData)">
                      <template #icon><icon-edit /></template>
                    </a-button>
                    <a-button type="text" size="mini" status="danger" @click="handleDelete(nodeData)">
                      <template #icon><icon-delete /></template>
                    </a-button>
                  </span>
                </div>
              </template>
            </a-tree>
            <div v-if="!treeData.length" class="tree-empty">
              <icon-home :size="32" style="color: #c9cdd4;" />
              <p>暂无仓库数据</p>
              <a-button type="primary" size="small" @click="openAddRoot">
                <template #icon><icon-plus /></template>
                新增仓库
              </a-button>
            </div>
          </div>
        </div>

        <div class="right-panel">
          <template v-if="selectedNode">
            <div class="detail-card">
              <div class="detail-header">
                <div class="detail-header-left">
                  <div class="detail-icon" :style="{ background: getLevelColor(selectedNode.level) }">
                    <component :is="getLevelIcon(selectedNode.level)" />
                  </div>
                  <div>
                    <div class="detail-name">{{ selectedNode.name }}</div>
                    <div class="detail-path">{{ fullPath }}</div>
                  </div>
                </div>
                <div class="detail-header-right">
                  <a-tag :color="getLevelColor(selectedNode.level)" size="small">
                    {{ levelLabel[selectedNode.level] }}
                  </a-tag>
                  <a-tag :color="selectedNode.status === 1 ? 'green' : 'red'" size="small">
                    {{ selectedNode.status === 1 ? '启用' : '停用' }}
                  </a-tag>
                </div>
              </div>

              <div class="detail-body">
                <a-descriptions :column="2" bordered size="medium">
                  <a-descriptions-item label="编码">{{ selectedNode.code || '-' }}</a-descriptions-item>
                  <a-descriptions-item label="层级">{{ levelLabel[selectedNode.level] }}</a-descriptions-item>
                  <a-descriptions-item label="地址" :span="2">{{ selectedNode.address || '-' }}</a-descriptions-item>
                  <a-descriptions-item label="排序">{{ selectedNode.sortNo ?? 0 }}</a-descriptions-item>
                  <a-descriptions-item label="创建时间">{{ selectedNode.createTime || '-' }}</a-descriptions-item>
                  <a-descriptions-item label="备注" :span="2">{{ selectedNode.remark || '-' }}</a-descriptions-item>
                </a-descriptions>
              </div>

              <div class="detail-actions">
                <a-button v-if="canAddChild" type="primary" @click="openAddChild">
                  <template #icon><icon-plus /></template>
                  新增{{ childLevelText }}
                </a-button>
                <a-button @click="openEditSelected">
                  <template #icon><icon-edit /></template>
                  编辑
                </a-button>
                <a-button status="danger" @click="handleDeleteSelected">
                  <template #icon><icon-delete /></template>
                  删除
                </a-button>
              </div>
            </div>

            <div class="children-card" v-if="selectedNode.level < 3">
              <div class="children-header">
                <span class="children-title">
                  {{ childLevelText }}列表
                  <a-tag color="gray" size="small">{{ childrenList.length }}</a-tag>
                </span>
                <a-button v-if="canAddChild" type="outline" size="small" @click="openAddChild">
                  <template #icon><icon-plus /></template>
                  新增{{ childLevelText }}
                </a-button>
              </div>
              <a-table
                v-if="childrenList.length"
                :data="childrenList"
                :columns="childColumns"
                :pagination="false"
                size="small"
                class="children-table"
              >
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
              <div v-else class="children-empty">
                暂无{{ childLevelText }}，点击上方按钮新增
              </div>
            </div>
          </template>

          <div v-else class="empty-state">
            <icon-home :size="48" style="color: #c9cdd4;" />
            <div class="empty-text">请在左侧选择一个仓库节点查看详情</div>
            <a-button type="primary" @click="openAddRoot">
              <template #icon><icon-plus /></template>
              新增仓库
            </a-button>
          </div>
        </div>
      </div>
    </div>

    <a-modal
      v-model:visible="modalVisible"
      :title="modalMode === 'add' ? `新增${levelLabel[modalForm.level] || '节点'}` : `编辑${levelLabel[modalForm.level] || '节点'}`"
      @ok="handleSubmit"
      :ok-loading="submitting"
      @cancel="modalVisible = false"
      width="520px"
    >
      <a-form :model="modalForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="名称" required>
              <a-input v-model="modalForm.name" placeholder="请输入名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="编码">
              <a-input v-model="modalForm.code" placeholder="请输入编码" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="层级">
          <a-radio-group v-model="modalForm.level" :disabled="modalMode === 'edit' || modalForm.parentId !== null">
            <a-radio :value="1">仓库</a-radio>
            <a-radio :value="2">分区</a-radio>
            <a-radio :value="3">货位</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="modalForm.level === 1" label="地址">
          <a-input v-model="modalForm.address" placeholder="请输入地址" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="状态">
              <a-radio-group v-model="modalForm.status">
                <a-radio :value="1">启用</a-radio>
                <a-radio :value="0">停用</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序号">
              <a-input-number v-model="modalForm.sortNo" :min="0" style="width: 100%" placeholder="越小越靠前" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注">
          <a-textarea v-model="modalForm.remark" placeholder="请输入备注" :max-length="200" :auto-size="{ minRows: 2, maxRows: 4 }" />
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
}

.layout-container {
  display: flex;
  gap: 20px;
  height: 100%;
  min-height: 560px;
}

.left-panel {
  width: 320px;
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

.tree-search {
  margin: 12px;
  width: calc(100% - 24px);
}

.tree-container {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px 12px;
}

.tree-node {
  display: flex;
  align-items: center;
  padding: 2px 0;
  width: 100%;
}

.tree-node-name {
  font-size: 13px;
  font-weight: 500;
  color: #2d3748;
}

.tree-node-code {
  font-size: 11px;
  color: #a1a1aa;
  margin-left: 6px;
}

.tree-node-actions {
  display: none;
  margin-left: auto;
  gap: 2px;
  align-items: center;
}

.tree-node:hover .tree-node-actions {
  display: flex;
}

.tree-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #a1a1aa;
  gap: 12px;
}

.tree-empty p {
  margin: 0;
  font-size: 13px;
}

.right-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: linear-gradient(135deg, #fff7f0 0%, #fff 100%);
  border-bottom: 1px solid #e2e8f0;
}

.detail-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.detail-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.detail-name {
  font-size: 18px;
  font-weight: 700;
  color: #1a202c;
  margin-bottom: 2px;
}

.detail-path {
  font-size: 12px;
  color: #718096;
}

.detail-header-right {
  display: flex;
  gap: 8px;
}

.detail-body {
  padding: 20px 24px;
}

.detail-actions {
  display: flex;
  gap: 8px;
  padding: 16px 24px;
  border-top: 1px solid #e2e8f0;
  background: #f7f9fc;
}

.children-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.children-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.children-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a202c;
  display: flex;
  align-items: center;
  gap: 8px;
}

.children-table {
  flex: 1;
}

.children-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  color: #a1a1aa;
  font-size: 13px;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.empty-text {
  font-size: 14px;
  color: #a1a1aa;
}

:deep(.arco-tree-node-title) {
  width: 100%;
}

:deep(.arco-tree-node-title:hover) {
  background: transparent;
}
</style>
