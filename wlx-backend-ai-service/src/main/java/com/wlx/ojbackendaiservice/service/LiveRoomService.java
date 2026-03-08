package com.wlx.ojbackendaiservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wlx.ojbackendmodel.model.entity.LiveRoom;
import com.wlx.ojbackendmodel.model.vo.LiveRoomVO;

/**
 * 直播间 服务接口
 */
public interface LiveRoomService extends IService<LiveRoom> {

    /**
     * 创建直播间
     */
    LiveRoom createLiveRoom(Long classId, Long teacherId, String title);

    /**
     * 开始直播
     */
    LiveRoom startLive(Long roomId);

    /**
     * 结束直播
     */
    LiveRoom endLive(Long roomId);

    /**
     * 查询班级当前进行中的直播
     */
    LiveRoom getActiveRoom(Long classId);

    /**
     * 发送弹幕
     */
    void sendDanmaku(Long classId, com.wlx.ojbackendmodel.model.vo.live.DanmakuMessageVO msg);

    /**
     * 获取最近N条弹幕
     */
    java.util.List<com.wlx.ojbackendmodel.model.vo.live.DanmakuMessageVO> getLatestDanmaku(Long classId, int count);

    /**
     * 将实体转为 VO
     */
    LiveRoomVO toVO(LiveRoom liveRoom);
}
