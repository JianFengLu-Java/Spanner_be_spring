package com.lujianfeng.spanner.service.task;

import com.lujianfeng.spanner.dto.task.TaskEventRequestDTO;
import com.lujianfeng.spanner.vo.task.GrowthLedgerItemVO;
import com.lujianfeng.spanner.vo.task.TaskConfigVO;
import com.lujianfeng.spanner.vo.task.TaskEventProcessResultVO;
import com.lujianfeng.spanner.vo.task.TodayRewardProgressVO;
import com.lujianfeng.spanner.vo.task.WalletLedgerItemVO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TaskRewardService {
    TaskEventProcessResultVO handleTaskEvent(TaskEventRequestDTO request);

    List<TaskConfigVO> listTaskConfig();

    TodayRewardProgressVO getTodayProgress(Long userId);

    Page<WalletLedgerItemVO> getWalletLedger(Long userId, int page, int size);

    Page<GrowthLedgerItemVO> getGrowthLedger(Long userId, int page, int size);

    void onMomentCreated(String momentId, Long actorUserId);

    void onCommentCreated(String commentId, String momentId, Long actorUserId, String content);
}
