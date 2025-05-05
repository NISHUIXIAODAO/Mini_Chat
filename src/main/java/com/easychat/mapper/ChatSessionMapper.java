package com.easychat.mapper;

import com.easychat.entity.DO.ChatSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 会话信息 Mapper 接口
 * </p>
 *
 * @author my
 * @since 2025-03-02
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    @Select("select * from chat_session where session_id = #{sessionId}")
    ChatSession boolSessionId(@Param("sessionId") String sessionId);
    
    @Select("select * from chat_session where session_id = #{sessionId}")
    ChatSession getBySessionId(@Param("sessionId") String sessionId);

    @Update("update chat_session set last_message = #{lastMessage} , last_receive_time = #{lastReceiveTime} where session_id = #{sessionId}")
    void updateBySessionId(@Param("sessionId") String sessionId,@Param("lastMessage") String lastMessage,@Param("lastReceiveTime") long lastReceiveTime);
}
