package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.UserRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.exception.DuplicateUserException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public List<UserResponse> getAllUsers() {
		return userRepository.findAll().stream().map(UserResponse::from).toList();
	}

	public UserResponse getUserById(Long id) {
		return UserResponse.from(findUser(id));
	}

	@Transactional
	public UserResponse createUser(UserRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw new DuplicateUserException("帳號", request.username());
		}
		if (userRepository.existsByEmail(request.email())) {
			throw new DuplicateUserException("電子郵件", request.email());
		}
		User user = new User(request.username(), request.name(), request.email(), request.age());
		return UserResponse.from(userRepository.save(user));
	}

	// 交易內修改 Entity，結束時 JPA 自動 UPDATE（dirty checking），不需要再呼叫 save()
	@Transactional
	public UserResponse updateUser(Long id, UserRequest request) {
		User user = findUser(id);
		if (userRepository.existsByUsernameAndIdNot(request.username(), id)) {
			throw new DuplicateUserException("帳號", request.username());
		}
		if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
			throw new DuplicateUserException("電子郵件", request.email());
		}
		user.update(request.username(), request.name(), request.email(), request.age());
		return UserResponse.from(user);
	}

	@Transactional
	public void deleteUser(Long id) {
		userRepository.delete(findUser(id));
	}

	private User findUser(Long id) {
		return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
	}
}
