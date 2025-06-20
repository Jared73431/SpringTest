package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.MessageDto;
import com.example.demo.dto.RoomDto;
import com.example.demo.dto.UserDto;
import com.example.demo.entity.Message;
import com.example.demo.entity.MessageAttachment;
import com.example.demo.entity.Room;
import com.example.demo.entity.User;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;

    public UserDto createOrGetUser(String username) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setIsOnline(true);
            user.setLastActive(LocalDateTime.now());
            userRepository.save(user);
            return convertToUserDto(user);
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setDisplayName(username);
        newUser.setIsOnline(true);
        newUser.setLastActive(LocalDateTime.now());
        User savedUser = userRepository.save(newUser);
        return convertToUserDto(savedUser);
    }

    public void updateUserOnlineStatus(String username, boolean online) {
        userRepository.updateUserOnlineStatus(username, online, LocalDateTime.now());
    }

    public List<UserDto> getOnlineUsers() {
        return userRepository.findByIsOnlineTrue()
                .stream()
                .map(this::convertToUserDto)
                .collect(Collectors.toList());
    }

    public List<RoomDto> getAllActiveRooms() {
        return roomRepository.findByIsActiveTrueOrderByCreatedAtAsc()
                .stream()
                .map(this::convertToRoomDto)
                .collect(Collectors.toList());
    }

    public RoomDto createRoom(RoomDto roomDto) {
        Room room = new Room();
        room.setRoomId(UUID.randomUUID().toString());
        room.setName(roomDto.getName());
        room.setDescription(roomDto.getDescription());
        room.setMaxUsers(roomDto.getMaxUsers() != null ? roomDto.getMaxUsers() : 100);
        room.setIsActive(true);

        Room savedRoom = roomRepository.save(room);
        return convertToRoomDto(savedRoom);
    }

    public MessageDto saveMessage(MessageDto messageDto) {
        Message message = new Message();
        message.setContent(messageDto.getContent());
        message.setMessageType(Message.MessageType.valueOf(messageDto.getMessageType()));
        message.setIsPrivate(messageDto.getIsPrivate());

        // 設置發送者（如果不存在則自動創建）
        User sender = ensureUserExists(messageDto.getSenderUsername());
        message.setSender(sender);

        // 設置接收者（私人消息，如果不存在則自動創建）
        if (messageDto.getIsPrivate() && messageDto.getRecipientUsername() != null) {
            User recipient = ensureUserExists(messageDto.getRecipientUsername());
            message.setRecipient(recipient);
        }

        // 設置房間（群組消息）
        if (!messageDto.getIsPrivate() && messageDto.getRoomId() != null) {
            Room room = roomRepository.findByRoomId(messageDto.getRoomId())
                    .orElseThrow(() -> new RuntimeException("房間不存在"));
            message.setRoom(room);
        }

        Message savedMessage = messageRepository.save(message);
        return convertToMessageDto(savedMessage);
    }

    public Page<MessageDto> getRoomMessages(String roomId, Pageable pageable) {
        Room room = roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("房間不存在"));

        List<Message> messages = messageRepository.findByRoomOrderByCreatedAtDesc(room, pageable);
        List<MessageDto> messageDtos = messages.stream()
                .map(this::convertToMessageDto)
                .collect(Collectors.toList());

        return new PageImpl<>(messageDtos, pageable, messageDtos.size());
    }

    public Page<MessageDto> getPrivateMessages(String user1, String user2, Pageable pageable) {
        User userA = userRepository.findByUsername(user1)
                .orElseThrow(() -> new RuntimeException("用戶不存在"));
        User userB = userRepository.findByUsername(user2)
                .orElseThrow(() -> new RuntimeException("用戶不存在"));

        List<Message> messages = messageRepository.findPrivateMessagesBetweenUsers(userA, userB, pageable);
        List<MessageDto> messageDtos = messages.stream()
                .map(this::convertToMessageDto)
                .collect(Collectors.toList());

        return new PageImpl<>(messageDtos, pageable, messageDtos.size());
    }

    // DTO轉換方法
    private UserDto convertToUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setDisplayName(user.getDisplayName());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setIsOnline(user.getIsOnline());
        dto.setLastActive(user.getLastActive());
        return dto;
    }

    private RoomDto convertToRoomDto(Room room) {
        RoomDto dto = new RoomDto();
        dto.setId(room.getId());
        dto.setRoomId(room.getRoomId());
        dto.setName(room.getName());
        dto.setDescription(room.getDescription());
        dto.setMaxUsers(room.getMaxUsers());
        dto.setIsActive(room.getIsActive());
        dto.setCreatedAt(room.getCreatedAt());
        return dto;
    }

    private MessageDto convertToMessageDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setMessageType(message.getMessageType().name());
        dto.setIsPrivate(message.getIsPrivate());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setSenderDisplayName(message.getSender().getDisplayName());

        if (message.getRecipient() != null) {
            dto.setRecipientUsername(message.getRecipient().getUsername());
        }

        if (message.getRoom() != null) {
            dto.setRoomId(message.getRoom().getRoomId());
        }

        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
    public User ensureUserExists(String username) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setDisplayName(username); // 使用用戶名作為顯示名
                    newUser.setIsOnline(true);
                    newUser.setCreatedAt(LocalDateTime.now());
                    newUser.setUpdatedAt(LocalDateTime.now());
                    newUser.setLastActive(LocalDateTime.now());

                    User savedUser = userRepository.save(newUser);
                    log.info("自動創建新用戶: {}", username);
                    return savedUser;
                });
    }

    /**
     * 根據 roomId 獲取房間對象
     */
    public Room getRoomByRoomId(String roomId) {
        return roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("房間不存在: " + roomId));
    }

    /**
     * 創建文件消息
     */
    public Message createFileMessage(String senderUsername, String content,
                                     boolean isPrivate, String recipientUsername, String roomId) {
        Message message = new Message();
        message.setContent(content != null ? content : "發送了一個文件");
        message.setMessageType(Message.MessageType.FILE); // 需要在你的 MessageType 枚舉中添加 FILE 類型
        message.setIsPrivate(isPrivate);

        // 設置發送者
        User sender = ensureUserExists(senderUsername);
        message.setSender(sender);

        // 設置接收者（私人消息）
        if (isPrivate && recipientUsername != null) {
            User recipient = ensureUserExists(recipientUsername);
            message.setRecipient(recipient);
        }

        // 設置房間（群組消息）
        if (!isPrivate && roomId != null) {
            Room room = getRoomByRoomId(roomId);
            message.setRoom(room);
        }

        return messageRepository.save(message);
    }

    /**
     * 保存帶有附件的消息
     */
    public MessageDto saveMessageWithAttachment(String senderUsername, String content,
                                                boolean isPrivate, String recipientUsername,
                                                String roomId, List<MessageAttachment> attachments) {
        Message message = createFileMessage(senderUsername, content, isPrivate, recipientUsername, roomId);

        // 設置附件關聯
        if (attachments != null) {
            attachments.forEach(attachment -> attachment.setMessage(message));
        }

        return convertToMessageDto(message);
    }
}
