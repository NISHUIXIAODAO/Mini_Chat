package com.easychat.mapper;

import com.easychat.entity.DO.ChatSessionUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 * 会话用户表 Mapper 接口
 * </p>
 *
 * @author my
 * @since 2025-03-02
 */
@Mapper
public interface ChatSessionUserMapper extends BaseMapper<ChatSessionUser> {

    @Select("<script>" +
            "    select " +
            "        u.*, " +
            "        c.last_message AS lastMessage, " +
            "        c.last_receive_time AS lastReceiveTime " +
            "    from chat_session_user u " +
            "    inner join chat_session c on c.session_id = u.session_id " +
            "    <where> " +
            "        <if test='userId != null'> " +
            "            u.user_id = #{userId} " +
            "        </if> " +
            "    </where> " +
            "</script>")
    List<ChatSessionUser> getSessionListById(@Param("userId") Integer userId);


    @Select("select user_id , contact_id from chat_session_user where user_id = #{applyUserId} and contact_id = #{receiveUserId}")
    ChatSessionUser boolByUserIdAndContactId(@Param("applyUserId") Integer applyUserId,@Param("receiveUserId") Integer receiveUserId);

    @Update("update chat_session_user set nick_name = #{nickName} where user_id = #{applyUserId} and contact_id = #{receiveUserId}")
    void updateByUserIdAndContactId(@Param("applyUserId") Integer applyUserId,@Param("receiveUserId") Integer receiveUserId ,@Param("nickName") String nickName);
}
