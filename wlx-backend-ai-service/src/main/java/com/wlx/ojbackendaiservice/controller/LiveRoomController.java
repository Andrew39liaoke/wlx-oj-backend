package com.wlx.ojbackendaiservice.controller;

import com.wlx.ojbackendaiservice.service.LiveRoomService;
import com.wlx.ojbackendaiservice.utils.WebRTCUtil;
import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.common.Result;
import com.wlx.ojbackendcommon.utils.JwtUtil;
import com.wlx.ojbackendmodel.model.dto.live.DanmakuDTO;
import com.wlx.ojbackendmodel.model.dto.live.SRSCallbackDTO;
import com.wlx.ojbackendmodel.model.entity.LiveRoom;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.vo.LiveRoomVO;
import com.wlx.ojbackendmodel.model.vo.live.DanmakuMessageVO;
import com.wlx.ojbackendserviceclient.service.UserFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;

/**
 * 直播间 REST API 控制器 (已按角色路径重构)
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class LiveRoomController {

    @Resource
    private LiveRoomService liveRoomService;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private WebRTCUtil webRTCUtil;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${live.srs.api-host:127.0.0.1}")
    private String srsHost;

    // ==========================================
    // 1. /user/live: 通用与观众接口
    // ==========================================

    @GetMapping("/user/live/active")
    @Operation(summary = "查询班级当前直播")
    public ResponseEntity<LiveRoomVO> getActiveLive(@RequestParam("classId") Long classId) {
        LiveRoom activeRoom = liveRoomService.getActiveRoom(classId);
        if (activeRoom == null) {
            return Result.success(null);
        }
        return Result.success(liveRoomService.toVO(activeRoom));
    }

    @GetMapping("/user/live/info")
    @Operation(summary = "查询直播间详情")
    public ResponseEntity<LiveRoomVO> getLiveRoomInfo(@RequestParam("roomId") Long roomId) {
        LiveRoom liveRoom = liveRoomService.getById(roomId);
        if (liveRoom == null) {
            return Result.error(ResopnseCodeEnum.NOT_FOUND_ERROR, "直播间不存在");
        }
        return Result.success(liveRoomService.toVO(liveRoom));
    }

    @PostMapping("/user/live/callback")
    @Operation(summary = "SRS 媒体服务器回调")
    public ResponseEntity<Integer> streamsCallback(@RequestBody SRSCallbackDTO srsCallbackDTO) {
        if (srsCallbackDTO.getAction() == null || srsCallbackDTO.getStream() == null) {
            log.warn("[SRS Callback] 缺少 action 或 stream 参数");
            return Result.error(ResopnseCodeEnum.PARAMS_ERROR, "参数错误");
        }

        String streamKey = srsCallbackDTO.getStream(); // 对应 classId
        String redisKey = "live:status:" + streamKey;
        int status = 0; // 0-关闭, 1-开启

        switch (srsCallbackDTO.getAction()) {
            case "on_publish":
                log.info("[SRS Callback] 流已开启: {}", streamKey);
                status = 1;
                break;
            case "on_unpublish":
                log.info("[SRS Callback] 流已断开: {}", streamKey);
                stringRedisTemplate.delete(redisKey);
                break;
            default:
                log.warn("[SRS Callback] 未知 action: {}", srsCallbackDTO.getAction());
        }

        if (status == 1) {
            stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(status));
        }

        // 解析 classId 广播状态更新
        try {
            cn.hutool.json.JSONObject statusUpdate = new cn.hutool.json.JSONObject();
            statusUpdate.set("type", "live_status");
            statusUpdate.set("status", status == 1 ? "started" : "ended");

            String channel = "live_room_channel:" + streamKey;
            stringRedisTemplate.convertAndSend(channel, statusUpdate.toString());
        } catch (Exception e) {
            log.error("[SRS Callback] 广播状态失败: {}", e.getMessage());
        }

        return Result.success(0);
    }

    // ==========================================
    // 2. /teacher/live: 教师特有接口 (推流与管理)
    // ==========================================

    @PostMapping("/teacher/live/create")
    @Operation(summary = "创建直播间")
    public ResponseEntity<LiveRoomVO> createLiveRoom(
            @RequestParam("classId") Long classId,
            @RequestParam(value = "title", required = false, defaultValue = "班级直播") String title,
            HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (loginUser == null) {
            return Result.error(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }
        try {
            LiveRoom liveRoom = liveRoomService.createLiveRoom(classId, loginUser.getId(), title);
            return Result.success(liveRoomService.toVO(liveRoom));
        } catch (Exception e) {
            return Result.error(ResopnseCodeEnum.OPERATION_ERROR, e.getMessage());
        }
    }

    @PostMapping("/teacher/live/start")
    @Operation(summary = "开始直播")
    public ResponseEntity<LiveRoomVO> startLive(
            @RequestParam("roomId") Long roomId,
            HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (loginUser == null) {
            return Result.error(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }
        try {
            LiveRoom liveRoom = liveRoomService.startLive(roomId);
            return Result.success(liveRoomService.toVO(liveRoom));
        } catch (Exception e) {
            return Result.error(ResopnseCodeEnum.OPERATION_ERROR, e.getMessage());
        }
    }

    @PostMapping("/teacher/live/end")
    @Operation(summary = "结束直播")
    public ResponseEntity<LiveRoomVO> endLive(
            @RequestParam("roomId") Long roomId,
            HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (loginUser == null) {
            return Result.error(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }
        try {
            LiveRoom liveRoom = liveRoomService.endLive(roomId);
            // 结束直播时，清理 SRS 中该流的所有客户端会话
            if (liveRoom.getStreamId() != null) {
                String srsApiBase = getSrsApiBase();
                webRTCUtil.cleanupAllClients(srsApiBase, "live", liveRoom.getStreamId());
            }
            return Result.success(liveRoomService.toVO(liveRoom));
        } catch (Exception e) {
            return Result.error(ResopnseCodeEnum.OPERATION_ERROR, e.getMessage());
        }
    }

    @PostMapping("/teacher/live/cleanup/{classId}")
    @Operation(summary = "清理SRS旧流会话 (教师)")
    public ResponseEntity<String> cleanupSrsStream(
            @PathVariable("classId") String classId,
            HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (loginUser == null) {
            return Result.error(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }
        String srsApiBase = getSrsApiBase();
        webRTCUtil.cleanupStream(srsApiBase, "live", classId);
        return Result.success("ok");
    }

    @PostMapping("/teacher/live/whip/{classId}")
    @Operation(summary = "WHIP 推流 (优先执行 SRS 清理)")
    public org.springframework.http.ResponseEntity<String> whipExchange(
            @PathVariable("classId") String classId,
            @RequestBody String sdpOffer) {
        webRTCUtil.cleanupStream(getSrsApiBase(), "live", classId);

        // 使用 WebRTCUtil 自动拼接
        String sdpAnswer = webRTCUtil.start(WebRTCUtil.Status.WHIP, classId, sdpOffer);
        return org.springframework.http.ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(MediaType.valueOf("application/sdp"))
                .body(sdpAnswer);
    }

    // ==========================================
    // 3. /student/live: 学生特有接口 (拉流)
    // ==========================================

    @PostMapping("/student/live/whep/{streamId}")
    @Operation(summary = "WHEP 拉流 (同步 e-code Redis 预检逻辑)")
    public org.springframework.http.ResponseEntity<String> whepExchange(
            @PathVariable("streamId") String streamId,
            @RequestBody String sdpOffer) {
        // streamId 目前与 classId 保持一致
        Long classId;
        try {
            classId = Long.parseLong(streamId);
        } catch (NumberFormatException e) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }

        // 核心同步逻辑：对齐 e-code，优先从 Redis 检查直播状态
        String redisKey = "live:status:" + streamId;
        String statusStr = stringRedisTemplate.opsForValue().get(redisKey);
        
        if (StringUtils.isBlank(statusStr) || !"1".equals(statusStr)) {
            LiveRoom activeRoom = liveRoomService.getActiveRoom(classId);
            if (activeRoom == null || activeRoom.getStatus() == null || activeRoom.getStatus() != 1) {
                return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            stringRedisTemplate.opsForValue().set(redisKey, "1");
        }

        // 使用 WebRTCUtil 自动拼接，保持与 e-code 逻辑高度一致
        String sdpAnswer = webRTCUtil.start(WebRTCUtil.Status.WHEP, streamId, sdpOffer);
        return org.springframework.http.ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(MediaType.valueOf("application/sdp"))
                .body(sdpAnswer);
    }

    /**
     * 获取 SRS API 基础 URL
     */
    private String getSrsApiBase() {
        return String.format("http://%s:1985", srsHost);
    }

    /**
     * 从请求头中提取 token 并获取登录用户
     */
    private User getLoginUser(HttpServletRequest request) {
        String token = request.getHeader(JwtUtil.HEADER);
        if (StringUtils.isBlank(token)) {
            return null;
        }
        try {
            // 2. 内部 Feign 调用，auth-service 已处理好 Redis 缓存关联
            return userFeignClient.getLoginUser(token);
        } catch (Exception e) {
            return null;
        }
    }

}
