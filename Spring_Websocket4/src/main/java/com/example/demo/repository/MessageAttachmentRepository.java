package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Message;
import com.example.demo.entity.MessageAttachment;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {

    List<MessageAttachment> findByMessage(Message message);

    List<MessageAttachment> findByIsImageTrue();

    void deleteByMessage(Message message);
}
