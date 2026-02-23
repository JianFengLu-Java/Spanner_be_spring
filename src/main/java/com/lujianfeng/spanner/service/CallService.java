package com.lujianfeng.spanner.service;

import com.lujianfeng.spanner.dto.call.CallActionRequestDTO;
import com.lujianfeng.spanner.dto.call.CallCreateRequestDTO;
import com.lujianfeng.spanner.dto.call.CallSignalRequestDTO;
import com.lujianfeng.spanner.vo.call.CallActionResultVO;
import com.lujianfeng.spanner.vo.call.CallSessionVO;
import com.lujianfeng.spanner.vo.call.RtcIceServersVO;

import java.util.Map;

public interface CallService {

    CallActionResultVO createCall(CallCreateRequestDTO requestDTO);

    CallActionResultVO acceptCall(String callId, CallActionRequestDTO requestDTO);

    CallActionResultVO rejectCall(String callId, CallActionRequestDTO requestDTO);

    CallActionResultVO cancelCall(String callId, CallActionRequestDTO requestDTO);

    CallActionResultVO endCall(String callId, CallActionRequestDTO requestDTO);

    CallSessionVO getCall(String callId);

    Map<String, Object> sendSignal(String callId, CallSignalRequestDTO requestDTO);

    Map<String, Object> heartbeat(String callId, CallActionRequestDTO requestDTO);

    Map<String, Object> history(Integer page, Integer size);

    RtcIceServersVO listIceServers();

    void markNoAnswerTimeoutSessions();
}
