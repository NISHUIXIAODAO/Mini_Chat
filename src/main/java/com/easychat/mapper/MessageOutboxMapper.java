package com.easychat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easychat.entity.DO.MessageOutbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MessageOutboxMapper extends BaseMapper<MessageOutbox> {
    @Select("select * from message_outbox where " +
            "(status = 'PENDING' and next_retry_at <= #{now}) or " +
            "(status = 'PROCESSING' and lease_until <= #{now}) " +
            "order by id asc limit #{limit}")
    List<MessageOutbox> findPublishable(@Param("now") long now, @Param("limit") int limit);

    @Update("update message_outbox set status = 'PROCESSING', lease_until = #{leaseUntil}, update_time = #{now} " +
            "where id = #{id} and ((status = 'PENDING' and next_retry_at <= #{now}) " +
            "or (status = 'PROCESSING' and lease_until <= #{now}))")
    int claim(@Param("id") Long id, @Param("now") long now, @Param("leaseUntil") long leaseUntil);

    @Update("update message_outbox set status = 'PUBLISHED', published_at = #{now}, lease_until = null, " +
            "last_error = null, update_time = #{now} where id = #{id} and status = 'PROCESSING'")
    int markPublished(@Param("id") Long id, @Param("now") long now);

    @Update("update message_outbox set status = #{status}, retry_count = #{retryCount}, next_retry_at = #{nextRetryAt}, " +
            "lease_until = null, last_error = #{lastError}, update_time = #{now} " +
            "where id = #{id} and status = 'PROCESSING'")
    int markFailure(@Param("id") Long id, @Param("status") String status, @Param("retryCount") int retryCount,
                    @Param("nextRetryAt") long nextRetryAt, @Param("lastError") String lastError, @Param("now") long now);

    @Update("update message_outbox set status = 'PENDING', retry_count = 0, next_retry_at = #{now}, lease_until = null, " +
            "last_error = null, update_time = #{now} where id = #{id} and status = 'FAILED'")
    int retryFailed(@Param("id") Long id, @Param("now") long now);
}
