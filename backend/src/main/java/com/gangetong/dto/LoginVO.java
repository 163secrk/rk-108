package com.gangetong.dto;

import com.gangetong.entity.User;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录返回结果VO
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * JWT Token
     */
    private String token;

    /**
     * 用户信息（不包含密码）
     */
    private User userInfo;
}
