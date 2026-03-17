package com.wlx.ojbackendquestionservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendmodel.model.dto.exam.*;
import com.wlx.ojbackendmodel.model.entity.*;
import com.wlx.ojbackendmodel.model.enums.*;
import com.wlx.ojbackendmodel.model.vo.*;
import com.wlx.ojbackendquestionservice.mapper.*;
import com.wlx.ojbackendquestionservice.service.ExamService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExamServiceImpl implements ExamService {

    @Resource
    private ExamQuestionMapper examQuestionMapper;
    @Resource
    private KnowledgePointMapper knowledgePointMapper;
    @Resource
    private KnowledgeDependencyMapper knowledgeDependencyMapper;
    @Resource
    private ExamPaperMapper examPaperMapper;
    @Resource
    private ExamPaperQuestionMapper examPaperQuestionMapper;
    @Resource
    private ExamAnswerRecordMapper examAnswerRecordMapper;
    @Resource
    private ExamAnswerDetailMapper examAnswerDetailMapper;
    @Resource
    private KnowledgeAbilityMapper knowledgeAbilityMapper;
    @Resource
    private PracticeRecommendationMapper practiceRecommendationMapper;

    private static final Gson GSON = new Gson();

    // ===== 考试题目管理 =====

    @Override
    public Long addExamQuestion(ExamQuestionAddRequest request, Long userId) {
        ExamQuestion question = new ExamQuestion();
        question.setTitle(request.getTitle());
        question.setQuestionType(request.getQuestionType());
        question.setOptions(GSON.toJson(request.getOptions()));
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setScore(request.getScore() != null ? request.getScore() : 5);
        question.setDifficulty(request.getDifficulty());
        question.setKnowledgeIds(request.getKnowledgeIds());
        question.setTags(request.getTags() != null ? GSON.toJson(request.getTags()) : null);
        question.setAnalysis(request.getAnalysis());
        question.setUserId(userId);
        examQuestionMapper.insert(question);
        return question.getId();
    }

    @Override
    public Boolean updateExamQuestion(ExamQuestionUpdateRequest request, User loginUser) {
        ExamQuestion old = examQuestionMapper.selectById(request.getId());
        if (old == null) throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);
        // 仅限题目创建者或管理员编辑
        if (!old.getUserId().equals(loginUser.getId()) && !UserRoleEnum.ADMIN.getValue().equals(loginUser.getRole())) {
            throw new BusinessException(ResopnseCodeEnum.NO_AUTH_ERROR);
        }
        ExamQuestion question = new ExamQuestion();
        question.setId(request.getId());
        if (request.getTitle() != null) question.setTitle(request.getTitle());
        if (request.getQuestionType() != null) question.setQuestionType(request.getQuestionType());
        if (request.getOptions() != null) question.setOptions(GSON.toJson(request.getOptions()));
        if (request.getCorrectAnswer() != null) question.setCorrectAnswer(request.getCorrectAnswer());
        if (request.getScore() != null) question.setScore(request.getScore());
        if (request.getDifficulty() != null) question.setDifficulty(request.getDifficulty());
        if (request.getKnowledgeIds() != null) question.setKnowledgeIds(request.getKnowledgeIds());
        if (request.getTags() != null) question.setTags(GSON.toJson(request.getTags()));
        if (request.getAnalysis() != null) question.setAnalysis(request.getAnalysis());
        return examQuestionMapper.updateById(question) > 0;
    }

    @Override
    public Boolean deleteExamQuestion(Long id, User loginUser) {
        ExamQuestion old = examQuestionMapper.selectById(id);
        if (old == null) throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);
        // 仅限题目创建者或管理员删除
        if (!old.getUserId().equals(loginUser.getId()) && !UserRoleEnum.ADMIN.getValue().equals(loginUser.getRole())) {
            throw new BusinessException(ResopnseCodeEnum.NO_AUTH_ERROR);
        }
        return examQuestionMapper.deleteById(id) > 0;
    }

    @Override
    public Page<ExamQuestionVO> listExamQuestionByPage(ExamQuestionQueryRequest request) {
        LambdaQueryWrapper<ExamQuestion> wrapper = new LambdaQueryWrapper<>();
        if (request.getQuestionType() != null) wrapper.eq(ExamQuestion::getQuestionType, request.getQuestionType());
        if (request.getDifficulty() != null) wrapper.eq(ExamQuestion::getDifficulty, request.getDifficulty());
        if (StringUtils.isNotBlank(request.getTitle())) wrapper.like(ExamQuestion::getTitle, request.getTitle());
        if (request.getPaperId() != null) {
            List<ExamPaperQuestion> pqs = examPaperQuestionMapper.selectList(
                    new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, request.getPaperId()));
            List<Long> qIds = pqs.stream().map(ExamPaperQuestion::getQuestionId).collect(Collectors.toList());
            if (qIds.isEmpty()) {
                return new Page<>(request.getCurrent(), request.getPageSize(), 0);
            }
            wrapper.in(ExamQuestion::getId, qIds);
        }
        wrapper.orderByDesc(ExamQuestion::getCreateTime);

        Page<ExamQuestion> page = examQuestionMapper.selectPage(
                new Page<>(request.getCurrent(), request.getPageSize()), wrapper);

        Page<ExamQuestionVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<ExamQuestionVO> voList = page.getRecords().stream().map(this::toExamQuestionVO).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    private ExamQuestionVO toExamQuestionVO(ExamQuestion q) {
        ExamQuestionVO vo = new ExamQuestionVO();
        vo.setId(q.getId());
        vo.setTitle(q.getTitle());
        vo.setQuestionType(q.getQuestionType());
        vo.setOptions(GSON.fromJson(q.getOptions(), new TypeToken<List<OptionItem>>(){}.getType()));
        vo.setCorrectAnswer(q.getCorrectAnswer());
        vo.setScore(q.getScore());
        vo.setDifficulty(q.getDifficulty());
        vo.setDifficultyLabel(DifficultyEnum.getLabel(q.getDifficulty()));
        vo.setKnowledgeIds(q.getKnowledgeIds());
        vo.setTags(q.getTags() != null ? GSON.fromJson(q.getTags(), new TypeToken<List<String>>(){}.getType()) : null);
        vo.setAnalysis(q.getAnalysis());
        vo.setUserId(q.getUserId());
        vo.setCreateTime(q.getCreateTime());
        // 解析知识点名称
        if (StringUtils.isNotBlank(q.getKnowledgeIds())) {
            List<Long> kIds = parseIds(q.getKnowledgeIds());
            List<KnowledgePoint> kps = knowledgePointMapper.selectBatchIds(kIds);
            vo.setKnowledgeTags(kps.stream().map(KnowledgePoint::getName).collect(Collectors.toList()));
        }
        return vo;
    }

    // ===== 知识点管理 =====

    @Override
    public List<KnowledgePointVO> listKnowledgePoints(Long classId) {
        LambdaQueryWrapper<KnowledgePoint> wrapper = new LambdaQueryWrapper<>();
        if (classId != null) {
            wrapper.and(w -> w.eq(KnowledgePoint::getClassId, classId).or().isNull(KnowledgePoint::getClassId));
        }
        wrapper.orderByAsc(KnowledgePoint::getSortOrder);
        List<KnowledgePoint> list = knowledgePointMapper.selectList(wrapper);
        return buildTree(list);
    }

    private List<KnowledgePointVO> buildTree(List<KnowledgePoint> list) {
        Map<Long, KnowledgePointVO> map = new LinkedHashMap<>();
        list.forEach(kp -> {
            KnowledgePointVO vo = new KnowledgePointVO();
            vo.setId(kp.getId()); vo.setName(kp.getName()); vo.setDescription(kp.getDescription());
            vo.setParentId(kp.getParentId()); vo.setClassId(kp.getClassId()); vo.setSortOrder(kp.getSortOrder());
            vo.setChildren(new ArrayList<>());
            map.put(kp.getId(), vo);
        });
        List<KnowledgePointVO> roots = new ArrayList<>();
        map.values().forEach(vo -> {
            if (vo.getParentId() != null && map.containsKey(vo.getParentId())) {
                map.get(vo.getParentId()).getChildren().add(vo);
            } else {
                roots.add(vo);
            }
        });
        return roots;
    }

    @Override
    public Long addKnowledgePoint(KnowledgePointRequest request) {
        KnowledgePoint kp = new KnowledgePoint();
        kp.setName(request.getName());
        kp.setDescription(request.getDescription());
        kp.setParentId(request.getParentId());
        kp.setClassId(request.getClassId());
        kp.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        knowledgePointMapper.insert(kp);
        return kp.getId();
    }

    @Override
    public Boolean updateKnowledgePoint(KnowledgePointRequest request) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        KnowledgePoint kp = knowledgePointMapper.selectById(request.getId());
        if (kp == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);
        }
        if (request.getName() != null) kp.setName(request.getName());
        if (request.getDescription() != null) kp.setDescription(request.getDescription());
        if (request.getParentId() != null) kp.setParentId(request.getParentId());
        if (request.getClassId() != null) kp.setClassId(request.getClassId());
        if (request.getSortOrder() != null) kp.setSortOrder(request.getSortOrder());
        return knowledgePointMapper.updateById(kp) > 0;
    }

    @Override
    public Boolean deleteKnowledgePoint(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        return knowledgePointMapper.deleteById(id) > 0;
    }

    @Override
    public List<KnowledgeDependencyVO> listKnowledgeDependencies() {
        List<KnowledgeDependency> list = knowledgeDependencyMapper.selectList(null);
        if (list.isEmpty()) return new ArrayList<>();

        // 获取所有涉及的知识点 ID
        Set<Long> kIds = new HashSet<>();
        list.forEach(item -> {
            kIds.add(item.getFromKnowledgeId());
            kIds.add(item.getToKnowledgeId());
        });

        // 批量查询知识点名称
        Map<Long, String> nameMap = new HashMap<>();
        if (!kIds.isEmpty()) {
            List<KnowledgePoint> kps = knowledgePointMapper.selectBatchIds(kIds);
            kps.forEach(kp -> nameMap.put(kp.getId(), kp.getName()));
        }

        return list.stream().map(item -> {
            KnowledgeDependencyVO vo = new KnowledgeDependencyVO();
            vo.setId(item.getId());
            vo.setFromKnowledgeId(item.getFromKnowledgeId());
            vo.setFromKnowledgeName(nameMap.getOrDefault(item.getFromKnowledgeId(), "未知"));
            vo.setToKnowledgeId(item.getToKnowledgeId());
            vo.setToKnowledgeName(nameMap.getOrDefault(item.getToKnowledgeId(), "未知"));
            vo.setDependencyType(item.getDependencyType());
            vo.setWeight(item.getWeight());
            vo.setCreateTime(item.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Boolean deleteKnowledgeDependency(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        return knowledgeDependencyMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public Boolean saveKnowledgeDependencies(KnowledgeDependencyRequest request) {
        if (request.getDependencies() == null) return true;
        for (KnowledgeDependencyRequest.DependencyItem item : request.getDependencies()) {
            // 检查是否已存在
            LambdaQueryWrapper<KnowledgeDependency> w = new LambdaQueryWrapper<>();
            w.eq(KnowledgeDependency::getFromKnowledgeId, item.getFromKnowledgeId())
             .eq(KnowledgeDependency::getToKnowledgeId, item.getToKnowledgeId());
            if (knowledgeDependencyMapper.selectCount(w) == 0) {
                KnowledgeDependency dep = new KnowledgeDependency();
                dep.setFromKnowledgeId(item.getFromKnowledgeId());
                dep.setToKnowledgeId(item.getToKnowledgeId());
                dep.setDependencyType(item.getDependencyType() != null ? item.getDependencyType() : 1);
                dep.setWeight(item.getWeight() != null ? item.getWeight() : 1.0);
                knowledgeDependencyMapper.insert(dep);
            }
        }
        return true;
    }

    @Override
    public KnowledgeGraphVO getKnowledgeGraph(Long classId) {
        List<KnowledgePoint> kps = knowledgePointMapper.selectList(
            new LambdaQueryWrapper<KnowledgePoint>()
                .and(w -> w.eq(KnowledgePoint::getClassId, classId).or().isNull(KnowledgePoint::getClassId)));
        List<KnowledgeDependency> deps = knowledgeDependencyMapper.selectList(null);

        KnowledgeGraphVO vo = new KnowledgeGraphVO();
        vo.setNodes(kps.stream().map(kp -> {
            KnowledgeGraphVO.GraphNode n = new KnowledgeGraphVO.GraphNode();
            n.setId(kp.getId()); n.setName(kp.getName());
            return n;
        }).collect(Collectors.toList()));
        vo.setEdges(deps.stream().map(d -> {
            KnowledgeGraphVO.GraphEdge e = new KnowledgeGraphVO.GraphEdge();
            e.setFrom(d.getFromKnowledgeId()); e.setTo(d.getToKnowledgeId());
            e.setType(d.getDependencyType()); e.setWeight(d.getWeight());
            return e;
        }).collect(Collectors.toList()));
        return vo;
    }

    // ===== 试卷管理 =====

    @Override
    @Transactional
    public ExamPaperVO generatePaper(ExamPaperGenerateRequest request, Long userId) {
        // 分层抽样组卷算法
        LambdaQueryWrapper<ExamQuestion> baseWrapper = new LambdaQueryWrapper<>();
        List<ExamQuestion> allQuestions = examQuestionMapper.selectList(baseWrapper);

        // 构建分层题池: key = "type_difficulty"
        Map<String, List<ExamQuestion>> pools = new HashMap<>();
        for (ExamQuestion q : allQuestions) {
            String key = q.getQuestionType() + "_" + q.getDifficulty();
            pools.computeIfAbsent(key, k -> new ArrayList<>()).add(q);
        }

        int singleCount = request.getSingleCount();
        int multiCount = request.getMultiCount();
        int totalQ = request.getTotalQuestions();
        int easyCount = request.getEasyCount();
        int mediumCount = request.getMediumCount();
        int hardCount = request.getHardCount();

        // 计算每层配额
        Map<String, Integer> quotas = new HashMap<>();
        quotas.put("1_1", Math.round((float) singleCount * easyCount / totalQ));
        quotas.put("1_2", Math.round((float) singleCount * mediumCount / totalQ));
        quotas.put("1_3", singleCount - quotas.getOrDefault("1_1", 0) - quotas.getOrDefault("1_2", 0));
        quotas.put("2_1", Math.round((float) multiCount * easyCount / totalQ));
        quotas.put("2_2", Math.round((float) multiCount * mediumCount / totalQ));
        quotas.put("2_3", multiCount - quotas.getOrDefault("2_1", 0) - quotas.getOrDefault("2_2", 0));

        List<ExamQuestion> selected = new ArrayList<>();
        Set<Long> selectedIds = new HashSet<>();

        // 知识点覆盖优先
        if (request.getKnowledgeIds() != null) {
            for (Long kId : request.getKnowledgeIds()) {
                if (selected.size() >= totalQ) break;
                for (Map.Entry<String, List<ExamQuestion>> entry : pools.entrySet()) {
                    if (quotas.getOrDefault(entry.getKey(), 0) <= 0) continue;
                    Optional<ExamQuestion> found = entry.getValue().stream()
                            .filter(q -> !selectedIds.contains(q.getId()) && containsKnowledge(q, kId))
                            .findFirst();
                    if (found.isPresent()) {
                        selected.add(found.get());
                        selectedIds.add(found.get().getId());
                        quotas.merge(entry.getKey(), -1, Integer::sum);
                        break;
                    }
                }
            }
        }

        // 按配额随机补充
        for (Map.Entry<String, Integer> entry : quotas.entrySet()) {
            int remaining = entry.getValue();
            if (remaining <= 0) continue;
            List<ExamQuestion> pool = pools.getOrDefault(entry.getKey(), new ArrayList<>());
            Collections.shuffle(pool);
            for (ExamQuestion q : pool) {
                if (remaining <= 0) break;
                if (!selectedIds.contains(q.getId())) {
                    selected.add(q);
                    selectedIds.add(q.getId());
                    remaining--;
                }
            }
        }

        // 随机排序
        Collections.shuffle(selected);

        // 计算总分
        int totalScore = selected.stream().mapToInt(ExamQuestion::getScore).sum();

        // 保存试卷
        ExamPaper paper = new ExamPaper();
        paper.setTitle(request.getTitle());
        paper.setDescription(request.getDescription());
        paper.setClassId(request.getClassId());
        paper.setTotalScore(totalScore);
        paper.setQuestionCount(selected.size());
        paper.setTimeLimit(request.getTimeLimit());
        paper.setSingleCount((int) selected.stream().filter(q -> q.getQuestionType() == 1).count());
        paper.setMultiCount((int) selected.stream().filter(q -> q.getQuestionType() == 2).count());
        paper.setDifficultyDist(GSON.toJson(Map.of("easy", easyCount, "medium", mediumCount, "hard", hardCount)));
        paper.setKnowledgeIds(request.getKnowledgeIds() != null ?
                request.getKnowledgeIds().stream().map(String::valueOf).collect(Collectors.joining(",")) : null);
        paper.setStatus(0);
        paper.setStartTime(request.getStartTime());
        paper.setEndTime(request.getEndTime());
        paper.setCreatorId(userId);
        examPaperMapper.insert(paper);

        // 保存试卷-题目关联
        for (int i = 0; i < selected.size(); i++) {
            ExamPaperQuestion pq = new ExamPaperQuestion();
            pq.setPaperId(paper.getId());
            pq.setQuestionId(selected.get(i).getId());
            pq.setQuestionOrder(i + 1);
            pq.setScore(selected.get(i).getScore());
            examPaperQuestionMapper.insert(pq);
        }

        return toPaperVO(paper);
    }

    @Override
    @Transactional
    public ExamPaperVO createPaper(ExamPaperCreateRequest request, Long userId) {
        List<ExamQuestion> questions = examQuestionMapper.selectBatchIds(request.getQuestionIds());
        int totalScore = questions.stream().mapToInt(ExamQuestion::getScore).sum();

        ExamPaper paper = new ExamPaper();
        paper.setTitle(request.getTitle());
        paper.setDescription(request.getDescription());
        paper.setClassId(request.getClassId());
        paper.setTotalScore(totalScore);
        paper.setQuestionCount(questions.size());
        paper.setTimeLimit(request.getTimeLimit());
        paper.setSingleCount((int) questions.stream().filter(q -> q.getQuestionType() == 1).count());
        paper.setMultiCount((int) questions.stream().filter(q -> q.getQuestionType() == 2).count());
        paper.setStatus(0);
        paper.setStartTime(request.getStartTime());
        paper.setEndTime(request.getEndTime());
        paper.setCreatorId(userId);
        examPaperMapper.insert(paper);

        for (int i = 0; i < questions.size(); i++) {
            ExamPaperQuestion pq = new ExamPaperQuestion();
            pq.setPaperId(paper.getId());
            pq.setQuestionId(questions.get(i).getId());
            pq.setQuestionOrder(i + 1);
            pq.setScore(questions.get(i).getScore());
            examPaperQuestionMapper.insert(pq);
        }
        return toPaperVO(paper);
    }

    @Override
    public Boolean publishPaper(Long paperId, Long userId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);
        if (!paper.getCreatorId().equals(userId)) throw new BusinessException(ResopnseCodeEnum.NO_AUTH_ERROR);
        paper.setStatus(1);
        return examPaperMapper.updateById(paper) > 0;
    }

    @Override
    public Boolean unpublishPaper(Long paperId, Long userId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);
        if (!paper.getCreatorId().equals(userId)) throw new BusinessException(ResopnseCodeEnum.NO_AUTH_ERROR);
        paper.setStatus(0);
        return examPaperMapper.updateById(paper) > 0;
    }

    @Override
    public Page<ExamPaperVO> listPapers(ExamPaperQueryRequest request, Long userId) {
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();
        if (request.getClassId() != null) wrapper.eq(ExamPaper::getClassId, request.getClassId());
        if (request.getStatus() != null) wrapper.eq(ExamPaper::getStatus, request.getStatus());
        wrapper.orderByDesc(ExamPaper::getCreateTime);
        Page<ExamPaper> page = examPaperMapper.selectPage(new Page<>(request.getCurrent(), request.getPageSize()), wrapper);

        Page<ExamPaperVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(p -> {
            ExamPaperVO vo = toPaperVO(p);
            // 检查当前用户是否已答
            LambdaQueryWrapper<ExamAnswerRecord> rw = new LambdaQueryWrapper<>();
            rw.eq(ExamAnswerRecord::getPaperId, p.getId()).eq(ExamAnswerRecord::getUserId, userId);
            vo.setHasAnswered(examAnswerRecordMapper.selectCount(rw) > 0);
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public ExamPaperVO getPaperById(Long paperId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);
        return toPaperVO(paper);
    }

    @Override
    @Transactional
    public Boolean addQuestionToPaper(Long paperId, Long questionId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);

        // 校验：如果当前试卷已经存在作答记录，则不允许再修改题目
        LambdaQueryWrapper<ExamAnswerRecord> recordQueryWrapper = new LambdaQueryWrapper<>();
        recordQueryWrapper.eq(ExamAnswerRecord::getPaperId, paperId);
        if (examAnswerRecordMapper.selectCount(recordQueryWrapper) > 0) {
            throw new BusinessException(ResopnseCodeEnum.OPERATION_ERROR, "试卷已存在作答记录，禁止修改题目");
        }

        // 检查是否已存在
        LambdaQueryWrapper<ExamPaperQuestion> w = new LambdaQueryWrapper<>();
        w.eq(ExamPaperQuestion::getPaperId, paperId).eq(ExamPaperQuestion::getQuestionId, questionId);
        if (examPaperQuestionMapper.selectCount(w) > 0) return true;

        // 获取当前题数
        Long count = examPaperQuestionMapper.selectCount(new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, paperId));
        Integer order = count.intValue();

        ExamQuestion question = examQuestionMapper.selectById(questionId);
        ExamPaperQuestion pq = new ExamPaperQuestion();
        pq.setPaperId(paperId);
        pq.setQuestionId(questionId);
        pq.setQuestionOrder(order + 1);
        pq.setScore(question.getScore());

        // 更新试卷总分和题数
        paper.setQuestionCount(paper.getQuestionCount() + 1);
        paper.setTotalScore(paper.getTotalScore() + question.getScore());
        if (question.getQuestionType() == 1) paper.setSingleCount(paper.getSingleCount() + 1);
        else paper.setMultiCount(paper.getMultiCount() + 1);

        examPaperMapper.updateById(paper);
        return examPaperQuestionMapper.insert(pq) > 0;
    }

    @Override
    public Boolean removeQuestionFromPaper(Long paperId, Long questionId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);

        // 校验：如果当前试卷已经存在作答记录，则不允许再修改题目
        LambdaQueryWrapper<ExamAnswerRecord> recordQueryWrapper = new LambdaQueryWrapper<>();
        recordQueryWrapper.eq(ExamAnswerRecord::getPaperId, paperId);
        if (examAnswerRecordMapper.selectCount(recordQueryWrapper) > 0) {
            throw new BusinessException(ResopnseCodeEnum.OPERATION_ERROR, "试卷已存在作答记录，禁止修改题目");
        }

        LambdaQueryWrapper<ExamPaperQuestion> w = new LambdaQueryWrapper<>();
        w.eq(ExamPaperQuestion::getPaperId, paperId).eq(ExamPaperQuestion::getQuestionId, questionId);
        ExamPaperQuestion pq = examPaperQuestionMapper.selectOne(w);
        if (pq == null) return true;

        ExamQuestion question = examQuestionMapper.selectById(questionId);

        // 更新试卷
        paper.setQuestionCount(paper.getQuestionCount() - 1);
        paper.setTotalScore(paper.getTotalScore() - question.getScore());
        if (question.getQuestionType() == 1) paper.setSingleCount(paper.getSingleCount() - 1);
        else paper.setMultiCount(paper.getMultiCount() - 1);

        examPaperMapper.updateById(paper);
        return examPaperQuestionMapper.deleteById(pq.getId()) > 0;
    }

    @Override
    public Boolean editPaper(ExamPaperEditRequest request, Long userId) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        }
        ExamPaper paper = examPaperMapper.selectById(request.getId());
        if (paper == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);
        }
        if (!paper.getCreatorId().equals(userId)) {
            throw new BusinessException(ResopnseCodeEnum.NO_AUTH_ERROR);
        }
        if (request.getStartTime() != null) paper.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) paper.setEndTime(request.getEndTime());
        if (request.getExamTime() != null) paper.setTimeLimit(request.getExamTime());
        return examPaperMapper.updateById(paper) > 0;
    }

    // ===== 答题 =====

    @Override
    public ExamTakeVO getPaperForTake(Long paperId, Long userId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);

        List<ExamPaperQuestion> pqs = examPaperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>()
                        .eq(ExamPaperQuestion::getPaperId, paperId)
                        .orderByAsc(ExamPaperQuestion::getQuestionOrder));
        List<Long> qIds = pqs.stream().map(ExamPaperQuestion::getQuestionId).collect(Collectors.toList());
        Map<Long, ExamQuestion> qMap = new HashMap<>();
        if (!qIds.isEmpty()) {
            examQuestionMapper.selectBatchIds(qIds).forEach(q -> qMap.put(q.getId(), q));
        }

        ExamTakeVO vo = new ExamTakeVO();
        vo.setPaperId(paperId);
        vo.setTitle(paper.getTitle());
        vo.setTimeLimit(paper.getTimeLimit());
        vo.setTotalScore(paper.getTotalScore());
        vo.setQuestionCount(paper.getQuestionCount());
        vo.setQuestions(pqs.stream().map(pq -> {
            ExamQuestion q = qMap.get(pq.getQuestionId());
            ExamTakeVO.TakeQuestionItem item = new ExamTakeVO.TakeQuestionItem();
            item.setId(q.getId());
            item.setQuestionOrder(pq.getQuestionOrder());
            item.setQuestionType(q.getQuestionType());
            item.setTitle(q.getTitle());
            item.setOptions(GSON.fromJson(q.getOptions(), new TypeToken<List<OptionItem>>(){}.getType()));
            item.setScore(pq.getScore());
            return item;
        }).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public Long startExam(Long paperId, Long userId) {
        // 检查是否已有记录
        LambdaQueryWrapper<ExamAnswerRecord> w = new LambdaQueryWrapper<>();
        w.eq(ExamAnswerRecord::getPaperId, paperId).eq(ExamAnswerRecord::getUserId, userId);
        ExamAnswerRecord existing = examAnswerRecordMapper.selectOne(w);
        if (existing != null) return existing.getId();

        ExamPaper paper = examPaperMapper.selectById(paperId);
        ExamAnswerRecord record = new ExamAnswerRecord();
        record.setPaperId(paperId);
        record.setUserId(userId);
        record.setTotalCount(paper.getQuestionCount());
        record.setStartTime(new Date());
        record.setStatus(0);
        examAnswerRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    @Transactional
    public ExamResultVO submitExam(ExamAnswerSubmitRequest request, Long userId) {
        ExamAnswerRecord record = examAnswerRecordMapper.selectById(request.getRecordId());
        if (record == null || !record.getUserId().equals(userId))
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR);
        if (record.getStatus() != 0)
            throw new BusinessException(ResopnseCodeEnum.OPERATION_ERROR, "已提交，不可重复提交");

        // 获取试卷题目和分值
        List<ExamPaperQuestion> pqs = examPaperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, request.getPaperId()));
        Map<Long, ExamPaperQuestion> pqMap = pqs.stream().collect(Collectors.toMap(ExamPaperQuestion::getQuestionId, pq -> pq));

        // 获取题目标准答案
        List<Long> qIds = pqs.stream().map(ExamPaperQuestion::getQuestionId).collect(Collectors.toList());
        Map<Long, ExamQuestion> qMap = new HashMap<>();
        if (!qIds.isEmpty()) {
            examQuestionMapper.selectBatchIds(qIds).forEach(q -> qMap.put(q.getId(), q));
        }

        // 用户答案映射
        Map<Long, String> answerMap = new HashMap<>();
        if (request.getAnswers() != null) {
            request.getAnswers().forEach(a -> answerMap.put(a.getQuestionId(), a.getUserAnswer()));
        }

        int totalScore = 0;
        int correctCount = 0;
        List<ExamAnswerDetailVO> detailVOs = new ArrayList<>();

        for (ExamPaperQuestion pq : pqs) {
            ExamQuestion q = qMap.get(pq.getQuestionId());
            if (q == null) continue;
            String userAnswer = answerMap.get(pq.getQuestionId());
            int fullScore = pq.getScore();
            int score;

            // 判分
            if (q.getQuestionType() == 1) {
                score = scoreSingleChoice(userAnswer, q.getCorrectAnswer(), fullScore);
            } else {
                score = scoreMultipleChoice(userAnswer, q.getCorrectAnswer(), fullScore);
            }

            int isCorrect = (score == fullScore) ? 1 : (score > 0 ? 2 : 0);
            totalScore += score;
            if (isCorrect == 1) correctCount++;

            // 保存详情
            ExamAnswerDetail detail = new ExamAnswerDetail();
            detail.setRecordId(record.getId());
            detail.setQuestionId(pq.getQuestionId());
            detail.setUserAnswer(userAnswer);
            detail.setIsCorrect(isCorrect);
            detail.setScoreObtained(score);
            detail.setScoreFull(fullScore);
            detail.setQuestionOrder(pq.getQuestionOrder());
            examAnswerDetailMapper.insert(detail);

            ExamAnswerDetailVO dvo = new ExamAnswerDetailVO();
            dvo.setQuestionId(pq.getQuestionId());
            dvo.setQuestionOrder(pq.getQuestionOrder());
            dvo.setQuestionTitle(q.getTitle());
            dvo.setQuestionType(q.getQuestionType());
            dvo.setUserAnswer(userAnswer);
            dvo.setCorrectAnswer(q.getCorrectAnswer());
            dvo.setIsCorrect(isCorrect);
            dvo.setScoreObtained(score);
            dvo.setScoreFull(fullScore);
            dvo.setAnalysis(q.getAnalysis());
            detailVOs.add(dvo);
        }

        // 更新记录
        record.setTotalScore(totalScore);
        record.setCorrectCount(correctCount);
        record.setAccuracyRate(pqs.isEmpty() ? 0 : (double) correctCount / pqs.size());
        record.setSubmitTime(new Date());
        record.setTimeSpent((int) ((record.getSubmitTime().getTime() - record.getStartTime().getTime()) / 1000));
        record.setStatus(2);
        examAnswerRecordMapper.updateById(record);

        // 异步计算能力评估（这里同步处理简化）
        calculateAbility(record.getId(), userId);
        // 生成练习推荐
        generateRecommendations(record.getId(), userId);

        ExamResultVO result = new ExamResultVO();
        result.setRecordId(record.getId());
        result.setTotalScore(totalScore);
        result.setTotalFullScore(pqs.stream().mapToInt(ExamPaperQuestion::getScore).sum());
        result.setCorrectCount(correctCount);
        result.setTotalCount(pqs.size());
        result.setAccuracyRate(record.getAccuracyRate());
        result.setTimeSpent(record.getTimeSpent());
        result.setDetails(detailVOs);
        return result;
    }

    @Override
    public ExamResultVO getExamResult(Long recordId, Long userId) {
        ExamAnswerRecord record = examAnswerRecordMapper.selectById(recordId);
        if (record == null) throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);

        List<ExamAnswerDetail> details = examAnswerDetailMapper.selectList(
                new LambdaQueryWrapper<ExamAnswerDetail>().eq(ExamAnswerDetail::getRecordId, recordId)
                        .orderByAsc(ExamAnswerDetail::getQuestionOrder));

        List<Long> qIds = details.stream().map(ExamAnswerDetail::getQuestionId).collect(Collectors.toList());
        Map<Long, ExamQuestion> qMap = new HashMap<>();
        if (!qIds.isEmpty()) {
            examQuestionMapper.selectBatchIds(qIds).forEach(q -> qMap.put(q.getId(), q));
        }

        ExamResultVO result = new ExamResultVO();
        result.setRecordId(recordId);
        result.setTotalScore(record.getTotalScore());
        // 获取试卷总分
        List<ExamPaperQuestion> pqs = examPaperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, record.getPaperId()));
        result.setTotalFullScore(pqs.stream().mapToInt(ExamPaperQuestion::getScore).sum());
        result.setCorrectCount(record.getCorrectCount());
        result.setTotalCount(record.getTotalCount());
        result.setAccuracyRate(record.getAccuracyRate());
        result.setTimeSpent(record.getTimeSpent());
        result.setDetails(details.stream().map(d -> {
            ExamQuestion q = qMap.get(d.getQuestionId());
            ExamAnswerDetailVO dvo = new ExamAnswerDetailVO();
            dvo.setQuestionId(d.getQuestionId());
            dvo.setQuestionOrder(d.getQuestionOrder());
            dvo.setQuestionTitle(q != null ? q.getTitle() : "");
            dvo.setQuestionType(q != null ? q.getQuestionType() : 1);
            dvo.setUserAnswer(d.getUserAnswer());
            dvo.setCorrectAnswer(q != null ? q.getCorrectAnswer() : "");
            dvo.setIsCorrect(d.getIsCorrect());
            dvo.setScoreObtained(d.getScoreObtained());
            dvo.setScoreFull(d.getScoreFull());
            dvo.setAnalysis(q != null ? q.getAnalysis() : "");
            return dvo;
        }).collect(Collectors.toList()));
        return result;
    }

    // ===== 能力评估 =====

    @Override
    public AbilityReportVO getAbilityReport(Long recordId, Long userId) {
        ExamAnswerRecord record = examAnswerRecordMapper.selectById(recordId);
        if (record == null) throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);

        ExamPaper paper = examPaperMapper.selectById(record.getPaperId());

        List<KnowledgeAbility> abilities = knowledgeAbilityMapper.selectList(
                new LambdaQueryWrapper<KnowledgeAbility>()
                        .eq(KnowledgeAbility::getRecordId, recordId)
                        .eq(KnowledgeAbility::getUserId, userId));

        List<KnowledgeAbilityVO> abilityVOs = abilities.stream().map(ka -> {
            KnowledgeAbilityVO vo = new KnowledgeAbilityVO();
            vo.setKnowledgeId(ka.getKnowledgeId());
            KnowledgePoint kp = knowledgePointMapper.selectById(ka.getKnowledgeId());
            vo.setKnowledgeName(kp != null ? kp.getName() : "未知");
            vo.setMasteryRate(ka.getMasteryRate());
            vo.setMasteryLevel(ka.getMasteryLevel());
            vo.setMasteryLabel(MasteryLevelEnum.getLabel(ka.getMasteryLevel()));
            vo.setCorrectCount(ka.getCorrectCount());
            vo.setTotalCount(ka.getTotalCount());
            vo.setObtainedScore(ka.getObtainedScore());
            vo.setTotalScore(ka.getTotalScore());
            return vo;
        }).collect(Collectors.toList());

        AbilityReportVO report = new AbilityReportVO();
        report.setRecordId(recordId);
        report.setUserId(userId);
        report.setExamTitle(paper != null ? paper.getTitle() : "");
        report.setTotalScore(record.getTotalScore());
        List<ExamPaperQuestion> pqs = examPaperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, record.getPaperId()));
        report.setTotalFullScore(pqs.stream().mapToInt(ExamPaperQuestion::getScore).sum());
        report.setAccuracyRate(record.getAccuracyRate());
        report.setTimeSpent(record.getTimeSpent());
        report.setKnowledgeAbilities(abilityVOs);
        report.setWeakKnowledges(abilityVOs.stream()
                .filter(v -> v.getMasteryRate() < 0.8)
                .sorted(Comparator.comparingDouble(KnowledgeAbilityVO::getMasteryRate))
                .collect(Collectors.toList()));
        return report;
    }

    private void calculateAbility(Long recordId, Long userId) {
        List<ExamAnswerDetail> details = examAnswerDetailMapper.selectList(
                new LambdaQueryWrapper<ExamAnswerDetail>().eq(ExamAnswerDetail::getRecordId, recordId));

        // 按知识点分组
        Map<Long, List<DetailWithQuestion>> knowledgeMap = new HashMap<>();
        for (ExamAnswerDetail d : details) {
            ExamQuestion q = examQuestionMapper.selectById(d.getQuestionId());
            if (q == null || StringUtils.isBlank(q.getKnowledgeIds())) continue;
            List<Long> kIds = parseIds(q.getKnowledgeIds());
            for (Long kId : kIds) {
                knowledgeMap.computeIfAbsent(kId, k -> new ArrayList<>())
                        .add(new DetailWithQuestion(d, q));
            }
        }

        // 计算每个知识点掌握度（加权得分率模型）
        for (Map.Entry<Long, List<DetailWithQuestion>> entry : knowledgeMap.entrySet()) {
            Long kId = entry.getKey();
            List<DetailWithQuestion> items = entry.getValue();

            double weightedScoreSum = 0;
            double weightedFullSum = 0;
            int correctCount = 0;
            int totalScoreVal = 0;
            int obtainedScoreVal = 0;

            for (DetailWithQuestion item : items) {
                double weight = DifficultyEnum.of(item.question.getDifficulty()).getWeight();
                weightedScoreSum += item.detail.getScoreObtained() * weight;
                weightedFullSum += item.detail.getScoreFull() * weight;
                if (item.detail.getIsCorrect() == 1) correctCount++;
                totalScoreVal += item.detail.getScoreFull();
                obtainedScoreVal += item.detail.getScoreObtained();
            }

            double masteryRate = weightedFullSum > 0 ? weightedScoreSum / weightedFullSum : 0;
            int masteryLevel = MasteryLevelEnum.calculateLevel(masteryRate);

            KnowledgeAbility ka = new KnowledgeAbility();
            ka.setUserId(userId);
            ka.setKnowledgeId(kId);
            ka.setRecordId(recordId);
            ka.setTotalScore(totalScoreVal);
            ka.setObtainedScore(obtainedScoreVal);
            ka.setCorrectCount(correctCount);
            ka.setTotalCount(items.size());
            ka.setMasteryRate(masteryRate);
            ka.setMasteryLevel(masteryLevel);
            knowledgeAbilityMapper.insert(ka);
        }
    }

    // ===== 练习推荐 =====

    @Override
    public List<PracticeRecommendationVO> getPracticeRecommendations(Long recordId, Long userId) {
        List<PracticeRecommendation> recs = practiceRecommendationMapper.selectList(
                new LambdaQueryWrapper<PracticeRecommendation>()
                        .eq(PracticeRecommendation::getRecordId, recordId)
                        .eq(PracticeRecommendation::getUserId, userId)
                        .orderByDesc(PracticeRecommendation::getPriority));

        // 按知识点分组
        Map<Long, List<PracticeRecommendation>> grouped = recs.stream()
                .collect(Collectors.groupingBy(PracticeRecommendation::getKnowledgeId, LinkedHashMap::new, Collectors.toList()));

        List<PracticeRecommendationVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<PracticeRecommendation>> entry : grouped.entrySet()) {
            Long kId = entry.getKey();
            List<PracticeRecommendation> items = entry.getValue();
            KnowledgePoint kp = knowledgePointMapper.selectById(kId);

            // 获取最新能力数据
            LambdaQueryWrapper<KnowledgeAbility> kaw = new LambdaQueryWrapper<>();
            kaw.eq(KnowledgeAbility::getUserId, userId).eq(KnowledgeAbility::getKnowledgeId, kId)
               .eq(KnowledgeAbility::getRecordId, recordId);
            KnowledgeAbility ka = knowledgeAbilityMapper.selectOne(kaw);

            PracticeRecommendationVO vo = new PracticeRecommendationVO();
            vo.setKnowledgeId(kId);
            vo.setKnowledgeName(kp != null ? kp.getName() : "未知");
            vo.setMasteryRate(ka != null ? ka.getMasteryRate() : 0);
            vo.setMasteryLabel(ka != null ? MasteryLevelEnum.getLabel(ka.getMasteryLevel()) : "未知");
            vo.setTotalRecommended(items.size());
            vo.setPracticedCount((int) items.stream().filter(r -> r.getIsPracticed() == 1).count());

            List<RecommendedQuestionVO> qVOs = items.stream().map(r -> {
                ExamQuestion q = examQuestionMapper.selectById(r.getQuestionId());
                RecommendedQuestionVO rvo = new RecommendedQuestionVO();
                rvo.setQuestionId(r.getQuestionId());
                rvo.setTitle(q != null ? q.getTitle() : "");
                rvo.setQuestionType(q != null ? q.getQuestionType() : 1);
                rvo.setDifficulty(r.getDifficulty());
                rvo.setDifficultyLabel(DifficultyEnum.getLabel(r.getDifficulty()));
                rvo.setRecommendType(r.getRecommendType());
                rvo.setRecommendReason(r.getRecommendReason());
                rvo.setPriority(r.getPriority());
                rvo.setIsPracticed(r.getIsPracticed() == 1);
                if (q != null && StringUtils.isNotBlank(q.getKnowledgeIds())) {
                    List<Long> kIds = parseIds(q.getKnowledgeIds());
                    List<KnowledgePoint> kps = knowledgePointMapper.selectBatchIds(kIds);
                    rvo.setKnowledgeTags(kps.stream().map(KnowledgePoint::getName).collect(Collectors.toList()));
                }
                return rvo;
            }).collect(Collectors.toList());

            vo.setQuestions(qVOs);
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public PracticeResultVO submitPractice(PracticeSubmitRequest request, Long userId) {
        int totalCount = 0, correctCount = 0;
        List<ExamAnswerDetailVO> detailVOs = new ArrayList<>();

        for (AnswerItem answer : request.getAnswers()) {
            ExamQuestion q = examQuestionMapper.selectById(answer.getQuestionId());
            if (q == null) continue;
            totalCount++;
            int score;
            if (q.getQuestionType() == 1) {
                score = scoreSingleChoice(answer.getUserAnswer(), q.getCorrectAnswer(), q.getScore());
            } else {
                score = scoreMultipleChoice(answer.getUserAnswer(), q.getCorrectAnswer(), q.getScore());
            }
            int isCorrect = (score == q.getScore()) ? 1 : (score > 0 ? 2 : 0);
            if (isCorrect == 1) correctCount++;

            // 标记已练习
            LambdaQueryWrapper<PracticeRecommendation> pw = new LambdaQueryWrapper<>();
            pw.eq(PracticeRecommendation::getRecordId, request.getRecordId())
              .eq(PracticeRecommendation::getQuestionId, answer.getQuestionId())
              .eq(PracticeRecommendation::getUserId, userId);
            PracticeRecommendation pr = practiceRecommendationMapper.selectOne(pw);
            if (pr != null) {
                pr.setIsPracticed(1);
                practiceRecommendationMapper.updateById(pr);
            }

            ExamAnswerDetailVO dvo = new ExamAnswerDetailVO();
            dvo.setQuestionId(q.getId());
            dvo.setQuestionTitle(q.getTitle());
            dvo.setQuestionType(q.getQuestionType());
            dvo.setUserAnswer(answer.getUserAnswer());
            dvo.setCorrectAnswer(q.getCorrectAnswer());
            dvo.setIsCorrect(isCorrect);
            dvo.setScoreObtained(score);
            dvo.setScoreFull(q.getScore());
            dvo.setAnalysis(q.getAnalysis());
            detailVOs.add(dvo);
        }

        PracticeResultVO result = new PracticeResultVO();
        result.setTotalCount(totalCount);
        result.setCorrectCount(correctCount);
        result.setAccuracyRate(totalCount > 0 ? (double) correctCount / totalCount : 0);
        result.setDetails(detailVOs);
        return result;
    }

    private void generateRecommendations(Long recordId, Long userId) {
        // 获取本次考试的能力评估数据
        List<KnowledgeAbility> abilities = knowledgeAbilityMapper.selectList(
                new LambdaQueryWrapper<KnowledgeAbility>()
                        .eq(KnowledgeAbility::getRecordId, recordId)
                        .eq(KnowledgeAbility::getUserId, userId));

        // 获取本次答题详情
        List<ExamAnswerDetail> details = examAnswerDetailMapper.selectList(
                new LambdaQueryWrapper<ExamAnswerDetail>().eq(ExamAnswerDetail::getRecordId, recordId));
        Set<Long> examQuestionIds = details.stream().map(ExamAnswerDetail::getQuestionId).collect(Collectors.toSet());

        // 提取错题
        List<ExamAnswerDetail> wrongDetails = details.stream()
                .filter(d -> d.getIsCorrect() != 1).collect(Collectors.toList());

        List<PracticeRecommendation> allRecs = new ArrayList<>();

        // 对每个知识点生成推荐
        for (KnowledgeAbility ka : abilities) {
            if (ka.getMasteryRate() >= 0.8) continue; // 已掌握，不推荐

            // 1. 规则推荐 (Rule-Based)
            List<PracticeRecommendation> ruleRecs = generateRuleBasedRecommendations(ka, examQuestionIds, userId, recordId);
            allRecs.addAll(ruleRecs);

            // 2. 知识图谱推荐 (Graph-Based)
            List<PracticeRecommendation> graphRecs = generateGraphBasedRecommendations(ka, examQuestionIds, userId, recordId);
            allRecs.addAll(graphRecs);
        }

        // 3. 相似度推荐 (Similarity-Based) - 基于错题
        List<PracticeRecommendation> similarRecs = generateSimilarityBasedRecommendations(wrongDetails, examQuestionIds, userId, recordId);
        allRecs.addAll(similarRecs);

        // 4. 推荐结果融合与去重排序 (Fusion)
        fuseAndSaveRecommendations(allRecs);
    }

    // ===== 统计 =====

    /**
     * 第一路召回：基于规则的练习推荐 (根据当前知识点掌握度进行梯级推题)
     * 1. 优先级和推题数：掌握度越低，优先级越高，目标推题数越多。
     * 2. 难度梯度配比：依据掌握度分配题目难度的梯度（掌握度越低，容易题占比越高）。
     * 3. 抽取机制：从题库中抽取该知识点的错题候选，按计算好的难度配额随机抽取。
     *
     * @param ka 当前评估的知识点能力对象 (记录了用户对该知识点的掌握度)
     * @param excludeIds 需排除的题目ID集合（例如本次考试已经做过的题目，避免重复）
     * @param userId 用户 ID
     * @param recordId 考试记录 ID
     * @return 规则推荐生成的练习列表
     */
    private List<PracticeRecommendation> generateRuleBasedRecommendations(KnowledgeAbility ka, Set<Long> excludeIds, Long userId, Long recordId) {
        List<PracticeRecommendation> recs = new ArrayList<>();
        double mastery = ka.getMasteryRate();
        int priority = mastery < 0.4 ? 100 : (mastery < 0.6 ? 80 : 50);
        int targetCount = mastery < 0.4 ? 10 : (mastery < 0.6 ? 8 : 5); // 掌握度越低推荐越多

        // 难度梯度配比
        double easyRatio = mastery < 0.4 ? 0.6 : (mastery < 0.6 ? 0.3 : 0.0);
        double mediumRatio = mastery < 0.4 ? 0.4 : (mastery < 0.6 ? 0.5 : 0.5);
        int easyCount = (int) Math.round(targetCount * easyRatio);
        int mediumCount = (int) Math.round(targetCount * mediumRatio);
        int hardCount = targetCount - easyCount - mediumCount;

        // 从题库查该知识点题目
        LambdaQueryWrapper<ExamQuestion> qw = new LambdaQueryWrapper<>();
        qw.like(ExamQuestion::getKnowledgeIds, String.valueOf(ka.getKnowledgeId()));
        List<ExamQuestion> candidates = examQuestionMapper.selectList(qw);

        Map<Integer, List<ExamQuestion>> diffMap = candidates.stream()
                .filter(q -> !excludeIds.contains(q.getId()))
                .collect(Collectors.groupingBy(ExamQuestion::getDifficulty));

        // 按梯度抽取
        List<ExamQuestion> selected = new ArrayList<>();
        extractByQuota(diffMap.getOrDefault(1, new ArrayList<>()), easyCount, selected);
        extractByQuota(diffMap.getOrDefault(2, new ArrayList<>()), mediumCount, selected);
        extractByQuota(diffMap.getOrDefault(3, new ArrayList<>()), hardCount, selected);

        for (ExamQuestion q : selected) {
            PracticeRecommendation rec = new PracticeRecommendation();
            rec.setUserId(userId);
            rec.setRecordId(recordId);
            rec.setKnowledgeId(ka.getKnowledgeId());
            rec.setQuestionId(q.getId());
            rec.setRecommendType(1); // 规则推荐
            rec.setRecommendReason(String.format("规则推荐：该知识点掌握度%.0f%%", mastery * 100));
            rec.setPriority(priority);
            rec.setDifficulty(q.getDifficulty());
            rec.setIsPracticed(0);
            recs.add(rec);
        }
        return recs;
    }

    /**
     * 通用随机抽题机制：按给定配额从备选题池中选取指定数量的题目
     * 打乱题池顺序以实现随机性，并从题库列表中移除已被选取的题目，避免重复抽取。
     *
     * @param pool 候选题池 (属于同一梯度/同类型的题目列表)
     * @param quota 需抽取的题目数量限额
     * @param result 抽取结果统一存放的列表
     */
    private void extractByQuota(List<ExamQuestion> pool, int quota, List<ExamQuestion> result) {
        if (pool == null || pool.isEmpty() || quota <= 0) return;
        Collections.shuffle(pool);
        int count = Math.min(quota, pool.size());
        result.addAll(pool.subList(0, count));
        pool.subList(0, count).clear(); // 避免重复抽取
    }

    /**
     * 第二路召回：基于知识图谱的练习推荐 (薄弱知识点溯源)
     * 核心思想：如果某个知识点薄弱，可能是因为它的前置（基础）知识点没掌握。
     * 1. 广度优先搜索（BFS）：向前追溯当前薄弱知识点的前置节点，最大追溯深度为 2 层。
     * 2. 随机巩固：对于每一个前置知识点，随机抽取最多 3 道题目作为巩固推荐。
     * 3. 衰减优先级：推荐的优先级随着图谱深度的增加而衰减，越近的前置知识点优先级越高。
     *
     * @param ka 当前薄弱的知识点能力对象
     * @param excludeIds 需要排除的题目ID集合
     * @param userId 用户 ID
     * @param recordId 考试记录 ID
     * @return 图谱追溯推荐的练习列表
     */
    private List<PracticeRecommendation> generateGraphBasedRecommendations(KnowledgeAbility ka, Set<Long> excludeIds, Long userId, Long recordId) {
        List<PracticeRecommendation> recs = new ArrayList<>();
        // BFS 查找前置知识点 (Depth=2)
        Queue<Long> queue = new LinkedList<>();
        Set<Long> visited = new HashSet<>();
        queue.offer(ka.getKnowledgeId());
        visited.add(ka.getKnowledgeId());

        int depth = 0;
        int maxDepth = 2; // 最多向前追溯两层

        while (!queue.isEmpty() && depth < maxDepth) {
            int levelSize = queue.size();
            depth++;
            for (int i = 0; i < levelSize; i++) {
                Long currentKId = queue.poll();
                // 找当前节点的前置
                List<KnowledgeDependency> deps = knowledgeDependencyMapper.selectList(
                        new LambdaQueryWrapper<KnowledgeDependency>().eq(KnowledgeDependency::getToKnowledgeId, currentKId));

                for (KnowledgeDependency dep : deps) {
                    Long preKId = dep.getFromKnowledgeId();
                    if (visited.contains(preKId)) continue;
                    visited.add(preKId);
                    queue.offer(preKId);

                    // 为找到的前置知识点抽题
                    LambdaQueryWrapper<ExamQuestion> preQw = new LambdaQueryWrapper<>();
                    preQw.like(ExamQuestion::getKnowledgeIds, String.valueOf(preKId));
                    List<ExamQuestion> preCandidates = examQuestionMapper.selectList(preQw);
                    Collections.shuffle(preCandidates);

                    KnowledgePoint preKp = knowledgePointMapper.selectById(preKId);
                    KnowledgePoint curKp = knowledgePointMapper.selectById(ka.getKnowledgeId());

                    int preCount = 0;
                    for (ExamQuestion q : preCandidates) {
                        if (preCount >= 3) break; // 每个前置知识点最多推3题
                        if (excludeIds.contains(q.getId())) continue;

                        PracticeRecommendation rec = new PracticeRecommendation();
                        rec.setUserId(userId);
                        rec.setRecordId(recordId);
                        rec.setKnowledgeId(preKId); // 注意：挂在被推荐知识点下
                        rec.setQuestionId(q.getId());
                        rec.setRecommendType(2); // 图谱推荐
                        rec.setRecommendReason(String.format("图谱推荐：「%s」是「%s」的关联/前置，建议巩固",
                                preKp != null ? preKp.getName() : "前置概念", curKp != null ? curKp.getName() : "薄弱点"));
                        // 优先级随深度衰减
                        int baseGraphPriority = 60;
                        rec.setPriority((int) (baseGraphPriority / depth * (dep.getDependencyType() == 1 ? 1.5 : 1.0)));
                        rec.setDifficulty(q.getDifficulty());
                        rec.setIsPracticed(0);
                        recs.add(rec);
                        preCount++;
                    }
                }
            }
        }
        return recs;
    }

    /**
     * 第三路召回：基于相似度的练习推荐 (协同过滤思想的错题举一反三)
     * 核心思想：找出用户做错的题，从题库中寻找出和这些错题高度相似的其他题目，帮助用户巩固同一考点或同一类型的题目。
     * 获取全站最新的部分题目作为候选集，分别与用户的错题计算相似度，并截取相似度最高的前 3 题。
     *
     * @param wrongDetails 本次考试的错题详情列表
     * @param excludeIds 需要排除的题目ID集合
     * @param userId 用户 ID
     * @param recordId 考试记录 ID
     * @return 相似度举一反三生成的推荐列表
     */
    private List<PracticeRecommendation> generateSimilarityBasedRecommendations(List<ExamAnswerDetail> wrongDetails, Set<Long> excludeIds, Long userId, Long recordId) {
        List<PracticeRecommendation> recs = new ArrayList<>();
        if (wrongDetails.isEmpty()) return recs;

        // 获取错题相关的题目信息
        List<Long> wrongQIds = wrongDetails.stream().map(ExamAnswerDetail::getQuestionId).collect(Collectors.toList());
        List<ExamQuestion> wrongQs = examQuestionMapper.selectBatchIds(wrongQIds);
        
        // 获取部分题库作为候选集合（避免全量计算太慢，这里简单取最新1000题演示）
        Page<ExamQuestion> page = examQuestionMapper.selectPage(new Page<>(1, 1000), new LambdaQueryWrapper<ExamQuestion>().orderByDesc(ExamQuestion::getCreateTime));
        List<ExamQuestion> candidates = page.getRecords();

        for (ExamQuestion wq : wrongQs) {
            List<PracticeRecommendation> simTempList = new ArrayList<>();
            for (ExamQuestion candidate : candidates) {
                if (excludeIds.contains(candidate.getId()) || candidate.getId().equals(wq.getId())) continue;
                
                double sim = getQuestionSimilarity(wq, candidate);
                if (sim > 0.4) { // 相似度阈值
                    PracticeRecommendation rec = new PracticeRecommendation();
                    rec.setUserId(userId);
                    rec.setRecordId(recordId);
                    // 如果相似找出来，就将知识点归属给其中一个相似的知识点
                    List<Long> kIds = parseIds(candidate.getKnowledgeIds());
                    rec.setKnowledgeId(kIds.isEmpty() ? 0L : kIds.get(0));
                    rec.setQuestionId(candidate.getId());
                    rec.setRecommendType(3); // 相似度推荐
                    rec.setRecommendReason("相似推荐：与错题「" + wq.getTitle() + "」知识点或题型相似");
                    rec.setPriority((int) (sim * 40)); // 转换相似度为优先级 (0~40)
                    rec.setDifficulty(candidate.getDifficulty());
                    rec.setIsPracticed(0);
                    simTempList.add(rec);
                }
            }
            // 每道错题取相似度最高的前3道
            simTempList.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
            int topN = Math.min(3, simTempList.size());
            recs.addAll(simTempList.subList(0, topN));
        }
        return recs;
    }

    /**
     * 题目内容相似度评估函数
     * 相似度由三部分加权组成，用于判断候选题目和错题是否相似：
     * 1. 知识点 Jaccard 相似度（核心依据，满贡献分 0.6）：交集大小 / 并集大小。
     * 2. 题型匹配度（满贡献分 0.2）：同种题型增加 0.2 分。
     * 3. 难度接近度（满贡献分 0.2）：难度级差越小，得分越高。
     *
     * @param q1 题目1 (通常为本次错题)
     * @param q2 题目2 (题库推选的候选推荐题)
     * @return 综合相似度评分 (取值范围 0.0 ~ 1.0)
     */
    private double getQuestionSimilarity(ExamQuestion q1, ExamQuestion q2) {
        // 1. 知识点 Jaccard 相似度
        Set<Long> k1 = new HashSet<>(parseIds(q1.getKnowledgeIds()));
        Set<Long> k2 = new HashSet<>(parseIds(q2.getKnowledgeIds()));
        Set<Long> intersection = new HashSet<>(k1);
        intersection.retainAll(k2);
        Set<Long> union = new HashSet<>(k1);
        union.addAll(k2);
        double knowledgeSim = union.isEmpty() ? 0 : (double) intersection.size() / union.size();

        // 2. 题型匹配 (+0.2)
        double typeSim = q1.getQuestionType().equals(q2.getQuestionType()) ? 0.2 : 0;

        // 3. 难度接近度
        double diffSim = 1.0 - Math.abs(q1.getDifficulty() - q2.getDifficulty()) / 3.0; // 难度差异越小得分越高
        diffSim *= 0.2;

        return knowledgeSim * 0.6 + typeSim + diffSim;
    }

    /**
     * 多路召回融合、去重排序与结果落库 (Fusion Layer)
     * 1. 跨路召回去重：不同的推荐策略（规则、图谱、相似度）可能会召回同一道题目。去重处理时，保留计算出最高优先级的推荐记录，并组合多种推荐理由以作提示。
     * 2. 动态截断限制：为避免推荐题目过多带来学习负担，通过 (优先级、难度、推荐策略类型) 综合排序后，硬性限制每个知识点最多仅保留 10 题并持久化。
     *
     * @param allRecs 多路召回最终收集上来的所有推荐候选列表
     */
    private void fuseAndSaveRecommendations(List<PracticeRecommendation> allRecs) {
        if (allRecs.isEmpty()) return;

        // 1. 去重，保留优先级最高的，并合并理由
        Map<Long, PracticeRecommendation> uniqueMap = new HashMap<>();
        for (PracticeRecommendation rec : allRecs) {
            PracticeRecommendation existing = uniqueMap.get(rec.getQuestionId());
            if (existing == null) {
                uniqueMap.put(rec.getQuestionId(), rec);
            } else {
                // 合并处理
                existing.setPriority(Math.max(existing.getPriority(), rec.getPriority()));
                if (!existing.getRecommendReason().contains(rec.getRecommendReason())) {
                    existing.setRecommendReason(existing.getRecommendReason() + " | " + rec.getRecommendReason());
                }
                existing.setRecommendType(4); // 标记为复合推荐
            }
        }

        // 2. 分组排序截断
        List<PracticeRecommendation> mergedRecs = new ArrayList<>(uniqueMap.values());
        Map<Long, List<PracticeRecommendation>> grouped = mergedRecs.stream()
                .collect(Collectors.groupingBy(PracticeRecommendation::getKnowledgeId));

        for (List<PracticeRecommendation> groupList : grouped.values()) {
            groupList.sort((a, b) -> {
                if (!a.getPriority().equals(b.getPriority())) return b.getPriority() - a.getPriority();
                if (!a.getDifficulty().equals(b.getDifficulty())) return a.getDifficulty() - b.getDifficulty();
                return a.getRecommendType() - b.getRecommendType();
            });
            int retainLimit = Math.min(10, groupList.size()); // 每个知识点最多推10题
            for (int i = 0; i < retainLimit; i++) {
                practiceRecommendationMapper.insert(groupList.get(i));
            }
        }
    }

    // ===== 统计 =====

    @Override
    public ExamStatsVO getExamStats(Long paperId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR);

        List<ExamAnswerRecord> records = examAnswerRecordMapper.selectList(
                new LambdaQueryWrapper<ExamAnswerRecord>()
                        .eq(ExamAnswerRecord::getPaperId, paperId)
                        .eq(ExamAnswerRecord::getStatus, 2));

        ExamStatsVO stats = new ExamStatsVO();
        stats.setPaperId(paperId);
        stats.setPaperTitle(paper.getTitle());
        stats.setSubmittedCount(records.size());

        if (records.isEmpty()) {
            stats.setAverageScore(0.0);
            stats.setPassRate(0.0);
            stats.setHighestScore(0);
            stats.setLowestScore(0);
            stats.setScoreDistribution(new ArrayList<>());
            return stats;
        }

        List<Integer> scores = records.stream().map(ExamAnswerRecord::getTotalScore)
                .filter(Objects::nonNull).collect(Collectors.toList());
        stats.setAverageScore(scores.stream().mapToInt(Integer::intValue).average().orElse(0));
        stats.setHighestScore(scores.stream().mapToInt(Integer::intValue).max().orElse(0));
        stats.setLowestScore(scores.stream().mapToInt(Integer::intValue).min().orElse(0));
        int passLine = (int) (paper.getTotalScore() * 0.6);
        stats.setPassRate((double) scores.stream().filter(s -> s >= passLine).count() / scores.size());

        // 分数段分布
        int[] segments = new int[5]; // 0-59, 60-69, 70-79, 80-89, 90-100
        String[] labels = {"0-59", "60-69", "70-79", "80-89", "90-100"};
        for (int s : scores) {
            int pct = (int) ((double) s / paper.getTotalScore() * 100);
            if (pct < 60) segments[0]++;
            else if (pct < 70) segments[1]++;
            else if (pct < 80) segments[2]++;
            else if (pct < 90) segments[3]++;
            else segments[4]++;
        }
        List<ExamStatsVO.ScoreSegment> distribution = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ExamStatsVO.ScoreSegment seg = new ExamStatsVO.ScoreSegment();
            seg.setLabel(labels[i]);
            seg.setCount(segments[i]);
            distribution.add(seg);
        }
        stats.setScoreDistribution(distribution);

        // 5. 聚合知识点维度统计 (用于雷达图)
        List<Long> recordIds = records.stream().map(ExamAnswerRecord::getId).collect(Collectors.toList());
        if (!recordIds.isEmpty()) {
            // 查询班级在该试卷下所有学生的知识点表现
            List<KnowledgeAbility> allAbilities = knowledgeAbilityMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeAbility>().in(KnowledgeAbility::getRecordId, recordIds));
            
            Map<Long, List<KnowledgeAbility>> groupById = allAbilities.stream()
                    .collect(Collectors.groupingBy(KnowledgeAbility::getKnowledgeId));
            
            List<ExamStatsVO.KnowledgeStats> kStats = new ArrayList<>();
            groupById.forEach((kId, list) -> {
                ExamStatsVO.KnowledgeStats ks = new ExamStatsVO.KnowledgeStats();
                KnowledgePoint kp = knowledgePointMapper.selectById(kId);
                ks.setKnowledgeName(kp != null ? kp.getName() : "未知");
                ks.setAverageMastery(list.stream().mapToDouble(KnowledgeAbility::getMasteryRate).average().orElse(0));
                kStats.add(ks);
            });
            stats.setKnowledgeStats(kStats);
        }

        // 6. 收集学生用时-得分分布 (用于散点图)
        stats.setStudentPoints(records.stream().map(r -> {
            ExamStatsVO.StudentPoint p = new ExamStatsVO.StudentPoint();
            p.setTimeSpent(r.getTimeSpent() / 60); // 秒转分
            p.setScore(r.getTotalScore());
            return p;
        }).collect(Collectors.toList()));

        return stats;
    }

    // ===== 判分算法 =====

    private int scoreSingleChoice(String userAnswer, String correctAnswer, int fullScore) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) return 0;
        return userAnswer.trim().equalsIgnoreCase(correctAnswer.trim()) ? fullScore : 0;
    }

    private int scoreMultipleChoice(String userAnswer, String correctAnswer, int fullScore) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) return 0;
        Set<String> S = parseAnswerSet(correctAnswer);
        Set<String> U = parseAnswerSet(userAnswer);
        if (S.equals(U)) return fullScore;
        double unitScore = (double) fullScore / S.size();
        Set<String> correct = new HashSet<>(S);
        correct.retainAll(U);
        Set<String> wrong = new HashSet<>(U);
        wrong.removeAll(S);
        double rawScore = correct.size() * unitScore - wrong.size() * unitScore;
        return Math.max(0, (int) Math.floor(rawScore));
    }

    private Set<String> parseAnswerSet(String answer) {
        return Arrays.stream(answer.split(","))
                .map(String::trim).map(String::toUpperCase).collect(Collectors.toSet());
    }

    // ===== 工具方法 =====

    private List<Long> parseIds(String ids) {
        if (StringUtils.isBlank(ids)) return new ArrayList<>();
        return Arrays.stream(ids.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).map(Long::parseLong).collect(Collectors.toList());
    }

    private boolean containsKnowledge(ExamQuestion q, Long knowledgeId) {
        if (StringUtils.isBlank(q.getKnowledgeIds())) return false;
        return parseIds(q.getKnowledgeIds()).contains(knowledgeId);
    }

    private ExamPaperVO toPaperVO(ExamPaper p) {
        ExamPaperVO vo = new ExamPaperVO();
        vo.setId(p.getId());
        vo.setTitle(p.getTitle());
        vo.setDescription(p.getDescription());
        vo.setClassId(p.getClassId());
        vo.setTotalScore(p.getTotalScore());
        vo.setQuestionCount(p.getQuestionCount());
        vo.setTimeLimit(p.getTimeLimit());
        vo.setSingleCount(p.getSingleCount());
        vo.setMultiCount(p.getMultiCount());
        vo.setStatus(p.getStatus());
        vo.setStatusLabel(ExamPaperStatusEnum.getLabel(p.getStatus()));
        vo.setStartTime(p.getStartTime());
        vo.setEndTime(p.getEndTime());
        vo.setCreatorId(p.getCreatorId());
        vo.setCreateTime(p.getCreateTime());
        return vo;
    }

    // 内部辅助类
    private static class DetailWithQuestion {
        ExamAnswerDetail detail;
        ExamQuestion question;
        DetailWithQuestion(ExamAnswerDetail d, ExamQuestion q) {
            this.detail = d;
            this.question = q;
        }
    }
}
