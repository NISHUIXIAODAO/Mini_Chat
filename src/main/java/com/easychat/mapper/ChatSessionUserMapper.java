package com.easychat.mapper;

import com.easychat.entity.DO.ChatSessionUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
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

    @Update("update chat_session_user set contact_name = #{nickName} where user_id = #{applyUserId} and contact_id = #{receiveUserId}")
    void updateByUserIdAndContactId(@Param("applyUserId") Integer applyUserId,@Param("receiveUserId") Integer receiveUserId ,@Param("nickName") String nickName);

    @Insert("insert into chat_session_user (user_id, contact_id, session_id, contact_name) " +
            "values (#{userId}, #{contactId}, #{sessionId}, #{contactName}) " +
            "on duplicate key update session_id = values(session_id), contact_name = values(contact_name)")
    void upsertByUserIdAndContactId(@Param("userId") Integer userId,
                                    @Param("contactId") Integer contactId,
                                    @Param("sessionId") String sessionId,
                                    @Param("contactName") String contactName);

    @Update("update chat_session_user set unread_count = coalesce(unread_count, 0) + 1 " +
            "where session_id = #{sessionId} and user_id = #{userId}")
    void incrementUnread(@Param("sessionId") String sessionId, @Param("userId") Integer userId);

    @Update("update chat_session_user set unread_count = 0, last_read_message_id = #{messageId}, last_read_time = #{readTime} " +
            "where session_id = #{sessionId} and user_id = #{userId}")
    void markSessionRead(@Param("sessionId") String sessionId,
                         @Param("userId") Integer userId,
                         @Param("messageId") Long messageId,
                         @Param("readTime") Long readTime);

    @Select("select user_id from chat_session_user where session_id = #{sessionId}")
    List<Integer> getUserIdsBySessionId(@Param("sessionId") String sessionId);

    @Select("select count(1) from chat_session_user where session_id = #{sessionId} and user_id = #{userId}")
    int countBySessionIdAndUserId(@Param("sessionId") String sessionId,
                                  @Param("userId") Integer userId);
}
