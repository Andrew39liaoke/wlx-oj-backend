package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class ExamStatsVO implements Serializable {
    private Long paperId;
    private String paperTitle;
    private Integer totalStudents;
    private Integer submittedCount;
    private Double averageScore;
    private Double passRate;
    private Integer highestScore;
    private Integer lowestScore;
    private List<ScoreSegment> scoreDistribution;
    /** 知识点掌握统计 (用于雷达图) */
    private List<KnowledgeStats> knowledgeStats;
    /** 学生得分-用时分布 (用于散点图) */
    private List<StudentPoint> studentPoints;
    private static final long serialVersionUID = 1L;

    @Data
    public static class KnowledgeStats implements Serializable {
        private String knowledgeName;
        private Double averageMastery;
        private static final long serialVersionUID = 1L;
    }

    @Data
    public static class StudentPoint implements Serializable {
        private Integer timeSpent;
        private Integer score;
        private static final long serialVersionUID = 1L;
    }

    @Data
    public static class ScoreSegment implements Serializable {
        private String label;
        private Integer count;
        private static final long serialVersionUID = 1L;
    }
}
