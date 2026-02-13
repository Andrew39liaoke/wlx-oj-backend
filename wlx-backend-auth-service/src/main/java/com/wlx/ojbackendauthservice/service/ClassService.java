package com.wlx.ojbackendauthservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wlx.ojbackendmodel.model.dto.Class.ClassQuestionQueryRequest;
import com.wlx.ojbackendmodel.model.dto.Class.ClassQueryRequest;
import com.wlx.ojbackendmodel.model.dto.Class.ClassStudentQueryRequest;
import com.wlx.ojbackendmodel.model.dto.question.QuestionAddRequest;
import com.wlx.ojbackendmodel.model.entity.Class;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.ClassVO;
import com.wlx.ojbackendmodel.model.vo.QuestionVO;

import java.util.List;
import java.util.Map;

public interface ClassService extends IService<Class> {
    /**
     * 添加班级
     *
     * @param name 班级名称
     * @param teacherId 教师ID
     * @return 新创建的班级ID
     */
    Long addClass(String name, Long teacherId);

    /**
     * 根据邀请码加入班级
     *
     * @param invitationCode 邀请码
     * @param studentId 学生ID
     * @return 是否加入成功
     */
    boolean joinClass(String invitationCode, Long studentId);

    /**
     * 退出班级
     *
     * @param classIds 班级ID列表
     * @param studentId 学生ID
     * @return 是否退出成功
     */
    boolean quitClass(List<Long> classIds, Long studentId);

    /**
     * 分页获取班级题目列表
     *
     * @param classQuestionQueryRequest 查询请求
     * @return 分页题目VO
     */
    Page<QuestionVO> getClassQuestionPage(ClassQuestionQueryRequest classQuestionQueryRequest);

    /**
     * 获取班级题目的用户提交信息
     *
     * @param classId 班级ID
     * @param studentId 学生ID
     * @return 题目提交信息列表
     */
    List<Map<String, Object>> getClassQuestionSubmitInfo(Long classId, Long studentId);

    /**
     * 分页获取班级列表
     *
     * @param classQueryRequest 查询请求
     * @return 分页班级VO
     */
    Page<ClassVO> getClassPage(ClassQueryRequest classQueryRequest);

    /**
     * 分页获取班级的学生列表
     *
     * @param classStudentQueryRequest 查询请求
     * @return 分页学生
     */
    Page<User> getClassStudentPage(ClassStudentQueryRequest classStudentQueryRequest);

    /**
     * 为班级添加题目
     *
     * @param classId 班级ID
     * @param questionAddRequest 题目添加请求
     * @return 是否添加成功
     */
    boolean addClassProblems(Long classId, QuestionAddRequest questionAddRequest);

    /**
     * 批量删除班级题目
     *
     * @param classId 班级ID
     * @param problemIds 题目ID列表
     * @return 是否删除成功
     */
    boolean deleteClassProblems(Long classId, List<Long> problemIds);
}
