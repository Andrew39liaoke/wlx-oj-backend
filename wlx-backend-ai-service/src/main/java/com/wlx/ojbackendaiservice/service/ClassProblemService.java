package com.wlx.ojbackendaiservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wlx.ojbackendmodel.model.entity.ClassProblem;
import com.wlx.ojbackendmodel.model.vo.ProblemPageVO;

import java.util.List;

public interface ClassProblemService extends IService<ClassProblem> {
    List<ProblemPageVO> getClassProblem(Integer classId, String name);
}
