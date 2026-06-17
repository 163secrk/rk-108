package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.dto.LoginDTO;
import com.gangetong.dto.LoginVO;
import com.gangetong.entity.User;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户登录
     *
     * @param loginDTO 登录参数
     * @return 登录结果（包含token和用户信息）
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    User getByUsername(String username);

    /**
     * 根据ID查询用户信息（不包含密码）
     *
     * @param id 用户ID
     * @return 用户信息
     */
    User getUserInfoById(Long id);
}
