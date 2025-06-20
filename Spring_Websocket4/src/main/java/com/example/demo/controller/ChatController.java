package com.example.demo.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.MessageDto;
import com.example.demo.dto.RoomDto;
import com.example.demo.dto.UserDto;
import com.example.demo.service.ChatService;
import com.example.demo.service.FileUploadService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;
    private final FileUploadService fileUploadService;

    @GetMapping("/rooms")
    public ResponseEntity<List<RoomDto>> getAllRooms() {
        return ResponseEntity.ok(chatService.getAllActiveRooms());
    }

    @PostMapping("/rooms")
    public ResponseEntity<RoomDto> createRoom(@RequestBody RoomDto roomDto) {
        return ResponseEntity.ok(chatService.createRoom(roomDto));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Page<MessageDto>> getRoomMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(chatService.getRoomMessages(roomId, PageRequest.of(page, size)));
    }

    @GetMapping("/users/online")
    public ResponseEntity<List<UserDto>> getOnlineUsers() {
        return ResponseEntity.ok(chatService.getOnlineUsers());
    }

    @GetMapping("/messages/private/{username}")
    public ResponseEntity<Page<MessageDto>> getPrivateMessages(
            @PathVariable String username,
            @RequestParam String currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(chatService.getPrivateMessages(currentUser, username, PageRequest.of(page, size)));
    }
}
