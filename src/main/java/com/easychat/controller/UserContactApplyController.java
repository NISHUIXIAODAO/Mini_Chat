package com.easychat.controller;

import com.easychat.entity.DTO.response.UserApplyListResponseDTO;
import com.easychat.entity.ResultVo;
import com.easychat.service.IJWTService;
import com.easychat.service.IUserContactApplyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 联系人申请
 * </p>
 *
 * @author my
 * @since 2025-03-03
 */
@RestController
@RequestMapping("/userContactApply")
public class UserContactApplyController {
    private final IUserContactApplyService userContactApplyService;
    private final IJWTService jwtService;

    public UserContactApplyController(IUserContactApplyService userContactApplyService, IJWTService jwtService) {
        this.userContactApplyService = userContactApplyService;
        this.jwtService = jwtService;
    }

    @GetMapping("/getApplyList")
    public ResultVo<List<UserApplyListResponseDTO>> getApplyList(HttpServletRequest request) {
        return ResultVo.success(userContactApplyService.getApplyList(jwtService.extractToken(request)));
    }

}
