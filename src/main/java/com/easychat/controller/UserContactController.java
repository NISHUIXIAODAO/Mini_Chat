package com.easychat.controller;

import com.easychat.entity.DTO.request.ApplyGroupAddDTO;
import com.easychat.entity.DTO.request.DisposeApplyDTO;
import com.easychat.entity.ResultVo;
import com.easychat.service.IUserContactService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * 前端控制器
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

    @GetMapping("/getContactList")
    public ResultVo<Object> getContactList(HttpServletRequest request, HttpServletResponse response) {
        try {
            return iUserContactService.getContactList(request, response);
        } catch (Exception e) {
            log.error("错误：{}", e);
        }
        return ResultVo.failed("UserContactController-getContactList 发生错误");
    }

    @PostMapping("/applyFriendAdd")
    public ResultVo<Object> applyFriendAdd(HttpServletRequest request, Integer contactId, String applyInfo) {
        try {
            return iUserContactService.applyFriendAdd(request.getHeader("token"), contactId, applyInfo);
        } catch (Exception e) {
            log.error("错误：{}", e);
        }
        return ResultVo.failed("UserContactController-applyFriendAdd 发生错误");
    }

    @PostMapping("/applyGroupAdd")
    public ResultVo<Object> applyGroupAdd(@RequestBody ApplyGroupAddDTO applyGroupAddDTO, HttpServletRequest request, HttpServletResponse response) {
        try {
            return iUserContactService.applyGroupAdd(applyGroupAddDTO, request, response);
        } catch (Exception e) {
            log.error("错误：{}", e);
        }
        return ResultVo.failed("UserContactController-applyGroupAdd 发生错误");
    }

    @PostMapping("/disposeApply")
    public ResultVo<Object> disposeApply(@RequestBody DisposeApplyDTO disposeApplyDTO, HttpServletRequest request, HttpServletResponse response) {
        try {
            return iUserContactService.disposeApply(disposeApplyDTO, request, response);
        } catch (Exception e) {
            log.error("错误：{}", e);
        }
        return ResultVo.failed("");
    }

}
