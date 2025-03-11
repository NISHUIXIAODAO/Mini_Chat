package com.easychat.mapper;

import com.easychat.entity.DO.UserInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * <p>
 * 用户信息 Mapper 接口
 * </p>
 *
 * @author scj
 * @since 2025-02-26
 */
@Mapper

public interface UserInfoMapper extends BaseMapper<UserInfo> {
    @Select("select user_id from user_info where email = #{email}")
    Integer getUserIdByEmail(@Param("email") String email);

    @Select("select * from user_info where user_id = #{userId}")
    UserInfo getUserById(@Param("userId") Integer userId);

    @Update("update user_info set last_login_time = #{lastLoginTime} where user_id = #{userId}")
    void updateLastLoginTimeById(@Param("userId") Integer userId, @Param("lastLoginTime") LocalDateTime lastLoginTime);

    @Update("update user_info set last_off_time = #{lastOffTime} where user_id = #{userId}")
    void updateLastOffTimeById(@Param("userId") Integer userId, @Param("lastOffTime") LocalDateTime lastOffTime);

    @Select("select join_type from user_info where user_id = #{userId}")
    Integer getUserJoinType(@Param("userId") Integer userId);

    @Select("select * from user_info where user_id = #{userId}")
    UserInfo getByUserId(@Param("userId") Integer userId);

    @Select("select nick_name from user_info where user_id = #{userId}")
    String getNickNameByUserId(@Param("userId") Integer userId);
}
