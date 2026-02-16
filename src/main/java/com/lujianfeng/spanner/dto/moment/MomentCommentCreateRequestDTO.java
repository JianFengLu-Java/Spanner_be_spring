package com.lujianfeng.spanner.dto.moment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MomentCommentCreateRequestDTO {
    private String text;
    private String parentCommentId;
    private String replyToAccount;
}
