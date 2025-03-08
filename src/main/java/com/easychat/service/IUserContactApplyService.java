package com.easychat.service;

import com.easychat.entity.DO.UserContactApply;
import com.baomidou.mybatisplus.extension.service.IService;
import com.easychat.entity.DTO.response.UserApplyListResponseDTO;

import java.util.List;

/**
 * <p>
 * 联系人申请 服务类
 * </p>
 *
 * @author my
 * @since 2025-03-03
 */
public interface IUserContactApplyService extends IService<UserContactApply> {
    public List<UserApplyListResponseDTO> getApplyList(String token);

}
