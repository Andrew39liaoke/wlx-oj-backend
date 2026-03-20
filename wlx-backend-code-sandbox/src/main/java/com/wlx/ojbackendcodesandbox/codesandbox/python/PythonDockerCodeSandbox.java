package com.wlx.ojbackendcodesandbox.codesandbox.python;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.wlx.ojbackendcodesandbox.codesandbox.CodeSandbox;
import com.wlx.ojbackendcodesandbox.model.ExecuteCodeRequest;
import com.wlx.ojbackendcodesandbox.model.ExecuteCodeResponse;
import com.wlx.ojbackendcodesandbox.model.ExecuteMessage;
import com.wlx.ojbackendcodesandbox.model.JudgeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Python Docker 代码沙箱实现
 */
@Slf4j
@Component
public class PythonDockerCodeSandbox implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_PYTHON_FILE_NAME = "main.py";

    private static final long TIME_OUT = 5000L;

    private static final String DOCKER_IMAGE = "python:3.11-alpine";

    private static final Boolean FIRST_INIT = true;

    @Value("${docker.host}")
    private String dockerHost;

    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();

        // 1. 保存代码到文件
        File userCodeFile = saveCodeToFile(code);

        // 2. 执行代码（Docker 方式）
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);

        // 3. 整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);

        // 4. 清理文件
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
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_PYTHON_FILE_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 2. Docker 方式执行代码
     */
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        
        // 配置 Docker Client 使用 Apache HttpClient 传输层
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();

        // 1. 获取镜像信息
        try {
            dockerClient.inspectImageCmd(DOCKER_IMAGE).exec();
        } catch (Exception e) {
            System.out.println("本地无镜像，开始拉取：" + DOCKER_IMAGE);
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(DOCKER_IMAGE);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion(5, TimeUnit.MINUTES);
                System.out.println("下载镜像完成");
            } catch (InterruptedException e1) {
                System.err.println("拉取镜像异常");
                throw new RuntimeException(e1);
            }
        }

        // 2. 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(DOCKER_IMAGE);
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(256 * 1024 * 1024L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)
                .withReadonlyRootfs(false)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(false) // 禁用 TTY 以获取纯净输出流
                .withCmd("tail", "-f", "/dev/null") // 保持容器挂起不断开
                .exec();
        System.out.println(createContainerResponse);
        String containerId = createContainerResponse.getId();

        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        try {
            // 3. 启动容器
            dockerClient.startContainerCmd(containerId).exec();

            // 4. 执行命令并获取结果
            for (int i = 0; i < inputList.size(); i++) {
                String inputArgs = inputList.get(i);
                // 安全增强：改用文件重定向，免疫 Shell 注入
                String inputFileName = userCodeParentPath + File.separator + (i + 1) + ".txt";
                FileUtil.writeString(inputArgs, inputFileName, StandardCharsets.UTF_8);
                
                String[] runCmdArray = new String[]{"sh", "-c", "python3 /app/" + GLOBAL_PYTHON_FILE_NAME + " < /app/" + (i + 1) + ".txt"};
                ExecuteMessage executeMessage = execInContainer(dockerClient, containerId, runCmdArray);
                executeMessageList.add(executeMessage);
            }
        } finally {
            // 5. 最后必定清理并销毁容器实体
            System.out.println("清理临时容器，容器ID: " + containerId);
            try {
                dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            } catch (Exception e) {
                System.out.println("清理容器异常: " + e.getMessage());
            }
        }
        
        return executeMessageList;
    }

    /**
     * 在容器中执行命令
     */
    private ExecuteMessage execInContainer(DockerClient dockerClient, String containerId, String[] cmdArray) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        final String[] message = {null};
        final String[] errorMessage = {null};
        long time = 0L;
        final long[] maxMemory = {0L};
        
        try {
            StopWatch stopWatch = new StopWatch();
            
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withWorkingDir("/app")
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            
            System.out.println("创建执行命令：" + execCreateCmdResponse);
            String execId = execCreateCmdResponse.getId();
            StringBuilder messageBuilder = new StringBuilder();
            StringBuilder errorBuilder = new StringBuilder();

            // 获取内存统计
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                }

                @Override
                public void close() throws IOException {}

                @Override
                public void onStart(Closeable closeable) {}

                @Override
                public void onError(Throwable throwable) {}

                @Override
                public void onComplete() {}
            });
            
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        String errorPayload = new String(frame.getPayload(), StandardCharsets.UTF_8);
                        errorBuilder.append(errorPayload);
                        System.out.println("输出错误结果：" + errorPayload);
                    } else {
                        String messagePayload = new String(frame.getPayload(), StandardCharsets.UTF_8);
                        messageBuilder.append(messagePayload);
                        System.out.println("输出结果：" + messagePayload);
                    }
                    super.onNext(frame);
                }
            };
            
            try {
                stopWatch.start();
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();
                
                // 获取退出码
                try {
                    Integer exitCode = dockerClient.inspectExecCmd(execId).exec().getExitCode();
                    executeMessage.setExitValue(exitCode);
                } catch (Exception e) {
                    executeMessage.setExitValue(-1);
                }
                
            } catch (InterruptedException e) {
                System.out.println("程序执行异常或超时");
                errorBuilder.append("执行超时或异常");
                executeMessage.setExitValue(-1);
                time = TIME_OUT + 10;
            } finally {
                statsCmd.close();
            }
            
            executeMessage.setMessage(StrUtil.trim(messageBuilder.toString()));
            executeMessage.setErrorMessage(StrUtil.trim(errorBuilder.toString()));
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            
        } catch (Exception e) {
            executeMessage.setErrorMessage(e.getMessage());
            executeMessage.setExitValue(1);
        }
        
        return executeMessage;
    }

    /**
     * 3. 获取输出结果
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        long maxTime = 0;
        long maxMemory = 0;

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
            Long memory = executeMessage.getMemory();
            if (memory != null) {
                maxMemory = Math.max(maxMemory, memory);
            }
        }

        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemory);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        
        return executeCodeResponse;
    }

    /**
     * 4. 删除文件
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
