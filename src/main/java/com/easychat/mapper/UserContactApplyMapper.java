package com.easychat.mapper;

import com.easychat.entity.DO.UserContactApply;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easychat.entity.DTO.response.UserApplyListResponseDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;


/**
 * <p>
 * 联系人申请 Mapper 接口
 * </p>
 *
 * @author my
 * @since 2025-03-03
 */
@Mapper
public interface UserContactApplyMapper extends BaseMapper<UserContactApply> {
    @Select("select * from user_contact_apply where apply_user_id = #{applyUserId} and receive_user_id = #{receiveUserId} and contact_id = #{contactId}")
    UserContactApply getByApplyUserIdAddReceiveUserIdAddContactId(@Param("applyUserId") Integer applyUserId,
                                                          @Param("receiveUserId") Integer receiveUserId,
                                                          @Param("contactId") Integer contactId);

    @Update("UPDATE user_contact_apply SET status = #{status}, last_apply_time = #{lastApplyTime}, apply_info = #{applyInfo} WHERE apply_id = #{applyId}")
    void updateByApplyId(@Param("applyId") Integer applyId,
                        @Param("status") Integer status,
                        @Param("lastApplyTime") Long lastApplyTime,
                        @Param("applyInfo") String applyInfo);

    @Select("select contact_type from user_contact_apply where apply_user_id = #{applyUserId} and receive_user_id = #{receiveUserId}")
    Integer getContactTypeByApplyUserIdAndReceiveUserId(@Param("applyUserId") Integer applyUserId , @Param("receiveUserId") Integer receiveUserId);

    @Select("select contact_type from user_contact_apply where receive_user_id = #{receiveUserId}")
    Integer getContactTypeByReceiveUserId(@Param("receiveUserId") Integer receiveUserId);

    @Update("update user_contact_apply set status = #{status} where apply_user_id = #{applyUserId} and receive_user_id = #{receiveUserId}")
    void setStatus(@Param("applyUserId") Integer applyUserId , @Param("receiveUserId") Integer receiveUserId , @Param("status") Integer status);

    @Select("select apply_user_id , apply_info , status , last_apply_time from user_contact_apply where receive_user_id = #{receiveUserId}")
    List<UserApplyListResponseDTO> getApplyList(@Param("receiveUserId") Integer receiveUserId);

    @Select("select apply_info from user_contact_apply where apply_user_id = #{applyUserId} and receive_user_id = #{receiveUserId}")
    String getApplyInfoByApplyUserIdAndReceiveUserId(@Param("applyUserId") Integer applyUserId , @Param("receiveUserId") Integer receiveUserId);
}
