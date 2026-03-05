package com.wlx.ojbackendaiservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wlx.ojbackendmodel.model.entity.ClassChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 班级实时聊天记录表 Mapper 接口
 */
@Mapper
public interface ClassChatMessageMapper extends BaseMapper<ClassChatMessage> {

}
