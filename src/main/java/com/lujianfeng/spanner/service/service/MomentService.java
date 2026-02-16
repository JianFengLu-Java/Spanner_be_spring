package com.lujianfeng.spanner.service.service;

import com.lujianfeng.spanner.dto.moment.MomentCommentCreateRequestDTO;
import com.lujianfeng.spanner.dto.moment.MomentCreateRequestDTO;
import com.lujianfeng.spanner.vo.moment.CursorPageVO;
import com.lujianfeng.spanner.vo.moment.MomentCommentItemVO;
import com.lujianfeng.spanner.vo.moment.MomentItemVO;
import com.lujianfeng.spanner.vo.moment.MomentLikeUserVO;

import java.util.Map;

public interface MomentService {
    CursorPageVO<MomentItemVO> listMoments(String tab, String keyword, String cursor, Integer size, Double lat, Double lng);

    MomentItemVO getMomentDetail(String momentId);

    MomentItemVO createMoment(MomentCreateRequestDTO request);

    MomentItemVO updateMoment(String momentId, MomentCreateRequestDTO request);

    void deleteMoment(String momentId);

    Map<String, Object> likeMoment(String momentId);

    Map<String, Object> unlikeMoment(String momentId);

    CursorPageVO<MomentCommentItemVO> listComments(String momentId, String cursor, Integer size, String parentCommentId, String sort);

    MomentCommentItemVO createComment(String momentId, MomentCommentCreateRequestDTO request);

    CursorPageVO<MomentLikeUserVO> listLikes(String momentId, String cursor, Integer size);
}
