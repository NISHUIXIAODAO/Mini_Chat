package com.easychat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.easychat.entity.ResultVo;
import com.easychat.entity.DO.UserInfo;
import com.easychat.entity.DTO.request.LoginDTO;
import com.easychat.entity.DTO.request.RegisterDTO;
import com.easychat.mapper.UserInfoMapper;
import com.easychat.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.easychat.utils.SessionIdUtils.md5;

/**
 * <p>
 * 用户信息 服务实现类
 * </p>
 *
 * @author scj
 * @since 2025-02-26
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

//    @Autowired
    private final UserInfoMapper userInfoMapper;
    @Autowired
    private JavaMailSenderImpl mailSender;
    @Autowired
    private IJWTService jwtService;
    @Autowired
    private IRedisService redisService;
    @Autowired
    private IUserContactService userContactService;


    /***
     * 登录
     * @param loginDTO
     * @param response
     * @param request
     * @return
     */
    @Override
    public ResultVo<Object> login(LoginDTO loginDTO, HttpServletResponse response, HttpServletRequest request) {

        //查询用户是否存在
        //根据邮箱查询用户ID
        Integer userId = userInfoMapper.getUserIdByEmail(loginDTO.getEmail());
        if (userId == null) {
            return ResultVo.failed("没有此用户");
        }
        //校验密码
        String password = userInfoMapper.getPasswordById(userId);
        if(password == null || !password.equals(md5(loginDTO.getPassword()))){
            return ResultVo.failed("密码不正确" + md5(loginDTO.getPassword()));
        }

        //生成jwt
        String token = jwtService.generateToken(userId);

        Long lastHeartBeat = redisService.getUserHeartBeat(userId);
        if(lastHeartBeat != null){
            return ResultVo.failed("用户已在别处登录");
        }

        //查询联系人(朋友)
        List<Integer> friendIdList = userContactService.getFriendIdList(userId);
        List<Integer> groupIdList = userContactService.getGroupIdList(userId);
        // 定义不同的键名来存储好友和群组 ID
        String friendKey = "user:" + userId  + ":friends";
        String groupKey = "user:" + userId  + ":groups";

        //将联系人列表存入redis
        if(!friendIdList.isEmpty()){
            redisService.addUserContactBatch(friendKey,friendIdList);
        }
        if(!groupIdList.isEmpty()){
            redisService.addUserContactBatch(groupKey,groupIdList);
        }

        String nickName = userInfoMapper.getNickNameByUserId(userId);
        HashMap<String, Object> userInfo = new HashMap<>();
        userInfo.put("token", token);
        userInfo.put("userId", userId);
        userInfo.put("nickName", nickName);

        return ResultVo.success(userInfo);
    }

    /***
     * 注册
     * @param registerDTO
     * @param response
     * @param request
     * @return
     */
    @Override
    public ResultVo<Object> register(RegisterDTO registerDTO, HttpServletResponse response, HttpServletRequest request) {
        //查询用户是否存在
        //根据邮箱查询用户ID
        Integer userId = userInfoMapper.getUserIdByEmail(registerDTO.getEmail());
        if (userId != null) {
            return ResultVo.failed("用户已存在");
        }
        //检验参数是否合法
        String regex = "\\w+@\\w+(\\.\\w{2,3})*\\.\\w{2,3}";
        if (!registerDTO.getEmail().matches(regex)) {
            return ResultVo.failed("邮箱格式不正确");
        }
        //校验验证码
        String code = redisService.verifyCode(registerDTO.getEmail());
        if(code == null){
            return ResultVo.failed("验证码发送失败");
        }
        if(!code.equals(registerDTO.getCode())){
            return ResultVo.failed("验证码错误");
        }
        log.info("验证码正确");
        //存入数据库
        UserInfo user = UserInfo.builder()
                .email(registerDTO.getEmail())
                .nickName(registerDTO.getNickName())
                .joinType(registerDTO.getJoinType())
                .sex(registerDTO.getSex())
                .password(md5(registerDTO.getPassword()))
                .personalSignature(null)
                .status(null)
                .createTime(LocalDateTime.now())
                .lastLoginTime(LocalDateTime.now())
                .areaName(null)
                .areaCode(null)
                .lastOffTime(System.currentTimeMillis())
                .build();
        userInfoMapper.insert(user);
        //获取注册用户的ID
        userId = userInfoMapper.getUserIdByEmail(registerDTO.getEmail());
        //创建机器人好友
        userContactService.addContact4Robot(userId);

        return ResultVo.success("注册成功");
    }

    /***
     * 发送验证码
     * @param email
     * @param response
     * @param request
     * @return
     */
    @Override
    public ResultVo sendCode(String email, HttpServletResponse response, HttpServletRequest request) {

        String code = generateCode();

        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setSubject("注册 Mini_Chat 验证码");
        simpleMailMessage.setText(code);
        simpleMailMessage.setTo(email);
        simpleMailMessage.setFrom("mamm127323@163.com");
        mailSender.send(simpleMailMessage);
        //将生成的code存到redis里  过期时间1分钟
        redisService.setCode(email, code,1, TimeUnit.MINUTES);

        return ResultVo.success("发送成功");
    }

    /***
     * 生成验证码
     * @return
     */
    private String generateCode(){
        Random random = new Random();
        String code = String.valueOf(random.nextInt( (900000)+ 100000));
        System.out.println(code);
        return code;
    }
}
