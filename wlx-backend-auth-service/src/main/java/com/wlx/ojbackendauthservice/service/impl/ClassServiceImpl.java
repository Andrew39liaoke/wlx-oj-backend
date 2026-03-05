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
import com.wlx.ojbackendmodel.model.dto.question.QuestionAddRequest;
import com.wlx.ojbackendmodel.model.dto.Class.ClassTeacherQueryRequest;
import com.wlx.ojbackendmodel.model.dto.Class.ClassStudentQueryRequest;
import com.wlx.ojbackendmodel.model.vo.ClassVO;
import com.wlx.ojbackendserviceclient.service.QuestionFeignClient;
import com.wlx.ojbackendserviceclient.service.QuestionFeignClient;
import com.wlx.ojbackendmodel.model.dto.Class.ClassQuestionQueryRequest;
import com.wlx.ojbackendmodel.model.entity.Class;
import com.wlx.ojbackendmodel.model.entity.ClassProblem;
import com.wlx.ojbackendmodel.model.entity.Question;
import com.wlx.ojbackendmodel.model.entity.QuestionSubmit;
import com.wlx.ojbackendmodel.model.entity.StudentClass;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.QuestionVO;
import com.wlx.ojbackendmodel.model.enums.QuestionSubmitStatusEnum;
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
     * 教师或管理员移除班级中的学生
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeClassStudent(com.wlx.ojbackendmodel.model.dto.Class.ClassStudentRemoveRequest removeRequest) {
        if (removeRequest == null || removeRequest.getClassId() == null || removeRequest.getStudentId() == null) {
            return false;
        }
        Long classId = removeRequest.getClassId();
        Long studentId = removeRequest.getStudentId();
        String userRole = removeRequest.getUserRole();
        Long userId = removeRequest.getUserId();

        // 检查班级是否存在
        Class classInfo = classMapper.selectById(classId);
        if (classInfo == null) {
            log.info("移除失败，班级不存在: classId={}", classId);
            return false;
        }

        // 权限校验：判断删除的老师是否为该班级的创建者，或者为admin
        if (!"admin".equals(userRole)) {
            // 需要验证是否为这个班级的老师
            if (userId == null || !userId.equals(classInfo.getTeacherId())) {
                log.info("移除失败，不是该班级的老师或无权操作: userId={}, classId={}", userId, classId);
                return false;
            }
        }

        // 删除关联
        int deleteCount = studentClassMapper.delete(
                Wrappers.<StudentClass>lambdaQuery()
                        .eq(StudentClass::getClassId, classId)
                        .eq(StudentClass::getStudentId, studentId)
        );

        // 更新人数
        if (deleteCount > 0 && classInfo.getJoinNumber() > 0) {
            classInfo.setJoinNumber(classInfo.getJoinNumber() - 1);
            classMapper.updateById(classInfo);
            log.info("成功移除班级成员: classId={}, studentId={}, 操作人={}", classId, studentId, userId);
        }

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
     * 获取指定班级所有人对某个题目的提交统计数据（通过率、提交数）
     */
    @Override
    public Map<String, Object> getClassQuestionSubmitStats(Long classId, Long questionId) {
        Map<String, Object> result = new HashMap<>();
        result.put("classSubmitNum", 0);
        result.put("classAcceptedNum", 0);

        if (classId == null || questionId == null) {
            return result;
        }

        // 查班级里的所有学生
        List<StudentClass> studentClasses = studentClassMapper.selectList(
                Wrappers.<StudentClass>lambdaQuery().eq(StudentClass::getClassId, classId)
        );

        if (CollectionUtils.isEmpty(studentClasses)) {
            return result;
        }

        List<Long> studentIds = studentClasses.stream()
                .map(StudentClass::getStudentId)
                .collect(Collectors.toList());

        List<QuestionSubmit> submits = questionFeignClient.listQuestionSubmitsByUserIdsAndQuestionId(studentIds, questionId);

        if (CollectionUtils.isEmpty(submits)) {
            return result;
        }

        long submitNum = submits.size();
        long acceptedNum = submits.stream()
                .filter(s -> QuestionSubmitStatusEnum.ACCEPTED.getValue().equals(s.getStatus()))
                .count();

        result.put("classSubmitNum", submitNum);
        result.put("classAcceptedNum", acceptedNum);

        log.info("统计班级题目提交信息: classId={}, questionId={}, submitNum={}, acceptedNum={}", classId, questionId, submitNum, acceptedNum);
        return result;
    }

    /**
     * 获取指定班级所有人对某个题目的详细提交状态（包含谁完成了、未完成、提交次数）
     */
    @Override
    public List<Map<String, Object>> getClassQuestionSubmitDetail(Long classId, Long questionId) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (classId == null || questionId == null) {
            return result;
        }

        // 查班级里的所有学生
        List<StudentClass> studentClasses = studentClassMapper.selectList(
                Wrappers.<StudentClass>lambdaQuery().eq(StudentClass::getClassId, classId)
        );

        if (CollectionUtils.isEmpty(studentClasses)) {
            return result;
        }

        List<Long> studentIds = studentClasses.stream()
                .map(StudentClass::getStudentId)
                .collect(Collectors.toList());

        // 查询这些学生的信息
        List<User> students = userService.listByIds(studentIds);
        Map<Long, User> userMap = students.stream().collect(Collectors.toMap(User::getId, u -> u));

        // 查询这些学生对该题的提交记录
        List<QuestionSubmit> submits = questionFeignClient.listQuestionSubmitsByUserIdsAndQuestionId(studentIds, questionId);

        // 按 userId 分组提交记录
        Map<Long, List<QuestionSubmit>> submitMap = submits.stream()
                .collect(Collectors.groupingBy(QuestionSubmit::getUserId));

        for (Long studentId : studentIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userMap.get(studentId);
            map.put("userId", studentId);
            map.put("userName", user != null ? user.getNickName() : "未知用户");
            map.put("userAvatar", user != null ? user.getUserAvatar() : null);

            List<QuestionSubmit> userSubmits = submitMap.getOrDefault(studentId, new ArrayList<>());
            map.put("submitCount", userSubmits.size());

            boolean isAccepted = userSubmits.stream()
                    .anyMatch(s -> QuestionSubmitStatusEnum.ACCEPTED.getValue().equals(s.getStatus()));
            map.put("isAccepted", isAccepted);

            result.add(map);
        }

        log.info("查询班级题目详细提交状态: classId={}, questionId={}, 学生数量={}", classId, questionId, result.size());
        return result;
    }

    /**
     * 分页获取班级列表
     */
    @Override
    public Page<ClassVO> getClassPage(ClassQueryRequest classQueryRequest) {
        Long id = classQueryRequest.getId();
        String className = classQueryRequest.getClassName();
        long current = classQueryRequest.getCurrent();
        long size = classQueryRequest.getPageSize();
        // 构建查询条件
        LambdaQueryWrapper<Class> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(id != null, Class::getId, id);
        queryWrapper.like(StringUtils.isNotBlank(className), Class::getName, className);
        queryWrapper.orderByDesc(Class::getCreateTime);
        // 分页查询
        Page<Class> page = this.page(new Page<>(current, size), queryWrapper);
        // 转换为VO
        Page<ClassVO> voPage = new Page<>(current, size, page.getTotal());
        List<ClassVO> voList = page.getRecords().stream().map(classEntity -> {
            ClassVO classVO = new ClassVO();
            BeanUtil.copyProperties(classEntity, classVO);
            // 根据教师ID查询教师名称
            if (classEntity.getTeacherId() != null) {
                User teacher = userService.getById(classEntity.getTeacherId());
                if (teacher != null) {
                    classVO.setTeacherName(teacher.getNickName());
                }
            }
            return classVO;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 分页获取某个学生所在的所有班级列表
     */
    @Override
    public Page<ClassVO> getStudentClasses(ClassQueryRequest classQueryRequest) {
        Long id = classQueryRequest.getId();
        Long studentId = classQueryRequest.getStudentId();
        String className = classQueryRequest.getClassName();
        long current = classQueryRequest.getCurrent();
        long size = classQueryRequest.getPageSize();
        if (studentId == null) {
            return new Page<>();
        }
        // 先根据学生ID查询学生加入的所有班级关联
        LambdaQueryWrapper<StudentClass> studentClassWrapper = new LambdaQueryWrapper<>();
        studentClassWrapper.eq(StudentClass::getStudentId, studentId);
        List<StudentClass> studentClasses = studentClassMapper.selectList(studentClassWrapper);
        if (CollectionUtils.isEmpty(studentClasses)) {
            return new Page<>();
        }
        // 收集班级ID
        List<Long> classIds = studentClasses.stream()
                .map(StudentClass::getClassId)
                .collect(Collectors.toList());
        // 根据班级ID列表和班级名称查询班级
        LambdaQueryWrapper<Class> classWrapper = new LambdaQueryWrapper<>();
        classWrapper.in(Class::getId, classIds);
        classWrapper.eq(id != null, Class::getId, id);
        classWrapper.like(StringUtils.isNotBlank(className), Class::getName, className);
        classWrapper.orderByDesc(Class::getCreateTime);
        // 分页查询
        Page<Class> page = this.page(new Page<>(current, size), classWrapper);
        // 转换为VO
        Page<ClassVO> voPage = new Page<>(current, size, page.getTotal());
        List<ClassVO> voList = page.getRecords().stream().map(classEntity -> {
            ClassVO classVO = new ClassVO();
            BeanUtil.copyProperties(classEntity, classVO);
            // 根据教师ID查询教师名称
            if (classEntity.getTeacherId() != null) {
                User teacher = userService.getById(classEntity.getTeacherId());
                if (teacher != null) {
                    classVO.setTeacherName(teacher.getNickName());
                }
            }
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

    /**
     * 分页获取某个教师创建的班级列表
     */
    @Override
    public Page<ClassVO> getTeacherClasses(ClassTeacherQueryRequest classTeacherQueryRequest) {
        Long teacherId = classTeacherQueryRequest.getTeacherId();
        String className = classTeacherQueryRequest.getClassName();
        long current = classTeacherQueryRequest.getCurrent();
        long size = classTeacherQueryRequest.getPageSize();
        if (teacherId == null) {
            return new Page<>();
        }

        // 根据教师ID和班级名称查询班级
        LambdaQueryWrapper<Class> classWrapper = new LambdaQueryWrapper<>();
        classWrapper.eq(Class::getTeacherId, teacherId);
        classWrapper.like(StringUtils.isNotBlank(className), Class::getName, className);
        classWrapper.orderByDesc(Class::getCreateTime);

        // 分页查询
        Page<Class> page = this.page(new Page<>(current, size), classWrapper);
        // 转换为VO
        Page<ClassVO> voPage = new Page<>(current, size, page.getTotal());
        if(CollectionUtils.isEmpty(page.getRecords())) {
            return voPage;
        }

        List<ClassVO> voList = page.getRecords().stream().map(classEntity -> {
            ClassVO classVO = new ClassVO();
            BeanUtil.copyProperties(classEntity, classVO);
            return classVO;
        }).collect(Collectors.toList());

        // 查询名字填充vo
        User teacher = userService.getById(teacherId);
        if (teacher != null) {
            voList.forEach(vo -> vo.setTeacherName(teacher.getNickName()));
        }

        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 批量删除班级
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteClasses(List<Long> classIds, Long teacherId) {
        if (CollectionUtils.isEmpty(classIds) || teacherId == null) {
            return false;
        }
        // 校验这些班级是否都属于该教师
        LambdaQueryWrapper<Class> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Class::getId, classIds)
                .eq(Class::getTeacherId, teacherId);

        List<Class> classList = classMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(classList)) {
            return false;
        }
        // 获取有权限删除的实际 classId 列表
        List<Long> validClassIds = classList.stream().map(Class::getId).collect(Collectors.toList());

        // 删除关联的学生记录
        studentClassMapper.delete(
                Wrappers.<StudentClass>lambdaQuery().in(StudentClass::getClassId, validClassIds)
        );
        // 删除关联的班级题目记录
        classProblemMapper.delete(
                Wrappers.<ClassProblem>lambdaQuery().in(ClassProblem::getClassId, validClassIds)
        );

        // 删除班级本身
        int deleteCount = classMapper.deleteBatchIds(validClassIds);
        log.info("教师删除班级: teacherId={}, 删除的classIds={}", teacherId, validClassIds);
        return deleteCount > 0;
    }

    /**
     * 获取班级统计图表数据
     */
    @Override
    public Map<String, Object> getClassStatsCharts(Long classId) {
        Map<String, Object> result = new HashMap<>();
        result.put("languageStats", new ArrayList<>());
        result.put("studentRanking", new ArrayList<>());
        result.put("dailyTrend", new ArrayList<>());
        result.put("questionDifficulty", new ArrayList<>());
        result.put("tagCoverage", new ArrayList<>());

        if (classId == null) {
            return result;
        }

        // 1. 查班级所有学生
        List<StudentClass> studentClasses = studentClassMapper.selectList(
                Wrappers.<StudentClass>lambdaQuery().eq(StudentClass::getClassId, classId)
        );
        if (CollectionUtils.isEmpty(studentClasses)) {
            return result;
        }
        List<Long> studentIds = studentClasses.stream()
                .map(StudentClass::getStudentId)
                .collect(Collectors.toList());

        // 2. 查班级所有题目
        List<ClassProblem> classProblems = classProblemMapper.selectList(
                Wrappers.<ClassProblem>lambdaQuery().eq(ClassProblem::getClassId, classId)
        );
        if (CollectionUtils.isEmpty(classProblems)) {
            return result;
        }
        List<Long> questionIds = classProblems.stream()
                .map(ClassProblem::getProblemId)
                .collect(Collectors.toList());

        // 3. 查所有学生对这些题目的提交记录
        List<QuestionSubmit> allSubmits = new ArrayList<>();
        for (Long questionId : questionIds) {
            List<QuestionSubmit> submits = questionFeignClient.listQuestionSubmitsByUserIdsAndQuestionId(studentIds, questionId);
            if (!CollectionUtils.isEmpty(submits)) {
                allSubmits.addAll(submits);
            }
        }

        if (CollectionUtils.isEmpty(allSubmits)) {
            return result;
        }

        // === 图表1: 编程语言分布 ===
        Map<String, Long> langMap = allSubmits.stream()
                .filter(s -> StringUtils.isNotBlank(s.getLanguage()))
                .collect(Collectors.groupingBy(QuestionSubmit::getLanguage, Collectors.counting()));
        List<Map<String, Object>> languageStats = new ArrayList<>();
        langMap.forEach((lang, count) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("language", lang);
            m.put("count", count);
            languageStats.add(m);
        });
        result.put("languageStats", languageStats);

        // === 图表2: 学生提交排行 Top 10 ===
        Map<Long, Long> userSubmitCount = allSubmits.stream()
                .collect(Collectors.groupingBy(QuestionSubmit::getUserId, Collectors.counting()));
        // 查用户信息
        List<User> users = userService.listByIds(studentIds);
        Map<Long, String> userNameMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u.getNickName() != null ? u.getNickName() : "未知用户", (a, b) -> a));
        List<Map<String, Object>> studentRanking = userSubmitCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("userName", userNameMap.getOrDefault(entry.getKey(), "未知用户"));
                    m.put("submitCount", entry.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        result.put("studentRanking", studentRanking);

        // === 图表3: 每日提交趋势（近30天）===
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate start = now.minusDays(29);
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Map<String, Long> dailyMap = allSubmits.stream()
                .filter(s -> s.getCreateTime() != null)
                .collect(Collectors.groupingBy(
                        s -> new java.text.SimpleDateFormat("yyyy-MM-dd").format(s.getCreateTime()),
                        Collectors.counting()
                ));
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            java.time.LocalDate day = start.plusDays(i);
            String dateStr = day.format(fmt);
            Map<String, Object> m = new HashMap<>();
            m.put("date", dateStr);
            m.put("count", dailyMap.getOrDefault(dateStr, 0L));
            dailyTrend.add(m);
        }
        result.put("dailyTrend", dailyTrend);

        // === 图表4: 题目难度排行（按通过率排序）===
        List<Map<String, Object>> questionDifficulty = new ArrayList<>();
        for (Long qId : questionIds) {
            Question q = questionFeignClient.getQuestionById(qId);
            if (q != null) {
                Map<String, Object> m = new HashMap<>();
                m.put("title", q.getTitle());
                int sNum = q.getSubmitNum() != null ? q.getSubmitNum() : 0;
                int aNum = q.getAcceptedNum() != null ? q.getAcceptedNum() : 0;
                double rate = sNum > 0 ? (aNum * 100.0 / sNum) : 0;
                m.put("passRate", Math.round(rate * 10.0) / 10.0);
                questionDifficulty.add(m);
            }
        }
        result.put("questionDifficulty", questionDifficulty);

        // === 图表5: 题目标签覆盖度 ===
        Map<String, Integer> tagCountMap = new HashMap<>();
        for (Long qId : questionIds) {
            Question q = questionFeignClient.getQuestionById(qId);
            if (q != null && StringUtils.isNotBlank(q.getTags())) {
                try {
                    com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(q.getTags()).getAsJsonArray();
                    for (int i = 0; i < arr.size(); i++) {
                        String tag = arr.get(i).getAsString();
                        tagCountMap.put(tag, tagCountMap.getOrDefault(tag, 0) + 1);
                    }
                } catch (Exception e) {
                    log.warn("解析题目标签失败: questionId={}, tags={}", qId, q.getTags());
                }
            }
        }
        List<Map<String, Object>> tagCoverage = new ArrayList<>();
        tagCountMap.forEach((tag, count) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("tag", tag);
            m.put("count", count);
            tagCoverage.add(m);
        });
        result.put("tagCoverage", tagCoverage);

        log.info("获取班级统计图表数据: classId={}, 总提交记录={}", classId, allSubmits.size());
        return result;
    }
}
