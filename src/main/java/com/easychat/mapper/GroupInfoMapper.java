package com.easychat.mapper;

import com.easychat.entity.DO.GroupInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easychat.entity.DO.UserInfo;
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

    @Select("select * from group_info where group_owner_id = #{userId} and group_name = #{groupName}")
    GroupInfo getByNameAndOwnerId(@Param("userId") Integer userId , @Param("groupName") String groupName);

    @Select("select * from group_info where group_id = #{receiveUserId}")
    GroupInfo getByGroupId(@Param("receiveUserId") Integer receiveUserId);

    @Select("select group_owner_id from group_info where group_id = #{groupId}")
    Integer getOwnerIdByGroupId(@Param("groupId") Integer groupId);

    @Select("select join_type from group_info where group_id = #{groupId}")
    Integer getJoinTypeByGroupId(@Param("groupId") Integer groupId);

    @Select("select group_id from group_info where group_owner_id = #{groupOwnerId}")
    Integer getGroupIdByOwnerId(@Param("groupOwnerId") Integer groupOwnerId);
}
