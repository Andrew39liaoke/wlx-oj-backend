package com.wlx.ojbackendcodesandbox.codesandbox.cpp;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.wlx.ojbackendcodesandbox.codesandbox.CodeSandbox;
import com.wlx.ojbackendcodesandbox.model.ExecuteCodeRequest;
import com.wlx.ojbackendcodesandbox.model.ExecuteCodeResponse;
import com.wlx.ojbackendcodesandbox.model.ExecuteMessage;
import com.wlx.ojbackendcodesandbox.model.JudgeInfo;
import lombok.extern.slf4j.Slf4j;
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
 * C++ Docker 代码沙箱实现
 */
@Slf4j
public class CppDockerCodeSandbox implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_CPP_FILE_NAME = "main.cpp";

    private static final long TIME_OUT = 5000L;

    private static final String DOCKER_IMAGE = "gcc:13.2";

    private static final Boolean FIRST_INIT = true;

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
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_CPP_FILE_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 2. Docker 方式执行代码
     */
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        
        // 获取默认的 Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        // 拉取镜像
        if (FIRST_INIT) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(DOCKER_IMAGE);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
        }

        System.out.println("下载完成");

        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(DOCKER_IMAGE);
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)
                .withReadonlyRootfs(true)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();
        System.out.println(createContainerResponse);
        String containerId = createContainerResponse.getId();

        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();

        // 编译代码
        String[] compileCmdArray = new String[]{"g++", "-o", "/app/main", "/app/" + GLOBAL_CPP_FILE_NAME};
        execInContainer(dockerClient, containerId, compileCmdArray);

        // 执行命令并获取结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            // 通过 shell 管道将输入传入 stdin，而非命令行参数
            String[] runCmdArray;
            if (inputArgs != null && !inputArgs.isEmpty()) {
                runCmdArray = new String[]{"sh", "-c", "echo '" + inputArgs + "' | /app/main"};
            } else {
                runCmdArray = new String[]{"/app/main"};
            }
            
            ExecuteMessage executeMessage = execInContainer(dockerClient, containerId, runCmdArray);
            executeMessageList.add(executeMessage);
        }
        
        // 清理容器
        dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        
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
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            
            System.out.println("创建执行命令：" + execCreateCmdResponse);
            String execId = execCreateCmdResponse.getId();
            
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
            statsCmd.exec(statisticsResultCallback);
            
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = (errorMessage[0] == null ? "" : errorMessage[0]) + new String(frame.getPayload());
                    } else {
                        message[0] = (message[0] == null ? "" : message[0]) + new String(frame.getPayload());
                    }
                    super.onNext(frame);
                }
            };
            
            stopWatch.start();
            dockerClient.execStartCmd(execId)
                    .exec(execStartResultCallback)
                    .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
            stopWatch.stop();
            time = stopWatch.getLastTaskTimeMillis();
            
            statsCmd.close();
            
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            executeMessage.setExitValue(0);
            
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
