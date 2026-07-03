package com.easychat.controller;

import com.easychat.entity.ResultVo;
import com.easychat.entity.DTO.request.SetGroupDTO;
import com.easychat.service.IGroupInfoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author my
 * @since 2025-02-27
 */
@RestController
@RequestMapping("/groupInfo")
public class GroupInfoController {

    private final IGroupInfoService iGroupInfoService;

    public GroupInfoController(IGroupInfoService iGroupInfoService) {
        this.iGroupInfoService = iGroupInfoService;
    }

    @PostMapping("/setGroup")
    public ResultVo setGroup(@RequestBody SetGroupDTO setGroupDTO,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        return iGroupInfoService.setGroup(setGroupDTO, request, response);
    }

}
