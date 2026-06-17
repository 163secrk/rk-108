package com.gangetong.controller;

import com.gangetong.common.Result;
import com.gangetong.dto.LoginDTO;
import com.gangetong.dto.LoginVO;
import com.gangetong.entity.Menu;
import com.gangetong.entity.User;
import com.gangetong.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证控制器
 * 提供登录、登出、获取用户信息、获取菜单等接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     * POST /api/auth/login
     *
     * @param loginDTO 登录参数（username, password）
     * @return 登录结果（包含token和用户信息）
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        try {
            LoginVO loginVO = userService.login(loginDTO);
            return Result.success("登录成功", loginVO);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前登录用户信息
     * GET /api/auth/userInfo
     * 需要携带有效Token
     *
     * @param request HttpServletRequest（从拦截器注入的属性中获取用户信息）
     * @return 用户信息（不包含密码）
     */
    @GetMapping("/userInfo")
    public Result<User> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized("用户未登录");
        }
        User user = userService.getUserInfoById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 用户登出
     * POST /api/auth/logout
     * （JWT是无状态的，登出主要由前端清除Token实现，后端仅返回成功提示）
     *
     * @return 登出成功
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("登出成功", null);
    }

    /**
     * 获取当前用户的菜单列表
     * GET /api/auth/menus
     * 根据当前用户角色返回不同的菜单权限
     *
     * @param request HttpServletRequest（从拦截器注入的属性中获取用户角色）
     * @return 菜单树列表
     */
    @GetMapping("/menus")
    public Result<List<Menu>> getMenus(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (role == null) {
            return Result.unauthorized("用户未登录");
        }

        List<Menu> menus = buildMenusByRole(role);
        return Result.success(menus);
    }

    /**
     * 根据角色构建菜单树
     *
     * @param role 用户角色
     * @return 菜单列表
     */
    private List<Menu> buildMenusByRole(String role) {
        List<Menu> allMenus = buildAllMenus();
        List<Menu> result = new ArrayList<>();

        switch (role) {
            case "ADMIN":
                // ADMIN：拥有所有菜单
                return allMenus;

            case "PURCHASER":
                // PURCHASER：基础资料(除系统设置的其他)、采购管理全部、库存管理(查询+调拨)
                for (Menu menu : allMenus) {
                    switch (menu.getPath()) {
                        case "basic":
                            // 保留基础资料下的所有子菜单（排除系统设置是另一个顶级菜单）
                            result.add(copyMenu(menu));
                            break;
                        case "purchase":
                            // 采购管理全部
                            result.add(copyMenu(menu));
                            break;
                        case "inventory":
                            // 库存管理：仅保留查询+调拨
                            result.add(filterMenuChildren(menu, Arrays.asList("query", "transfer")));
                            break;
                        default:
                            break;
                    }
                }
                return result;

            case "SALER":
                // SALER：基础资料(钢材参数/商品档案/往来单位)、库存管理(查询)、销售管理全部
                for (Menu menu : allMenus) {
                    switch (menu.getPath()) {
                        case "basic":
                            // 基础资料：钢材参数/商品档案/往来单位
                            result.add(filterMenuChildren(menu, Arrays.asList("steelParam", "product", "partner")));
                            break;
                        case "inventory":
                            // 库存管理：仅查询
                            result.add(filterMenuChildren(menu, Arrays.asList("query")));
                            break;
                        case "sales":
                            // 销售管理全部
                            result.add(copyMenu(menu));
                            break;
                        default:
                            break;
                    }
                }
                return result;

            case "FINANCE":
                // FINANCE：基础资料(钢材参数/商品档案/仓库设置/往来单位)、采购管理(采购合同)、
                // 销售管理(订单)、财务管理全部、数据看板
                for (Menu menu : allMenus) {
                    switch (menu.getPath()) {
                        case "basic":
                            // 基础资料：钢材参数/商品档案/仓库设置/往来单位
                            result.add(filterMenuChildren(menu, Arrays.asList("steelParam", "product", "warehouse", "partner")));
                            break;
                        case "purchase":
                            // 采购管理：仅采购合同
                            result.add(filterMenuChildren(menu, Arrays.asList("contract")));
                            break;
                        case "sales":
                            // 销售管理：仅订单
                            result.add(filterMenuChildren(menu, Arrays.asList("order")));
                            break;
                        case "finance":
                            // 财务管理全部
                            result.add(copyMenu(menu));
                            break;
                        case "dashboard":
                            // 数据看板
                            result.add(copyMenu(menu));
                            break;
                        default:
                            break;
                    }
                }
                return result;

            default:
                // 未知角色返回空菜单
                return new ArrayList<>();
        }
    }

    /**
     * 构建完整的菜单树（所有菜单）
     */
    private List<Menu> buildAllMenus() {
        List<Menu> menus = new ArrayList<>();

        // 基础资料
        menus.add(new Menu("basic", "基础资料", "icon-storage", Arrays.asList(
                new Menu("steelParam", "钢材参数", "icon-file"),
                new Menu("product", "商品档案", "icon-sku"),
                new Menu("warehouse", "仓库设置", "icon-building"),
                new Menu("partner", "往来单位", "icon-user-group")
        )));

        // 采购管理
        menus.add(new Menu("purchase", "采购管理", "icon-cart", Arrays.asList(
                new Menu("contract", "采购合同", "icon-file-copy"),
                new Menu("stockIn", "入库", "icon-import")
        )));

        // 库存管理
        menus.add(new Menu("inventory", "库存管理", "icon-list", Arrays.asList(
                new Menu("query", "库存查询", "icon-search"),
                new Menu("transfer", "调拨", "icon-exchange"),
                new Menu("check", "盘点", "icon-thunderbolt")
        )));

        // 销售管理
        menus.add(new Menu("sales", "销售管理", "icon-tag", Arrays.asList(
                new Menu("order", "销售订单", "icon-file-document"),
                new Menu("stockOut", "出库", "icon-export")
        )));

        // 财务管理
        menus.add(new Menu("finance", "财务管理", "icon-yen-circle", Arrays.asList(
                new Menu("receivablePayable", "应收应付", "icon-account"),
                new Menu("cost", "成本核算", "icon-calculator")
        )));

        // 数据看板
        menus.add(new Menu("dashboard", "数据看板", "icon-dashboard"));

        // 系统设置
        menus.add(new Menu("system", "系统设置", "icon-settings"));

        return menus;
    }

    /**
     * 复制菜单（深拷贝）
     */
    private Menu copyMenu(Menu source) {
        if (source == null) {
            return null;
        }
        Menu target = new Menu();
        target.setPath(source.getPath());
        target.setName(source.getName());
        target.setIcon(source.getIcon());
        if (source.getChildren() != null) {
            List<Menu> children = source.getChildren().stream()
                    .map(this::copyMenu)
                    .collect(Collectors.toList());
            target.setChildren(children);
        }
        return target;
    }

    /**
     * 过滤菜单子节点（仅保留指定path的子菜单）
     *
     * @param source       原菜单
     * @param allowedPaths 允许的子菜单path列表
     * @return 过滤后的菜单（如果没有可用子菜单则返回null）
     */
    private Menu filterMenuChildren(Menu source, List<String> allowedPaths) {
        if (source == null || source.getChildren() == null) {
            return source;
        }
        Menu target = new Menu();
        target.setPath(source.getPath());
        target.setName(source.getName());
        target.setIcon(source.getIcon());

        List<Menu> filteredChildren = source.getChildren().stream()
                .filter(child -> allowedPaths.contains(child.getPath()))
                .map(this::copyMenu)
                .collect(Collectors.toList());

        if (filteredChildren.isEmpty()) {
            // 如果过滤后没有子菜单，则整个菜单不显示
            return null;
        }
        target.setChildren(filteredChildren);
        return target;
    }
}
