package com.wlx.ojbackendquestionservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wlx.ojbackendcommon.common.DeleteRequest;
import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.common.Result;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendcommon.utils.JwtUtil;
import com.wlx.ojbackendmodel.model.dto.exam.*;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.*;
import com.wlx.ojbackendquestionservice.service.ExamService;
import com.wlx.ojbackendserviceclient.service.UserFeignClient;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考试模块控制器
 */
@RestController
@RequestMapping("/exam")
@Slf4j
public class ExamController {
    @Resource
    private ExamService examService;

    @Resource
    private UserFeignClient userFeignClient;

    private User getLoginUser(HttpServletRequest request) {
        return userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER));
    }

    // ===== E01 添加考试题目 =====
    @PostMapping("/question/add")
    public ResponseEntity<Long> addExamQuestion(@RequestBody ExamQuestionAddRequest req, HttpServletRequest request) {
        if (req == null) throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        User user = getLoginUser(request);
        return Result.success(examService.addExamQuestion(req, user.getId()));
    }

    // ===== E02 更新考试题目 =====
    @PostMapping("/question/update")
    public ResponseEntity<Boolean> updateExamQuestion(@RequestBody ExamQuestionUpdateRequest req, HttpServletRequest request) {
        if (req == null || req.getId() == null) throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        User user = getLoginUser(request);
        return Result.success(examService.updateExamQuestion(req, user));
    }

    // ===== E03 删除考试题目 =====
    @PostMapping("/question/delete")
    public ResponseEntity<Boolean> deleteExamQuestion(@RequestBody DeleteRequest req, HttpServletRequest request) {
        if (req == null || req.getId() <= 0) throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        User user = getLoginUser(request);
        return Result.success(examService.deleteExamQuestion(req.getId(), user));
    }

    // ===== E04 分页查询考试题目 =====
    @PostMapping("/question/list/page")
    public ResponseEntity<Page<ExamQuestionVO>> listExamQuestionByPage(@RequestBody ExamQuestionQueryRequest req) {
        return Result.success(examService.listExamQuestionByPage(req));
    }

    // ===== E05 获取知识点列表 =====
    @GetMapping("/knowledge/list")
    public ResponseEntity<List<KnowledgePointVO>> listKnowledgePoints(@RequestParam(required = false) Long classId) {
        return Result.success(examService.listKnowledgePoints(classId));
    }

    // ===== E06 添加知识点 =====
    @PostMapping("/knowledge/add")
    public ResponseEntity<Long> addKnowledgePoint(@RequestBody KnowledgePointRequest req, HttpServletRequest request) {
        getLoginUser(request); // 验证登录
        return Result.success(examService.addKnowledgePoint(req));
    }

    // ===== E26 更新知识点 =====
    @PostMapping("/knowledge/update")
    public ResponseEntity<Boolean> updateKnowledgePoint(@RequestBody KnowledgePointRequest req, HttpServletRequest request) {
        getLoginUser(request);
        return Result.success(examService.updateKnowledgePoint(req));
    }

    // ===== E27 删除知识点 =====
    @PostMapping("/knowledge/delete")
    public ResponseEntity<Boolean> deleteKnowledgePoint(@RequestBody DeleteRequest req, HttpServletRequest request) {
        getLoginUser(request);
        return Result.success(examService.deleteKnowledgePoint(req.getId()));
    }

    // ===== E07 管理知识点依赖关系 =====
    @PostMapping("/knowledge/dependency/save")
    public ResponseEntity<Boolean> saveKnowledgeDependencies(@RequestBody KnowledgeDependencyRequest req, HttpServletRequest request) {
        getLoginUser(request);
        return Result.success(examService.saveKnowledgeDependencies(req));
    }

    // ===== E28 获取知识点依赖列表 =====
    @GetMapping("/knowledge/dependency/list")
    public ResponseEntity<List<KnowledgeDependencyVO>> listKnowledgeDependencies(HttpServletRequest request) {
        getLoginUser(request);
        return Result.success(examService.listKnowledgeDependencies());
    }

    // ===== E29 删除知识点依赖 =====
    @PostMapping("/knowledge/dependency/delete")
    public ResponseEntity<Boolean> deleteKnowledgeDependency(@RequestBody DeleteRequest req, HttpServletRequest request) {
        getLoginUser(request);
        return Result.success(examService.deleteKnowledgeDependency(req.getId()));
    }

    // ===== E08 智能组卷 =====
    @PostMapping("/paper/generate")
    public ResponseEntity<ExamPaperVO> generatePaper(@RequestBody ExamPaperGenerateRequest req, HttpServletRequest request) {
        User user = getLoginUser(request);
        return Result.success(examService.generatePaper(req, user.getId()));
    }

    // ===== E09 手动组卷 =====
    @PostMapping("/paper/create")
    public ResponseEntity<ExamPaperVO> createPaper(@RequestBody ExamPaperCreateRequest req, HttpServletRequest request) {
        User user = getLoginUser(request);
        return Result.success(examService.createPaper(req, user.getId()));
    }

    // ===== E25 更新试卷（基础信息） =====
    @PostMapping("/paper/edit")
    public ResponseEntity<Boolean> editPaper(@RequestBody ExamPaperEditRequest req, HttpServletRequest request) {
        if (req == null || req.getId() == null) throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        User user = getLoginUser(request);
        return Result.success(examService.editPaper(req, user.getId()));
    }

    // ===== E10 发布试卷 =====
    @PostMapping("/paper/publish")
    public ResponseEntity<Boolean> publishPaper(@RequestParam Long paperId, HttpServletRequest request) {
        User user = getLoginUser(request);
        return Result.success(examService.publishPaper(paperId, user.getId()));
    }

    // ===== E24 下架试卷 =====
    @PostMapping("/paper/unpublish")
    public ResponseEntity<Boolean> unpublishPaper(@RequestParam Long paperId, HttpServletRequest request) {
        User user = getLoginUser(request);
        return Result.success(examService.unpublishPaper(paperId, user.getId()));
    }

    // ===== E11 查询班级试卷列表 =====
    @PostMapping("/paper/list")
    public ResponseEntity<Page<ExamPaperVO>> listPapers(@RequestBody ExamPaperQueryRequest req, HttpServletRequest request) {
        User user = getLoginUser(request);
        return Result.success(examService.listPapers(req, user.getId()));
    }

    // ===== E21 获取试卷详情 =====
    @GetMapping("/paper/get")
    public ResponseEntity<ExamPaperVO> getPaperById(@RequestParam Long paperId) {
        return Result.success(examService.getPaperById(paperId));
    }

    // ===== E22 添加题目到试卷 =====
    @PostMapping("/paper/question/add")
    public ResponseEntity<Boolean> addQuestionToPaper(@RequestParam Long paperId, @RequestParam Long questionId) {
        return Result.success(examService.addQuestionToPaper(paperId, questionId));
    }

    // ===== E23 从试卷移除题目 =====
    @PostMapping("/paper/question/remove")
    public ResponseEntity<Boolean> removeQuestionFromPaper(@RequestParam Long paperId, @RequestParam Long questionId) {
        return Result.success(examService.removeQuestionFromPaper(paperId, questionId));
    }

    // ===== E12 获取试卷详情（答题用） =====
    @GetMapping("/paper/take/{paperId}")
    public ResponseEntity<ExamTakeVO> getPaperForTake(@PathVariable Long paperId, HttpServletRequest request) {
        User user = getLoginUser(request);
        return Result.success(examService.getPaperForTake(paperId, user.getId()));
    }

    // ===== E13 开始答题 =====
    @PostMapping("/answer/start")
    public ResponseEntity<Long> startExam(@RequestParam Long paperId, HttpServletRequest request) {
        User user = getLoginUser(request);
        return Result.success(examService.startExam(paperId, user.getId()));
    }

    // ===== E14 提交试卷 =====
    @PostMapping("/answer/submit")
    public ResponseEntity<ExamResultVO> submitExamAnswer(@RequestBody ExamAnswerSubmitRequest req, HttpServletRequest request) {
        User user = getLoginUser(request);
        return Result.success(examService.submitExam(req, user.getId()));
    }

    // ===== E15 获取答题结果 =====
    @GetMapping("/answer/result/{recordId}")
    public ResponseEntity<ExamResultVO> getExamResult(@PathVariable Long recordId, HttpServletRequest request) {
        User user = getLoginUser(request);
        return Result.success(examService.getExamResult(recordId, user.getId()));
    }

    // ===== E16 获取能力分析报告 =====
    @GetMapping("/ability/{recordId}")
    public ResponseEntity<AbilityReportVO> getAbilityReport(@PathVariable Long recordId, HttpServletRequest request) {
        User user = getLoginUser(request);
        return Result.success(examService.getAbilityReport(recordId, user.getId()));
    }

    // ===== E17 获取练习推荐列表 =====
    @GetMapping("/practice/recommend/{recordId}")
    public ResponseEntity<List<PracticeRecommendationVO>> getPracticeRecommendations(@PathVariable Long recordId, HttpServletRequest request) {
        User user = getLoginUser(request);
        return Result.success(examService.getPracticeRecommendations(recordId, user.getId()));
    }

    // ===== E18 提交练习答案 =====
    @PostMapping("/practice/submit")
    public ResponseEntity<PracticeResultVO> submitPractice(@RequestBody PracticeSubmitRequest req, HttpServletRequest request) {
        User user = getLoginUser(request);
        return Result.success(examService.submitPractice(req, user.getId()));
    }

    // ===== E19 获取知识图谱数据 =====
    @GetMapping("/knowledge/graph/{classId}")
    public ResponseEntity<KnowledgeGraphVO> getKnowledgeGraph(@PathVariable Long classId) {
        return Result.success(examService.getKnowledgeGraph(classId));
    }

    // ===== E20 获取班级考试统计 =====
    @GetMapping("/stats/{paperId}")
    public ResponseEntity<ExamStatsVO> getExamStats(@PathVariable Long paperId) {
        return Result.success(examService.getExamStats(paperId));
    }
}
