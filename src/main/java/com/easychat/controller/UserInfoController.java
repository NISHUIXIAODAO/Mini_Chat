package com.easychat.controller;

import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.entity.ResultVo;
import com.easychat.entity.DTO.request.LoginDTO;
import com.easychat.entity.DTO.request.RegisterDTO;
import com.easychat.service.IUserInfoService;
import com.easychat.webSocket.MessageHandler;
import lombok.extern.slf4j.Slf4j;
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
 * @author scj
 * @since 2025-02-26
 */
@RestController
@RequestMapping("/userInfo")
@Slf4j
public class UserInfoController {
    @Autowired
    private IUserInfoService iUserInfoService;
    @Resource
    private MessageHandler messageHandler;


    @PostMapping("/login")
    public ResultVo<Object> login(@RequestBody LoginDTO loginDTO, HttpServletResponse response, HttpServletRequest request){
        try{
            return iUserInfoService.login(loginDTO,response,request);
        }catch (Exception e){
            log.info("错误：{}",e);
        }
        return ResultVo.failed("发生错误");
    }
    @PostMapping("/register")
    public ResultVo<Object> register(@RequestBody RegisterDTO registerDTO, HttpServletResponse response, HttpServletRequest request){
        try{

            return iUserInfoService.register(registerDTO,response,request);
        }catch (Exception e){
            log.info("错误：{}",e);
        }
        return ResultVo.failed("发生错误");
    }

    @GetMapping("/sendCode")
    public ResultVo<Object> sendCode(String email, HttpServletResponse response, HttpServletRequest request){
        try{
            return iUserInfoService.sendCode(email,response,request);
        }catch (Exception e){
            log.info("错误：{}",e);
        }
        return ResultVo.failed("发生错误：验证码发送失败");
    }



    @GetMapping("/test")
    public ResultVo<Object> test(){
        MessageSendDTO sendDto = new MessageSendDTO();
        sendDto.setMessageContent("你好：" + System.currentTimeMillis());
        messageHandler.sendMessage(sendDto);
        return ResultVo.success();
    }

}
