package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.dto.group.GroupAdminUpdateRequestDTO;
import com.lujianfeng.spanner.dto.group.GroupAnnouncementCreateRequestDTO;
import com.lujianfeng.spanner.dto.group.GroupAnnouncementUpdateRequestDTO;
import com.lujianfeng.spanner.dto.group.GroupBatchInviteRequestDTO;
import com.lujianfeng.spanner.dto.group.GroupCreateRequestDTO;
import com.lujianfeng.spanner.dto.group.GroupInviteRequestDTO;
import com.lujianfeng.spanner.dto.group.GroupMemberRoleUpdateRequestDTO;
import com.lujianfeng.spanner.dto.group.GroupMessageClearRequestDTO;
import com.lujianfeng.spanner.dto.group.GroupProfileUpdateRequestDTO;
import com.lujianfeng.spanner.dto.group.GroupReportCreateRequestDTO;
import com.lujianfeng.spanner.dto.group.GroupSettingsMyUpdateRequestDTO;
import com.lujianfeng.spanner.dto.group.GroupSettingsPermissionUpdateRequestDTO;
import com.lujianfeng.spanner.entity.group.ChatGroupEntity;
import com.lujianfeng.spanner.entity.message.GroupMessageEntity;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.repository.GroupMessageRepository;
import com.lujianfeng.spanner.service.ChatGroupService;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.vo.group.GroupInfoVO;
import com.lujianfeng.spanner.vo.message.GroupMessageVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/groups")
public class GroupController {

    private final UserService userService;
    private final ChatGroupService chatGroupService;
    private final GroupMessageRepository groupMessageRepository;

    public GroupController(UserService userService,
                           ChatGroupService chatGroupService,
                           GroupMessageRepository groupMessageRepository) {
        this.userService = userService;
        this.chatGroupService = chatGroupService;
        this.groupMessageRepository = groupMessageRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createGroup(@RequestBody GroupCreateRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            GroupInfoVO groupInfo = chatGroupService.createGroup(currentUser, requestDTO);
            return ResponseEntity.ok(success("创建群组成功", groupInfo));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @GetMapping("/{groupNo}")
    public ResponseEntity<Map<String, Object>> getGroup(@PathVariable String groupNo) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            GroupInfoVO groupInfo = chatGroupService.getGroupInfo(currentUser, groupNo);
            return ResponseEntity.ok(success("查询群信息成功", groupInfo));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> listMyGroups(@RequestParam(required = false) Integer page,
                                                            @RequestParam(required = false) Integer size,
                                                            @RequestParam(required = false) String keyword) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("查询我加入的群成功", chatGroupService.listMyGroups(currentUser, page, size, keyword)));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @GetMapping("/{groupNo}/profile")
    public ResponseEntity<Map<String, Object>> getGroupProfile(@PathVariable String groupNo) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("查询群资料成功", chatGroupService.getGroupProfile(currentUser, groupNo)));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @GetMapping("/{groupNo}/settings/detail")
    public ResponseEntity<Map<String, Object>> getSettingsDetail(@PathVariable String groupNo) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("查询群设置详情成功", chatGroupService.getGroupSettingsDetail(currentUser, groupNo)));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @GetMapping("/{groupNo}/members")
    public ResponseEntity<Map<String, Object>> listMembers(@PathVariable String groupNo,
                                                           @RequestParam(required = false) Integer page,
                                                           @RequestParam(required = false) Integer size,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) String role) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            Map<String, Object> data = chatGroupService.listGroupMembersPage(currentUser, groupNo, page, size, keyword, role);
            return ResponseEntity.ok(success("查询群成员成功", data));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @GetMapping("/{groupNo}/members/preview")
    public ResponseEntity<Map<String, Object>> listMembersPreview(@PathVariable String groupNo,
                                                                   @RequestParam(required = false) Integer size) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("查询群成员预览成功", chatGroupService.listGroupMembersPreview(currentUser, groupNo, size)));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @PostMapping("/{groupNo}/join")
    public ResponseEntity<Map<String, Object>> joinGroup(@PathVariable String groupNo) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            GroupInfoVO groupInfo = chatGroupService.joinGroup(currentUser, groupNo);
            return ResponseEntity.ok(success("加入群组成功", groupInfo));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @PostMapping("/{groupNo}/invite")
    public ResponseEntity<Map<String, Object>> inviteFriend(@PathVariable String groupNo,
                                                            @RequestBody GroupInviteRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            GroupInfoVO groupInfo =
                    chatGroupService.inviteFriend(currentUser, groupNo, requestDTO == null ? null : requestDTO.getFriendAccount());
            return ResponseEntity.ok(success("邀请好友入群成功", groupInfo));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @PostMapping("/{groupNo}/members/batch-invite")
    public ResponseEntity<Map<String, Object>> batchInvite(@PathVariable String groupNo,
                                                            @RequestBody GroupBatchInviteRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("批量邀请完成",
                    chatGroupService.batchInvite(currentUser, groupNo, requestDTO == null ? null : requestDTO.getAccounts())));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @PostMapping("/{groupNo}/quit")
    public ResponseEntity<Map<String, Object>> quitGroup(@PathVariable String groupNo) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("退群成功", chatGroupService.quitGroupWithResult(currentUser, groupNo)));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @DeleteMapping("/{groupNo}")
    public ResponseEntity<Map<String, Object>> dissolveGroup(@PathVariable String groupNo) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            chatGroupService.dissolveGroup(currentUser, groupNo);
            return ResponseEntity.ok(success("解散群聊成功", Map.of("groupNo", groupNo, "dissolved", true)));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @PutMapping("/{groupNo}/settings/my")
    public ResponseEntity<Map<String, Object>> updateMySettings(@PathVariable String groupNo,
                                                                 @RequestBody GroupSettingsMyUpdateRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("更新我的群设置成功", chatGroupService.updateMySettings(currentUser,
                    groupNo,
                    requestDTO == null ? null : requestDTO.getMessageMute(),
                    requestDTO == null ? null : requestDTO.getChatPinned(),
                    requestDTO == null ? null : requestDTO.getSaveToContacts())));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @PutMapping("/{groupNo}/settings/permissions")
    public ResponseEntity<Map<String, Object>> updatePermissions(@PathVariable String groupNo,
                                                                  @RequestBody GroupSettingsPermissionUpdateRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("更新群权限配置成功", chatGroupService.updateGroupPermissions(currentUser,
                    groupNo,
                    requestDTO == null ? null : requestDTO.getInviteMode(),
                    requestDTO == null ? null : requestDTO.getMemberCanEditGroupName(),
                    requestDTO == null ? null : requestDTO.getJoinVerificationEnabled(),
                    requestDTO == null ? null : requestDTO.getAnnouncementPermission())));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @PutMapping("/{groupNo}/profile")
    public ResponseEntity<Map<String, Object>> updateGroupProfile(@PathVariable String groupNo,
                                                                   @RequestBody GroupProfileUpdateRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("更新群资料成功", chatGroupService.updateGroupProfile(
                    currentUser,
                    groupNo,
                    requestDTO == null ? null : requestDTO.getGroupName(),
                    requestDTO == null ? null : requestDTO.getGroupAvatarUrl(),
                    requestDTO == null ? null : requestDTO.getSummary()
            )));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @GetMapping("/{groupNo}/announcements/latest")
    public ResponseEntity<Map<String, Object>> getLatestAnnouncement(@PathVariable String groupNo) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("查询最新公告成功", chatGroupService.getLatestAnnouncement(currentUser, groupNo)));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @GetMapping("/{groupNo}/announcements")
    public ResponseEntity<Map<String, Object>> listAnnouncements(@PathVariable String groupNo,
                                                                  @RequestParam(required = false) Integer page,
                                                                  @RequestParam(required = false) Integer size) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("查询群公告历史成功", chatGroupService.listAnnouncements(currentUser, groupNo, page, size)));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @PostMapping("/{groupNo}/announcements")
    public ResponseEntity<Map<String, Object>> createAnnouncement(@PathVariable String groupNo,
                                                                   @RequestBody GroupAnnouncementCreateRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("发布群公告成功",
                    chatGroupService.createAnnouncement(currentUser, groupNo, requestDTO == null ? null : requestDTO.getContent())));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @PutMapping("/{groupNo}/announcements/{announcementId}")
    public ResponseEntity<Map<String, Object>> updateAnnouncement(@PathVariable String groupNo,
                                                                   @PathVariable String announcementId,
                                                                   @RequestBody GroupAnnouncementCreateRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("更新群公告成功",
                    chatGroupService.updateAnnouncementById(currentUser,
                            groupNo,
                            announcementId,
                            requestDTO == null ? null : requestDTO.getContent())));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @PutMapping("/{groupNo}/announcement")
    public ResponseEntity<Map<String, Object>> updateAnnouncement(@PathVariable String groupNo,
                                                                   @RequestBody GroupAnnouncementUpdateRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            GroupInfoVO groupInfo = chatGroupService.updateAnnouncement(currentUser, groupNo,
                    requestDTO == null ? null : requestDTO.getAnnouncement());
            return ResponseEntity.ok(success("更新群公告成功", groupInfo));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @PutMapping("/{groupNo}/admins")
    public ResponseEntity<Map<String, Object>> setAdmin(@PathVariable String groupNo,
                                                         @RequestBody GroupAdminUpdateRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            chatGroupService.setAdmin(currentUser, groupNo, requestDTO == null ? null : requestDTO.getAccount(), true);
            return ResponseEntity.ok(success("设置管理员成功", Map.of("groupNo", groupNo)));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @DeleteMapping("/{groupNo}/admins/{account}")
    public ResponseEntity<Map<String, Object>> removeAdmin(@PathVariable String groupNo, @PathVariable String account) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            chatGroupService.setAdmin(currentUser, groupNo, account, false);
            return ResponseEntity.ok(success("取消管理员成功", Map.of("groupNo", groupNo)));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @PutMapping("/{groupNo}/members/{account}/role")
    public ResponseEntity<Map<String, Object>> updateMemberRole(@PathVariable String groupNo,
                                                                 @PathVariable String account,
                                                                 @RequestBody GroupMemberRoleUpdateRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            chatGroupService.updateMemberRole(currentUser, groupNo, account, requestDTO == null ? null : requestDTO.getRole());
            return ResponseEntity.ok(success("更新成员角色成功", Map.of("groupNo", groupNo, "account", account)));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @DeleteMapping("/{groupNo}/members/{account}")
    public ResponseEntity<Map<String, Object>> kickMember(@PathVariable String groupNo, @PathVariable String account) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            chatGroupService.kickMember(currentUser, groupNo, account);
            return ResponseEntity.ok(success("移出成员成功", Map.of("groupNo", groupNo, "account", account)));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @PostMapping("/{groupNo}/members/{account}/remove")
    public ResponseEntity<Map<String, Object>> removeMember(@PathVariable String groupNo, @PathVariable String account) {
        return kickMember(groupNo, account);
    }

    @PostMapping("/{groupNo}/kick")
    public ResponseEntity<Map<String, Object>> kickMember(@PathVariable String groupNo,
                                                           @RequestBody GroupAdminUpdateRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            String account = requestDTO == null ? null : requestDTO.getAccount();
            chatGroupService.kickMember(currentUser, groupNo, account);
            return ResponseEntity.ok(success("移出成员成功", Map.of("groupNo", groupNo, "account", account)));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @GetMapping("/{groupNo}/media/overview")
    public ResponseEntity<Map<String, Object>> mediaOverview(@PathVariable String groupNo) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("查询群媒体统计成功", chatGroupService.getMediaOverview(currentUser, groupNo)));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @PostMapping("/{groupNo}/messages/clear")
    public ResponseEntity<Map<String, Object>> clearMessages(@PathVariable String groupNo,
                                                              @RequestBody(required = false) GroupMessageClearRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("清空群消息成功",
                    chatGroupService.clearGroupMessages(currentUser, groupNo, requestDTO == null ? null : requestDTO.getScope())));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @PostMapping("/{groupNo}/reports")
    public ResponseEntity<Map<String, Object>> reportGroup(@PathVariable String groupNo,
                                                            @RequestBody GroupReportCreateRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("提交举报成功", chatGroupService.reportGroup(currentUser,
                    groupNo,
                    requestDTO == null ? null : requestDTO.getReasonType(),
                    requestDTO == null ? null : requestDTO.getDescription(),
                    requestDTO == null ? null : requestDTO.getEvidenceUrls())));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @GetMapping("/{groupNo}/messages/history")
    public ResponseEntity<Map<String, Object>> getGroupMessageHistory(@PathVariable String groupNo,
                                                                      @RequestParam(defaultValue = "1") Integer page,
                                                                      @RequestParam(defaultValue = "20") Integer size) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            ChatGroupEntity group = chatGroupService.findGroupByNo(groupNo);
            chatGroupService.ensureMember(group.getId(), currentUser.getAccount());

            int safePage = page == null || page < 1 ? 1 : page;
            int safeSize = size == null || size < 1 ? 20 : Math.min(size, 100);
            Pageable pageable = PageRequest.of(safePage - 1, safeSize);

            Page<GroupMessageEntity> pageResult = groupMessageRepository.findByGroupNoOrderBySentAtDesc(group.getGroupNo(), pageable);
            List<GroupMessageVO> messages = new ArrayList<>(pageResult.getContent().stream().map(this::toVO).toList());
            Collections.reverse(messages);

            Map<String, Object> data = new HashMap<>();
            data.put("messages", messages);
            data.put("page", safePage);
            data.put("size", safeSize);
            data.put("total", pageResult.getTotalElements());
            data.put("totalPages", pageResult.getTotalPages());
            data.put("hasMore", pageResult.hasNext());
            return ResponseEntity.ok(success("查询群聊记录成功", data));
        } catch (Exception e) {
            return failure(e);
        }
    }

    private GroupMessageVO toVO(GroupMessageEntity entity) {
        return GroupMessageVO.builder()
                .messageId(entity.getMessageId())
                .groupNo(entity.getGroupNo())
                .from(entity.getFromAccount())
                .content(entity.getContent())
                .clientMessageId(entity.getClientMessageId())
                .sentAt(entity.getSentAt())
                .build();
    }

    private UserEntity currentUser() {
        return userService.getCurrentUserEntity();
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
    }

    private ResponseEntity<Map<String, Object>> failure(Exception e) {
        if (e instanceof IllegalArgumentException) {
            if (e.getMessage() != null && e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage()));
            }
            return ResponseEntity.badRequest().body(error(400, e.getMessage()));
        }
        if (e instanceof IllegalStateException) {
            String message = e.getMessage() == null ? "操作失败" : e.getMessage();
            if (message.contains("已满") || message.contains("冲突") || message.contains("先转让群主")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error(409, message));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, message));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务异常"));
    }

    private Map<String, Object> success(String message, Object data) {
        return Map.of(
                "code", 200,
                "status", "success",
                "message", message,
                "data", data == null ? Map.of() : data
        );
    }

    private Map<String, Object> error(int code, String message) {
        return Map.of(
                "code", code,
                "status", "error",
                "message", message
        );
    }
}
