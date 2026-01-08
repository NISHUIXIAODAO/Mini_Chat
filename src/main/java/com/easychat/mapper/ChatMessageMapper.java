package com.easychat.mapper;

import com.easychat.entity.DO.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

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
            + "<if test='lastTimestamp != null'>"
            + "AND send_time &lt; #{lastTimestamp} "
            + "</if>"
            + "ORDER BY send_time DESC "
            + "LIMIT #{pageSize}"
            + "</script>")
    @ResultMap("BaseResultMap")
    List<ChatMessage> getMessageHistory(@Param("sessionId") String sessionId,
                                      @Param("lastTimestamp") Long lastTimestamp,
                                      @Param("pageSize") Integer pageSize);

    @Select("<script>"
            + "SELECT * FROM chat_message "
            + "WHERE contact_id = #{contactId} "
            + "<if test='lastTimestamp != null'>"
            + "AND send_time &lt; #{lastTimestamp} "
            + "</if>"
            + "ORDER BY send_time DESC "
            + "LIMIT #{pageSize}"
            + "</script>")
    @ResultMap("BaseResultMap")
    List<ChatMessage> getMessageHistoryByContactId(@Param("contactId") Integer contactId,
                                                 @Param("lastTimestamp") Long lastTimestamp,
                                                 @Param("pageSize") Integer pageSize);
}
