package com.wlx.ojbackendmodel.model.vo;

import com.wlx.ojbackendmodel.model.dto.exam.OptionItem;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class ExamQuestionVO implements Serializable {
    private Long id;
    private String title;
    private Integer questionType;
    private List<OptionItem> options;
    private String correctAnswer;
    private Integer score;
    private Integer difficulty;
    private String difficultyLabel;
    private String knowledgeIds;
    private List<String> knowledgeTags;
    private List<String> tags;
    private String analysis;
    private Long userId;
    private String userName;
    private Date createTime;
    private static final long serialVersionUID = 1L;
}
