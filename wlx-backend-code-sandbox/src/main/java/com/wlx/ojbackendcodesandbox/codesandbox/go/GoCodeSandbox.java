package com.wlx.ojbackendcodesandbox.codesandbox.go;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.wlx.ojbackendcodesandbox.codesandbox.CodeSandbox;
import com.wlx.ojbackendcodesandbox.model.ExecuteCodeRequest;
import com.wlx.ojbackendcodesandbox.model.ExecuteCodeResponse;
import com.wlx.ojbackendcodesandbox.model.ExecuteMessage;
import com.wlx.ojbackendcodesandbox.model.JudgeInfo;
import com.wlx.ojbackendcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Go 代码沙箱实现
 */
@Slf4j
public class GoCodeSandbox implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_GO_FILE_NAME = "main.go";

    private static final String GLOBAL_EXE_FILE_NAME = "main.exe";

    private static final long TIME_OUT = 5000L;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();

        // 1. 保存代码到文件
        File userCodeFile = saveCodeToFile(code);

        // 2. 编译代码
        File exeFile = compileFile(userCodeFile);

        // 3. 执行代码
        List<ExecuteMessage> executeMessageList = runFile(exeFile, inputList);

        // 4. 整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);

        // 5. 清理文件
        boolean b = deleteFile(userCodeFile);
        if (!b) {
            log.error("deleteFile error, userCodeFilePath = {}", userCodeFile.getAbsolutePath());
        }
        return outputResponse;
    }

    /**
     * 1. 保存代码到文件
     */
    public File saveCodeToFile(String code) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_GO_FILE_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 2. 编译代码
     */
    public File compileFile(File userCodeFile) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        String exePath = userCodeParentPath + File.separator + GLOBAL_EXE_FILE_NAME;
        // Windows 下生成 .exe，Linux 下不需要扩展名
        String compileCmd = String.format("go build -o %s %s", exePath, userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            if (executeMessage.getExitValue() != 0) {
                throw new RuntimeException("编译错误: " + executeMessage.getErrorMessage());
            }
            return new File(exePath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 3. 执行文件
     */
    public List<ExecuteMessage> runFile(File exeFile, List<String> inputList) {
        String userCodeParentPath = exeFile.getParentFile().getAbsolutePath();

        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            // 不再将输入拼在命令行参数中，而是通过 stdin 管道传入
            String runCmd = exeFile.getAbsolutePath();
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
                    java.io.OutputStream outputStream = runProcess.getOutputStream();
                    outputStream.write(inputArgs.getBytes());
                    outputStream.write('\n');
                    outputStream.flush();
                    outputStream.close();
                }
                
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                timeoutThread.interrupt();
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException("执行错误", e);
            }
        }
        return executeMessageList;
    }

    /**
     * 4. 获取输出结果
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        long maxTime = 0;

        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }

        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        
        return executeCodeResponse;
    }

    /**
     * 5. 删除文件
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
}
