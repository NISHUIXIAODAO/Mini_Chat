package com.easychat.mapper;

import com.easychat.entity.DO.UserContact;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easychat.enums.FriendStatusEnum;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author my
 * @since 2025-03-01
 */
@Mapper
public interface UserContactMapper extends BaseMapper<UserContact> {

    @Select("SELECT contact_id FROM user_contact WHERE user_id = #{userId} AND contact_type = #{contactType};")
    List<Integer> getListById(@Param("userId") Integer userId ,@Param("contactType") int contactType);

    @Select("select * from user_contact where user_id = #{contactId} and contact_id = #{userId}")
    UserContact getReceiveInfo(@Param("contactId") Integer contactId,@Param("userId") Integer userId);

    @Insert("insert into user_contact (user_id , contact_id , contact_type , create_time , status , last_update_time)" +
            "value (#{userId} , #{contactId} , #{contactType} , #{createTime} , #{status} , #{lastUpdateTime})")
    void insertContact(@Param("userId") Integer userId,
                    @Param("contactId") Integer contactId,
                    @Param("contactType") int contactType,
                    @Param("createTime") LocalDateTime createTime,
                    @Param("status") Integer status,
                    @Param("lastUpdateTime") LocalDateTime lastUpdateTime);

    @Select("select count(*) from user_contact where contact_id = #{groupId} and status = #{status}")
    Integer getGroupCountByContactIdAndStatus(@Param("groupId") Integer groupId,@Param("status") Integer status);

    @Select("select status from user_contact where user_id = #{userId} and contact_id = #{contactId}")
    Integer getStatusByUserIdAndContactId(@Param("contactId") Integer userId,@Param("userId") Integer contactId);
    @Select("select contact_type from user_contact where contact_id = #{contactId}")
    Integer getContactTypeByContactId(@Param("contactId") Integer contactId);
}
