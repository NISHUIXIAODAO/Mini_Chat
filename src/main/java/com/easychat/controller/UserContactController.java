package com.easychat.controller;

import com.easychat.entity.DTO.request.ApplyGroupAddDTO;
import com.easychat.entity.DTO.request.DisposeApplyDTO;
import com.easychat.entity.ResultVo;
import com.easychat.service.IJWTService;
import com.easychat.service.IUserContactService;
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
public class UserContactController {
    @Autowired
    private IUserContactService iUserContactService;
    @Autowired
    private IJWTService jwtService;

    @GetMapping("/getContactList")
    public ResultVo<Object> getContactList(HttpServletRequest request, HttpServletResponse response) {
        return iUserContactService.getContactList(request, response);
    }

    @PostMapping("/applyFriendAdd")
    public ResultVo<Object> applyFriendAdd(HttpServletRequest request, Integer contactId, String applyInfo) {
        return iUserContactService.applyFriendAdd(jwtService.extractToken(request), contactId, applyInfo);
    }

    @PostMapping("/applyGroupAdd")
    public ResultVo<Object> applyGroupAdd(@RequestBody ApplyGroupAddDTO applyGroupAddDTO, HttpServletRequest request, HttpServletResponse response) {
        return iUserContactService.applyGroupAdd(applyGroupAddDTO, request, response);
    }

    @PostMapping("/disposeApply")
    public ResultVo<Object> disposeApply(@RequestBody DisposeApplyDTO disposeApplyDTO, HttpServletRequest request, HttpServletResponse response) {
        return iUserContactService.disposeApply(disposeApplyDTO, request, response);
    }

}
