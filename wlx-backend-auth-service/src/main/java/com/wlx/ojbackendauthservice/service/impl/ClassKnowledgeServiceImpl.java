package com.wlx.ojbackendauthservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wlx.ojbackendauthservice.mapper.ClassKnowledgeMapper;
import com.wlx.ojbackendauthservice.service.ClassKnowledgeService;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendmodel.model.dto.file.FileUploadResponse;
import com.wlx.ojbackendmodel.model.entity.ClassKnowledge;
import com.wlx.ojbackendmodel.model.entity.FileInfo;
import com.wlx.ojbackendmodel.model.vo.ClassKnowledgeVO;
import com.wlx.ojbackendserviceclient.service.FileFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 班级知识库服务实现
 */
@Service
@Slf4j
public class ClassKnowledgeServiceImpl extends ServiceImpl<ClassKnowledgeMapper, ClassKnowledge>
        implements ClassKnowledgeService {

    @Resource
    private FileFeignClient fileFeignClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addClassKnowledge(MultipartFile file, Long classId, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "文件不能为空");
        }
        if (classId == null || classId <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "班级ID不能为空");
        }

        try {
            // 调用文件服务上传文件
            var uploadResponse = fileFeignClient.uploadFile(file, "class_knowledge", userId);
            if (uploadResponse == null || uploadResponse.getData() == null) {
                throw new BusinessException(ResopnseCodeEnum.OPERATION_ERROR, "文件上传失败");
            }

            FileUploadResponse fileUploadResponse = uploadResponse.getData();
            Long fileInfoId = fileUploadResponse.getFileId();

            // 保存班级知识库记录
            ClassKnowledge classKnowledge = new ClassKnowledge();
            classKnowledge.setClassId(classId);
            classKnowledge.setFileInfoId(fileInfoId);

            return this.save(classKnowledge);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("添加班级知识库失败", e);
            throw new BusinessException(ResopnseCodeEnum.SYSTEM_ERROR, "添加班级知识库失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteClassKnowledge(Long id, Long userId) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "知识库记录ID不能为空");
        }

        // 查询知识库记录
        ClassKnowledge classKnowledge = this.getById(id);
        if (classKnowledge == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR, "知识库记录不存在");
        }

        try {
            // 删除文件
            Long fileInfoId = classKnowledge.getFileInfoId();
            if (fileInfoId != null) {
                fileFeignClient.deleteFile(fileInfoId, userId);
            }

            // 删除知识库记录
            return this.removeById(id);
        } catch (Exception e) {
            log.error("删除班级知识库失败", e);
            throw new BusinessException(ResopnseCodeEnum.SYSTEM_ERROR, "删除班级知识库失败：" + e.getMessage());
        }
    }

    @Override
    public List<ClassKnowledgeVO> listClassKnowledge(Long classId) {
        if (classId == null || classId <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "班级ID不能为空");
        }

        // 查询班级知识库记录
        LambdaQueryWrapper<ClassKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassKnowledge::getClassId, classId);
        wrapper.orderByDesc(ClassKnowledge::getCreateTime);
        List<ClassKnowledge> classKnowledgeList = this.list(wrapper);

        // 转换为VO
        List<ClassKnowledgeVO> voList = new ArrayList<>();
        for (ClassKnowledge classKnowledge : classKnowledgeList) {
            ClassKnowledgeVO vo = new ClassKnowledgeVO();
            BeanUtils.copyProperties(classKnowledge, vo);

            // 获取文件信息
            Long fileInfoId = classKnowledge.getFileInfoId();
            if (fileInfoId != null) {
                try {
                    var fileInfoResponse = fileFeignClient.getFileInfo(fileInfoId);
                    if (fileInfoResponse != null && fileInfoResponse.getData() != null) {
                        FileInfo fileInfo = fileInfoResponse.getData();
                        vo.setFileName(fileInfo.getFileName());
                        vo.setFileSize(fileInfo.getFileSize());
                        vo.setFileType(fileInfo.getFileType());
                        vo.setFileUrl(fileInfo.getFileUrl());
                        vo.setUserId(fileInfo.getUserId());
                    }
                } catch (Exception e) {
                    log.error("获取文件信息失败，fileInfoId: {}", fileInfoId, e);
                }
            }

            voList.add(vo);
        }

        return voList;
    }
}
