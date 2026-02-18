package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.dto.group.GroupAdminUpdateRequestDTO;
import com.lujianfeng.spanner.dto.group.GroupAnnouncementUpdateRequestDTO;
import com.lujianfeng.spanner.dto.group.GroupCreateRequestDTO;
import com.lujianfeng.spanner.dto.group.GroupInviteRequestDTO;
import com.lujianfeng.spanner.entity.group.ChatGroupEntity;
import com.lujianfeng.spanner.entity.message.GroupMessageEntity;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.repository.GroupMessageRepository;
import com.lujianfeng.spanner.service.ChatGroupService;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.vo.group.GroupInfoVO;
import com.lujianfeng.spanner.vo.group.GroupMemberVO;
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
        }
        try {
            GroupInfoVO groupInfo = chatGroupService.createGroup(currentUser, requestDTO);
            return ResponseEntity.ok(success("创建群组成功", groupInfo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(409, e.getMessage()));
        }
    }

    @GetMapping("/{groupNo}")
    public ResponseEntity<Map<String, Object>> getGroup(@PathVariable String groupNo) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
        }
        try {
            GroupInfoVO groupInfo = chatGroupService.getGroupInfo(currentUser, groupNo);
            return ResponseEntity.ok(success("查询群信息成功", groupInfo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, e.getMessage()));
        }
    }

    @GetMapping("/{groupNo}/members")
    public ResponseEntity<Map<String, Object>> listMembers(@PathVariable String groupNo) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
        }
        try {
            List<GroupMemberVO> members = chatGroupService.listGroupMembers(currentUser, groupNo);
            Map<String, Object> data = new HashMap<>();
            data.put("members", members);
            data.put("count", members.size());
            return ResponseEntity.ok(success("查询群成员成功", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, e.getMessage()));
        }
    }

    @PostMapping("/{groupNo}/join")
    public ResponseEntity<Map<String, Object>> joinGroup(@PathVariable String groupNo) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
        }
        try {
            GroupInfoVO groupInfo = chatGroupService.joinGroup(currentUser, groupNo);
            return ResponseEntity.ok(success("加入群组成功", groupInfo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(409, e.getMessage()));
        }
    }

    @PostMapping("/{groupNo}/invite")
    public ResponseEntity<Map<String, Object>> inviteFriend(@PathVariable String groupNo,
                                                            @RequestBody GroupInviteRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
        }
        try {
            GroupInfoVO groupInfo =
                    chatGroupService.inviteFriend(currentUser, groupNo, requestDTO == null ? null : requestDTO.getFriendAccount());
            return ResponseEntity.ok(success("邀请好友入群成功", groupInfo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(409, e.getMessage()));
        }
    }

    @PostMapping("/{groupNo}/quit")
    public ResponseEntity<Map<String, Object>> quitGroup(@PathVariable String groupNo) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
        }
        try {
            chatGroupService.quitGroup(currentUser, groupNo);
            return ResponseEntity.ok(success("退群成功", Map.of("groupNo", groupNo)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(409, e.getMessage()));
        }
    }

    @PutMapping("/{groupNo}/announcement")
    public ResponseEntity<Map<String, Object>> updateAnnouncement(@PathVariable String groupNo,
                                                                  @RequestBody GroupAnnouncementUpdateRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
        }
        try {
            GroupInfoVO groupInfo = chatGroupService.updateAnnouncement(currentUser, groupNo,
                    requestDTO == null ? null : requestDTO.getAnnouncement());
            return ResponseEntity.ok(success("更新群公告成功", groupInfo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, e.getMessage()));
        }
    }

    @PutMapping("/{groupNo}/admins")
    public ResponseEntity<Map<String, Object>> setAdmin(@PathVariable String groupNo,
                                                         @RequestBody GroupAdminUpdateRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
        }
        try {
            chatGroupService.setAdmin(currentUser, groupNo, requestDTO == null ? null : requestDTO.getAccount(), true);
            return ResponseEntity.ok(success("设置管理员成功", Map.of("groupNo", groupNo)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, e.getMessage()));
        }
    }

    @DeleteMapping("/{groupNo}/admins/{account}")
    public ResponseEntity<Map<String, Object>> removeAdmin(@PathVariable String groupNo, @PathVariable String account) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
        }
        try {
            chatGroupService.setAdmin(currentUser, groupNo, account, false);
            return ResponseEntity.ok(success("取消管理员成功", Map.of("groupNo", groupNo)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, e.getMessage()));
        }
    }

    @DeleteMapping("/{groupNo}/members/{account}")
    public ResponseEntity<Map<String, Object>> kickMember(@PathVariable String groupNo, @PathVariable String account) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
        }
        try {
            chatGroupService.kickMember(currentUser, groupNo, account);
            return ResponseEntity.ok(success("移出成员成功", Map.of("groupNo", groupNo, "account", account)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, e.getMessage()));
        }
    }

    @PostMapping("/{groupNo}/kick")
    public ResponseEntity<Map<String, Object>> kickMember(@PathVariable String groupNo,
                                                           @RequestBody GroupAdminUpdateRequestDTO requestDTO) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
        }
        try {
            String account = requestDTO == null ? null : requestDTO.getAccount();
            chatGroupService.kickMember(currentUser, groupNo, account);
            return ResponseEntity.ok(success("移出成员成功", Map.of("groupNo", groupNo, "account", account)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, e.getMessage()));
        }
    }

    @GetMapping("/{groupNo}/messages/history")
    public ResponseEntity<Map<String, Object>> getGroupMessageHistory(@PathVariable String groupNo,
                                                                      @RequestParam(defaultValue = "1") Integer page,
                                                                      @RequestParam(defaultValue = "20") Integer size) {
        UserEntity currentUser = currentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
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
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, e.getMessage()));
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
