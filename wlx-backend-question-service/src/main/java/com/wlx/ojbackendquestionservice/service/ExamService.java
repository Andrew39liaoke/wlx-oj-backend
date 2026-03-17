package com.wlx.ojbackendquestionservice.service;

import com.wlx.ojbackendmodel.model.entity.User;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wlx.ojbackendmodel.model.dto.exam.*;
import com.wlx.ojbackendmodel.model.entity.ExamQuestion;
import com.wlx.ojbackendmodel.model.vo.*;

import java.util.List;

/**
 * 考试模块核心服务接口
 */
public interface ExamService {

    // ===== 考试题目管理 =====
    Long addExamQuestion(ExamQuestionAddRequest request, Long userId);
    Boolean updateExamQuestion(ExamQuestionUpdateRequest request, User loginUser);
    Boolean deleteExamQuestion(Long id, User loginUser);
    Page<ExamQuestionVO> listExamQuestionByPage(ExamQuestionQueryRequest request);

    // ===== 知识点管理 =====
    List<KnowledgePointVO> listKnowledgePoints(Long classId);
    Long addKnowledgePoint(KnowledgePointRequest request);
    Boolean updateKnowledgePoint(KnowledgePointRequest request);
    Boolean deleteKnowledgePoint(Long id);
    Boolean saveKnowledgeDependencies(KnowledgeDependencyRequest request);
    List<KnowledgeDependencyVO> listKnowledgeDependencies();
    Boolean deleteKnowledgeDependency(Long id);
    KnowledgeGraphVO getKnowledgeGraph(Long classId);

    // ===== 试卷管理 =====
    ExamPaperVO generatePaper(ExamPaperGenerateRequest request, Long userId);
    ExamPaperVO createPaper(ExamPaperCreateRequest request, Long userId);
    Boolean publishPaper(Long paperId, Long userId);
    Boolean unpublishPaper(Long paperId, Long userId);
    Page<ExamPaperVO> listPapers(ExamPaperQueryRequest request, Long userId);
    ExamPaperVO getPaperById(Long paperId);
    Boolean addQuestionToPaper(Long paperId, Long questionId);
    Boolean removeQuestionFromPaper(Long paperId, Long questionId);
    Boolean editPaper(ExamPaperEditRequest request, Long userId);

    // ===== 答题 =====
    ExamTakeVO getPaperForTake(Long paperId, Long userId);
    Long startExam(Long paperId, Long userId);
    ExamResultVO submitExam(ExamAnswerSubmitRequest request, Long userId);
    ExamResultVO getExamResult(Long recordId, Long userId);

    // ===== 能力评估 =====
    AbilityReportVO getAbilityReport(Long recordId, Long userId);

    // ===== 练习推荐 =====
    List<PracticeRecommendationVO> getPracticeRecommendations(Long recordId, Long userId);
    PracticeResultVO submitPractice(PracticeSubmitRequest request, Long userId);

    // ===== 统计 =====
    ExamStatsVO getExamStats(Long paperId);
}
