package com.lujianfeng.spanner.entity.call;

public enum CallStatusEnum {
    RINGING,
    ANSWERED,
    CONNECTING,
    CONNECTED,
    REJECTED,
    CANCELED,
    NO_ANSWER,
    BUSY,
    ENDED,
    FAILED;

    public boolean isTerminal() {
        return this == REJECTED
                || this == CANCELED
                || this == NO_ANSWER
                || this == BUSY
                || this == ENDED
                || this == FAILED;
    }
}
