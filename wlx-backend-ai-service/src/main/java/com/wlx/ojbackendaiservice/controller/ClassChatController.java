package com.wlx.ojbackendaiservice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wlx.ojbackendaiservice.service.ClassChatMessageService;
import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendcommon.common.Result;
import com.wlx.ojbackendmodel.model.entity.ClassChatMessage;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.ClassChatMessageVO;
import com.wlx.ojbackendserviceclient.service.UserFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai/chat")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class ClassChatController {

    @Resource
    private ClassChatMessageService classChatMessageService;

    @Resource
    private UserFeignClient userFeignClient;

    @GetMapping("/history")
    @Operation(summary = "获取班级群聊历史消息")
    public ResponseEntity<List<ClassChatMessageVO>> getChatHistory(@RequestParam("classId") Long classId) {
        System.out.println("classId = " + classId);
        LambdaQueryWrapper<ClassChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassChatMessage::getClassId, classId)
                .orderByDesc(ClassChatMessage::getCreateTime);
        
        // 懒加载 20 条消息
        Page<ClassChatMessage> page = classChatMessageService.page(new Page<>(1, 20), wrapper);
        List<ClassChatMessage> records = page.getRecords();
        
        if (records == null || records.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        // 由于取出来的时间倒序(从晚到早)，需要反转变成正常对话顺序
        Collections.reverse(records);

        Set<Long> senderIds = records.stream()
                .map(ClassChatMessage::getSenderId)
                .collect(Collectors.toSet());

        // 获取用户信息
        List<User> userList = userFeignClient.listByIds(senderIds);
        Map<Long, User> userMap = userList.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<ClassChatMessageVO> voList = records.stream().map(msg -> {
            ClassChatMessageVO vo = new ClassChatMessageVO();
            vo.setId(msg.getId());
            vo.setClassId(msg.getClassId());
            vo.setSenderId(msg.getSenderId());
            vo.setContent(msg.getContent());
            vo.setMessageType(msg.getMessageType());
            vo.setImageUrl(msg.getImageUrl());
            vo.setCreateTime(msg.getCreateTime());

            User user = userMap.get(msg.getSenderId());
            if (user != null) {
                vo.setUserName(user.getUserName() != null ? user.getUserName() : "未知用户");
                vo.setUserAvatar(user.getUserAvatar());
            } else {
                vo.setUserName("未知用户");
                vo.setUserAvatar("");
            }
            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList);
    }

}
