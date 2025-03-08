package com.easychat.mapper;

import com.easychat.entity.DO.GroupInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author scj
 * @since 2025-02-27
 */
@Mapper

public interface GroupInfoMapper extends BaseMapper<GroupInfo> {

    @Select("select * from group_info where user_id = #{userId} and group_name = #{groupName}")
    GroupInfo getByNameAndOwnerId(@Param("userId") Integer userId , @Param("groupName") String groupName);
}
