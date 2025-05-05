package com.easychat.service;

import com.easychat.entity.DTO.request.ApplyGroupAddDTO;
import com.easychat.entity.DTO.request.DisposeApplyDTO;
import com.easychat.entity.ResultVo;
import com.easychat.entity.DO.UserContact;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author my
 * @since 2025-03-01
 */
public interface IUserContactService extends IService<UserContact> {
    List<Integer> getFriendIdList(Integer userId);
    List<Integer> getGroupIdList(Integer userId);

    void addContact4Robot(Integer userId);
    ResultVo<Object> applyFriendAdd(String token, Integer contactId, String applyInfo);
    ResultVo<Object> disposeApply(DisposeApplyDTO disposeApplyDTO , HttpServletRequest request, HttpServletResponse response);
    ResultVo<Object> applyGroupAdd(ApplyGroupAddDTO applyGroupAddDTO, HttpServletRequest request, HttpServletResponse response);

    ResultVo<Object> getContactList(HttpServletRequest request, HttpServletResponse response);
}
