package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.dto.PurchaseContractDTO;
import com.gangetong.entity.InTransitStock;
import com.gangetong.entity.Partner;
import com.gangetong.entity.Product;
import com.gangetong.entity.PurchaseContract;
import com.gangetong.entity.PurchaseContractItem;
import com.gangetong.entity.SteelSpec;
import com.gangetong.entity.User;
import com.gangetong.mapper.InTransitStockMapper;
import com.gangetong.mapper.PartnerMapper;
import com.gangetong.mapper.ProductMapper;
import com.gangetong.mapper.PurchaseContractItemMapper;
import com.gangetong.mapper.PurchaseContractMapper;
import com.gangetong.mapper.SteelSpecMapper;
import com.gangetong.mapper.UserMapper;
import com.gangetong.service.InTransitStockService;
import com.gangetong.service.PurchaseContractItemService;
import com.gangetong.service.PurchaseContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PurchaseContractServiceImpl extends ServiceImpl<PurchaseContractMapper, PurchaseContract> implements PurchaseContractService {

    @Autowired
    private PartnerMapper partnerMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private SteelSpecMapper steelSpecMapper;

    @Autowired
    private PurchaseContractItemService contractItemService;

    @Autowired
    private InTransitStockService inTransitStockService;

    @Autowired
    private InTransitStockMapper inTransitStockMapper;

    @Autowired
    private PurchaseContractItemMapper contractItemMapper;

    private static final Map<String, String> STATUS_TEXT_MAP = new HashMap<>();

    static {
        STATUS_TEXT_MAP.put("DRAFT", "草稿");
        STATUS_TEXT_MAP.put("AUDITED", "已审核");
        STATUS_TEXT_MAP.put("COMPLETED", "已完成");
    }

    @Override
    public List<PurchaseContract> listAll() {
        LambdaQueryWrapper<PurchaseContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(PurchaseContract::getCreateTime);
        List<PurchaseContract> list = this.list(wrapper);
        fillRelatedData(list);
        return list;
    }

    @Override
    public List<PurchaseContract> listByStatus(String status) {
        LambdaQueryWrapper<PurchaseContract> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PurchaseContract::getStatus, status);
        }
        wrapper.orderByDesc(PurchaseContract::getCreateTime);
        List<PurchaseContract> list = this.list(wrapper);
        fillRelatedData(list);
        return list;
    }

    private void fillRelatedData(List<PurchaseContract> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return;
        }

        List<Long> supplierIds = contracts.stream()
                .map(PurchaseContract::getSupplierId)
                .distinct()
                .collect(Collectors.toList());
        List<Partner> suppliers = partnerMapper.selectBatchIds(supplierIds);
        Map<Long, Partner> supplierMap = suppliers.stream()
                .collect(Collectors.toMap(Partner::getId, p -> p));

        List<Long> createByIds = contracts.stream()
                .map(PurchaseContract::getCreateBy)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> userNameMap = new HashMap<>();
        if (!createByIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(createByIds);
            userNameMap = users.stream()
                    .collect(Collectors.toMap(User::getId, User::getNickname));
        }

        for (PurchaseContract contract : contracts) {
            Partner supplier = supplierMap.get(contract.getSupplierId());
            if (supplier != null) {
                contract.setSupplierName(supplier.getName());
                contract.setSupplierCode(supplier.getCode());
            }
            contract.setStatusText(STATUS_TEXT_MAP.get(contract.getStatus()));
            if (contract.getCreateBy() != null) {
                contract.setCreateByName(userNameMap.get(contract.getCreateBy()));
            }
        }
    }

    @Override
    public PurchaseContract getDetailById(Long id) {
        PurchaseContract contract = this.getById(id);
        if (contract != null) {
            Partner supplier = partnerMapper.selectById(contract.getSupplierId());
            if (supplier != null) {
                contract.setSupplierName(supplier.getName());
                contract.setSupplierCode(supplier.getCode());
            }
            contract.setStatusText(STATUS_TEXT_MAP.get(contract.getStatus()));
            if (contract.getCreateBy() != null) {
                User user = userMapper.selectById(contract.getCreateBy());
                if (user != null) {
                    contract.setCreateByName(user.getNickname());
                }
            }
            List<PurchaseContractItem> items = contractItemService.listByContractId(id);
            contract.setItems(items);
        }
        return contract;
    }

    @Override
    @Transactional
    public boolean add(PurchaseContractDTO dto) {
        PurchaseContract contract = new PurchaseContract();
        contract.setContractNo(dto.getContractNo() != null ? dto.getContractNo() : generateContractNo());
        contract.setSupplierId(dto.getSupplierId());
        contract.setSignDate(dto.getSignDate());
        contract.setDeliveryDate(dto.getDeliveryDate());
        contract.setStatus("DRAFT");
        contract.setTotalAmount(dto.getTotalAmount() != null ? dto.getTotalAmount() : BigDecimal.ZERO);
        contract.setTotalWeight(dto.getTotalWeight() != null ? dto.getTotalWeight() : BigDecimal.ZERO);
        contract.setRemark(dto.getRemark());
        contract.setCreateBy(dto.getCreateBy());
        contract.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        contract.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        boolean saved = this.save(contract);
        if (!saved) {
            return false;
        }

        saveContractItems(contract.getId(), dto.getItems());
        recalculateContractAmount(contract.getId());

        return true;
    }

    @Override
    @Transactional
    public boolean update(PurchaseContractDTO dto) {
        PurchaseContract contract = this.getById(dto.getId());
        if (contract == null) {
            return false;
        }
        if (!"DRAFT".equals(contract.getStatus())) {
            throw new RuntimeException("只有草稿状态的合同可以修改");
        }

        contract.setSupplierId(dto.getSupplierId());
        contract.setSignDate(dto.getSignDate());
        contract.setDeliveryDate(dto.getDeliveryDate());
        contract.setTotalAmount(dto.getTotalAmount() != null ? dto.getTotalAmount() : BigDecimal.ZERO);
        contract.setTotalWeight(dto.getTotalWeight() != null ? dto.getTotalWeight() : BigDecimal.ZERO);
        contract.setRemark(dto.getRemark());
        contract.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        boolean updated = this.updateById(contract);
        if (!updated) {
            return false;
        }

        if (dto.getDeletedItemIds() != null && !dto.getDeletedItemIds().isEmpty()) {
            contractItemService.removeByIds(dto.getDeletedItemIds());
        }

        saveContractItems(contract.getId(), dto.getItems());
        recalculateContractAmount(contract.getId());

        return true;
    }

    private void saveContractItems(Long contractId, List<PurchaseContractItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            PurchaseContractItem item = items.get(i);
            item.setContractId(contractId);
            item.setSortNo(i + 1);

            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                item.setMaterialId(product.getMaterialId());
                item.setSpecId(product.getSpecId());
            }

            if (item.getQuantity() == null) {
                item.setQuantity(0);
            }
            if (item.getUnitPrice() == null) {
                item.setUnitPrice(BigDecimal.ZERO);
            }

            BigDecimal amount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setAmount(amount);

            if (item.getSpecId() != null && item.getQuantity() != null) {
                SteelSpec spec = steelSpecMapper.selectById(item.getSpecId());
                if (spec != null && spec.getWeightPerMeter() != null && spec.getLength() != null) {
                    try {
                        BigDecimal lengthM = new BigDecimal(spec.getLength()).divide(BigDecimal.valueOf(1000), 4, BigDecimal.ROUND_HALF_UP);
                        BigDecimal weight = spec.getWeightPerMeter()
                                .multiply(lengthM)
                                .multiply(BigDecimal.valueOf(item.getQuantity()));
                        item.setWeight(weight);
                    } catch (Exception e) {
                        item.setWeight(BigDecimal.ZERO);
                    }
                } else {
                    item.setWeight(BigDecimal.ZERO);
                }
            } else {
                item.setWeight(BigDecimal.ZERO);
            }

            if (item.getId() == null) {
                item.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                contractItemService.save(item);
            } else {
                contractItemService.updateById(item);
            }
        }
    }

    private void recalculateContractAmount(Long contractId) {
        List<PurchaseContractItem> items = contractItemService.listByContractId(contractId);
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (PurchaseContractItem item : items) {
            if (item.getAmount() != null) {
                totalAmount = totalAmount.add(item.getAmount());
            }
            if (item.getWeight() != null) {
                totalWeight = totalWeight.add(item.getWeight());
            }
        }

        PurchaseContract contract = new PurchaseContract();
        contract.setId(contractId);
        contract.setTotalAmount(totalAmount);
        contract.setTotalWeight(totalWeight);
        this.updateById(contract);
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        PurchaseContract contract = this.getById(id);
        if (contract == null) {
            return false;
        }
        if (!"DRAFT".equals(contract.getStatus())) {
            throw new RuntimeException("只有草稿状态的合同可以删除");
        }

        contractItemService.deleteByContractId(id);
        inTransitStockService.deleteByContractId(id);
        return this.removeById(id);
    }

    @Override
    @Transactional
    public boolean audit(Long id) {
        PurchaseContract contract = this.getById(id);
        if (contract == null) {
            return false;
        }
        if (!"DRAFT".equals(contract.getStatus())) {
            throw new RuntimeException("只有草稿状态的合同可以审核");
        }

        List<PurchaseContractItem> items = contractItemService.listByContractId(id);
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("合同明细不能为空");
        }

        for (PurchaseContractItem item : items) {
            inTransitStockService.addInTransitStock(item, id);
        }

        contract.setStatus("AUDITED");
        contract.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return this.updateById(contract);
    }

    @Override
    @Transactional
    public boolean complete(Long id) {
        PurchaseContract contract = this.getById(id);
        if (contract == null) {
            return false;
        }
        if (!"AUDITED".equals(contract.getStatus())) {
            throw new RuntimeException("只有已审核状态的合同可以完成");
        }

        List<PurchaseContractItem> items = contractItemService.listByContractId(id);
        boolean allCompleted = true;
        for (PurchaseContractItem item : items) {
            int remaining = (item.getQuantity() != null ? item.getQuantity() : 0)
                    - (item.getInStockQuantity() != null ? item.getInStockQuantity() : 0);
            if (remaining > 0) {
                allCompleted = false;
                break;
            }
        }

        if (!allCompleted) {
            throw new RuntimeException("还有未完成入库的明细，不能标记为完成");
        }

        inTransitStockService.deleteByContractId(id);

        contract.setStatus("COMPLETED");
        contract.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return this.updateById(contract);
    }

    @Override
    public String generateContractNo() {
        String prefix = "CG" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<PurchaseContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(PurchaseContract::getContractNo, prefix + "%");
        wrapper.orderByDesc(PurchaseContract::getContractNo);
        wrapper.last("limit 1");
        PurchaseContract last = this.getOne(wrapper);

        int sequence = 1;
        if (last != null && last.getContractNo() != null) {
            try {
                String seqStr = last.getContractNo().substring(prefix.length());
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (Exception e) {
                sequence = 1;
            }
        }
        return prefix + String.format("%04d", sequence);
    }
}
