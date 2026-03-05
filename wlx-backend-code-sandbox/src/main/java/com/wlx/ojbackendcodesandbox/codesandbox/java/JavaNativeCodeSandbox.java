package com.wlx.ojbackendcodesandbox.codesandbox.java;

import com.wlx.ojbackendcodesandbox.codesandbox.JavaCodeSandboxTemplate;
import com.wlx.ojbackendcodesandbox.model.ExecuteCodeRequest;
import com.wlx.ojbackendcodesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

/**
 * Java 原生代码沙箱实现（直接复用模板方法）
 */
@Component
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
