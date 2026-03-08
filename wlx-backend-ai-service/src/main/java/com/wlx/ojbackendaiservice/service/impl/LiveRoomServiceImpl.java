package com.wlx.ojbackendaiservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wlx.ojbackendaiservice.mapper.LiveRoomMapper;
import com.wlx.ojbackendaiservice.service.LiveRoomService;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendcommon.exception.ThrowUtils;
import com.wlx.ojbackendmodel.model.entity.LiveRoom;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.LiveRoomVO;
import com.wlx.ojbackendmodel.model.vo.live.DanmakuMessageVO;
import com.wlx.ojbackendserviceclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Date;

/**
 * 直播间 服务实现类
 */
@Service
@Slf4j
public class LiveRoomServiceImpl extends ServiceImpl<LiveRoomMapper, LiveRoom> implements LiveRoomService {

    @Resource
    private UserFeignClient userFeignClient;

    @Override
    public LiveRoom createLiveRoom(Long classId, Long teacherId, String title) {
        // 检查是否已有进行中的直播
        LiveRoom activeRoom = getActiveRoom(classId);
        if (activeRoom != null) {
            return activeRoom;
        }

        LiveRoom liveRoom = new LiveRoom();
        liveRoom.setClassId(classId);
        liveRoom.setTeacherId(teacherId);
        liveRoom.setTitle(title != null ? title : "班级直播");
        liveRoom.setStatus(0); // 未开始
        // 统合流ID：直接使用 classId，方便前后端与 SRS 对齐
        liveRoom.setStreamId(String.valueOf(classId));
        liveRoom.setViewerCount(0);
        liveRoom.setCreateTime(new Date());
        liveRoom.setUpdateTime(new Date());
        save(liveRoom);
        return liveRoom;
    }

    @Override
    public LiveRoom startLive(Long roomId) {
        LiveRoom liveRoom = getById(roomId);
        ThrowUtils.throwIf(liveRoom == null, ResopnseCodeEnum.NOT_FOUND_ERROR, "直播间不存在");
        ThrowUtils.throwIf(liveRoom.getStatus() != 0, ResopnseCodeEnum.OPERATION_ERROR, "直播间状态不正确，无法开始直播");
        
        liveRoom.setStatus(1); // 直播中
        liveRoom.setStartTime(new Date());
        liveRoom.setUpdateTime(new Date());
        updateById(liveRoom);
        return liveRoom;
    }

    @Override
    public LiveRoom endLive(Long roomId) {
        LiveRoom liveRoom = getById(roomId);
        ThrowUtils.throwIf(liveRoom == null, ResopnseCodeEnum.NOT_FOUND_ERROR, "直播间不存在");
        ThrowUtils.throwIf(liveRoom.getStatus() != 1, ResopnseCodeEnum.OPERATION_ERROR, "直播间未在直播中，无法结束");
        
        liveRoom.setStatus(2); // 已结束
        liveRoom.setEndTime(new Date());
        liveRoom.setUpdateTime(new Date());
        updateById(liveRoom);
        return liveRoom;
    }

    @Override
    public LiveRoom getActiveRoom(Long classId) {
        LambdaQueryWrapper<LiveRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LiveRoom::getClassId, classId)
                .eq(LiveRoom::getStatus, 1) // 直播中
                .orderByDesc(LiveRoom::getCreateTime)
                .last("LIMIT 1");
        return getOne(wrapper);
    }

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void sendDanmaku(Long classId, DanmakuMessageVO msg) {
        String key = "danmaku:store:" + classId;
        stringRedisTemplate.opsForStream().add(StreamRecords.newRecord()
                .ofObject(msg)
                .withStreamKey(key));
    }

    @Override
    public java.util.List<DanmakuMessageVO> getLatestDanmaku(Long classId, int count) {
        String key = "danmaku:store:" + classId;
        java.util.List<ObjectRecord<String, DanmakuMessageVO>> range =
            stringRedisTemplate.opsForStream().range(DanmakuMessageVO.class,
                        key,
                        Range.unbounded(),
                        Limit.limit().count(count));
        
        return range.stream()
                .map(org.springframework.data.redis.connection.stream.Record::getValue)
                .toList();
    }

    @Override
    public LiveRoomVO toVO(LiveRoom liveRoom) {
        if (liveRoom == null) {
            return null;
        }
        LiveRoomVO vo = new LiveRoomVO();
        vo.setId(liveRoom.getId());
        vo.setClassId(liveRoom.getClassId());
        vo.setTeacherId(liveRoom.getTeacherId());
        vo.setTitle(liveRoom.getTitle());
        vo.setStatus(liveRoom.getStatus());
        vo.setStreamId(liveRoom.getStreamId());
        vo.setStartTime(liveRoom.getStartTime());
        vo.setEndTime(liveRoom.getEndTime());
        vo.setViewerCount(liveRoom.getViewerCount());
        vo.setCreateTime(liveRoom.getCreateTime());

        // 填充教师信息
        if (liveRoom.getTeacherId() != null) {
            try {
                User teacher = userFeignClient.getById(liveRoom.getTeacherId());
                if (teacher != null) {
                    vo.setTeacherName(teacher.getUserName());
                    vo.setTeacherAvatar(teacher.getUserAvatar());
                }
            } catch (Exception e) {
                log.error("获取教师信息失败: {}", e.getMessage());
                vo.setTeacherName("未知教师");
            }
        }
        return vo;
    }
}
