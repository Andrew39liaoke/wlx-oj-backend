package com.wlx.ojbackendauthservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wlx.ojbackendmodel.model.entity.ClassProblem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 班级题目关联表 Mapper
 */
@Mapper
public interface ClassProblemMapper extends BaseMapper<ClassProblem> {
}
