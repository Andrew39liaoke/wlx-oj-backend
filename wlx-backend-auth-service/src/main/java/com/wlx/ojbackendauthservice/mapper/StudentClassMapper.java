package com.wlx.ojbackendauthservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wlx.ojbackendmodel.model.entity.StudentClass;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生班级关联表 Mapper
 */
@Mapper
public interface StudentClassMapper extends BaseMapper<StudentClass> {
}
