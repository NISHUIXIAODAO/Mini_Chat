package com.easychat.mapper;

import com.easychat.entity.DO.UserContactApply;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easychat.entity.DTO.response.UserApplyListResponseDTO;
import org.apache.ibatis.annotations.Insert;
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

    @Insert("insert into user_contact_apply (apply_user_id, receive_user_id, contact_type, contact_id, last_apply_time, status, apply_info) " +
            "values (#{applyUserId}, #{receiveUserId}, #{contactType}, #{contactId}, #{lastApplyTime}, #{status}, #{applyInfo}) " +
            "on duplicate key update contact_type = values(contact_type), last_apply_time = values(last_apply_time), status = values(status), apply_info = values(apply_info)")
    void upsertApply(@Param("applyUserId") Integer applyUserId,
                     @Param("receiveUserId") Integer receiveUserId,
                     @Param("contactType") Integer contactType,
                     @Param("contactId") Integer contactId,
                     @Param("lastApplyTime") Long lastApplyTime,
                     @Param("status") Integer status,
                     @Param("applyInfo") String applyInfo);

    @Select("select * from user_contact_apply where apply_id = #{applyId}")
    UserContactApply getByApplyId(@Param("applyId") Integer applyId);

    @Select("select * from user_contact_apply where apply_user_id = #{applyUserId} and receive_user_id = #{receiveUserId} and contact_id = #{contactId} and contact_type = #{contactType}")
    UserContactApply getByApplyUserIdAndReceiveUserIdAndContactIdAndContactType(@Param("applyUserId") Integer applyUserId,
                                                                                @Param("receiveUserId") Integer receiveUserId,
                                                                                @Param("contactId") Integer contactId,
                                                                                @Param("contactType") Integer contactType);

    @Select("select * from user_contact_apply where apply_user_id = #{applyUserId} and receive_user_id = #{receiveUserId} and status = #{status} order by last_apply_time desc, apply_id desc limit 1")
    UserContactApply getLatestByApplyUserIdAndReceiveUserIdAndStatus(@Param("applyUserId") Integer applyUserId,
                                                                     @Param("receiveUserId") Integer receiveUserId,
                                                                     @Param("status") Integer status);

    @Select("select * from user_contact_apply where apply_user_id = #{applyUserId} and receive_user_id = #{receiveUserId} order by last_apply_time desc, apply_id desc limit 1")
    UserContactApply getLatestByApplyUserIdAndReceiveUserId(@Param("applyUserId") Integer applyUserId,
                                                            @Param("receiveUserId") Integer receiveUserId);

    @Select("select contact_type from user_contact_apply where apply_user_id = #{applyUserId} and receive_user_id = #{receiveUserId}")
    Integer getContactTypeByApplyUserIdAndReceiveUserId(@Param("applyUserId") Integer applyUserId , @Param("receiveUserId") Integer receiveUserId);

    @Select("select contact_type from user_contact_apply where receive_user_id = #{receiveUserId}")
    Integer getContactTypeByReceiveUserId(@Param("receiveUserId") Integer receiveUserId);

    @Update("update user_contact_apply set status = #{status} where apply_user_id = #{applyUserId} and receive_user_id = #{receiveUserId}")
    void setStatus(@Param("applyUserId") Integer applyUserId , @Param("receiveUserId") Integer receiveUserId , @Param("status") Integer status);

    @Update("update user_contact_apply set status = #{newStatus} where apply_id = #{applyId} and status = #{oldStatus}")
    int updateStatusByApplyIdAndStatus(@Param("applyId") Integer applyId,
                                       @Param("oldStatus") Integer oldStatus,
                                       @Param("newStatus") Integer newStatus);

    @Select("select apply_id as applyId, apply_user_id as applyUserId, contact_id as contactId, contact_type as contactType, apply_info as applyInfo, status, last_apply_time as lastApplyTime from user_contact_apply where receive_user_id = #{receiveUserId}")
    List<UserApplyListResponseDTO> getApplyList(@Param("receiveUserId") Integer receiveUserId);

    @Select("select apply_info from user_contact_apply where apply_user_id = #{applyUserId} and receive_user_id = #{receiveUserId}")
    String getApplyInfoByApplyUserIdAndReceiveUserId(@Param("applyUserId") Integer applyUserId , @Param("receiveUserId") Integer receiveUserId);
}
