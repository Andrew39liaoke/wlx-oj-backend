package com.wlx.ojbackendcodesandbox.codesandbox.java;

import cn.hutool.core.io.resource.ResourceUtil;
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
import com.wlx.ojbackendcodesandbox.codesandbox.JavaCodeSandboxTemplate;
import com.wlx.ojbackendcodesandbox.model.ExecuteCodeRequest;
import com.wlx.ojbackendcodesandbox.model.ExecuteCodeResponse;
import com.wlx.ojbackendcodesandbox.model.ExecuteMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {

    private static final long TIME_OUT = 5000L;

    private static final Boolean FIRST_INIT = false;

    @Value("${docker.host}")
    private String dockerHost;

    public static void main(String[] args) {
        JavaDockerCodeSandbox javaNativeCodeSandbox = new JavaDockerCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/unsafeCode/RunFileError.java", StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/simpleCompute/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    /**
     * 3、创建容器，把文件复制到容器内
     * @param userCodeFile
     * @param inputList
     * @return
     */
    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        // 配置 Docker Client 使用 Apache HttpClient 传输层，避免 Jersey 冲突
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

        // 1. 获取并检测镜像
        String image = "openjdk:17-alpine";
        boolean hasImage = true;
        try {
            dockerClient.inspectImageCmd(image).exec();
        } catch (com.github.dockerjava.api.exception.NotFoundException e) {
            hasImage = false;
        }
        if (!hasImage) {
            System.out.println("本地无镜像，开始拉取：" + image);
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                // 设置 5 分钟超时，防止网络卡死
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion(5, TimeUnit.MINUTES);
                System.out.println("下载镜像完成");
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常或超时");
                throw new RuntimeException("拉取镜像异常", e);
            }
        }

        // 2. 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1000 * 1000L); // 限存 100M
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(false)
                .withReadonlyRootfs(false)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(false) // 禁用 TTY，避免输出被污染且防止 Stdout/Stderr 混淆
                .withCmd("tail", "-f", "/dev/null") // 保持容器长驻留，确保后续可安全执行 exec
                .withStdInOnce(false)
                .exec();
        
        System.out.println(createContainerResponse);
        String containerId = createContainerResponse.getId();

        // 3. 启动容器并在 finally 中确保清理
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        try {
            dockerClient.startContainerCmd(containerId).exec();

            // 为每个输入构建测试环境，执行程序
            for (int i = 0; i < inputList.size(); i++) {
                String inputArgs = inputList.get(i);
                StopWatch stopWatch = new StopWatch();
                
                // 与本地运行的命令 "java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main" 保持完全一致的防注入方式
                String[] cmdArray;
                if (inputArgs != null && !inputArgs.isEmpty()) {
                    String inputFilePath = userCodeParentPath + File.separator + i + ".txt";
                    cn.hutool.core.io.FileUtil.writeString(inputArgs, inputFilePath, StandardCharsets.UTF_8);
                    cmdArray = new String[]{"sh", "-c", "java -Xmx256m -Dfile.encoding=UTF-8 -cp /app Main < /app/" + i + ".txt"};
                } else {
                    cmdArray = new String[]{"java", "-Xmx256m", "-Dfile.encoding=UTF-8", "-cp", "/app", "Main"};
                }
                
                ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                        .withCmd(cmdArray)
                        .withWorkingDir("/app")
                        .withAttachStderr(true)
                        .withAttachStdin(true)
                        .withAttachStdout(true)
                        .exec();
                
                System.out.println("创建执行命令：" + execCreateCmdResponse);
                ExecuteMessage executeMessage = new ExecuteMessage();
                StringBuilder messageBuilder = new StringBuilder();
                StringBuilder errorBuilder = new StringBuilder();
                long time = 0L;
                
                String execId = execCreateCmdResponse.getId();
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

                final long[] maxMemory = {0L};

                // 获取占用的内存
                StatsCmd statsCmd = dockerClient.statsCmd(containerId);
                ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                    @Override
                    public void onNext(Statistics statistics) {
                        System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
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

                try {
                    stopWatch.start();
                    dockerClient.execStartCmd(execId)
                            .exec(execStartResultCallback)
                            // 这里原为 SECONDS，TIME_OUT 是 5000L 意思是 5000 秒。现在改正为 MILLISECONDS
                            .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                    stopWatch.stop();
                    time = stopWatch.getLastTaskTimeMillis();
                    
                    // 新增：获取真正的退出状态码
                    try {
                        Integer exitCode = dockerClient.inspectExecCmd(execId).exec().getExitCode();
                        executeMessage.setExitValue(exitCode);
                    } catch (Exception e) {
                        System.out.println("无法获取容器退出状态码: " + e.getMessage());
                        executeMessage.setExitValue(-1); // 网络或解析失败时算作非正常
                    }
                    
                } catch (InterruptedException e) {
                    System.out.println("程序执行异常或超时");
                    errorBuilder.append("执行超时或异常");
                    executeMessage.setExitValue(-1); // 超时算作非零错误退出
                    // 为了统一也能被上层判定为超时，需要补发一个超量时间参数给 time
                    time = TIME_OUT + 10;
                } finally {
                    statsCmd.close();
                }

                // trim 处理，确保和本地 readLine 剥离尾部换行的表现一致
                executeMessage.setMessage(StrUtil.trim(messageBuilder.toString()));
                executeMessage.setErrorMessage(StrUtil.trim(errorBuilder.toString()));
                executeMessage.setTime(time);
                executeMessage.setMemory(maxMemory[0]);
                executeMessageList.add(executeMessage);
            }
        } finally {
            // 最后必定清理并销毁容器实体，保持本地环境轻巧
            System.out.println("清理临时容器，容器ID: " + containerId);
            try {
                dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            } catch (Exception e) {
                System.out.println("清理容器时抛出异常: " + e.getMessage());
            }
        }

        return executeMessageList;
    }
}



