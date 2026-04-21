package com.example.shop.controller;

import com.example.shop.entity.ChatSenderRole;
import com.example.shop.entity.SupportChatMessage;
import com.example.shop.exception.BadRequestException;
import com.example.shop.repository.SupportChatMessageRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat/api")
public class ChatApiController {

    private final SupportChatMessageRepository supportChatMessageRepository;

    public ChatApiController(SupportChatMessageRepository supportChatMessageRepository) {
        this.supportChatMessageRepository = supportChatMessageRepository;
    }

    @GetMapping("/user/messages")
    public List<SupportChatMessage> getUserMessages(Authentication authentication) {
        return supportChatMessageRepository.findByUsernameOrderByCreatedAtAscIdAsc(authentication.getName());
    }

    @PostMapping("/user/messages")
    public SupportChatMessage sendFromUser(@RequestBody Map<String, String> payload,
                                           Authentication authentication) {
        return saveMessage(authentication.getName(), ChatSenderRole.USER, payload.get("content"));
    }

    @GetMapping("/admin/conversations")
    public List<Map<String, Object>> getAdminConversations() {
        List<SupportChatMessage> all = supportChatMessageRepository.findAllByOrderByCreatedAtDescIdDesc();
        LinkedHashMap<String, Map<String, Object>> latestByUser = new LinkedHashMap<>();

        for (SupportChatMessage message : all) {
            if (latestByUser.containsKey(message.getUsername())) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("username", message.getUsername());
            item.put("lastMessage", message.getContent());
            item.put("lastAt", message.getCreatedAt());
            latestByUser.put(message.getUsername(), item);
        }

        return new ArrayList<>(latestByUser.values());
    }

    @GetMapping("/admin/messages/{username}")
    public List<SupportChatMessage> getAdminMessages(@PathVariable String username) {
        return supportChatMessageRepository.findByUsernameOrderByCreatedAtAscIdAsc(username);
    }

    @PostMapping("/admin/messages/{username}")
    public SupportChatMessage sendFromAdmin(@PathVariable String username,
                                            @RequestBody Map<String, String> payload) {
        return saveMessage(username, ChatSenderRole.ADMIN, payload.get("content"));
    }

    private SupportChatMessage saveMessage(String username, ChatSenderRole role, String rawContent) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty()) {
            throw new BadRequestException("Nội dung chat không được để trống");
        }

        SupportChatMessage message = new SupportChatMessage();
        message.setUsername(username);
        message.setSenderRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        return supportChatMessageRepository.save(message);
    }
}
