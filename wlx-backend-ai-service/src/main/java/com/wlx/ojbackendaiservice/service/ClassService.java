package com.wlx.ojbackendaiservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wlx.ojbackendmodel.model.entity.ClassProblem;
import com.wlx.ojbackendmodel.model.vo.ClassProblemPassRateVO;
import com.wlx.ojbackendmodel.model.vo.ClassProblemSubmissionsVO;
import com.wlx.ojbackendmodel.model.vo.ClassProblemTagNumVO;
import com.wlx.ojbackendmodel.model.vo.ClassVO;

import java.util.List;

public interface ClassService extends IService<Class> {
    List<ClassVO> pageQueryByName(String name, Integer teacherId, Integer studentId, String order, Boolean isAsc);

    /**
     * 根据班级id查询班级题目提交情况
     * @param classId
     * @return
     */
    List<ClassProblemSubmissionsVO> selectClassProblemSubmissionsByClassId(Integer classId);


    /**
     * 查询班级题目通过率排行榜
     * @param classId 班级ID
     * @return 题目通过率列表
     */
    List<ClassProblemPassRateVO> selectClassProblemPassRate(Integer classId);


    /**
     * 查询班级题目标签数量
     * @param classId 班级ID
     * @return 班级题目标签数量列表
     */
    List<ClassProblemTagNumVO> getClassProblemTagNum(Integer classId);
}
