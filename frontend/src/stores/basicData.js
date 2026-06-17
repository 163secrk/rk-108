import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getMaterialList,
  addMaterial as apiAddMaterial,
  updateMaterial as apiUpdateMaterial,
  deleteMaterial as apiDeleteMaterial,
  getSpecList,
  addSpec as apiAddSpec,
  updateSpec as apiUpdateSpec,
  deleteSpec as apiDeleteSpec,
  getProductList,
  addProduct as apiAddProduct,
  updateProduct as apiUpdateProduct,
  deleteProduct as apiDeleteProduct
} from '@/api/basicData'

export const useBasicDataStore = defineStore('basicData', () => {
  const materials = ref([])
  const specs = ref([])
  const products = ref([])
  const selectedMaterialId = ref(null)
  const loading = ref(false)

  const filteredSpecs = computed(() => {
    if (!selectedMaterialId.value) return specs.value
    return specs.value.filter(s => s.materialId === selectedMaterialId.value)
  })

  const filteredProducts = computed(() => {
    if (!selectedMaterialId.value) return products.value
    return products.value.filter(p => p.materialId === selectedMaterialId.value)
  })

  const materialOptions = computed(() => {
    return materials.value.map(m => ({ value: m.id, label: m.name }))
  })

  function getMaterialById(id) {
    return materials.value.find(m => m.id === id)
  }

  function getSpecById(id) {
    return specs.value.find(s => s.id === id)
  }

  function getProductById(id) {
    return products.value.find(p => p.id === id)
  }

  function selectMaterial(id) {
    selectedMaterialId.value = selectedMaterialId.value === id ? null : id
  }

  async function fetchMaterials() {
    try {
      const res = await getMaterialList()
      materials.value = res.data || []
      return materials.value
    } catch (e) {
      console.error('获取材质列表失败', e)
      throw e
    }
  }

  async function fetchSpecs(materialId = null) {
    try {
      const res = await getSpecList(materialId ? { materialId } : {})
      specs.value = res.data || []
      return specs.value
    } catch (e) {
      console.error('获取规格列表失败', e)
      throw e
    }
  }

  async function fetchProducts(materialId = null) {
    try {
      const res = await getProductList(materialId ? { materialId } : {})
      products.value = res.data || []
      return products.value
    } catch (e) {
      console.error('获取商品列表失败', e)
      throw e
    }
  }

  async function fetchAllData() {
    loading.value = true
    try {
      await Promise.all([
        fetchMaterials(),
        fetchSpecs(),
        fetchProducts()
      ])
    } finally {
      loading.value = false
    }
  }

  async function addMaterial(data) {
    const res = await apiAddMaterial(data)
    if (res.code === 200 && res.data) {
      materials.value.push(res.data)
      return res.data
    }
    return null
  }

  async function updateMaterial(id, data) {
    const res = await apiUpdateMaterial({ id, ...data })
    if (res.code === 200 && res.data) {
      const index = materials.value.findIndex(m => m.id === id)
      if (index !== -1) {
        materials.value[index] = res.data
      }
      return res.data
    }
    return null
  }

  async function deleteMaterial(id) {
    const res = await apiDeleteMaterial(id)
    if (res.code === 200) {
      materials.value = materials.value.filter(m => m.id !== id)
      specs.value = specs.value.filter(s => s.materialId !== id)
      products.value = products.value.filter(p => p.materialId !== id)
      if (selectedMaterialId.value === id) {
        selectedMaterialId.value = null
      }
      return true
    }
    return false
  }

  async function addSpec(data) {
    const res = await apiAddSpec(data)
    if (res.code === 200 && res.data) {
      specs.value.push(res.data)
      return res.data
    }
    return null
  }

  async function updateSpec(id, data) {
    const res = await apiUpdateSpec({ id, ...data })
    if (res.code === 200 && res.data) {
      const index = specs.value.findIndex(s => s.id === id)
      if (index !== -1) {
        specs.value[index] = res.data
      }
      return res.data
    }
    return null
  }

  async function deleteSpec(id) {
    const res = await apiDeleteSpec(id)
    if (res.code === 200) {
      specs.value = specs.value.filter(s => s.id !== id)
      return true
    }
    return false
  }

  function getSpecsByMaterialId(materialId) {
    return specs.value.filter(s => s.materialId === materialId)
  }

  async function addProduct(data) {
    const res = await apiAddProduct(data)
    if (res.code === 200 && res.data) {
      products.value.push(res.data)
      return res.data
    }
    return null
  }

  async function updateProduct(id, data) {
    const res = await apiUpdateProduct({ id, ...data })
    if (res.code === 200 && res.data) {
      const index = products.value.findIndex(p => p.id === id)
      if (index !== -1) {
        products.value[index] = res.data
      }
      return res.data
    }
    return null
  }

  async function deleteProduct(id) {
    const res = await apiDeleteProduct(id)
    if (res.code === 200) {
      products.value = products.value.filter(p => p.id !== id)
      return true
    }
    return false
  }

  function calculateTotalWeight(product) {
    const lengthInMeters = parseFloat(product.length) / 1000
    const quantity = parseFloat(product.quantity) || 0
    const weightPerMeter = parseFloat(product.weightPerMeter) || 0
    const totalWeight = lengthInMeters * quantity * weightPerMeter
    return Math.round(totalWeight * 10000) / 10000
  }

  function calculateTotalAmount(product) {
    const totalWeight = calculateTotalWeight(product)
    const unitPrice = parseFloat(product.unitPrice) || 0
    return Math.round(totalWeight * unitPrice * 100) / 100
  }

  return {
    materials,
    specs,
    products,
    selectedMaterialId,
    loading,
    filteredSpecs,
    filteredProducts,
    materialOptions,
    getMaterialById,
    getSpecById,
    getProductById,
    selectMaterial,
    fetchMaterials,
    fetchSpecs,
    fetchProducts,
    fetchAllData,
    addMaterial,
    updateMaterial,
    deleteMaterial,
    addSpec,
    updateSpec,
    deleteSpec,
    getSpecsByMaterialId,
    addProduct,
    updateProduct,
    deleteProduct,
    calculateTotalWeight,
    calculateTotalAmount
  }
})
