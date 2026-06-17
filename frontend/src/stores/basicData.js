import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useBasicDataStore = defineStore('basicData', () => {
  const materials = ref([
    { id: 1, name: 'Q235B', description: '普通碳素结构钢', standard: 'GB/T 700', createTime: '2024-01-15' },
    { id: 2, name: 'Q355D', description: '低合金高强度结构钢', standard: 'GB/T 1591', createTime: '2024-01-16' },
    { id: 3, name: 'Q355B', description: '低合金高强度结构钢', standard: 'GB/T 1591', createTime: '2024-02-01' },
    { id: 4, name: '20#', description: '优质碳素结构钢', standard: 'GB/T 8163', createTime: '2024-02-10' },
    { id: 5, name: '45#', description: '优质碳素结构钢', standard: 'GB/T 699', createTime: '2024-03-05' }
  ])

  const specs = ref([
    { id: 1, materialId: 1, materialName: 'Q235B', diameter: '57', wallThickness: '3.5', length: '6000', weightPerMeter: 4.62, createTime: '2024-01-15' },
    { id: 2, materialId: 1, materialName: 'Q235B', diameter: '76', wallThickness: '4.0', length: '6000', weightPerMeter: 7.10, createTime: '2024-01-16' },
    { id: 3, materialId: 1, materialName: 'Q235B', diameter: '89', wallThickness: '4.5', length: '6000', weightPerMeter: 9.38, createTime: '2024-01-20' },
    { id: 4, materialId: 2, materialName: 'Q355D', diameter: '108', wallThickness: '5.0', length: '9000', weightPerMeter: 12.70, createTime: '2024-02-01' },
    { id: 5, materialId: 2, materialName: 'Q355D', diameter: '133', wallThickness: '6.0', length: '9000', weightPerMeter: 18.79, createTime: '2024-02-10' },
    { id: 6, materialId: 3, materialName: 'Q355B', diameter: '159', wallThickness: '6.0', length: '6000', weightPerMeter: 22.64, createTime: '2024-02-15' },
    { id: 7, materialId: 3, materialName: 'Q355B', diameter: '219', wallThickness: '8.0', length: '12000', weightPerMeter: 41.63, createTime: '2024-03-01' },
    { id: 8, materialId: 4, materialName: '20#', diameter: '32', wallThickness: '3.0', length: '6000', weightPerMeter: 2.15, createTime: '2024-03-05' },
    { id: 9, materialId: 4, materialName: '20#', diameter: '48', wallThickness: '3.5', length: '6000', weightPerMeter: 3.84, createTime: '2024-03-10' },
    { id: 10, materialId: 5, materialName: '45#', diameter: '102', wallThickness: '5.0', length: '6000', weightPerMeter: 11.96, createTime: '2024-03-15' }
  ])

  const products = ref([
    { id: 1, productCode: 'P202403001', productName: '无缝钢管 Q235B φ57×3.5', materialId: 1, materialName: 'Q235B', specId: 1, diameter: '57', wallThickness: '3.5', length: '6000', weightPerMeter: 4.62, quantity: 100, unit: '支', unitPrice: 4500, createTime: '2024-03-01' },
    { id: 2, productCode: 'P202403002', productName: '无缝钢管 Q235B φ76×4.0', materialId: 1, materialName: 'Q235B', specId: 2, diameter: '76', wallThickness: '4.0', length: '6000', weightPerMeter: 7.10, quantity: 80, unit: '支', unitPrice: 4600, createTime: '2024-03-02' },
    { id: 3, productCode: 'P202403003', productName: '无缝钢管 Q355D φ108×5.0', materialId: 2, materialName: 'Q355D', specId: 4, diameter: '108', wallThickness: '5.0', length: '9000', weightPerMeter: 12.70, quantity: 50, unit: '支', unitPrice: 5200, createTime: '2024-03-05' },
    { id: 4, productCode: 'P202403004', productName: '无缝钢管 Q355B φ219×8.0', materialId: 3, materialName: 'Q355B', specId: 7, diameter: '219', wallThickness: '8.0', length: '12000', weightPerMeter: 41.63, quantity: 30, unit: '支', unitPrice: 4800, createTime: '2024-03-10' },
    { id: 5, productCode: 'P202403005', productName: '无缝钢管 20# φ48×3.5', materialId: 4, materialName: '20#', specId: 9, diameter: '48', wallThickness: '3.5', length: '6000', weightPerMeter: 3.84, quantity: 200, unit: '支', unitPrice: 5500, createTime: '2024-03-12' }
  ])

  const selectedMaterialId = ref(null)
  const nextMaterialId = ref(6)
  const nextSpecId = ref(11)
  const nextProductId = ref(6)

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

  function selectMaterial(id) {
    selectedMaterialId.value = id
  }

  function addMaterial(data) {
    const newItem = {
      id: nextMaterialId.value++,
      ...data,
      createTime: new Date().toISOString().split('T')[0]
    }
    materials.value.push(newItem)
    return newItem
  }

  function updateMaterial(id, data) {
    const index = materials.value.findIndex(m => m.id === id)
    if (index !== -1) {
      materials.value[index] = { ...materials.value[index], ...data }
      return materials.value[index]
    }
    return null
  }

  function deleteMaterial(id) {
    const index = materials.value.findIndex(m => m.id === id)
    if (index !== -1) {
      materials.value.splice(index, 1)
      specs.value = specs.value.filter(s => s.materialId !== id)
      products.value = products.value.filter(p => p.materialId !== id)
      if (selectedMaterialId.value === id) {
        selectedMaterialId.value = null
      }
      return true
    }
    return false
  }

  function addSpec(data) {
    const material = getMaterialById(data.materialId)
    const newItem = {
      id: nextSpecId.value++,
      materialName: material?.name || '',
      ...data,
      createTime: new Date().toISOString().split('T')[0]
    }
    specs.value.push(newItem)
    return newItem
  }

  function updateSpec(id, data) {
    const index = specs.value.findIndex(s => s.id === id)
    if (index !== -1) {
      const material = getMaterialById(data.materialId || specs.value[index].materialId)
      specs.value[index] = {
        ...specs.value[index],
        ...data,
        materialName: material?.name || specs.value[index].materialName
      }
      return specs.value[index]
    }
    return null
  }

  function deleteSpec(id) {
    const index = specs.value.findIndex(s => s.id === id)
    if (index !== -1) {
      specs.value.splice(index, 1)
      return true
    }
    return false
  }

  function getSpecsByMaterialId(materialId) {
    return specs.value.filter(s => s.materialId === materialId)
  }

  function addProduct(data) {
    const material = getMaterialById(data.materialId)
    const spec = getSpecById(data.specId)
    const newItem = {
      id: nextProductId.value++,
      materialName: material?.name || '',
      diameter: spec?.diameter || '',
      wallThickness: spec?.wallThickness || '',
      length: spec?.length || '',
      weightPerMeter: spec?.weightPerMeter || 0,
      ...data,
      createTime: new Date().toISOString().split('T')[0]
    }
    products.value.push(newItem)
    return newItem
  }

  function updateProduct(id, data) {
    const index = products.value.findIndex(p => p.id === id)
    if (index !== -1) {
      const material = getMaterialById(data.materialId || products.value[index].materialId)
      const spec = getSpecById(data.specId || products.value[index].specId)
      products.value[index] = {
        ...products.value[index],
        ...data,
        materialName: material?.name || products.value[index].materialName,
        diameter: spec?.diameter || products.value[index].diameter,
        wallThickness: spec?.wallThickness || products.value[index].wallThickness,
        length: spec?.length || products.value[index].length,
        weightPerMeter: spec?.weightPerMeter || products.value[index].weightPerMeter
      }
      return products.value[index]
    }
    return null
  }

  function deleteProduct(id) {
    const index = products.value.findIndex(p => p.id === id)
    if (index !== -1) {
      products.value.splice(index, 1)
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
    filteredSpecs,
    filteredProducts,
    materialOptions,
    getMaterialById,
    getSpecById,
    selectMaterial,
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
