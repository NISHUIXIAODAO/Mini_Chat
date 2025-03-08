package com.easychat.service;

import com.easychat.entity.ResultVo;
import com.easychat.entity.DO.UserContact;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author my
 * @since 2025-03-01
 */
public interface IUserContactService extends IService<UserContact> {

    void addContact4Robot(Integer userId);
    ResultVo applyAdd(String token, Integer contactId, String applyInfo);
    ResultVo disposeApply(Integer applyUserId , String token , Integer status);
}
