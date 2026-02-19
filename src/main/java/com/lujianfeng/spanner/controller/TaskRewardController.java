package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.dto.task.TaskEventRequestDTO;
import com.lujianfeng.spanner.entity.task.GrowthAccountEntity;
import com.lujianfeng.spanner.entity.user.WalletAccountEntity;
import com.lujianfeng.spanner.repository.GrowthAccountRepository;
import com.lujianfeng.spanner.repository.WalletAccountRepository;
import com.lujianfeng.spanner.service.task.TaskRewardService;
import com.lujianfeng.spanner.vo.task.GrowthLedgerItemVO;
import com.lujianfeng.spanner.vo.task.TaskEventProcessResultVO;
import com.lujianfeng.spanner.vo.task.WalletLedgerItemVO;
import org.springframework.data.domain.Page;
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
public class TaskRewardController {

    private final TaskRewardService taskRewardService;
    private final WalletAccountRepository walletAccountRepository;
    private final GrowthAccountRepository growthAccountRepository;

    public TaskRewardController(TaskRewardService taskRewardService,
                                WalletAccountRepository walletAccountRepository,
                                GrowthAccountRepository growthAccountRepository) {
        this.taskRewardService = taskRewardService;
        this.walletAccountRepository = walletAccountRepository;
        this.growthAccountRepository = growthAccountRepository;
    }

    @PostMapping("/task-events")
    public ResponseEntity<Map<String, Object>> handleTaskEvent(@RequestBody TaskEventRequestDTO request) {
        try {
            TaskEventProcessResultVO result = taskRewardService.handleTaskEvent(request);
            int code = result.isDuplicate() ? 409 : 200;
            String message = result.isDuplicate() ? "事件重复，返回已有处理结果" : "处理成功";
            return ResponseEntity.status(code == 200 ? HttpStatus.OK : HttpStatus.CONFLICT)
                    .body(success(code, message, result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "INVALID_EVENT"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @GetMapping("/tasks/config")
    public ResponseEntity<Map<String, Object>> listTaskConfig() {
        return ResponseEntity.ok(success(200, "查询成功", taskRewardService.listTaskConfig()));
    }

    @GetMapping("/users/{id}/rewards/today")
    public ResponseEntity<Map<String, Object>> getTodayRewards(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(success(200, "查询成功", taskRewardService.getTodayProgress(userId)));
    }

    @GetMapping("/users/{id}/wallet/ledger")
    public ResponseEntity<Map<String, Object>> getWalletLedger(@PathVariable("id") Long userId,
                                                                @RequestParam(required = false, defaultValue = "0") Integer page,
                                                                @RequestParam(required = false, defaultValue = "20") Integer size) {
        int safePage = Math.max(0, page == null ? 0 : page);
        int safeSize = Math.min(100, Math.max(1, size == null ? 20 : size));
        Page<WalletLedgerItemVO> ledger = taskRewardService.getWalletLedger(userId, safePage, safeSize);
        WalletAccountEntity account = walletAccountRepository.findByUserId(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("account", Map.of(
                "userId", userId,
                "balanceCent", account == null || account.getBalanceCent() == null ? 0L : account.getBalanceCent(),
                "version", account == null || account.getRewardVersion() == null ? 0 : account.getRewardVersion()
        ));
        data.put("items", ledger.getContent());
        data.put("page", ledger.getNumber());
        data.put("size", ledger.getSize());
        data.put("total", ledger.getTotalElements());
        return ResponseEntity.ok(success(200, "查询成功", data));
    }

    @GetMapping("/users/{id}/growth/ledger")
    public ResponseEntity<Map<String, Object>> getGrowthLedger(@PathVariable("id") Long userId,
                                                                @RequestParam(required = false, defaultValue = "0") Integer page,
                                                                @RequestParam(required = false, defaultValue = "20") Integer size) {
        int safePage = Math.max(0, page == null ? 0 : page);
        int safeSize = Math.min(100, Math.max(1, size == null ? 20 : size));
        Page<GrowthLedgerItemVO> ledger = taskRewardService.getGrowthLedger(userId, safePage, safeSize);
        GrowthAccountEntity account = growthAccountRepository.findByUserId(userId).orElse(null);

        Map<String, Object> data = new HashMap<>();
        data.put("account", Map.of(
                "userId", userId,
                "growthValue", account == null || account.getGrowthValue() == null ? 0L : account.getGrowthValue(),
                "version", account == null || account.getVersion() == null ? 0 : account.getVersion()
        ));
        data.put("items", ledger.getContent());
        data.put("page", ledger.getNumber());
        data.put("size", ledger.getSize());
        data.put("total", ledger.getTotalElements());
        return ResponseEntity.ok(success(200, "查询成功", data));
    }

    private Map<String, Object> success(int code, String message, Object data) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("status", "success");
        body.put("message", message);
        body.put("data", data == null ? Map.of() : data);
        return body;
    }

    private Map<String, Object> error(int code, String message, String errorCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("status", "error");
        body.put("message", message);
        body.put("errorCode", errorCode);
        return body;
    }
}
