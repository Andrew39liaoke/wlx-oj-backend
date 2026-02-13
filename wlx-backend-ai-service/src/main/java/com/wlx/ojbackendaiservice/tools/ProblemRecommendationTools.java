package com.wlx.ojbackendaiservice.tools;

import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendserviceclient.service.UserFeignClient;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProblemRecommendationTools {
    @Resource
    private UserFeignClient userFeignClient;

    @Tool(description = "查询用户信息")
    public Map<String, Object> queryStudentInfo(@ToolParam(description = "用户id") Long userId) {
        Map<String, Object> map = new HashMap<>();
        User user = userFeignClient.getById(userId);
        if (user != null) {
            map.put("用户名", user.getUserName());
            map.put("学生用户id", user.getId());
            map.put("用户角色", user.getRole());
        }
        return map;
    }

    @Tool(description = "查询当前学生加入的所有班级")
    public List<Map<String, Object>> queryStudentClass(@ToolParam(description = "学生用户id") Long studentId) {
        List<Map<String, Object>> list = userFeignClient.getStudentClasses(studentId);
        return list;
    }

    @Tool(description = "查询当前学生在班内的所有题目")
    public List<Map<String, Object>> queryStudentClassProblemCompletion(@ToolParam(description = "学生用户id") Long studentId){
        List<Map<String, Object>> list = userFeignClient.getStudentClassProblems(studentId);
        return list;
    }

}
