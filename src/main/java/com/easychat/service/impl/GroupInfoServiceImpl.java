package com.easychat.service.impl;

import com.easychat.entity.DO.GroupInfo;
import com.easychat.entity.ResultVo;
import com.easychat.entity.DTO.request.SetGroupDTO;
import com.easychat.mapper.GroupInfoMapper;
import com.easychat.service.IGroupInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author scj
 * @since 2025-02-27
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GroupInfoServiceImpl extends ServiceImpl<GroupInfoMapper, GroupInfo> implements IGroupInfoService {
    @Autowired
    private JWTServiceImpl jwtService;
//    @Autowired
    private final GroupInfoMapper groupInfoMapper;

    @Override
    public ResultVo setGroup(SetGroupDTO setGroupDTO,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        //通过解析token，获得群主id
        String token = request.getHeader("token");
        Integer userId = jwtService.getUserId(token);

        GroupInfo group = GroupInfo.builder()
                .groupName(setGroupDTO.getGroupName())
                .groupOwnerId(userId)
                .createTime(LocalDateTime.now())
                .groupNotice(setGroupDTO.getGroupNotice())
                .joinType(setGroupDTO.getJoinType())
                .build();
        int insert = groupInfoMapper.insert(group);
        if(insert <= 0){
            return ResultVo.failed("注册失败");
        }
        //创建会话 todo




        return ResultVo.success("注册成功");
    }
}
