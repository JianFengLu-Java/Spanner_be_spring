package com.lujianfeng.spanner.dto.moment;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MomentCreateRequestDTO {
    private String title;
    private String contentText;
    private String contentHtml;
    private List<String> images;
}
