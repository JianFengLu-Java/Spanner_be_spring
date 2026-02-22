package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.message.MessageReactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageReactionRepository extends JpaRepository<MessageReactionEntity, Long> {
    Optional<MessageReactionEntity> findByMessageTypeAndMessageRefIdAndReactionKey(String messageType,
                                                                                    String messageRefId,
                                                                                    String reactionKey);

    List<MessageReactionEntity> findByMessageTypeAndMessageRefIdOrderByUpdatedAtDescReactionKeyAsc(String messageType,
                                                                                                     String messageRefId);

    void deleteByMessageTypeAndMessageRefIdAndReactionKey(String messageType, String messageRefId, String reactionKey);
}
