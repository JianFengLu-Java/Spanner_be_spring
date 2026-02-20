package com.lujianfeng.spanner.service;

import java.time.LocalDateTime;

public class CloudDocVersionConflictException extends RuntimeException {

    private final Long latestVersion;
    private final LocalDateTime latestUpdatedAt;

    public CloudDocVersionConflictException(Long latestVersion, LocalDateTime latestUpdatedAt) {
        super("文档版本冲突");
        this.latestVersion = latestVersion;
        this.latestUpdatedAt = latestUpdatedAt;
    }

    public Long getLatestVersion() {
        return latestVersion;
    }

    public LocalDateTime getLatestUpdatedAt() {
        return latestUpdatedAt;
    }
}
