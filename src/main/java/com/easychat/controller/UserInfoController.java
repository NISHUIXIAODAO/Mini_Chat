package com.easychat.controller;

import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.entity.ResultVo;
import com.easychat.entity.DTO.request.LoginDTO;
import com.easychat.entity.DTO.request.RegisterDTO;
import com.easychat.kafka.KafkaMessageProducer;
import com.easychat.service.IJWTService;
import com.easychat.service.IUserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * 用户信息 前端控制器
 * </p>
 *
 * @author may
 * @since 2025-02-26
 */
@RestController
@RequestMapping("/userInfo")
public class UserInfoController {
    @Autowired
    private IUserInfoService iUserInfoService;
    @Resource
    private KafkaMessageProducer kafkaMessageProducer;
    @Autowired
    private IJWTService jwtService;


    @PostMapping("/login")
    public ResultVo<Object> login(@RequestBody LoginDTO loginDTO, HttpServletResponse response, HttpServletRequest request) {
        return iUserInfoService.login(loginDTO, response, request);
    }

    @PostMapping("/logout")
    public ResultVo<Object> logout(HttpServletRequest request){
        String token = jwtService.extractToken(request);
        // 主动退出时将当前 token 拉黑，直到 JWT 自身过期为止。
        jwtService.blacklistToken(token);
        return ResultVo.success("退出登录成功");
    }

    @PostMapping("/register")
    public ResultVo<Object> register(@RequestBody RegisterDTO registerDTO, HttpServletResponse response, HttpServletRequest request) {
        return iUserInfoService.register(registerDTO, response, request);
    }

    @GetMapping("/sendCode")
    public ResultVo<Object> sendCode(String email, HttpServletResponse response, HttpServletRequest request) {
        return iUserInfoService.sendCode(email, response, request);
    }

}
