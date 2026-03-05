package com.wlx.ojbackendcodesandbox.controller;

import com.wlx.ojbackendcodesandbox.codesandbox.CodeSandbox;
import com.wlx.ojbackendcodesandbox.codesandbox.cpp.CppCodeSandbox;
import com.wlx.ojbackendcodesandbox.codesandbox.cpp.CppDockerCodeSandbox;
import com.wlx.ojbackendcodesandbox.codesandbox.go.GoCodeSandbox;
import com.wlx.ojbackendcodesandbox.codesandbox.go.GoDockerCodeSandbox;
import com.wlx.ojbackendcodesandbox.codesandbox.java.JavaNativeCodeSandbox;
import com.wlx.ojbackendcodesandbox.codesandbox.java.JavaDockerCodeSandbox;
import com.wlx.ojbackendcodesandbox.codesandbox.python.PythonCodeSandbox;
import com.wlx.ojbackendcodesandbox.codesandbox.python.PythonDockerCodeSandbox;
import com.wlx.ojbackendcodesandbox.model.ExecuteCodeRequest;
import com.wlx.ojbackendcodesandbox.model.ExecuteCodeResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
public class CodeSandboxController {

    // 定义鉴权请求头和密钥
    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_SECRET = "secretKey";

    // 本地版本代码沙箱映射
    private static final Map<String, CodeSandbox> NATIVE_CODE_SANDBOX_MAP = new HashMap<>();

    // Docker 版本代码沙箱映射
    private static final Map<String, CodeSandbox> DOCKER_CODE_SANDBOX_MAP = new HashMap<>();

    static {
        // 本地版本
        NATIVE_CODE_SANDBOX_MAP.put("java", new JavaNativeCodeSandbox());
        NATIVE_CODE_SANDBOX_MAP.put("cpp", new CppCodeSandbox());
        NATIVE_CODE_SANDBOX_MAP.put("c++", new CppCodeSandbox());
        NATIVE_CODE_SANDBOX_MAP.put("go", new GoCodeSandbox());
        NATIVE_CODE_SANDBOX_MAP.put("python", new PythonCodeSandbox());
        NATIVE_CODE_SANDBOX_MAP.put("python3", new PythonCodeSandbox());

        // Docker 版本
        DOCKER_CODE_SANDBOX_MAP.put("java", new JavaDockerCodeSandbox());
        DOCKER_CODE_SANDBOX_MAP.put("cpp", new CppDockerCodeSandbox());
        DOCKER_CODE_SANDBOX_MAP.put("c++", new CppDockerCodeSandbox());
        DOCKER_CODE_SANDBOX_MAP.put("go", new GoDockerCodeSandbox());
        DOCKER_CODE_SANDBOX_MAP.put("python", new PythonDockerCodeSandbox());
    }


    /**
     * 执行代码（默认使用本地版本）
     */
    @PostMapping("/executeCode")
    public ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request,
                                           HttpServletResponse response) {
        return executeCodeWithMode(executeCodeRequest, request, response, "local");
    }

    /**
     * 执行代码 - 本地版本
     */
    @PostMapping("/executeCode/local")
    public ExecuteCodeResponse executeCodeLocal(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request,
                                                HttpServletResponse response) {
        return executeCodeWithMode(executeCodeRequest, request, response, "local");
    }

    /**
     * 执行代码 - Docker 版本
     */
    @PostMapping("/executeCode/docker")
    public ExecuteCodeResponse executeCodeDocker(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request,
                                                 HttpServletResponse response) {
        return executeCodeWithMode(executeCodeRequest, request, response, "docker");
    }

    /**
     * 根据模式执行代码
     */
    private ExecuteCodeResponse executeCodeWithMode(ExecuteCodeRequest executeCodeRequest, HttpServletRequest request,
                                                    HttpServletResponse response, String mode) {
        // 基本的认证
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader)) {
            response.setStatus(403);
            return null;
        }
        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空");
        }

        // 获取语言类型
        String language = executeCodeRequest.getLanguage();
        if (language == null || language.isEmpty()) {
            throw new RuntimeException("语言类型不能为空");
        }

        // 根据模式选择对应的代码沙箱
        Map<String, CodeSandbox> sandboxMap = "docker".equals(mode) ? DOCKER_CODE_SANDBOX_MAP : NATIVE_CODE_SANDBOX_MAP;
        CodeSandbox codeSandbox = sandboxMap.get(language.toLowerCase());
        if (codeSandbox == null) {
            throw new RuntimeException("不支持的语言类型: " + language);
        }

        return codeSandbox.executeCode(executeCodeRequest);
    }

    /**
     * 获取支持的语言列表
     */
    @GetMapping("/languages")
    public Map<String, String> getSupportedLanguages() {
        Map<String, String> languages = new HashMap<>();
        languages.put("java", "Java");
        languages.put("cpp", "C++");
        languages.put("go", "Go");
        languages.put("python", "Python");
        return languages;
    }

    /**
     * 获取支持的执行模式
     */
    @GetMapping("/modes")
    public Map<String, String> getSupportedModes() {
        Map<String, String> modes = new HashMap<>();
        modes.put("local", "本地执行");
        modes.put("docker", "Docker 容器执行");
        return modes;
    }
    
}
