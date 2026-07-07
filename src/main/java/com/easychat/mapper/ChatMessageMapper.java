package com.easychat.mapper;

import com.easychat.entity.DO.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 * 聊天消息表 Mapper 接口
 * </p>
 *
 * @author my
 * @since 2025-03-02
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("<script>" +
            "    SELECT * FROM chat_message " +
            "    WHERE contact_id IN " +
            "    <foreach item='id' collection='contactIdList' open='(' separator=',' close=')'> " +
            "        #{id} " +
            "    </foreach> " +
            "    AND send_time > #{lastReceiveTime} " +
            "    ORDER BY send_time DESC " +
            "</script>")
    @ResultMap("BaseResultMap")
    List<ChatMessage> getChatMessages(@Param("contactIdList") List<Integer> contactIdList,@Param("lastReceiveTime") Long lastReceiveTime);

//    @Select("select contact_type from chat_message where message_id = #{messageId}")
//    Integer getContactTypeByMessageId(@Param("messageId") Long messageId);
    @Select("select contact_type from chat_message where contact_id = #{contactId} and session_id = #{sessionId}")
    Integer getContactTypeByContactId(@Param("contactId") Integer contactId , @Param("sessionId") String sessionId);

    @Select("<script>"
            + "SELECT * FROM chat_message "
            + "WHERE session_id = #{sessionId} "
            + "<if test='lastMessageId != null'>"
            + "<choose>"
            + "<when test='forward != null and forward'>"
            + "AND message_id &gt; #{lastMessageId} "
            + "</when>"
            + "<otherwise>"
            + "AND message_id &lt; #{lastMessageId} "
            + "</otherwise>"
            + "</choose>"
            + "</if>"
            + "<if test='lastTimestamp != null'>"
            + "AND send_time &lt; #{lastTimestamp} "
            + "</if>"
            + "<choose>"
            + "<when test='forward != null and forward'>"
            + "ORDER BY message_id ASC "
            + "</when>"
            + "<otherwise>"
            + "ORDER BY send_time DESC "
            + "</otherwise>"
            + "</choose>"
            + "LIMIT #{pageSize}"
            + "</script>")
    @ResultMap("BaseResultMap")
    List<ChatMessage> getMessageHistory(@Param("sessionId") String sessionId,
                                      @Param("lastTimestamp") Long lastTimestamp,
                                      @Param("lastMessageId") Long lastMessageId,
                                      @Param("forward") Boolean forward,
                                      @Param("pageSize") Integer pageSize);

    @Select("<script>"
            + "SELECT * FROM chat_message "
            + "WHERE contact_id = #{contactId} "
            + "<if test='lastMessageId != null'>"
            + "<choose>"
            + "<when test='forward != null and forward'>"
            + "AND message_id &gt; #{lastMessageId} "
            + "</when>"
            + "<otherwise>"
            + "AND message_id &lt; #{lastMessageId} "
            + "</otherwise>"
            + "</choose>"
            + "</if>"
            + "<if test='lastTimestamp != null'>"
            + "AND send_time &lt; #{lastTimestamp} "
            + "</if>"
            + "<choose>"
            + "<when test='forward != null and forward'>"
            + "ORDER BY message_id ASC "
            + "</when>"
            + "<otherwise>"
            + "ORDER BY send_time DESC "
            + "</otherwise>"
            + "</choose>"
            + "LIMIT #{pageSize}"
            + "</script>")
    @ResultMap("BaseResultMap")
    List<ChatMessage> getMessageHistoryByContactId(@Param("contactId") Integer contactId,
                                                 @Param("lastTimestamp") Long lastTimestamp,
                                                 @Param("lastMessageId") Long lastMessageId,
                                                 @Param("forward") Boolean forward,
                                                 @Param("pageSize") Integer pageSize);

    @Update("update chat_message set status = greatest(status, #{status}), delivered_time = coalesce(delivered_time, #{ackTime}) " +
            "where message_id = #{messageId} and send_user_id <> #{userId}")
    int markDelivered(@Param("messageId") Long messageId,
                      @Param("userId") Integer userId,
                      @Param("status") Integer status,
                      @Param("ackTime") Long ackTime);

    @Update("update chat_message set status = #{status}, read_time = coalesce(read_time, #{readTime}) " +
            "where session_id = #{sessionId} and send_user_id <> #{userId} and message_id <= #{messageId}")
    int markReadBySession(@Param("sessionId") String sessionId,
                          @Param("userId") Integer userId,
                          @Param("messageId") Long messageId,
                          @Param("status") Integer status,
                          @Param("readTime") Long readTime);

    @Select("select max(message_id) from chat_message where session_id = #{sessionId}")
    Long getMaxMessageIdBySessionId(@Param("sessionId") String sessionId);

    @Update("update chat_message set status = greatest(status, #{status}), delivered_time = coalesce(delivered_time, #{deliveredTime}) " +
            "where message_id = #{messageId}")
    int updateStatusAndDeliveredTime(@Param("messageId") Long messageId,
                                     @Param("status") Integer status,
                                     @Param("deliveredTime") Long deliveredTime);

    @Update("update chat_message set status = #{status}, read_time = coalesce(read_time, #{readTime}) " +
            "where message_id = #{messageId}")
    int updateStatusAndReadTime(@Param("messageId") Long messageId,
                                @Param("status") Integer status,
                                @Param("readTime") Long readTime);

    @Update("update chat_message set status = #{status}, read_time = coalesce(read_time, #{readTime}) " +
            "where session_id = #{sessionId} and message_id <= #{messageId}")
    int updateReadStatusBySession(@Param("sessionId") String sessionId,
                                  @Param("messageId") Long messageId,
                                  @Param("status") Integer status,
                                  @Param("readTime") Long readTime);

    @Update("update chat_message cm set " +
            "cm.status = coalesce((select min(s.status) from chat_message_user_status s where s.message_id = cm.message_id), cm.status), " +
            "cm.read_time = case when coalesce((select min(s2.status) from chat_message_user_status s2 where s2.message_id = cm.message_id), cm.status) >= #{readStatus} " +
            "then coalesce(cm.read_time, #{readTime}) else cm.read_time end " +
            "where cm.session_id = #{sessionId} and cm.message_id <= #{messageId}")
    int updateReadStatusBySessionFromUserStatus(@Param("sessionId") String sessionId,
                                                @Param("messageId") Long messageId,
                                                @Param("readStatus") Integer readStatus,
                                                @Param("readTime") Long readTime);
}
