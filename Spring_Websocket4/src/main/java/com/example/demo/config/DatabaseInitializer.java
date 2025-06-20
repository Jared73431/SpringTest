package com.example.demo.config;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.demo.entity.Room;
import com.example.demo.repository.RoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final RoomRepository roomRepository;

    @Override
    public void run(String... args) throws Exception {
        // 創建默認房間
        if (roomRepository.count() == 0) {
            createDefaultRooms();
        }
    }

    private void createDefaultRooms() {
        Room generalRoom = new Room();
        generalRoom.setRoomId(UUID.randomUUID().toString());
        generalRoom.setName("一般討論");
        generalRoom.setDescription("歡迎大家在這裡自由討論");
        generalRoom.setMaxUsers(100);
        generalRoom.setIsActive(true);
        roomRepository.save(generalRoom);

        Room techRoom = new Room();
        techRoom.setRoomId(UUID.randomUUID().toString());
        techRoom.setName("技術討論");
        techRoom.setDescription("技術相關話題討論區");
        techRoom.setMaxUsers(50);
        techRoom.setIsActive(true);
        roomRepository.save(techRoom);

        Room randomRoom = new Room();
        randomRoom.setRoomId(UUID.randomUUID().toString());
        randomRoom.setName("隨機聊天");
        randomRoom.setDescription("隨意聊天的地方");
        randomRoom.setMaxUsers(30);
        randomRoom.setIsActive(true);
        roomRepository.save(randomRoom);

        log.info("默認房間創建完成");
    }
}
