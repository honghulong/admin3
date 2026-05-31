package tech.wetech.admin3.infra.mcp.xiaoyi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.ServerTransportSecurityValidator;
import io.modelcontextprotocol.server.transport.WebMvcStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import tech.wetech.admin3.sys.service.DictService;
import tech.wetech.admin3.sys.service.LeaveService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Configuration
public class XiaoYiMcpConfig {

  @Bean
  McpJsonMapper xiaoyiMcpJsonMapper(ObjectMapper mapper) {
    return new JacksonMcpJsonMapper(mapper);
  }

  @Bean
  WebMvcStatelessServerTransport xiaoyiWebMvcStatelessServerTransport(@Qualifier("xiaoyiMcpJsonMapper") McpJsonMapper xiaoyiMcpJsonMapper) {
    return WebMvcStatelessServerTransport.builder()
      .jsonMapper(xiaoyiMcpJsonMapper)
      .messageEndpoint("/xiaoyi-mcp/message")
      .securityValidator(ServerTransportSecurityValidator.NOOP)
      .build();
  }

  @Bean
  RouterFunction<ServerResponse> xiaoyiMcpRouterFunction(@Qualifier("xiaoyiWebMvcStatelessServerTransport") WebMvcStatelessServerTransport xiaoyiWebMvcStatelessServerTransport) {
    return xiaoyiWebMvcStatelessServerTransport.getRouterFunction();
  }

  @Bean
  McpStatelessSyncServer xiaoyiMcpStatelessSyncServer(@Qualifier("xiaoyiWebMvcStatelessServerTransport") WebMvcStatelessServerTransport xiaoyiWebMvcStatelessServerTransport, LeaveService leaveService, DictService dictService, @Qualifier("xiaoyiMcpJsonMapper") McpJsonMapper xiaoyiMcpJsonMapper) {
    return McpServer.sync(xiaoyiWebMvcStatelessServerTransport)
      .serverInfo("xiaoyi-leave-mcp-server", "1.0.0")
      .capabilities(McpSchema.ServerCapabilities.builder()
        .tools(true)
        .logging()
        .build())
      .tools(
        createQueryLeavesTool(leaveService, xiaoyiMcpJsonMapper),
        createCreateLeaveTool(leaveService, dictService, xiaoyiMcpJsonMapper),
        createUpdateLeaveTool(leaveService, dictService, xiaoyiMcpJsonMapper),
        createCancelLeaveTool(leaveService, xiaoyiMcpJsonMapper)
      )
      .build();
  }

  private McpStatelessServerFeatures.SyncToolSpecification createQueryLeavesTool(LeaveService leaveService, McpJsonMapper jsonMapper) {
    McpSchema.Tool tool = McpSchema.Tool.builder()
      .name("query_leaves_by_username")
      .description("按用户名查询自己的请假信息，返回的status字段说明: 0=刚提交, 1=审核通过, 2=被退回, 3=已销假")
      .inputSchema(jsonMapper, """
        {
          "type": "object",
          "properties": {
            "username": {
              "type": "string",
              "description": "用户名"
            }
          },
          "required": ["username"]
        }
        """)
      .build();
    return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {
      String username = String.valueOf(request.arguments().get("username"));
      var leaves = leaveService.findLeavesByUsername(username);
      StringBuilder result = new StringBuilder();
      for (var leave : leaves) {
        result.append(String.format("""
          请假记录:
          ID: %s
          请假类型: %s
          开始时间: %s
          结束时间: %s
          请假原因: %s
          状态: %s
          销假时间: %s
          --------------------
          """,
          leave.id(),
          leave.leaveTypeLabel(),
          leave.startTime(),
          leave.endTime(),
          leave.leaveReason() != null ? leave.leaveReason() : "",
          leave.leaveStatus(),
          leave.cancelTime() != null ? leave.cancelTime().toString() : ""
        ));
      }
      if (result.isEmpty()) {
        return new McpSchema.CallToolResult("未找到该用户的请假记录", false);
      }
      return new McpSchema.CallToolResult(result.toString(), false);
    });
  }

  private static LocalDateTime parseDateTime(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("时间不能为空");
    }
    value = value.trim();
    try {
      return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    } catch (DateTimeParseException e) {
      try {
        return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      } catch (DateTimeParseException e2) {
        throw new IllegalArgumentException("时间格式不正确，请使用 yyyy-MM-dd HH:mm:ss 或 ISO 格式，例如: 2026-06-02 08:00:00 或 2026-06-02T08:00:00");
      }
    }
  }

  private McpStatelessServerFeatures.SyncToolSpecification createCreateLeaveTool(LeaveService leaveService, DictService dictService, McpJsonMapper jsonMapper) {
    McpSchema.Tool tool = McpSchema.Tool.builder()
      .name("create_leave")
      .description("新增一条用户请假记录，创建成功后状态为0(刚提交)")
      .inputSchema(jsonMapper, """
        {
          "type": "object",
          "properties": {
            "username": {
              "type": "string",
              "description": "用户名"
            },
            "leaveType": {
              "type": "string",
              "description": "请假类型，只接受汉字，可选值: 病假、事假、年假、婚假、丧假、产假、调休"
            },
            "startTime": {
              "type": "string",
              "description": "开始时间, 格式: yyyy-MM-dd HH:mm:ss"
            },
            "endTime": {
              "type": "string",
              "description": "结束时间, 格式: yyyy-MM-dd HH:mm:ss"
            },
            "leaveReason": {
              "type": "string",
              "description": "请假原因"
            }
          },
          "required": ["username", "leaveType", "startTime", "endTime"]
        }
        """)
      .build();
    return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {
      try {
        String username = String.valueOf(request.arguments().get("username"));
        String leaveTypeLabel = String.valueOf(request.arguments().get("leaveType"));
        String leaveType = dictService.findValueByDictCodeAndLabel("leave_type", leaveTypeLabel);
        LocalDateTime startTime = parseDateTime(String.valueOf(request.arguments().get("startTime")));
        LocalDateTime endTime = parseDateTime(String.valueOf(request.arguments().get("endTime")));
        String leaveReason = request.arguments().containsKey("leaveReason") ? String.valueOf(request.arguments().get("leaveReason")) : null;

        var leave = leaveService.createLeaveByUsername(username, leaveType, startTime, endTime, leaveReason);
        String result = String.format("""
          请假创建成功!
          ID: %s
          请假类型: %s
          开始时间: %s
          结束时间: %s
          请假原因: %s
          状态: %s
          """,
          leave.id(),
          leave.leaveTypeLabel(),
          leave.startTime(),
          leave.endTime(),
          leave.leaveReason() != null ? leave.leaveReason() : "",
          leave.leaveStatus()
        );
        return new McpSchema.CallToolResult(result, false);
      } catch (Exception e) {
        return new McpSchema.CallToolResult("创建请假失败: " + e.getMessage(), true);
      }
    });
  }

  private McpStatelessServerFeatures.SyncToolSpecification createUpdateLeaveTool(LeaveService leaveService, DictService dictService, McpJsonMapper jsonMapper) {
    McpSchema.Tool tool = McpSchema.Tool.builder()
      .name("update_leave")
      .description("修改请假记录，status字段说明: 0=刚提交, 1=审核通过, 2=被退回, 3=已销假")
      .inputSchema(jsonMapper, """
        {
          "type": "object",
          "properties": {
            "leaveId": {
              "type": "number",
              "description": "请假记录ID"
            },
            "leaveType": {
              "type": "string",
              "description": "请假类型，只接受汉字，可选值: 病假、事假、年假、婚假、丧假、产假、调休"
            },
            "startTime": {
              "type": "string",
              "description": "开始时间, 格式: yyyy-MM-dd HH:mm:ss"
            },
            "endTime": {
              "type": "string",
              "description": "结束时间, 格式: yyyy-MM-dd HH:mm:ss"
            },
            "leaveReason": {
              "type": "string",
              "description": "请假原因"
            }
          },
          "required": ["leaveId", "leaveType", "startTime", "endTime"]
        }
        """)
      .build();
    return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {
      try {
        Long leaveId = Long.valueOf(String.valueOf(request.arguments().get("leaveId")));
        String leaveTypeLabel = String.valueOf(request.arguments().get("leaveType"));
        String leaveType = dictService.findValueByDictCodeAndLabel("leave_type", leaveTypeLabel);
        LocalDateTime startTime = parseDateTime(String.valueOf(request.arguments().get("startTime")));
        LocalDateTime endTime = parseDateTime(String.valueOf(request.arguments().get("endTime")));
        String leaveReason = request.arguments().containsKey("leaveReason") ? String.valueOf(request.arguments().get("leaveReason")) : null;

        var leave = leaveService.updateLeave(leaveId, leaveType, startTime, endTime, leaveReason);
        String result = String.format("""
          请假修改成功!
          ID: %s
          请假类型: %s
          开始时间: %s
          结束时间: %s
          请假原因: %s
          状态: %s
          """,
          leave.id(),
          leave.leaveTypeLabel(),
          leave.startTime(),
          leave.endTime(),
          leave.leaveReason() != null ? leave.leaveReason() : "",
          leave.leaveStatus()
        );
        return new McpSchema.CallToolResult(result, false);
      } catch (Exception e) {
        return new McpSchema.CallToolResult("修改请假失败: " + e.getMessage(), true);
      }
    });
  }

  private McpStatelessServerFeatures.SyncToolSpecification createCancelLeaveTool(LeaveService leaveService, McpJsonMapper jsonMapper) {
    McpSchema.Tool tool = McpSchema.Tool.builder()
      .name("cancel_leave")
      .description("销假（不能删除，只能销假），销假后状态变为3(已销假)")
      .inputSchema(jsonMapper, """
        {
          "type": "object",
          "properties": {
            "leaveId": {
              "type": "number",
              "description": "请假记录ID"
            }
          },
          "required": ["leaveId"]
        }
        """)
      .build();
    return new McpStatelessServerFeatures.SyncToolSpecification(tool, (ctx, request) -> {
      try {
        Long leaveId = Long.valueOf(String.valueOf(request.arguments().get("leaveId")));
        var leave = leaveService.cancelLeave(leaveId);
        String result = String.format("""
          销假成功!
          ID: %s
          请假类型: %s
          开始时间: %s
          结束时间: %s
          状态: %s
          销假时间: %s
          """,
          leave.id(),
          leave.leaveTypeLabel(),
          leave.startTime(),
          leave.endTime(),
          leave.leaveStatus(),
          leave.cancelTime()
        );
        return new McpSchema.CallToolResult(result, false);
      } catch (Exception e) {
        return new McpSchema.CallToolResult("销假失败: " + e.getMessage(), true);
      }
    });
  }
}
