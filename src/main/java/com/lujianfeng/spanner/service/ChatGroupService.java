package com.lujianfeng.spanner.service;

import com.lujianfeng.spanner.dto.group.GroupCreateRequestDTO;
import com.lujianfeng.spanner.entity.group.ChatGroupAnnouncementEntity;
import com.lujianfeng.spanner.entity.group.ChatGroupEntity;
import com.lujianfeng.spanner.entity.group.ChatGroupMemberEntity;
import com.lujianfeng.spanner.entity.group.ChatGroupProfileEntity;
import com.lujianfeng.spanner.entity.group.ChatGroupReportEntity;
import com.lujianfeng.spanner.entity.group.ChatGroupUserSettingsEntity;
import com.lujianfeng.spanner.entity.group.GroupAnnouncementPermissionEnum;
import com.lujianfeng.spanner.entity.group.GroupInviteModeEnum;
import com.lujianfeng.spanner.entity.group.GroupMemberRoleEnum;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.entity.user.UserRelationEnum;
import com.lujianfeng.spanner.repository.ChatGroupAnnouncementRepository;
import com.lujianfeng.spanner.repository.ChatGroupMemberRepository;
import com.lujianfeng.spanner.repository.ChatGroupProfileRepository;
import com.lujianfeng.spanner.repository.ChatGroupReportRepository;
import com.lujianfeng.spanner.repository.ChatGroupRepository;
import com.lujianfeng.spanner.repository.ChatGroupUserSettingsRepository;
import com.lujianfeng.spanner.repository.GroupMessageRepository;
import com.lujianfeng.spanner.repository.UserRelationRepository;
import com.lujianfeng.spanner.repository.UserRepository;
import com.lujianfeng.spanner.vo.group.GroupAnnouncementVO;
import com.lujianfeng.spanner.vo.group.GroupBatchInviteFailedVO;
import com.lujianfeng.spanner.vo.group.GroupBatchInviteResultVO;
import com.lujianfeng.spanner.vo.group.GroupInfoVO;
import com.lujianfeng.spanner.vo.group.GroupMediaOverviewVO;
import com.lujianfeng.spanner.vo.group.GroupMemberItemVO;
import com.lujianfeng.spanner.vo.group.GroupMemberVO;
import com.lujianfeng.spanner.vo.group.GroupProfileVO;
import com.lujianfeng.spanner.vo.group.GroupQuitResultVO;
import com.lujianfeng.spanner.vo.group.GroupReportResultVO;
import com.lujianfeng.spanner.vo.group.GroupSettingsDetailVO;
import com.lujianfeng.spanner.vo.group.GroupUserSettingsVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ChatGroupService {
    public static final int GROUP_MAX_MEMBERS = 500;
    private static final Logger log = LoggerFactory.getLogger(ChatGroupService.class);

    private final ChatGroupRepository chatGroupRepository;
    private final ChatGroupMemberRepository chatGroupMemberRepository;
    private final ChatGroupProfileRepository chatGroupProfileRepository;
    private final ChatGroupAnnouncementRepository chatGroupAnnouncementRepository;
    private final ChatGroupUserSettingsRepository chatGroupUserSettingsRepository;
    private final ChatGroupReportRepository chatGroupReportRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final UserRepository userRepository;
    private final UserRelationRepository userRelationRepository;

    public ChatGroupService(ChatGroupRepository chatGroupRepository,
                            ChatGroupMemberRepository chatGroupMemberRepository,
                            ChatGroupProfileRepository chatGroupProfileRepository,
                            ChatGroupAnnouncementRepository chatGroupAnnouncementRepository,
                            ChatGroupUserSettingsRepository chatGroupUserSettingsRepository,
                            ChatGroupReportRepository chatGroupReportRepository,
                            GroupMessageRepository groupMessageRepository,
                            UserRepository userRepository,
                            UserRelationRepository userRelationRepository) {
        this.chatGroupRepository = chatGroupRepository;
        this.chatGroupMemberRepository = chatGroupMemberRepository;
        this.chatGroupProfileRepository = chatGroupProfileRepository;
        this.chatGroupAnnouncementRepository = chatGroupAnnouncementRepository;
        this.chatGroupUserSettingsRepository = chatGroupUserSettingsRepository;
        this.chatGroupReportRepository = chatGroupReportRepository;
        this.groupMessageRepository = groupMessageRepository;
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
        String announcement = sanitizeText(requestDTO == null ? null : requestDTO.getAnnouncement());
        if (announcement != null && announcement.length() > 1000) {
            throw new IllegalArgumentException("announcement 长度不能超过 1000");
        }

        ChatGroupEntity group = new ChatGroupEntity();
        group.setGroupNo(generateUniqueGroupNo());
        group.setGroupName(groupName);
        group.setOwnerAccount(currentUser.getAccount());
        group.setAnnouncement(announcement);
        group.setMaxMembers(GROUP_MAX_MEMBERS);
        group.setInviteMode(GroupInviteModeEnum.ADMIN_ONLY.name());
        group.setMemberCanEditGroupName(false);
        group.setJoinVerificationEnabled(true);
        group.setAnnouncementPermission(GroupAnnouncementPermissionEnum.OWNER_ADMIN.name());
        ChatGroupEntity savedGroup = chatGroupRepository.save(group);
        resolveOrCreateGroupProfile(savedGroup);

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

    public GroupSettingsDetailVO getGroupSettingsDetail(UserEntity currentUser, String groupNo) {
        ChatGroupEntity group = findGroupByNo(groupNo);
        ChatGroupMemberEntity member = ensureMember(group.getId(), currentUser == null ? null : currentUser.getAccount());

        ChatGroupProfileEntity profileEntity = resolveOrCreateGroupProfile(group);
        GroupProfileVO profile = toGroupProfile(group, profileEntity, member.getRole(), chatGroupMemberRepository.countByGroupId(group.getId()));
        GroupUserSettingsVO mySettings = toUserSettingsVO(resolveOrCreateUserSettings(group, currentUser.getAccount()));
        GroupAnnouncementVO latestAnnouncement = getLatestAnnouncementInternal(group);
        GroupMediaOverviewVO mediaOverview = getMediaOverview(currentUser, groupNo);

        return GroupSettingsDetailVO.builder()
                .groupProfile(profile)
                .mySettings(mySettings)
                .latestAnnouncement(latestAnnouncement)
                .mediaOverview(mediaOverview)
                .build();
    }

    public GroupProfileVO getGroupProfile(UserEntity currentUser, String groupNo) {
        ChatGroupEntity group = findGroupByNo(groupNo);
        ChatGroupMemberEntity member = ensureMember(group.getId(), currentUser == null ? null : currentUser.getAccount());
        ChatGroupProfileEntity profileEntity = resolveOrCreateGroupProfile(group);
        return toGroupProfile(group, profileEntity, member.getRole(), chatGroupMemberRepository.countByGroupId(group.getId()));
    }

    public List<GroupMemberVO> listGroupMembers(UserEntity currentUser, String groupNo) {
        ChatGroupEntity group = findGroupByNo(groupNo);
        ensureMember(group.getId(), currentUser == null ? null : currentUser.getAccount());
        List<ChatGroupMemberEntity> members = chatGroupMemberRepository.findByGroupIdOrderByJoinedAtAsc(group.getId());
        Map<String, UserEntity> users = loadUsersAsMap(members.stream().map(ChatGroupMemberEntity::getUserAccount).toList());
        return members.stream()
                .map(member -> GroupMemberVO.builder()
                        .account(member.getUserAccount())
                        .role(member.getRole().name())
                        .isVip(isVip(users.get(member.getUserAccount())))
                        .joinedAt(member.getJoinedAt())
                        .build())
                .toList();
    }

    public Map<String, Object> listMyGroups(UserEntity currentUser, Integer page, Integer size, String keywordRaw) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        String keyword = trim(keywordRaw);

        List<ChatGroupMemberEntity> myMemberships = chatGroupMemberRepository.findByUserAccountOrderByJoinedAtDesc(currentUser.getAccount());
        if (myMemberships.isEmpty()) {
            return Map.of(
                    "records", List.of(),
                    "page", safePage,
                    "size", safeSize,
                    "total", 0,
                    "totalPages", 0,
                    "hasMore", false
            );
        }

        Map<Long, ChatGroupMemberEntity> memberByGroupId = new HashMap<>();
        List<Long> groupIds = new ArrayList<>();
        for (ChatGroupMemberEntity item : myMemberships) {
            memberByGroupId.put(item.getGroupId(), item);
            groupIds.add(item.getGroupId());
        }

        List<ChatGroupEntity> groups = chatGroupRepository.findAllById(groupIds);
        Map<Long, ChatGroupEntity> groupById = new HashMap<>();
        for (ChatGroupEntity group : groups) {
            groupById.put(group.getId(), group);
        }

        Map<Long, ChatGroupProfileEntity> profileByGroupId = new HashMap<>();
        chatGroupProfileRepository.findByGroupIdIn(groupIds).forEach(profile -> profileByGroupId.put(profile.getGroupId(), profile));

        List<GroupProfileVO> allRecords = new ArrayList<>();
        for (Long groupId : groupIds) {
            ChatGroupEntity group = groupById.get(groupId);
            ChatGroupMemberEntity member = memberByGroupId.get(groupId);
            if (group == null || member == null) {
                continue;
            }
            if (keyword != null) {
                String lower = keyword.toLowerCase();
                boolean match = containsIgnoreCase(group.getGroupNo(), lower) || containsIgnoreCase(group.getGroupName(), lower);
                if (!match) {
                    continue;
                }
            }
            ChatGroupProfileEntity profile = profileByGroupId.get(groupId);
            allRecords.add(toGroupProfile(group, profile, member.getRole(), chatGroupMemberRepository.countByGroupId(groupId)));
        }

        int total = allRecords.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        int fromIndex = Math.min((safePage - 1) * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);
        List<GroupProfileVO> records = allRecords.subList(fromIndex, toIndex);

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("page", safePage);
        data.put("size", safeSize);
        data.put("total", total);
        data.put("totalPages", totalPages);
        data.put("hasMore", safePage < totalPages);
        return data;
    }

    public Map<String, Object> listGroupMembersPage(UserEntity currentUser,
                                                    String groupNo,
                                                    Integer page,
                                                    Integer size,
                                                    String keywordRaw,
                                                    String roleRaw) {
        ChatGroupEntity group = findGroupByNo(groupNo);
        ensureMember(group.getId(), currentUser == null ? null : currentUser.getAccount());

        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 50 : Math.min(size, 200);
        String keyword = trim(keywordRaw);
        GroupMemberRoleEnum roleFilter = parseRoleNullable(roleRaw);

        List<ChatGroupMemberEntity> members = chatGroupMemberRepository.findByGroupIdOrderByJoinedAtAsc(group.getId());
        Map<String, UserEntity> users = loadUsersAsMap(members.stream().map(ChatGroupMemberEntity::getUserAccount).toList());

        List<GroupMemberItemVO> filtered = members.stream()
                .filter(member -> roleFilter == null || member.getRole() == roleFilter)
                .map(member -> toMemberItem(member, users.get(member.getUserAccount())))
                .filter(item -> matchKeyword(item, keyword))
                .sorted(Comparator.comparing(GroupMemberItemVO::getJoinedAt))
                .toList();

        int total = filtered.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        int fromIndex = Math.min((safePage - 1) * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);
        List<GroupMemberItemVO> records = filtered.subList(fromIndex, toIndex);
        Map<String, Map<String, Object>> groupMemberProfileMap = buildGroupMemberProfileMap(records, users);

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("members", records);
        data.put("groupMemberProfileMap", groupMemberProfileMap);
        data.put("count", total);
        data.put("page", safePage);
        data.put("size", safeSize);
        data.put("total", total);
        data.put("totalPages", totalPages);
        data.put("hasMore", safePage < totalPages);
        return data;
    }

    public List<GroupMemberItemVO> listGroupMembersPreview(UserEntity currentUser, String groupNo, Integer size) {
        ChatGroupEntity group = findGroupByNo(groupNo);
        ensureMember(group.getId(), currentUser == null ? null : currentUser.getAccount());

        int safeSize = size == null || size < 1 ? 9 : Math.min(size, 18);
        List<ChatGroupMemberEntity> members = chatGroupMemberRepository.findByGroupIdOrderByJoinedAtAsc(group.getId());
        Map<String, UserEntity> users = loadUsersAsMap(members.stream().map(ChatGroupMemberEntity::getUserAccount).toList());

        return members.stream()
                .limit(safeSize)
                .map(member -> toMemberItem(member, users.get(member.getUserAccount())))
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
        ChatGroupMemberEntity operator = ensureMember(group.getId(), currentUser.getAccount());
        if (!canInvite(operator.getRole(), parseInviteMode(group.getInviteMode()))) {
            throw new IllegalStateException("当前群配置不允许你邀请成员");
        }

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
            return toGroupInfo(group, operator.getRole(), chatGroupMemberRepository.countByGroupId(group.getId()));
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

        return toGroupInfo(group, operator.getRole(), memberCount + 1);
    }

    @Transactional
    public GroupBatchInviteResultVO batchInvite(UserEntity currentUser, String groupNo, Collection<String> accountsRaw) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        ChatGroupEntity group = findGroupByNo(groupNo);
        ChatGroupMemberEntity operator = ensureMember(group.getId(), currentUser.getAccount());
        if (!canInvite(operator.getRole(), parseInviteMode(group.getInviteMode()))) {
            throw new IllegalStateException("当前群配置不允许你邀请成员");
        }
        if (accountsRaw == null || accountsRaw.isEmpty()) {
            throw new IllegalArgumentException("accounts 不能为空");
        }

        Set<String> accounts = new HashSet<>();
        for (String account : accountsRaw) {
            String clean = trim(account);
            if (clean != null) {
                accounts.add(clean);
            }
        }
        if (accounts.isEmpty()) {
            throw new IllegalArgumentException("accounts 不能为空");
        }

        List<String> successAccounts = new ArrayList<>();
        List<GroupBatchInviteFailedVO> failed = new ArrayList<>();
        long memberCount = chatGroupMemberRepository.countByGroupId(group.getId());

        for (String targetAccount : accounts) {
            if (Objects.equals(targetAccount, currentUser.getAccount())) {
                failed.add(GroupBatchInviteFailedVO.builder()
                        .account(targetAccount)
                        .reasonCode("INVALID_TARGET")
                        .reason("不能邀请自己")
                        .build());
                continue;
            }
            UserEntity targetUser = userRepository.findByAccount(targetAccount);
            if (targetUser == null) {
                failed.add(GroupBatchInviteFailedVO.builder()
                        .account(targetAccount)
                        .reasonCode("USER_NOT_FOUND")
                        .reason("用户不存在")
                        .build());
                continue;
            }
            if (chatGroupMemberRepository.existsByGroupIdAndUserAccount(group.getId(), targetAccount)) {
                failed.add(GroupBatchInviteFailedVO.builder()
                        .account(targetAccount)
                        .reasonCode("ALREADY_IN_GROUP")
                        .reason("用户已在群内")
                        .build());
                continue;
            }
            boolean isFriend = userRelationRepository.existsByUserAndFriendAndRelationType(currentUser, targetUser, UserRelationEnum.ACCEPTED)
                    || userRelationRepository.existsByUserAndFriendAndRelationType(targetUser, currentUser, UserRelationEnum.ACCEPTED);
            if (!isFriend) {
                failed.add(GroupBatchInviteFailedVO.builder()
                        .account(targetAccount)
                        .reasonCode("NOT_FRIEND")
                        .reason("仅可邀请好友")
                        .build());
                continue;
            }
            if (memberCount >= group.getMaxMembers()) {
                failed.add(GroupBatchInviteFailedVO.builder()
                        .account(targetAccount)
                        .reasonCode("GROUP_FULL")
                        .reason("群人数已满")
                        .build());
                continue;
            }

            ChatGroupMemberEntity member = new ChatGroupMemberEntity();
            member.setGroupId(group.getId());
            member.setUserAccount(targetAccount);
            member.setRole(GroupMemberRoleEnum.MEMBER);
            chatGroupMemberRepository.save(member);
            memberCount++;
            successAccounts.add(targetAccount);
        }

        return GroupBatchInviteResultVO.builder()
                .successAccounts(successAccounts)
                .failed(failed)
                .build();
    }

    @Transactional
    public GroupQuitResultVO quitGroupWithResult(UserEntity currentUser, String groupNo) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        ChatGroupEntity group = findGroupByNo(groupNo);
        ChatGroupMemberEntity member = findMember(group.getId(), currentUser.getAccount());
        if (member == null) {
            return GroupQuitResultVO.builder()
                    .groupNo(groupNo)
                    .quit(true)
                    .shouldRemoveLocalSession(true)
                    .build();
        }
        if (member.getRole() == GroupMemberRoleEnum.OWNER) {
            throw new IllegalStateException("群主暂不支持退群，请先转让群主");
        }

        chatGroupMemberRepository.deleteByGroupIdAndUserAccount(group.getId(), currentUser.getAccount());
        log.info("audit=group_quit operatorAccount={} groupNo={} targetAccount={} timestamp={}",
                currentUser.getAccount(), groupNo, currentUser.getAccount(), LocalDateTime.now());

        return GroupQuitResultVO.builder()
                .groupNo(groupNo)
                .quit(true)
                .shouldRemoveLocalSession(true)
                .build();
    }

    @Transactional
    public void quitGroup(UserEntity currentUser, String groupNo) {
        quitGroupWithResult(currentUser, groupNo);
    }

    @Transactional
    public GroupInfoVO updateAnnouncement(UserEntity currentUser, String groupNo, String announcementRaw) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        ChatGroupEntity group = findGroupByNo(groupNo);
        ensureMember(group.getId(), currentUser.getAccount());
        ChatGroupAnnouncementEntity latest = chatGroupAnnouncementRepository.findTopByGroupIdOrderByUpdatedAtDesc(group.getId()).orElse(null);
        if (latest == null) {
            createAnnouncement(currentUser, groupNo, announcementRaw);
        } else {
            updateAnnouncementById(currentUser, groupNo, latest.getAnnouncementId(), announcementRaw);
        }
        ChatGroupMemberEntity member = ensureMember(group.getId(), currentUser.getAccount());
        return toGroupInfo(group, member.getRole(), chatGroupMemberRepository.countByGroupId(group.getId()));
    }

    @Transactional
    public GroupAnnouncementVO createAnnouncement(UserEntity currentUser, String groupNo, String contentRaw) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        ChatGroupEntity group = findGroupByNo(groupNo);
        ChatGroupMemberEntity operator = ensureMember(group.getId(), currentUser.getAccount());
        ensureCanPublishAnnouncement(group, operator.getRole());

        String content = sanitizeText(contentRaw);
        if (content == null) {
            throw new IllegalArgumentException("content 不能为空");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("content 长度不能超过 1000");
        }

        ChatGroupAnnouncementEntity entity = new ChatGroupAnnouncementEntity();
        entity.setAnnouncementId(generateAnnouncementId());
        entity.setGroupId(group.getId());
        entity.setGroupNo(groupNo);
        entity.setContent(content);
        entity.setPublisherAccount(currentUser.getAccount());
        ChatGroupAnnouncementEntity saved = chatGroupAnnouncementRepository.save(entity);

        group.setAnnouncement(content);
        chatGroupRepository.save(group);

        log.info("audit=group_announcement_create operatorAccount={} groupNo={} timestamp={}",
                currentUser.getAccount(), groupNo, LocalDateTime.now());

        return toAnnouncementVO(saved);
    }

    @Transactional
    public GroupAnnouncementVO updateAnnouncementById(UserEntity currentUser,
                                                      String groupNo,
                                                      String announcementIdRaw,
                                                      String contentRaw) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        String announcementId = trim(announcementIdRaw);
        if (announcementId == null) {
            throw new IllegalArgumentException("announcementId 不能为空");
        }
        String content = sanitizeText(contentRaw);
        if (content == null) {
            throw new IllegalArgumentException("content 不能为空");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("content 长度不能超过 1000");
        }

        ChatGroupEntity group = findGroupByNo(groupNo);
        ChatGroupMemberEntity operator = ensureMember(group.getId(), currentUser.getAccount());
        ensureCanPublishAnnouncement(group, operator.getRole());

        ChatGroupAnnouncementEntity entity = chatGroupAnnouncementRepository.findByAnnouncementIdAndGroupId(announcementId, group.getId())
                .orElseThrow(() -> new IllegalArgumentException("公告不存在"));
        entity.setContent(content);
        ChatGroupAnnouncementEntity saved = chatGroupAnnouncementRepository.save(entity);

        group.setAnnouncement(content);
        chatGroupRepository.save(group);

        log.info("audit=group_announcement_update operatorAccount={} groupNo={} announcementId={} timestamp={}",
                currentUser.getAccount(), groupNo, announcementId, LocalDateTime.now());

        return toAnnouncementVO(saved);
    }

    public GroupAnnouncementVO getLatestAnnouncement(UserEntity currentUser, String groupNo) {
        ChatGroupEntity group = findGroupByNo(groupNo);
        ensureMember(group.getId(), currentUser == null ? null : currentUser.getAccount());
        return getLatestAnnouncementInternal(group);
    }

    public Map<String, Object> listAnnouncements(UserEntity currentUser, String groupNo, Integer page, Integer size) {
        ChatGroupEntity group = findGroupByNo(groupNo);
        ensureMember(group.getId(), currentUser == null ? null : currentUser.getAccount());

        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);
        Page<ChatGroupAnnouncementEntity> result = chatGroupAnnouncementRepository.findByGroupIdOrderByUpdatedAtDesc(group.getId(), pageable);

        List<GroupAnnouncementVO> records = result.getContent().stream().map(this::toAnnouncementVO).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("page", safePage);
        data.put("size", safeSize);
        data.put("total", result.getTotalElements());
        data.put("totalPages", result.getTotalPages());
        data.put("hasMore", result.hasNext());
        return data;
    }

    @Transactional
    public GroupUserSettingsVO updateMySettings(UserEntity currentUser,
                                                String groupNo,
                                                Boolean messageMute,
                                                Boolean chatPinned,
                                                Boolean saveToContacts) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        ChatGroupEntity group = findGroupByNo(groupNo);
        ensureMember(group.getId(), currentUser.getAccount());

        ChatGroupUserSettingsEntity settings = resolveOrCreateUserSettings(group, currentUser.getAccount());
        if (messageMute != null) {
            settings.setMessageMute(messageMute);
        }
        if (chatPinned != null) {
            settings.setChatPinned(chatPinned);
        }
        if (saveToContacts != null) {
            settings.setSaveToContacts(saveToContacts);
        }
        ChatGroupUserSettingsEntity saved = chatGroupUserSettingsRepository.save(settings);
        return toUserSettingsVO(saved);
    }

    @Transactional
    public GroupProfileVO updateGroupPermissions(UserEntity currentUser,
                                                 String groupNo,
                                                 String inviteModeRaw,
                                                 Boolean memberCanEditGroupName,
                                                 Boolean joinVerificationEnabled,
                                                 String announcementPermissionRaw) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        ChatGroupEntity group = findGroupByNo(groupNo);
        ChatGroupMemberEntity operator = ensureMember(group.getId(), currentUser.getAccount());
        if (operator.getRole() == GroupMemberRoleEnum.MEMBER) {
            throw new IllegalStateException("仅群主或管理员可修改群权限配置");
        }

        if (trim(inviteModeRaw) != null) {
            group.setInviteMode(parseInviteMode(inviteModeRaw).name());
        }
        if (memberCanEditGroupName != null) {
            group.setMemberCanEditGroupName(memberCanEditGroupName);
        }
        if (joinVerificationEnabled != null) {
            group.setJoinVerificationEnabled(joinVerificationEnabled);
        }
        if (trim(announcementPermissionRaw) != null) {
            group.setAnnouncementPermission(parseAnnouncementPermission(announcementPermissionRaw).name());
        }
        ChatGroupEntity saved = chatGroupRepository.save(group);

        log.info("audit=group_permissions_update operatorAccount={} groupNo={} beforeAfterChanged=true timestamp={}",
                currentUser.getAccount(), groupNo, LocalDateTime.now());

        ChatGroupProfileEntity profileEntity = resolveOrCreateGroupProfile(saved);
        return toGroupProfile(saved, profileEntity, operator.getRole(), chatGroupMemberRepository.countByGroupId(group.getId()));
    }

    @Transactional
    public GroupProfileVO updateGroupProfile(UserEntity currentUser,
                                             String groupNo,
                                             String groupNameRaw,
                                             String groupAvatarUrlRaw,
                                             String summaryRaw) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        ChatGroupEntity group = findGroupByNo(groupNo);
        ChatGroupMemberEntity operator = ensureMember(group.getId(), currentUser.getAccount());
        ChatGroupProfileEntity profileEntity = resolveOrCreateGroupProfile(group);

        boolean updateName = groupNameRaw != null;
        boolean updateAvatar = groupAvatarUrlRaw != null;
        boolean updateSummary = summaryRaw != null;

        if (!updateName && !updateAvatar && !updateSummary) {
            throw new IllegalArgumentException("至少提供一个可更新字段");
        }

        if (updateName) {
            String groupName = trim(groupNameRaw);
            if (groupName == null) {
                throw new IllegalArgumentException("groupName 不能为空");
            }
            if (groupName.length() > 64) {
                throw new IllegalArgumentException("groupName 长度不能超过 64");
            }
            if (!canEditGroupName(operator.getRole(), group)) {
                throw new IllegalStateException("无权限修改群名称");
            }
            group.setGroupName(groupName);
        }
        if (updateAvatar || updateSummary) {
            if (operator.getRole() == GroupMemberRoleEnum.MEMBER) {
                throw new IllegalStateException("仅群主或管理员可修改群头像/简介");
            }
            if (updateAvatar) {
                String groupAvatarUrl = trim(groupAvatarUrlRaw);
                if (groupAvatarUrl != null && groupAvatarUrl.length() > 500) {
                    throw new IllegalArgumentException("groupAvatarUrl 长度不能超过 500");
                }
                profileEntity.setGroupAvatarUrl(groupAvatarUrl);
            }
            if (updateSummary) {
                String summary = sanitizeText(summaryRaw);
                if (summary != null && summary.length() > 500) {
                    throw new IllegalArgumentException("summary 长度不能超过 500");
                }
                profileEntity.setSummary(summary);
            }
        }

        ChatGroupEntity saved = chatGroupRepository.save(group);
        ChatGroupProfileEntity savedProfile = chatGroupProfileRepository.save(profileEntity);
        log.info("audit=group_profile_update operatorAccount={} groupNo={} timestamp={}",
                currentUser.getAccount(), groupNo, LocalDateTime.now());
        return toGroupProfile(saved, savedProfile, operator.getRole(), chatGroupMemberRepository.countByGroupId(group.getId()));
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
        GroupMemberRoleEnum before = targetMember.getRole();
        targetMember.setRole(admin ? GroupMemberRoleEnum.ADMIN : GroupMemberRoleEnum.MEMBER);
        chatGroupMemberRepository.save(targetMember);

        log.info("audit=group_role_update operatorAccount={} groupNo={} targetAccount={} before={} after={} timestamp={}",
                currentUser.getAccount(), groupNo, targetAccount, before, targetMember.getRole(), LocalDateTime.now());
    }

    @Transactional
    public void updateMemberRole(UserEntity currentUser, String groupNo, String targetAccountRaw, String roleRaw) {
        GroupMemberRoleEnum targetRole = parseRole(roleRaw);
        if (targetRole == GroupMemberRoleEnum.OWNER) {
            throw new IllegalArgumentException("不支持直接设置 OWNER");
        }
        setAdmin(currentUser, groupNo, targetAccountRaw, targetRole == GroupMemberRoleEnum.ADMIN);
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
        log.info("audit=group_member_remove operatorAccount={} groupNo={} targetAccount={} timestamp={}",
                currentUser.getAccount(), groupNo, targetAccount, LocalDateTime.now());
    }

    @Transactional
    public void dissolveGroup(UserEntity currentUser, String groupNo) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        ChatGroupEntity group = findGroupByNo(groupNo);
        ChatGroupMemberEntity operator = ensureMember(group.getId(), currentUser.getAccount());
        if (operator.getRole() != GroupMemberRoleEnum.OWNER) {
            throw new IllegalStateException("仅群主可解散群聊");
        }

        chatGroupAnnouncementRepository.deleteByGroupId(group.getId());
        chatGroupProfileRepository.deleteByGroupId(group.getId());
        chatGroupUserSettingsRepository.deleteByGroupId(group.getId());
        chatGroupMemberRepository.deleteByGroupId(group.getId());
        groupMessageRepository.deleteByGroupNo(group.getGroupNo());
        chatGroupRepository.delete(group);

        log.info("audit=group_dissolve operatorAccount={} groupNo={} timestamp={}",
                currentUser.getAccount(), groupNo, LocalDateTime.now());
    }

    @Transactional
    public GroupReportResultVO reportGroup(UserEntity currentUser,
                                           String groupNo,
                                           String reasonTypeRaw,
                                           String descriptionRaw,
                                           Collection<String> evidenceUrlsRaw) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        ChatGroupEntity group = findGroupByNo(groupNo);
        ensureMember(group.getId(), currentUser.getAccount());

        String reasonType = trim(reasonTypeRaw);
        if (reasonType == null) {
            throw new IllegalArgumentException("reasonType 不能为空");
        }
        if (reasonType.length() > 64) {
            throw new IllegalArgumentException("reasonType 长度不能超过 64");
        }

        String description = sanitizeText(descriptionRaw);
        if (description != null && description.length() > 1000) {
            throw new IllegalArgumentException("description 长度不能超过 1000");
        }

        List<String> evidenceUrls = new ArrayList<>();
        if (evidenceUrlsRaw != null) {
            for (String evidenceUrl : evidenceUrlsRaw) {
                String clean = trim(evidenceUrl);
                if (clean != null) {
                    if (clean.length() > 500) {
                        throw new IllegalArgumentException("evidenceUrl 长度不能超过 500");
                    }
                    evidenceUrls.add(clean);
                }
            }
        }

        ChatGroupReportEntity report = new ChatGroupReportEntity();
        report.setReportNo(generateReportNo());
        report.setGroupId(group.getId());
        report.setGroupNo(groupNo);
        report.setReporterAccount(currentUser.getAccount());
        report.setReasonType(reasonType);
        report.setDescription(description);
        report.setEvidenceUrls(String.join(",", evidenceUrls));
        chatGroupReportRepository.save(report);

        return GroupReportResultVO.builder().reportNo(report.getReportNo()).build();
    }

    public GroupMediaOverviewVO getMediaOverview(UserEntity currentUser, String groupNo) {
        ChatGroupEntity group = findGroupByNo(groupNo);
        ensureMember(group.getId(), currentUser == null ? null : currentUser.getAccount());

        long fileCount = groupMessageRepository.countFileMessagesByGroupNo(groupNo);
        long imageVideoCount = groupMessageRepository.countImageOrVideoMessagesByGroupNo(groupNo);
        long linkCount = groupMessageRepository.countLinkMessagesByGroupNo(groupNo);

        return GroupMediaOverviewVO.builder()
                .groupNo(groupNo)
                .fileCount(fileCount)
                .imageVideoCount(imageVideoCount)
                .linkCount(linkCount)
                .build();
    }

    @Transactional
    public Map<String, Object> clearGroupMessages(UserEntity currentUser, String groupNo, String scopeRaw) {
        if (currentUser == null || isBlank(currentUser.getAccount())) {
            throw new IllegalArgumentException("未登录");
        }
        ChatGroupEntity group = findGroupByNo(groupNo);
        ensureMember(group.getId(), currentUser.getAccount());

        ChatGroupUserSettingsEntity settings = resolveOrCreateUserSettings(group, currentUser.getAccount());
        settings.setLastClearedAt(LocalDateTime.now());
        chatGroupUserSettingsRepository.save(settings);

        String scope = trim(scopeRaw);
        if (scope == null) {
            scope = "SELF";
        }

        return Map.of(
                "groupNo", groupNo,
                "cleared", true,
                "scope", scope,
                "clearedAt", settings.getLastClearedAt()
        );
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

    private GroupProfileVO toGroupProfile(ChatGroupEntity group,
                                          ChatGroupProfileEntity groupProfile,
                                          GroupMemberRoleEnum myRole,
                                          long memberCount) {
        return GroupProfileVO.builder()
                .groupNo(group.getGroupNo())
                .groupName(group.getGroupName())
                .groupAvatarUrl(groupProfile == null ? null : groupProfile.getGroupAvatarUrl())
                .summary(groupProfile == null ? null : groupProfile.getSummary())
                .ownerAccount(group.getOwnerAccount())
                .myRole(myRole == null ? null : myRole.name())
                .memberCount(memberCount)
                .maxMembers(group.getMaxMembers())
                .inviteMode(defaultString(group.getInviteMode(), GroupInviteModeEnum.ADMIN_ONLY.name()))
                .memberCanEditGroupName(Boolean.TRUE.equals(group.getMemberCanEditGroupName()))
                .joinVerificationEnabled(group.getJoinVerificationEnabled() == null || group.getJoinVerificationEnabled())
                .announcementPermission(defaultString(group.getAnnouncementPermission(), GroupAnnouncementPermissionEnum.OWNER_ADMIN.name()))
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    private ChatGroupProfileEntity resolveOrCreateGroupProfile(ChatGroupEntity group) {
        return chatGroupProfileRepository.findByGroupId(group.getId())
                .orElseGet(() -> {
                    ChatGroupProfileEntity entity = new ChatGroupProfileEntity();
                    entity.setGroupId(group.getId());
                    entity.setGroupNo(group.getGroupNo());
                    entity.setGroupAvatarUrl(group.getGroupAvatarUrl());
                    entity.setSummary(group.getSummary());
                    return chatGroupProfileRepository.save(entity);
                });
    }

    private GroupMemberItemVO toMemberItem(ChatGroupMemberEntity member, UserEntity user) {
        return GroupMemberItemVO.builder()
                .account(member.getUserAccount())
                .name(user == null ? null : user.getRealName())
                .avatarUrl(user == null ? null : user.getAvatarUrl())
                .role(member.getRole().name())
                .status("OFFLINE")
                .joinedAt(member.getJoinedAt())
                .isVip(isVip(user))
                .muted(false)
                .blacklisted(false)
                .build();
    }

    private boolean matchKeyword(GroupMemberItemVO item, String keyword) {
        if (keyword == null) {
            return true;
        }
        String lower = keyword.toLowerCase();
        return containsIgnoreCase(item.getAccount(), lower) || containsIgnoreCase(item.getName(), lower);
    }

    private boolean containsIgnoreCase(String value, String keywordLower) {
        return value != null && value.toLowerCase().contains(keywordLower);
    }

    private Map<String, UserEntity> loadUsersAsMap(List<String> accounts) {
        Map<String, UserEntity> map = new HashMap<>();
        if (accounts == null || accounts.isEmpty()) {
            return map;
        }
        userRepository.findByAccountIn(accounts).forEach(user -> map.put(user.getAccount(), user));
        return map;
    }

    private Map<String, Map<String, Object>> buildGroupMemberProfileMap(List<GroupMemberItemVO> records,
                                                                         Map<String, UserEntity> users) {
        Map<String, Map<String, Object>> profileMap = new HashMap<>();
        if (records == null || records.isEmpty()) {
            return profileMap;
        }
        for (GroupMemberItemVO record : records) {
            if (record == null || isBlank(record.getAccount())) {
                continue;
            }
            UserEntity user = users == null ? null : users.get(record.getAccount());
            Map<String, Object> profile = new HashMap<>();
            profile.put("account", record.getAccount());
            profile.put("name", record.getName());
            profile.put("avatarUrl", record.getAvatarUrl());
            profile.put("role", record.getRole());
            profile.put("status", record.getStatus());
            profile.put("joinedAt", record.getJoinedAt());
            profile.put("isVip", isVip(user));
            profileMap.put(record.getAccount(), profile);
        }
        return profileMap;
    }

    private boolean isVip(UserEntity user) {
        return user != null
                && user.getVipExpireAt() != null
                && user.getVipExpireAt().isAfter(LocalDateTime.now());
    }

    private ChatGroupUserSettingsEntity resolveOrCreateUserSettings(ChatGroupEntity group, String userAccount) {
        return chatGroupUserSettingsRepository.findByGroupIdAndUserAccount(group.getId(), userAccount)
                .orElseGet(() -> {
                    ChatGroupUserSettingsEntity entity = new ChatGroupUserSettingsEntity();
                    entity.setGroupId(group.getId());
                    entity.setGroupNo(group.getGroupNo());
                    entity.setUserAccount(userAccount);
                    entity.setMessageMute(false);
                    entity.setChatPinned(false);
                    entity.setSaveToContacts(false);
                    return chatGroupUserSettingsRepository.save(entity);
                });
    }

    private GroupUserSettingsVO toUserSettingsVO(ChatGroupUserSettingsEntity settings) {
        return GroupUserSettingsVO.builder()
                .groupNo(settings.getGroupNo())
                .messageMute(Boolean.TRUE.equals(settings.getMessageMute()))
                .chatPinned(Boolean.TRUE.equals(settings.getChatPinned()))
                .saveToContacts(Boolean.TRUE.equals(settings.getSaveToContacts()))
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    private GroupAnnouncementVO getLatestAnnouncementInternal(ChatGroupEntity group) {
        return chatGroupAnnouncementRepository.findTopByGroupIdOrderByUpdatedAtDesc(group.getId())
                .map(this::toAnnouncementVO)
                .orElse(null);
    }

    private GroupAnnouncementVO toAnnouncementVO(ChatGroupAnnouncementEntity entity) {
        UserEntity publisher = userRepository.findByAccount(entity.getPublisherAccount());
        return GroupAnnouncementVO.builder()
                .announcementId(entity.getAnnouncementId())
                .groupNo(entity.getGroupNo())
                .content(entity.getContent())
                .publisherAccount(entity.getPublisherAccount())
                .publisherName(publisher == null ? null : publisher.getRealName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private void ensureCanPublishAnnouncement(ChatGroupEntity group, GroupMemberRoleEnum role) {
        GroupAnnouncementPermissionEnum permission = parseAnnouncementPermission(group.getAnnouncementPermission());
        if (permission == GroupAnnouncementPermissionEnum.OWNER_ONLY && role != GroupMemberRoleEnum.OWNER) {
            throw new IllegalStateException("仅群主可发布公告");
        }
        if (permission == GroupAnnouncementPermissionEnum.OWNER_ADMIN
                && role != GroupMemberRoleEnum.OWNER
                && role != GroupMemberRoleEnum.ADMIN) {
            throw new IllegalStateException("仅群主或管理员可发布公告");
        }
    }

    private boolean canInvite(GroupMemberRoleEnum role, GroupInviteModeEnum inviteMode) {
        if (inviteMode == GroupInviteModeEnum.ALL) {
            return true;
        }
        return role == GroupMemberRoleEnum.OWNER || role == GroupMemberRoleEnum.ADMIN;
    }

    private boolean canEditGroupName(GroupMemberRoleEnum role, ChatGroupEntity group) {
        if (role == GroupMemberRoleEnum.OWNER || role == GroupMemberRoleEnum.ADMIN) {
            return true;
        }
        return role == GroupMemberRoleEnum.MEMBER && Boolean.TRUE.equals(group.getMemberCanEditGroupName());
    }

    private GroupMemberRoleEnum parseRole(String roleRaw) {
        String role = trim(roleRaw);
        if (role == null) {
            throw new IllegalArgumentException("role 不能为空");
        }
        try {
            return GroupMemberRoleEnum.valueOf(role);
        } catch (Exception e) {
            throw new IllegalArgumentException("role 非法，仅支持 OWNER/ADMIN/MEMBER");
        }
    }

    private GroupMemberRoleEnum parseRoleNullable(String roleRaw) {
        String role = trim(roleRaw);
        if (role == null) {
            return null;
        }
        return parseRole(role);
    }

    private GroupInviteModeEnum parseInviteMode(String modeRaw) {
        String mode = trim(modeRaw);
        if (mode == null) {
            return GroupInviteModeEnum.ADMIN_ONLY;
        }
        try {
            return GroupInviteModeEnum.valueOf(mode);
        } catch (Exception e) {
            throw new IllegalArgumentException("inviteMode 非法，仅支持 ALL/ADMIN_ONLY");
        }
    }

    private GroupAnnouncementPermissionEnum parseAnnouncementPermission(String permissionRaw) {
        String permission = trim(permissionRaw);
        if (permission == null) {
            return GroupAnnouncementPermissionEnum.OWNER_ADMIN;
        }
        try {
            return GroupAnnouncementPermissionEnum.valueOf(permission);
        } catch (Exception e) {
            throw new IllegalArgumentException("announcementPermission 非法，仅支持 OWNER_ONLY/OWNER_ADMIN");
        }
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

    private String generateAnnouncementId() {
        return "ga_" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private String generateReportNo() {
        return "gr_" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private String sanitizeText(String value) {
        String clean = trim(value);
        if (clean == null) {
            return null;
        }
        return clean.replaceAll("<[^>]*>", "");
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

    private String defaultString(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }
}
