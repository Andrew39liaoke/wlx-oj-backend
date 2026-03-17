package com.wlx.ojbackendaiservice.tools;

import com.wlx.ojbackendmodel.model.vo.KnowledgePointVO;
import com.wlx.ojbackendserviceclient.service.QuestionFeignClient;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识点相关的 AI 工具。
 * 提供给大语言模型调用，用于获取系统中的关联知识点树结构数据。
 */
@Component
public class KnowledgeTools {

    @Resource
    private QuestionFeignClient questionFeignClient;

    @Tool(description = "获取系统所有关联知识点树。在生成或推荐考试客观题、编程题或者获取知识大纲时，可以查询此接口以获取现有的知识点ID(knowledgeIds)和对应的分类层级")
    public List<KnowledgePointVO> getKnowledgePoints(
            @ToolParam(description = "需要查询的班级ID。如果不填(或null)则查询通用的知识点大纲；如果填入数字则查询班级专有的知识点大纲。", required = false) Long classId) {
        return questionFeignClient.listKnowledgePoints(classId);
    }
}
