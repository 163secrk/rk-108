package com.gangetong.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单实体类
 */
@Data
public class Menu implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 菜单路径
     */
    private String path;

    /**
     * 菜单名称（显示名称）
     */
    private String name;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 子菜单列表
     */
    private List<Menu> children;

    public Menu() {
    }

    public Menu(String path, String name, String icon) {
        this.path = path;
        this.name = name;
        this.icon = icon;
    }

    public Menu(String path, String name, String icon, List<Menu> children) {
        this.path = path;
        this.name = name;
        this.icon = icon;
        this.children = children;
    }
}
