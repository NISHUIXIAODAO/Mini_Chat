package com.easychat.controller;

import com.easychat.entity.ResultVo;
import com.easychat.entity.DTO.request.SetGroupDTO;
import com.easychat.service.IGroupInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 * @author scj
 * @since 2025-02-27
 */
@RestController
@RequestMapping("/groupInfo")
@Slf4j
public class GroupInfoController {

    @Autowired
    IGroupInfoService iGroupInfoService;

    @PostMapping("/setGroup")
    public ResultVo setGroup(@RequestBody SetGroupDTO setGroupDTO,
                             HttpServletRequest request,
                             HttpServletResponse response){
        try{
            return iGroupInfoService.setGroup(setGroupDTO,request,response);
        }catch (Exception e){
            log.info("错误：{}",e);
        }
        return null;
    }

}
