package com.wlx.ojbackendauthservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wlx.ojbackendmodel.model.entity.ClassKnowledge;
import com.wlx.ojbackendmodel.model.vo.ClassKnowledgeVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 班级知识库服务
 */
public interface ClassKnowledgeService extends IService<ClassKnowledge> {

    /**
     * 添加班级知识库文件
     *
     * @param file 文件
     * @param classId 班级ID
     * @param userId 用户ID
     * @return 是否添加成功
     */
    boolean addClassKnowledge(MultipartFile file, Long classId, Long userId);

    /**
     * 删除班级知识库文件
     *
     * @param id 知识库记录ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteClassKnowledge(Long id, Long userId);

    /**
     * 获取班级知识库列表
     *
     * @param classId 班级ID
     * @return 知识库列表
     */
    List<ClassKnowledgeVO> listClassKnowledge(Long classId);
}
