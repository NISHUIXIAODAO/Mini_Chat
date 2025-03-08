package com.easychat.service;

import com.easychat.entity.ResultVo;
import com.easychat.entity.DO.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.easychat.entity.DTO.request.LoginDTO;
import com.easychat.entity.DTO.request.RegisterDTO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * 用户信息 服务类
 * </p>
 *
 * @author scj
 * @since 2025-02-26
 */
public interface IUserInfoService extends IService<UserInfo> {
    ResultVo login(LoginDTO loginDTO, HttpServletResponse response, HttpServletRequest request);

    ResultVo register(RegisterDTO registerDTO, HttpServletResponse response, HttpServletRequest request);

    ResultVo sendCode(String email, HttpServletResponse response, HttpServletRequest request);
}
