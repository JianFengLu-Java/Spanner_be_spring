package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.dto.call.CallActionRequestDTO;
import com.lujianfeng.spanner.dto.call.CallCreateRequestDTO;
import com.lujianfeng.spanner.dto.call.CallSignalRequestDTO;
import com.lujianfeng.spanner.service.CallBizException;
import com.lujianfeng.spanner.service.CallService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
public class CallController {

    private final CallService callService;

    public CallController(CallService callService) {
        this.callService = callService;
    }

    @PostMapping("/calls")
    public ResponseEntity<Map<String, Object>> createCall(@RequestBody(required = false) CallCreateRequestDTO requestDTO) {
        try {
            return ResponseEntity.ok(success("发起通话成功", callService.createCall(requestDTO)));
        } catch (CallBizException e) {
            return toErrorResponse(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error(500, "INTERNAL_ERROR", "服务异常"));
        }
    }

    @PostMapping("/calls/{callId}/accept")
    public ResponseEntity<Map<String, Object>> acceptCall(@PathVariable String callId,
                                                           @RequestBody(required = false) CallActionRequestDTO requestDTO) {
        try {
            return ResponseEntity.ok(success("接听成功", callService.acceptCall(callId, requestDTO)));
        } catch (CallBizException e) {
            return toErrorResponse(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error(500, "INTERNAL_ERROR", "服务异常"));
        }
    }

    @PostMapping("/calls/{callId}/reject")
    public ResponseEntity<Map<String, Object>> rejectCall(@PathVariable String callId,
                                                           @RequestBody(required = false) CallActionRequestDTO requestDTO) {
        try {
            return ResponseEntity.ok(success("拒绝成功", callService.rejectCall(callId, requestDTO)));
        } catch (CallBizException e) {
            return toErrorResponse(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error(500, "INTERNAL_ERROR", "服务异常"));
        }
    }

    @PostMapping("/calls/{callId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelCall(@PathVariable String callId,
                                                           @RequestBody(required = false) CallActionRequestDTO requestDTO) {
        try {
            return ResponseEntity.ok(success("取消成功", callService.cancelCall(callId, requestDTO)));
        } catch (CallBizException e) {
            return toErrorResponse(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error(500, "INTERNAL_ERROR", "服务异常"));
        }
    }

    @PostMapping("/calls/{callId}/end")
    public ResponseEntity<Map<String, Object>> endCall(@PathVariable String callId,
                                                        @RequestBody(required = false) CallActionRequestDTO requestDTO) {
        try {
            return ResponseEntity.ok(success("挂断成功", callService.endCall(callId, requestDTO)));
        } catch (CallBizException e) {
            return toErrorResponse(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error(500, "INTERNAL_ERROR", "服务异常"));
        }
    }

    @GetMapping("/calls/{callId}")
    public ResponseEntity<Map<String, Object>> getCall(@PathVariable String callId) {
        try {
            return ResponseEntity.ok(success("查询通话详情成功", callService.getCall(callId)));
        } catch (CallBizException e) {
            return toErrorResponse(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error(500, "INTERNAL_ERROR", "服务异常"));
        }
    }

    @PostMapping("/calls/{callId}/signals")
    public ResponseEntity<Map<String, Object>> signal(@PathVariable String callId,
                                                       @RequestBody(required = false) CallSignalRequestDTO requestDTO) {
        try {
            return ResponseEntity.ok(success("信令转发成功", callService.sendSignal(callId, requestDTO)));
        } catch (CallBizException e) {
            return toErrorResponse(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error(500, "INTERNAL_ERROR", "服务异常"));
        }
    }

    @PostMapping("/calls/{callId}/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat(@PathVariable String callId,
                                                          @RequestBody(required = false) CallActionRequestDTO requestDTO) {
        try {
            return ResponseEntity.ok(success("保活成功", callService.heartbeat(callId, requestDTO)));
        } catch (CallBizException e) {
            return toErrorResponse(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error(500, "INTERNAL_ERROR", "服务异常"));
        }
    }

    @GetMapping("/calls/history")
    public ResponseEntity<Map<String, Object>> history(@RequestParam(required = false) Integer page,
                                                        @RequestParam(required = false) Integer size) {
        try {
            return ResponseEntity.ok(success("查询通话记录成功", callService.history(page, size)));
        } catch (CallBizException e) {
            return toErrorResponse(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error(500, "INTERNAL_ERROR", "服务异常"));
        }
    }

    @GetMapping("/rtc/ice-servers")
    public ResponseEntity<Map<String, Object>> iceServers() {
        try {
            return ResponseEntity.ok(success("查询 ICE Server 成功", callService.listIceServers()));
        } catch (CallBizException e) {
            return toErrorResponse(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error(500, "INTERNAL_ERROR", "服务异常"));
        }
    }

    private ResponseEntity<Map<String, Object>> toErrorResponse(CallBizException e) {
        int code = e.getCode();
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status)
                .body(error(code, e.getErrorCode(), e.getMessage()));
    }

    private Map<String, Object> success(String message, Object data) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", 200);
        body.put("status", "success");
        body.put("message", message);
        body.put("data", data == null ? Map.of() : data);
        return body;
    }

    private Map<String, Object> error(int code, String errorCode, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("status", "error");
        body.put("message", message);
        body.put("errorCode", errorCode);
        return body;
    }
}
