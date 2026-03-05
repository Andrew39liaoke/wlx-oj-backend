package com.wlx.ojbackendcodesandbox.codesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.wlx.ojbackendcodesandbox.model.ExecuteCodeRequest;
import com.wlx.ojbackendcodesandbox.model.ExecuteCodeResponse;
import com.wlx.ojbackendcodesandbox.model.ExecuteMessage;
import com.wlx.ojbackendcodesandbox.model.JudgeInfo;
import com.wlx.ojbackendcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Java 代码沙箱模板方法的实现
 */
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long TIME_OUT = 8000L;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

//        1. 把用户的代码保存为文件
        File userCodeFile = saveCodeToFile(code);

        try {
//            2. 编译代码，得到 class 文件
            ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
            System.out.println(compileFileExecuteMessage);
        } catch (Exception e) {
            // 编译失败，返回编译错误响应
            deleteFile(userCodeFile);
            ExecuteCodeResponse errorResponse = new ExecuteCodeResponse();
            errorResponse.setOutputList(new ArrayList<>());
            errorResponse.setMessage("编译出错: " + e.getMessage());
            errorResponse.setStatus(3);
            JudgeInfo judgeInfo = new JudgeInfo();
            judgeInfo.setMessage("编译出错");
            errorResponse.setJudgeInfo(judgeInfo);
            return errorResponse;
        }

        // 3. 执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);

//        4. 收集整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);

//        5. 文件清理
        boolean b = deleteFile(userCodeFile);
        if (!b) {
            log.error("deleteFile error, userCodeFilePath = {}", userCodeFile.getAbsolutePath());
        }
        return outputResponse;
    }


    /**
     * 1. 把用户的代码保存为文件
     * @param code 用户代码
     * @return
     */
    public File saveCodeToFile(String code) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局代码目录是否存在，没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        // 把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 2、编译代码
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            if (executeMessage.getExitValue() != 0) {
                throw new RuntimeException("编译错误");
            }
            return executeMessage;
        } catch (Exception e) {
//            return getErrorResponse(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 3、执行文件，获得执行结果列表
     * @param userCodeFile
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();

        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            // 不再将输入拼在命令行参数中，而是通过 stdin 管道传入
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main", userCodeParentPath);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 超时控制
                Thread timeoutThread = new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时了，中断");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        // 正常结束时线程被中断，忽略即可
                    }
                });
                timeoutThread.setDaemon(true);
                timeoutThread.start();
                // 通过 stdin 管道将输入写入进程
                if (inputArgs != null && !inputArgs.isEmpty()) {
                    OutputStream outputStream = runProcess.getOutputStream();
                    outputStream.write(inputArgs.getBytes());
                    outputStream.write('\n');
                    outputStream.flush();
                    outputStream.close();
                }
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                // 进程正常结束，中断超时线程
                timeoutThread.interrupt();
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException("执行错误", e);
            }
        }
        return executeMessageList;
    }

    /**
     * 4、获取输出结果
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        // 取用时最大值，便于判断是否超时
        long maxTime = 0;
        boolean hasError = false;
        String errorMsg = null;
        for (ExecuteMessage executeMessage : executeMessageList) {
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
            // 检查是否执行失败（exitValue != 0）
            if (executeMessage.getExitValue() != null && executeMessage.getExitValue() != 0) {
                hasError = true;
                // 判断是否超时（执行时间接近或超过超时阈值）
                if (time != null && time >= TIME_OUT) {
                    errorMsg = "超出时间限制";
                } else {
                    // 运行时错误
                    String errDetail = executeMessage.getErrorMessage();
                    if (StrUtil.isNotBlank(errDetail)) {
                        errorMsg = "执行出错: " + errDetail;
                    } else {
                        errorMsg = "执行出错";
                    }
                }
                break;
            }
            outputList.add(executeMessage.getMessage());
        }
        // 如果有错误，补齐剩余输出为空字符串
        if (hasError) {
            while (outputList.size() < executeMessageList.size()) {
                outputList.add("");
            }
            executeCodeResponse.setMessage(errorMsg);
            executeCodeResponse.setStatus(3);
        } else {
            // 正常运行完成
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        // 将错误信息也传递到 judgeInfo.message
        if (hasError) {
            judgeInfo.setMessage(errorMsg);
        }
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 5、删除文件
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }

    /**
     * 6、获取错误响应
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 表示代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
