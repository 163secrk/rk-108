package com.gangetong.controller;

import com.gangetong.common.Result;
import com.gangetong.entity.Material;
import com.gangetong.entity.Partner;
import com.gangetong.entity.Product;
import com.gangetong.entity.SteelSpec;
import com.gangetong.entity.Warehouse;
import com.gangetong.service.MaterialService;
import com.gangetong.service.PartnerService;
import com.gangetong.service.ProductService;
import com.gangetong.service.SteelSpecService;
import com.gangetong.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/basic")
public class BasicDataController {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private SteelSpecService steelSpecService;

    @Autowired
    private ProductService productService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private PartnerService partnerService;

    // ========================================
    // 材质 Material 接口
    // ========================================

    @GetMapping("/material/list")
    public Result<List<Material>> listMaterials() {
        List<Material> list = materialService.listAll();
        return Result.success(list);
    }

    @GetMapping("/material/{id}")
    public Result<Material> getMaterial(@PathVariable Long id) {
        Material material = materialService.getById(id);
        if (material == null) {
            return Result.error("材质不存在");
        }
        return Result.success(material);
    }

    @PostMapping("/material")
    public Result<Material> addMaterial(@RequestBody Material material) {
        if (material.getName() == null || material.getName().trim().isEmpty()) {
            return Result.error("材质名称不能为空");
        }
        boolean success = materialService.add(material);
        if (success) {
            return Result.success("添加成功", material);
        }
        return Result.error("添加失败");
    }

    @PutMapping("/material")
    public Result<Material> updateMaterial(@RequestBody Material material) {
        if (material.getId() == null) {
            return Result.error("ID不能为空");
        }
        boolean success = materialService.update(material);
        if (success) {
            return Result.success("修改成功", materialService.getById(material.getId()));
        }
        return Result.error("修改失败");
    }

    @DeleteMapping("/material/{id}")
    @Transactional
    public Result<Void> deleteMaterial(@PathVariable Long id) {
        steelSpecService.deleteByMaterialId(id);
        productService.deleteByMaterialId(id);
        boolean success = materialService.delete(id);
        if (success) {
            return Result.success("删除成功", null);
        }
        return Result.error("删除失败");
    }

    // ========================================
    // 规格 SteelSpec 接口
    // ========================================

    @GetMapping("/spec/list")
    public Result<List<SteelSpec>> listSpecs(@RequestParam(required = false) Long materialId) {
        List<SteelSpec> list;
        if (materialId != null) {
            list = steelSpecService.listByMaterialId(materialId);
        } else {
            list = steelSpecService.listAll();
        }
        return Result.success(list);
    }

    @GetMapping("/spec/{id}")
    public Result<SteelSpec> getSpec(@PathVariable Long id) {
        SteelSpec spec = steelSpecService.getById(id);
        if (spec == null) {
            return Result.error("规格不存在");
        }
        return Result.success(spec);
    }

    @PostMapping("/spec")
    public Result<SteelSpec> addSpec(@RequestBody SteelSpec spec) {
        if (spec.getMaterialId() == null) {
            return Result.error("材质ID不能为空");
        }
        if (spec.getDiameter() == null || spec.getDiameter().trim().isEmpty()) {
            return Result.error("直径不能为空");
        }
        if (spec.getWallThickness() == null || spec.getWallThickness().trim().isEmpty()) {
            return Result.error("壁厚不能为空");
        }
        if (spec.getWeightPerMeter() == null) {
            return Result.error("每米理论重量不能为空");
        }
        boolean success = steelSpecService.add(spec);
        if (success) {
            return Result.success("添加成功", steelSpecService.getById(spec.getId()));
        }
        return Result.error("添加失败");
    }

    @PutMapping("/spec")
    public Result<SteelSpec> updateSpec(@RequestBody SteelSpec spec) {
        if (spec.getId() == null) {
            return Result.error("ID不能为空");
        }
        boolean success = steelSpecService.update(spec);
        if (success) {
            return Result.success("修改成功", steelSpecService.getById(spec.getId()));
        }
        return Result.error("修改失败");
    }

    @DeleteMapping("/spec/{id}")
    public Result<Void> deleteSpec(@PathVariable Long id) {
        boolean success = steelSpecService.delete(id);
        if (success) {
            return Result.success("删除成功", null);
        }
        return Result.error("删除失败");
    }

    // ========================================
    // 商品 Product 接口
    // ========================================

    @GetMapping("/product/list")
    public Result<List<Product>> listProducts(@RequestParam(required = false) Long materialId) {
        List<Product> list;
        if (materialId != null) {
            list = productService.listByMaterialId(materialId);
        } else {
            list = productService.listAll();
        }
        return Result.success(list);
    }

    @GetMapping("/product/{id}")
    public Result<Product> getProduct(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            return Result.error("商品不存在");
        }
        return Result.success(product);
    }

    @PostMapping("/product")
    public Result<Product> addProduct(@RequestBody Product product) {
        if (product.getProductCode() == null || product.getProductCode().trim().isEmpty()) {
            return Result.error("商品编号不能为空");
        }
        if (product.getProductName() == null || product.getProductName().trim().isEmpty()) {
            return Result.error("商品名称不能为空");
        }
        if (product.getMaterialId() == null) {
            return Result.error("材质ID不能为空");
        }
        if (product.getSpecId() == null) {
            return Result.error("规格ID不能为空");
        }
        boolean success = productService.add(product);
        if (success) {
            return Result.success("添加成功", productService.getById(product.getId()));
        }
        return Result.error("添加失败");
    }

    @PutMapping("/product")
    public Result<Product> updateProduct(@RequestBody Product product) {
        if (product.getId() == null) {
            return Result.error("ID不能为空");
        }
        boolean success = productService.update(product);
        if (success) {
            return Result.success("修改成功", productService.getById(product.getId()));
        }
        return Result.error("修改失败");
    }

    @DeleteMapping("/product/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        boolean success = productService.delete(id);
        if (success) {
            return Result.success("删除成功", null);
        }
        return Result.error("删除失败");
    }

    // ========================================
    // 仓库 Warehouse 接口
    // ========================================

    @GetMapping("/warehouse/list")
    public Result<List<Warehouse>> listWarehouses() {
        List<Warehouse> list = warehouseService.listAll();
        return Result.success(list);
    }

    @GetMapping("/warehouse/tree")
    public Result<List<Warehouse>> warehouseTree() {
        List<Warehouse> tree = warehouseService.treeList();
        return Result.success(tree);
    }

    @GetMapping("/warehouse/{id}")
    public Result<Warehouse> getWarehouse(@PathVariable Long id) {
        Warehouse warehouse = warehouseService.getById(id);
        if (warehouse == null) {
            return Result.error("仓库不存在");
        }
        return Result.success(warehouse);
    }

    @PostMapping("/warehouse")
    public Result<Warehouse> addWarehouse(@RequestBody Warehouse warehouse) {
        if (warehouse.getName() == null || warehouse.getName().trim().isEmpty()) {
            return Result.error("仓库名称不能为空");
        }
        if (warehouse.getCode() == null || warehouse.getCode().trim().isEmpty()) {
            return Result.error("仓库编码不能为空");
        }
        boolean success = warehouseService.add(warehouse);
        if (success) {
            return Result.success("添加成功", warehouseService.getById(warehouse.getId()));
        }
        return Result.error("添加失败");
    }

    @PutMapping("/warehouse")
    public Result<Warehouse> updateWarehouse(@RequestBody Warehouse warehouse) {
        if (warehouse.getId() == null) {
            return Result.error("ID不能为空");
        }
        boolean success = warehouseService.update(warehouse);
        if (success) {
            return Result.success("修改成功", warehouseService.getById(warehouse.getId()));
        }
        return Result.error("修改失败");
    }

    @DeleteMapping("/warehouse/{id}")
    @Transactional
    public Result<Void> deleteWarehouse(@PathVariable Long id) {
        boolean success = warehouseService.delete(id);
        if (success) {
            return Result.success("删除成功", null);
        }
        return Result.error("删除失败");
    }

    // ========================================
    // 往来单位 Partner 接口
    // ========================================

    @GetMapping("/partner/list")
    public Result<List<Partner>> listPartners(@RequestParam(required = false) String type) {
        List<Partner> list;
        if (type != null && !type.isEmpty()) {
            list = partnerService.listByType(type);
        } else {
            list = partnerService.listAll();
        }
        return Result.success(list);
    }

    @GetMapping("/partner/{id}")
    public Result<Partner> getPartner(@PathVariable Long id) {
        Partner partner = partnerService.getById(id);
        if (partner == null) {
            return Result.error("往来单位不存在");
        }
        return Result.success(partner);
    }

    @PostMapping("/partner")
    public Result<Partner> addPartner(@RequestBody Partner partner) {
        if (partner.getName() == null || partner.getName().trim().isEmpty()) {
            return Result.error("单位名称不能为空");
        }
        if (partner.getCode() == null || partner.getCode().trim().isEmpty()) {
            return Result.error("单位编码不能为空");
        }
        if (partner.getType() == null || partner.getType().trim().isEmpty()) {
            return Result.error("单位类型不能为空");
        }
        boolean success = partnerService.add(partner);
        if (success) {
            return Result.success("添加成功", partnerService.getById(partner.getId()));
        }
        return Result.error("添加失败");
    }

    @PutMapping("/partner")
    public Result<Partner> updatePartner(@RequestBody Partner partner) {
        if (partner.getId() == null) {
            return Result.error("ID不能为空");
        }
        boolean success = partnerService.update(partner);
        if (success) {
            return Result.success("修改成功", partnerService.getById(partner.getId()));
        }
        return Result.error("修改失败");
    }

    @DeleteMapping("/partner/{id}")
    public Result<Void> deletePartner(@PathVariable Long id) {
        boolean success = partnerService.delete(id);
        if (success) {
            return Result.success("删除成功", null);
        }
        return Result.error("删除失败");
    }
}
