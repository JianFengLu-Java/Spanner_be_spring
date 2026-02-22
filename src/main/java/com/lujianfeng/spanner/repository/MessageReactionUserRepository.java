package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.message.MessageReactionUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageReactionUserRepository extends JpaRepository<MessageReactionUserEntity, Long> {
    Optional<MessageReactionUserEntity> findByMessageTypeAndMessageRefIdAndReactionKeyAndUserAccount(String messageType,
                                                                                                       String messageRefId,
                                                                                                       String reactionKey,
                                                                                                       String userAccount);

    List<MessageReactionUserEntity> findByMessageTypeAndMessageRefIdAndReactionKeyOrderByUserAccountAsc(String messageType,
                                                                                                          String messageRefId,
                                                                                                          String reactionKey);

    long countByMessageTypeAndMessageRefIdAndReactionKey(String messageType, String messageRefId, String reactionKey);
}
