package com.wlx.ojbackendaiservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wlx.ojbackendaiservice.mapper.ClassChatMessageMapper;
import com.wlx.ojbackendaiservice.service.ClassChatMessageService;
import com.wlx.ojbackendmodel.model.entity.ClassChatMessage;
import org.springframework.stereotype.Service;

/**
 * 班级实时聊天记录表 服务实现类
 */
@Service
public class ClassChatMessageServiceImpl extends ServiceImpl<ClassChatMessageMapper, ClassChatMessage> implements ClassChatMessageService {

}
