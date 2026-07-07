package com.easychat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easychat.entity.DO.ChatMessageUserStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ChatMessageUserStatusMapper extends BaseMapper<ChatMessageUserStatus> {

    @Insert("insert into chat_message_user_status (message_id, session_id, user_id, status) " +
            "values (#{messageId}, #{sessionId}, #{userId}, #{status}) " +
            "on duplicate key update session_id = values(session_id), status = greatest(status, values(status))")
    void upsertPending(@Param("messageId") Long messageId,
                       @Param("sessionId") String sessionId,
                       @Param("userId") Integer userId,
                       @Param("status") Integer status);

    @Update("update chat_message_user_status set status = greatest(status, #{status}), delivered_time = coalesce(delivered_time, #{ackTime}) " +
            "where message_id = #{messageId} and user_id = #{userId}")
    int markDelivered(@Param("messageId") Long messageId,
                      @Param("userId") Integer userId,
                      @Param("status") Integer status,
                      @Param("ackTime") Long ackTime);

    @Update("update chat_message_user_status set status = #{status}, read_time = coalesce(read_time, #{readTime}) " +
            "where session_id = #{sessionId} and user_id = #{userId} and message_id <= #{messageId}")
    int markReadBySession(@Param("sessionId") String sessionId,
                          @Param("userId") Integer userId,
                          @Param("messageId") Long messageId,
                          @Param("status") Integer status,
                          @Param("readTime") Long readTime);

    @Select("select min(status) from chat_message_user_status where message_id = #{messageId}")
    Integer getMinStatusByMessageId(@Param("messageId") Long messageId);

}
