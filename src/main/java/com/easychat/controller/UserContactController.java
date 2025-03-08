package com.easychat.controller;

import com.easychat.entity.ResultVo;
import com.easychat.service.IUserContactService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author my
 * @since 2025-03-01
 */
@RestController
@RequestMapping("/userContact")
@Slf4j
public class UserContactController {
    @Autowired
    private IUserContactService iUserContactService;


    @PostMapping("/applyAdd")
    public ResultVo<Object> applyAdd(String token, Integer contactId, String applyInfo){
        try {
            return iUserContactService.applyAdd(token,contactId,applyInfo);
        }catch (Exception e){
            log.error("错误：{}",e);
        }
        return ResultVo.failed("UserContactController-applyAdd 发生错误");
    }

    @PostMapping("/disposeApply")
    public ResultVo<Object> disposeApply(Integer applyUserId , String token , Integer status){
        try{
            return iUserContactService.disposeApply(applyUserId,token,status);
        }catch (Exception e){
            log.error("错误：{}",e);
        }
        return ResultVo.failed("");
    }

}
