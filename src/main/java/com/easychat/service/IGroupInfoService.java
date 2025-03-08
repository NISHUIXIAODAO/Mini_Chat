package com.easychat.service;

import com.easychat.entity.DO.GroupInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.easychat.entity.ResultVo;
import com.easychat.entity.DTO.request.SetGroupDTO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author scj
 * @since 2025-02-27
 */
public interface IGroupInfoService extends IService<GroupInfo> {

    ResultVo setGroup(SetGroupDTO setGroupDTO,
                      HttpServletRequest request,
                      HttpServletResponse response);
}
