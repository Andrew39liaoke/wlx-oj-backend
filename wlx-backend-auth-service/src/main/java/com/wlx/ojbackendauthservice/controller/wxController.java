package com.wlx.ojbackendauthservice.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.google.gson.Gson;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendcommon.common.Result;
import com.wlx.ojbackendcommon.utils.HttpRequestUtil;
import com.wlx.ojbackendcommon.utils.JwtUtil;
import com.wlx.ojbackendmodel.model.dto.user.WeChatRequest;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendmodel.model.token.JwtToken;
import com.wlx.ojbackendmodel.model.wx.Token;
import com.wlx.ojbackendmodel.model.wx.wxUser;
import com.wlx.ojbackendauthservice.model.Account;
import com.wlx.ojbackendauthservice.service.UserService;
import com.wlx.ojbackendauthservice.utils.WeChatUtils;
import jakarta.annotation.Resource;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wechat")
public class wxController {
    @Resource
    private Account account;
    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    Map<String, String> loginMap = new HashMap<>();

    @GetMapping
    public String checkWebChat(WeChatRequest weChatRequestDTO) throws NoSuchAlgorithmException {
        if (WeChatUtils.checkWeChat(weChatRequestDTO)){
            return weChatRequestDTO.getEchostr();
        }

        return "error";
    }

    @GetMapping("/qrCode")
    public ResponseEntity<Map<String, String>> getQrCode(){
        // 1. 生成唯一标识 (UUID)
        String uuid = IdUtil.simpleUUID();
        String content = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=your_appid&redirect_uri=REDIRECT_URI&response_type=code&scope=snsapi_userinfo&state=your_state#wechat_redirect";
        content = content.replace("your_appid",account.getAppId())
                .replace("REDIRECT_URI",account.getDomain() + account.getRedirectUri())
                .replace("your_state",uuid);
        int width = 200;
        int height = 200;
        QrConfig config = new QrConfig(width, height);
        config.setImg(FileUtil.file("logo.png"));
        String qrCode = QrCodeUtil.generateAsBase64(content, config, "png");
        Map<String, String> map = new HashMap<>();
        map.put("uuid", uuid);
        map.put("qrCode", qrCode);
        System.out.println("qrCode = " + qrCode);
        return Result.success(map);
    }

    @GetMapping("/login")
    public ResponseEntity<String> login(String code, String state){
        // code - 临时授权码
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=your_appid&secret=your_appsecret" +
                "&code=your_code&grant_type=authorization_code";
        url = url.replace("your_appid", account.getAppId())
                .replace("your_appsecret", account.getAppSecret())
                .replace("your_code", code);
        // 发送get请求
        HttpResponse httpResponse = HttpRequestUtil.doGet(url);
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        if(statusCode == 200){
            HttpEntity entity = httpResponse.getEntity();
            try {
                String entity_string = EntityUtils.toString(entity);
                Token token = new Gson().fromJson(entity_string, Token.class);
                String access_token = token.getAccess_token();
                String open_id = token.getOpenid();
                // 根据 access_token 和 openid 请求用户信息
                String userUrl = "https://api.weixin.qq.com/sns/userinfo?access_token=your_accesstoken&openid=your_openid&lang=zh_CN";
                userUrl = userUrl.replace("your_accesstoken", access_token)
                        .replace("your_openid", open_id);
                HttpResponse userResponse = HttpRequestUtil.doGet(userUrl);
                HttpEntity userEntity = userResponse.getEntity();
                try {
                    String user_entity_string = EntityUtils.toString(userEntity);
                    wxUser wxUser = new Gson().fromJson(user_entity_string, wxUser.class);
                    System.out.println("wxUser = " + wxUser);
                    // 查找或创建系统用户（根据 openid）
                    User systemUser = userService.findOrCreateByWxOpenId(wxUser);
                    // 生成 JWT token（使用 systemUser.userName 作为 username）
                    String userName = systemUser.getUserName();
                    List<String> roles = userService.getRoleNamesByUsername(userName);
                    String userTypeByRoles = userService.getUserTypeByRoles(roles);
                    String tokenStr = JwtUtil.generateToken(userName,userTypeByRoles);
                    // 使用 Shiro 登录一次以便在当前请求上下文中建立 Subject（便于授权）
                    try {
                        JwtToken jwtToken = new JwtToken(tokenStr);
                        SecurityUtils.getSubject().login(jwtToken);
                    } catch (Exception e) {
                        // 登录失败仍返回token到客户端，但可以记录异常
                        System.err.println("Shiro login error: " + e.getMessage());
                    }
                    loginMap.put(state, tokenStr);
                    // 返回token给前端（前端也可从响应体中获取）
                    System.out.println("tokenStr = " + tokenStr);
                    return Result.success(tokenStr);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return Result.authFailure("error");
    }


    /**
     * 前端轮询检查接口
     * @param state 场景值（前端传来的唯一标识，目前测试可能是 "123"）
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkLogin(String state) {
        if (state == null || state.isEmpty()) {
            state = "123"; // 兼容测试
        }
        // 尝试从 Map 中获取 token
        String token = loginMap.remove(state);

        if (token != null) {
            // 拿到了！返回给前端
            return Result.success(token);
        } else {
            // 还没扫码或已过期
            return Result.error(ResopnseCodeEnum.STATE_ERROR);
        }
    }

}
