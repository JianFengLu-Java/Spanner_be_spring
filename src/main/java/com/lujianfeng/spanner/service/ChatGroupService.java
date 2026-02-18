package com.lujianfeng.spanner.service;

import com.lujianfeng.spanner.dto.group.GroupCreateRequestDTO;
import com.lujianfeng.spanner.entity.group.ChatGroupEntity;
import com.lujianfeng.spanner.entity.group.ChatGroupMemberEntity;
import com.lujianfeng.spanner.entity.group.GroupMemberRoleEnum;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.entity.user.UserRelationEnum;
import com.lujianfeng.spanner.repository.ChatGroupMemberRepository;
import com.lujianfeng.spanner.repository.ChatGroupRepository;
import com.lujianfeng.spanner.repository.UserRelationRepository;
import com.lujianfeng.spanner.repository.UserRepository;
import com.lujianfeng.spanner.vo.group.GroupInfoVO;
import com.lujianfeng.spanner.vo.group.GroupMemberVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ChatGroupService {
    public static final int GROUP_MAX_MEMBERS = 500;

    private final ChatGroupRepository chatGroupRepository;
    private final ChatGroupMemberRepository chatGroupMemberRepository;
    private final UserRepository userRepository;
    private final UserRelationRepository userRelationRepository;

    public ChatGroupService(ChatGroupRepository chatGroupRepository,
                            ChatGroupMemberRepository chatGroupMemberRepository,
                            UserRepository userRepository,
                            UserRelationRepository userRelationRepository) {
        this.chatGroupRepository = chatGroupRepository;
        this.chatGroupMemberRepository = chatGroupMemberRepository;
        this.userRepository = userRepository;
        this.userRelationRepository = userRelationRepository;
    }

    @Transactional
    public GroupInfoVO createGroup(UserEntity currentUser, GroupCreateRequestDTO requestDTO) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        String groupName = trim(requestDTO == null ? null : requestDTO.getGroupName());
        if (groupName == null) {
            throw new IllegalArgumentException("groupName 不能为空");
        }
        if (groupName.length() > 64) {
            throw new IllegalArgumentException("groupName 长度不能超过 64");
        }
        String announcement = trim(requestDTO == null ? null : requestDTO.getAnnouncement());
        if (announcement != null && announcement.length() > 1000) {
            throw new IllegalArgumentException("announcement 长度不能超过 1000");
        }

        ChatGroupEntity group = new ChatGroupEntity();
        group.setGroupNo(generateUniqueGroupNo());
        group.setGroupName(groupName);
        group.setOwnerAccount(currentUser.getAccount());
        group.setAnnouncement(announcement);
        group.setMaxMembers(GROUP_MAX_MEMBERS);
        ChatGroupEntity savedGroup = chatGroupRepository.save(group);

        ChatGroupMemberEntity ownerMember = new ChatGroupMemberEntity();
        ownerMember.setGroupId(savedGroup.getId());
        ownerMember.setUserAccount(currentUser.getAccount());
        ownerMember.setRole(GroupMemberRoleEnum.OWNER);
        chatGroupMemberRepository.save(ownerMember);

        return toGroupInfo(savedGroup, GroupMemberRoleEnum.OWNER, 1L);
    }

    public GroupInfoVO getGroupInfo(UserEntity currentUser, String groupNo) {
        ChatGroupEntity group = findGroupByNo(groupNo);
        ChatGroupMemberEntity member = findMember(group.getId(), currentUser == null ? null : currentUser.getAccount());
        if (member == null) {
            throw new IllegalStateException("你不在该群，无法查看群资料");
        }
        return toGroupInfo(group, member.getRole(), chatGroupMemberRepository.countByGroupId(group.getId()));
    }

    public List<GroupMemberVO> listGroupMembers(UserEntity currentUser, String groupNo) {
        ChatGroupEntity group = findGroupByNo(groupNo);
        ensureMember(group.getId(), currentUser == null ? null : currentUser.getAccount());
        return chatGroupMemberRepository.findByGroupIdOrderByJoinedAtAsc(group.getId()).stream()
                .map(member -> GroupMemberVO.builder()
                        .account(member.getUserAccount())
                        .role(member.getRole().name())
                        .joinedAt(member.getJoinedAt())
                        .build())
                .toList();
    }

    @Transactional
    public GroupInfoVO joinGroup(UserEntity currentUser, String groupNo) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        ChatGroupEntity group = findGroupByNo(groupNo);
        ChatGroupMemberEntity existed = findMember(group.getId(), currentUser.getAccount());
        if (existed != null) {
            return toGroupInfo(group, existed.getRole(), chatGroupMemberRepository.countByGroupId(group.getId()));
        }

        long memberCount = chatGroupMemberRepository.countByGroupId(group.getId());
        if (memberCount >= group.getMaxMembers()) {
            throw new IllegalStateException("该群人数已满，最多 500 人");
        }

        ChatGroupMemberEntity member = new ChatGroupMemberEntity();
        member.setGroupId(group.getId());
        member.setUserAccount(currentUser.getAccount());
        member.setRole(GroupMemberRoleEnum.MEMBER);
        chatGroupMemberRepository.save(member);

        return toGroupInfo(group, GroupMemberRoleEnum.MEMBER, memberCount + 1);
    }

    @Transactional
    public GroupInfoVO inviteFriend(UserEntity currentUser, String groupNo, String friendAccountRaw) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        String friendAccount = trim(friendAccountRaw);
        if (friendAccount == null) {
            throw new IllegalArgumentException("friendAccount 不能为空");
        }
        if (friendAccount.equals(currentUser.getAccount())) {
            throw new IllegalArgumentException("不能邀请自己");
        }

        ChatGroupEntity group = findGroupByNo(groupNo);
        ensureMember(group.getId(), currentUser.getAccount());

        UserEntity friend = userRepository.findByAccount(friendAccount);
        if (friend == null) {
            throw new IllegalArgumentException("好友账号不存在");
        }
        boolean isFriend = userRelationRepository.existsByUserAndFriendAndRelationType(currentUser, friend, UserRelationEnum.ACCEPTED)
                || userRelationRepository.existsByUserAndFriendAndRelationType(friend, currentUser, UserRelationEnum.ACCEPTED);
        if (!isFriend) {
            throw new IllegalStateException("仅可邀请好友入群");
        }

        ChatGroupMemberEntity existed = findMember(group.getId(), friendAccount);
        if (existed != null) {
            return toGroupInfo(group, ensureMember(group.getId(), currentUser.getAccount()).getRole(),
                    chatGroupMemberRepository.countByGroupId(group.getId()));
        }

        long memberCount = chatGroupMemberRepository.countByGroupId(group.getId());
        if (memberCount >= group.getMaxMembers()) {
            throw new IllegalStateException("该群人数已满，最多 500 人");
        }

        ChatGroupMemberEntity member = new ChatGroupMemberEntity();
        member.setGroupId(group.getId());
        member.setUserAccount(friendAccount);
        member.setRole(GroupMemberRoleEnum.MEMBER);
        chatGroupMemberRepository.save(member);

        return toGroupInfo(group, ensureMember(group.getId(), currentUser.getAccount()).getRole(), memberCount + 1);
    }

    @Transactional
    public void quitGroup(UserEntity currentUser, String groupNo) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        ChatGroupEntity group = findGroupByNo(groupNo);
        ChatGroupMemberEntity member = ensureMember(group.getId(), currentUser.getAccount());
        if (member.getRole() == GroupMemberRoleEnum.OWNER) {
            throw new IllegalStateException("群主暂不支持退群，请先转让群主");
        }
        chatGroupMemberRepository.deleteByGroupIdAndUserAccount(group.getId(), currentUser.getAccount());
    }

    @Transactional
    public GroupInfoVO updateAnnouncement(UserEntity currentUser, String groupNo, String announcementRaw) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        ChatGroupEntity group = findGroupByNo(groupNo);
        ChatGroupMemberEntity operator = ensureMember(group.getId(), currentUser.getAccount());
        if (operator.getRole() != GroupMemberRoleEnum.OWNER && operator.getRole() != GroupMemberRoleEnum.ADMIN) {
            throw new IllegalStateException("仅群主或管理员可修改公告");
        }
        String announcement = trim(announcementRaw);
        if (announcement != null && announcement.length() > 1000) {
            throw new IllegalArgumentException("announcement 长度不能超过 1000");
        }
        group.setAnnouncement(announcement);
        ChatGroupEntity saved = chatGroupRepository.save(group);
        return toGroupInfo(saved, operator.getRole(), chatGroupMemberRepository.countByGroupId(group.getId()));
    }

    @Transactional
    public void setAdmin(UserEntity currentUser, String groupNo, String targetAccountRaw, boolean admin) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        ChatGroupEntity group = findGroupByNo(groupNo);
        ChatGroupMemberEntity operator = ensureMember(group.getId(), currentUser.getAccount());
        if (operator.getRole() != GroupMemberRoleEnum.OWNER) {
            throw new IllegalStateException("仅群主可设置管理员");
        }
        String targetAccount = trim(targetAccountRaw);
        if (targetAccount == null) {
            throw new IllegalArgumentException("account 不能为空");
        }
        ChatGroupMemberEntity targetMember = ensureMember(group.getId(), targetAccount);
        if (targetMember.getRole() == GroupMemberRoleEnum.OWNER) {
            throw new IllegalStateException("不能调整群主角色");
        }
        targetMember.setRole(admin ? GroupMemberRoleEnum.ADMIN : GroupMemberRoleEnum.MEMBER);
        chatGroupMemberRepository.save(targetMember);
    }

    @Transactional
    public void kickMember(UserEntity currentUser, String groupNo, String targetAccountRaw) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        String targetAccount = trim(targetAccountRaw);
        if (targetAccount == null) {
            throw new IllegalArgumentException("account 不能为空");
        }
        ChatGroupEntity group = findGroupByNo(groupNo);
        ChatGroupMemberEntity operator = ensureMember(group.getId(), currentUser.getAccount());
        ChatGroupMemberEntity target = ensureMember(group.getId(), targetAccount);

        if (operator.getUserAccount().equals(target.getUserAccount())) {
            throw new IllegalStateException("不允许踢出自己");
        }
        if (target.getRole() == GroupMemberRoleEnum.OWNER) {
            throw new IllegalStateException("不能踢出群主");
        }
        if (operator.getRole() == GroupMemberRoleEnum.MEMBER) {
            throw new IllegalStateException("仅群主或管理员可踢人");
        }
        if (operator.getRole() == GroupMemberRoleEnum.ADMIN && target.getRole() == GroupMemberRoleEnum.ADMIN) {
            throw new IllegalStateException("管理员不能互相踢出");
        }

        chatGroupMemberRepository.deleteByGroupIdAndUserAccount(group.getId(), target.getUserAccount());
    }

    public ChatGroupEntity findGroupByNo(String groupNoRaw) {
        String groupNo = trim(groupNoRaw);
        if (groupNo == null) {
            throw new IllegalArgumentException("groupNo 不能为空");
        }
        return chatGroupRepository.findByGroupNo(groupNo)
                .orElseThrow(() -> new IllegalArgumentException("群组不存在"));
    }

    public ChatGroupMemberEntity ensureMember(Long groupId, String accountRaw) {
        String account = trim(accountRaw);
        ChatGroupMemberEntity member = findMember(groupId, account);
        if (member == null) {
            throw new IllegalStateException("你不在该群");
        }
        return member;
    }

    public ChatGroupMemberEntity findMember(Long groupId, String accountRaw) {
        String account = trim(accountRaw);
        if (groupId == null || account == null) {
            return null;
        }
        return chatGroupMemberRepository.findByGroupIdAndUserAccount(groupId, account).orElse(null);
    }

    private GroupInfoVO toGroupInfo(ChatGroupEntity group, GroupMemberRoleEnum myRole, long memberCount) {
        return GroupInfoVO.builder()
                .groupNo(group.getGroupNo())
                .groupName(group.getGroupName())
                .ownerAccount(group.getOwnerAccount())
                .announcement(group.getAnnouncement())
                .maxMembers(group.getMaxMembers())
                .memberCount(memberCount)
                .myRole(myRole == null ? null : myRole.name())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    private String generateUniqueGroupNo() {
        for (int i = 0; i < 10; i++) {
            String candidate = buildGroupNoCandidate();
            if (!chatGroupRepository.existsByGroupNo(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("生成群号失败，请重试");
    }

    private String buildGroupNoCandidate() {
        long base = System.currentTimeMillis() % 1_000_000_000L;
        int random = ThreadLocalRandom.current().nextInt(100, 999);
        return String.valueOf(base) + random;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }
}
