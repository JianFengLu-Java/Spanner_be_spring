package com.lujianfeng.spanner.service.service;

import com.lujianfeng.spanner.vo.emoji.EmojiPackItemVO;
import com.lujianfeng.spanner.vo.user.PageResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface EmojiPackService {
    EmojiPackItemVO uploadToGallery(MultipartFile file, String displayName);

    PageResultVO<EmojiPackItemVO> listMyGallery(Integer page, Integer size, String keyword);

    void deleteFromGallery(String emojiId);

    EmojiPackItemVO favoriteEmoji(String emojiId);

    void unfavoriteEmoji(String emojiId);

    PageResultVO<EmojiPackItemVO> listMyFavorites(Integer page, Integer size);
}
