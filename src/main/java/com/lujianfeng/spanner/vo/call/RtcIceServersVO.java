package com.lujianfeng.spanner.vo.call;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RtcIceServersVO {
    private List<IceServerItemVO> servers;

    @Getter
    @Builder
    public static class IceServerItemVO {
        private List<String> urls;
        private String username;
        private String credential;
        private Integer ttlSeconds;
    }
}
