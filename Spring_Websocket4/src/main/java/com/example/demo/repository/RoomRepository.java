package com.example.demo.repository;

import com.example.demo.entity.Room;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomId(String roomId);

    List<Room> findByIsActiveTrueOrderByCreatedAtAsc();
}
