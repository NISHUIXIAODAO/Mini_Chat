package com.easychat.controller;

import com.easychat.entity.DTO.response.UserApplyListResponseDTO;
import com.easychat.service.IUserContactApplyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 联系人申请
 * </p>
 *
 * @author my
 * @since 2025-03-03
 */
@Slf4j
@RestController
@RequestMapping("/userContactApply")
public class UserContactApplyController {
    @Autowired
    private IUserContactApplyService userContactApplyService;

    @GetMapping("/getApplyList")
    public List<UserApplyListResponseDTO> getApplyList(@RequestParam String token){
        try{
            return userContactApplyService.getApplyList(token);
        }catch (Exception e){
            log.error("处理好友申请错误：{}",e);
        }
        return null;
    }

}
