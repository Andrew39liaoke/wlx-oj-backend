package com.wlx.ojbackendcodesandbox.codesandbox;


import com.wlx.ojbackendcodesandbox.model.ExecuteCodeRequest;
import com.wlx.ojbackendcodesandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱接口定义
 */
public interface CodeSandbox {

    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
