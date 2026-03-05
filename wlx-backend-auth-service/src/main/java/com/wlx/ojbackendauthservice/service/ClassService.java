package com.wlx.ojbackendauthservice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wlx.ojbackendmodel.model.dto.Class.*;
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
     * 移除班级学生（教师或管理员操作）
     *
     * @param removeRequest 移除请求DTO
     * @return 是否移除成功
     */
    boolean removeClassStudent(ClassStudentRemoveRequest removeRequest);

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
     * 获取指定班级所有人对某个题目的提交统计数据（通过率、提交数）
     *
     * @param classId 班级ID
     * @param questionId 题目ID
     * @return 包含提交数和通过数的 Map
     */
    Map<String, Object> getClassQuestionSubmitStats(Long classId, Long questionId);

    /**
     * 获取指定班级所有人对某个题目的详细提交状态（包含谁完成了、未完成、提交次数）
     *
     * @param classId 班级ID
     * @param questionId 题目ID
     * @return 包含学生名称、完成状态、提交次数的详情列表
     */
    List<Map<String, Object>> getClassQuestionSubmitDetail(Long classId, Long questionId);


    /**
     * 分页获取班级列表
     *
     * @param classQueryRequest 查询请求
     * @return 分页班级VO
     */
    Page<ClassVO> getClassPage(ClassQueryRequest classQueryRequest);

    /**
     * 分页获取某个学生所在的所有班级列表
     *
     * @param classQueryRequest 查询请求（包含学生ID和班级名称）
     * @return 分页班级VO
     */
    Page<ClassVO> getStudentClasses(ClassQueryRequest classQueryRequest);

    /**
     * 分页获取某个教师创建的班级列表
     * @param classTeacherQueryRequest 查询请求（包含教师ID和名称）
     * @return 分页班级VO
     */
    Page<ClassVO> getTeacherClasses(ClassTeacherQueryRequest classTeacherQueryRequest);

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
    /**
     * 批量删除班级
     *
     * @param classIds 班级ID列表
     * @param teacherId 教师ID
     * @return 是否删除成功
     */
    boolean deleteClasses(List<Long> classIds, Long teacherId);

    /**
     * 获取班级统计图表数据（语言分布、学生排行、状态分布、每日提交趋势）
     *
     * @param classId 班级ID
     * @return 聚合统计数据
     */
    Map<String, Object> getClassStatsCharts(Long classId);
}
