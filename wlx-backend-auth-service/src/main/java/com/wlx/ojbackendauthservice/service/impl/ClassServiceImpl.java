package com.wlx.ojbackendauthservice.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wlx.ojbackendauthservice.mapper.ClassMapper;
import com.wlx.ojbackendauthservice.mapper.ClassProblemMapper;
import com.wlx.ojbackendauthservice.mapper.StudentClassMapper;
import com.wlx.ojbackendauthservice.service.ClassService;
import com.wlx.ojbackendauthservice.service.UserService;
import com.wlx.ojbackendmodel.model.dto.Class.ClassQueryRequest;
import com.wlx.ojbackendmodel.model.dto.Class.ClassStudentQueryRequest;
import com.wlx.ojbackendmodel.model.dto.question.QuestionAddRequest;
import com.wlx.ojbackendmodel.model.vo.ClassVO;
import com.wlx.ojbackendserviceclient.service.QuestionFeignClient;
import com.wlx.ojbackendmodel.model.dto.Class.ClassQuestionQueryRequest;
import com.wlx.ojbackendmodel.model.entity.Class;
import com.wlx.ojbackendmodel.model.entity.ClassProblem;
import com.wlx.ojbackendmodel.model.entity.Question;
import com.wlx.ojbackendmodel.model.entity.QuestionSubmit;
import com.wlx.ojbackendmodel.model.entity.StudentClass;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.QuestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClassServiceImpl extends ServiceImpl<ClassMapper, Class> implements ClassService {
    private final ClassMapper classMapper;
    private final StudentClassMapper studentClassMapper;
    private final ClassProblemMapper classProblemMapper;
    private final QuestionFeignClient questionFeignClient;
    private final UserService userService;
    /**
     * 添加班级，需要老师身份验证
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addClass(String name, Long teacherId) {
        if (StringUtils.isBlank(name) || teacherId == null) {
            return null;
        }
        Class classEntity = new Class();
        classEntity.setName(name);
        classEntity.setTeacherId(teacherId);
        // 生成8位UUID邀请码
        classEntity.setInvitationCode(IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase());
        classEntity.setJoinNumber(0);
        this.save(classEntity);
        log.info("创建班级成功: classId={}, name={}, teacherId={}", classEntity.getId(), name, teacherId);
        return classEntity.getId();
    }

    /**
     * 根据邀请码加入班级
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean joinClass(String invitationCode, Long studentId) {
        if (StringUtils.isBlank(invitationCode) || studentId == null) {
            return false;
        }
        // 根据邀请码查询班级
        Class classInfo = classMapper.selectOne(
                Wrappers.<Class>lambdaQuery().eq(Class::getInvitationCode, invitationCode)
        );
        if (classInfo == null) {
            log.info("邀请码无效: invitationCode={}", invitationCode);
            return false;
        }
        // 检查是否已加入该班级
        StudentClass existRecord = studentClassMapper.selectOne(
                Wrappers.<StudentClass>lambdaQuery()
                        .eq(StudentClass::getStudentId, studentId)
                        .eq(StudentClass::getClassId, classInfo.getId())
        );
        if (existRecord != null) {
            log.info("学生已加入该班级: studentId={}, classId={}", studentId, classInfo.getId());
            return false;
        }
        // 添加学生班级关联
        StudentClass studentClass = new StudentClass();
        studentClass.setStudentId(studentId);
        studentClass.setClassId(classInfo.getId());
        studentClassMapper.insert(studentClass);
        // 更新班级人数
        classInfo.setJoinNumber(classInfo.getJoinNumber() + 1);
        classMapper.updateById(classInfo);
        log.info("学生加入班级成功: studentId={}, classId={}", studentId, classInfo.getId());
        return true;
    }

    /**
     * 退出班级
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitClass(List<Long> classIds, Long studentId) {
        if (CollectionUtils.isEmpty(classIds) || studentId == null) {
            return false;
        }
        // 删除学生班级关联
        int deleteCount = studentClassMapper.delete(
                Wrappers.<StudentClass>lambdaQuery()
                        .eq(StudentClass::getStudentId, studentId)
                        .in(StudentClass::getClassId, classIds)
        );
        // 更新班级人数
        if (deleteCount > 0) {
            for (Long classId : classIds) {
                Class classInfo = classMapper.selectById(classId);
                if (classInfo != null && classInfo.getJoinNumber() > 0) {
                    classInfo.setJoinNumber(classInfo.getJoinNumber() - 1);
                    classMapper.updateById(classInfo);
                }
            }
        }
        log.info("学生退出班级: studentId={}, classIds={}", studentId, classIds);
        return deleteCount > 0;
    }

    /**
     * 分页获取班级题目列表
     */
    @Override
    public Page<QuestionVO> getClassQuestionPage(ClassQuestionQueryRequest classQuestionQueryRequest) {
        Long classId = classQuestionQueryRequest.getClassId();
        if (classId == null) {
            return new Page<>();
        }
        // 获取班级所有题目关联
        List<ClassProblem> classProblems = classProblemMapper.selectList(
                Wrappers.<ClassProblem>lambdaQuery().eq(ClassProblem::getClassId, classId)
        );
        if (CollectionUtils.isEmpty(classProblems)) {
            return new Page<>();
        }
        // 收集所有题目ID
        List<Long> problemIds = classProblems.stream()
                .map(ClassProblem::getProblemId)
                .collect(Collectors.toList());
        long total = problemIds.size();
        // 分页参数
        long current = classQuestionQueryRequest.getCurrent();
        long size = classQuestionQueryRequest.getPageSize();
        // 计算分页的ID列表
        int start = (int) ((current - 1) * size);
        int end = Math.min(start + (int) size, problemIds.size());
        if (start >= problemIds.size()) {
            return new Page<>(current, size, total);
        }
        // 获取分页的题目ID列表
        List<Long> pageProblemIds = new ArrayList<>(problemIds.subList(start, end));
        // 通过Feign批量调用获取题目信息
        List<Question> questions = questionFeignClient.listQuestionsByIds(pageProblemIds);
        // 构建VO
        List<QuestionVO> questionVOS = questions.stream()
                .map(QuestionVO::objToVo)
                .collect(Collectors.toList());
        Page<QuestionVO> page = new Page<>(current, size, total);
        page.setRecords(questionVOS);
        return page;
    }

    /**
     * 获取班级题目的用户提交信息
     */
    @Override
    public List<Map<String, Object>> getClassQuestionSubmitInfo(Long classId, Long studentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (classId == null || studentId == null) {
            return result;
        }
        // 获取班级所有题目关联
        List<ClassProblem> classProblems = classProblemMapper.selectList(
                Wrappers.<ClassProblem>lambdaQuery().eq(ClassProblem::getClassId, classId)
        );
        if (CollectionUtils.isEmpty(classProblems)) {
            return result;
        }
        // 收集题目ID
        List<Long> problemIds = classProblems.stream()
                .map(ClassProblem::getProblemId)
                .collect(Collectors.toList());
        // 通过Feign调用获取题目信息
        List<Question> questions = new ArrayList<>();
        for (Long problemId : problemIds) {
            Question question = questionFeignClient.getQuestionById(problemId);
            if (question != null) {
                questions.add(question);
            }
        }
        // 构建题目ID到题目的映射
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        // 获取用户的提交记录
        Set<Long> questionIdSet = questions.stream()
                .map(Question::getId)
                .collect(Collectors.toSet());
        // 通过Feign批量获取提交记录
        List<QuestionSubmit> submits = questionFeignClient.listQuestionSubmitsByUserAndQuestions(studentId, new ArrayList<>(questionIdSet));
        // 构建 (questionId -> submit) 的映射
        Map<Long, QuestionSubmit> submitMap = submits.stream()
                .collect(Collectors.toMap(QuestionSubmit::getQuestionId, s -> s, (s1, s2) -> s1));
        // 构建返回结果
        for (ClassProblem cp : classProblems) {
            Question question = questionMap.get(cp.getProblemId());
            if (question != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("题目id", question.getId());
                map.put("题目标题", question.getTitle());
                map.put("题目标签", question.getTags());
                QuestionSubmit submit = submitMap.get(question.getId());
                if (submit != null) {
                    map.put("提交id", submit.getId());
                    map.put("提交状态", submit.getStatus());
                    map.put("判题信息", submit.getJudgeInfo());
                    map.put("编程语言", submit.getLanguage());
                } else {
                    map.put("提交状态", null);
                }
                result.add(map);
            }
        }
        log.info("获取班级题目提交信息: classId={}, studentId={}, 结果数量={}", classId, studentId, result.size());
        return result;
    }

    /**
     * 分页获取班级列表
     */
    @Override
    public Page<ClassVO> getClassPage(ClassQueryRequest classQueryRequest) {
        String className = classQueryRequest.getClassName();
        long current = classQueryRequest.getCurrent();
        long size = classQueryRequest.getPageSize();
        // 构建查询条件
        LambdaQueryWrapper<Class> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(className), Class::getName, className);
        queryWrapper.orderByDesc(Class::getCreateTime);
        // 分页查询
        Page<Class> page = this.page(new Page<>(current, size), queryWrapper);
        // 转换为VO
        Page<ClassVO> voPage = new Page<>(current, size, page.getTotal());
        List<ClassVO> voList = page.getRecords().stream().map(classEntity -> {
            ClassVO classVO = new ClassVO();
            BeanUtil.copyProperties(classEntity, classVO);
            return classVO;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 分页获取班级的学生列表
     */
    @Override
    public Page<User> getClassStudentPage(ClassStudentQueryRequest classStudentQueryRequest) {
        Long classId = classStudentQueryRequest.getClassId();
        long current = classStudentQueryRequest.getCurrent();
        long size = classStudentQueryRequest.getPageSize();
        if (classId == null) {
            return new Page<>();
        }
        // 获取班级所有学生关联
        List<StudentClass> studentClasses = studentClassMapper.selectList(
                Wrappers.<StudentClass>lambdaQuery().eq(StudentClass::getClassId, classId)
        );
        if (CollectionUtils.isEmpty(studentClasses)) {
            return new Page<>();
        }
        // 收集学生ID
        List<Long> studentIds = studentClasses.stream()
                .map(StudentClass::getStudentId)
                .collect(Collectors.toList());
        long total = studentIds.size();
        // 计算分页的ID列表
        int start = (int) ((current - 1) * size);
        int end = Math.min(start + (int) size, studentIds.size());
        if (start >= studentIds.size()) {
            return new Page<>(current, size, total);
        }
        // 只获取分页的学生ID
        List<Long> pageStudentIds = new ArrayList<>(studentIds.subList(start, end));
        // 批量查询分页学生信息
        List<User> users = userService.listByIds(pageStudentIds);
        Page<User> page = new Page<>(current, size, total);
        page.setRecords(users);
        return page;
    }

    /**
     * 为班级添加题目
     */
    @Override
    public boolean addClassProblems(Long classId, QuestionAddRequest questionAddRequest) {
        if (classId == null || questionAddRequest == null) {
            return false;
        }
        // 检查班级是否存在
        Class classInfo = classMapper.selectById(classId);
        if (classInfo == null) {
            log.info("班级不存在: classId={}", classId);
            return false;
        }
        // 通过Feign调用创建题目
        Long questionId = questionFeignClient.addQuestion(questionAddRequest);
        if (questionId == null) {
            log.info("创建题目失败");
            return false;
        }
        // 添加班级题目关联
        ClassProblem classProblem = new ClassProblem();
        classProblem.setClassId(classId);
        classProblem.setProblemId(questionId);
        classProblemMapper.insert(classProblem);
        log.info("为班级添加题目成功: classId={}, questionId={}", classId, questionId);
        return true;
    }

    /**
     * 批量删除班级题目
     */
    @Override
    public boolean deleteClassProblems(Long classId, List<Long> problemIds) {
        if (classId == null || CollectionUtils.isEmpty(problemIds)) {
            return false;
        }
        int deleteCount = classProblemMapper.delete(
                Wrappers.<ClassProblem>lambdaQuery()
                        .eq(ClassProblem::getClassId, classId)
                        .in(ClassProblem::getProblemId, problemIds)
        );
        log.info("删除班级题目: classId={}, 删除数量={}", classId, deleteCount);
        return deleteCount > 0;
    }
}
