package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.config.CacheNames;
import com.example.demo.dto.UserDto;
import com.example.demo.dto.UserRequest;
import com.example.demo.entity.User;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.UserRepository;

/**
 * 使用者的商業邏輯，以 Spring Cache 註解宣告快取行為（實際存放在 Redis，見 RedisConfig）。
 *
 * <pre>
 * 快取            key       寫入                          清除
 * users           id        查詢、修改（@CachePut）        刪除
 * usersByEmail    email     查詢                          修改、刪除
 * allUsers        all       查詢全部                      新增、修改、刪除
 * </pre>
 *
 * 原則：<b>任何會讓快取內容變舊的操作，都要更新或清除對應的快取</b>。
 * 修正前 email 快取與 allUsers 快取在修改、刪除、新增後都沒有處理，會讀到舊資料（最長到 TTL 10 分鐘）。
 *
 * log 只在「真的查資料庫」時輸出，搭配 SQL log 可以觀察快取是否命中。
 */
@Service
public class UserService {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * 回傳 Optional：查不到時是 empty。
	 * unless = "#result == null"：#result 指的是 Optional 裡面的值，查不到時不寫入快取。
	 * 修正前回傳 null，而 CacheManager 設定了 disableCachingNullValues()，
	 * Spring 嘗試快取 null 時拋出 IllegalArgumentException → 500，Controller 的 404 判斷永遠不會執行。
	 */
	@Cacheable(cacheNames = CacheNames.USERS, key = "#id", unless = "#result == null")
	public Optional<UserDto> findById(Long id) {
		log.info("從資料庫查詢用戶 ID: {}", id);
		return userRepository.findById(id).map(UserDto::from);
	}

	@Cacheable(cacheNames = CacheNames.USERS_BY_EMAIL, key = "#email", unless = "#result == null")
	public Optional<UserDto> findByEmail(String email) {
		log.info("從資料庫查詢用戶 Email: {}", email);
		return userRepository.findByEmail(email).map(UserDto::from);
	}

	// key 固定為 'all'：沒有參數時預設的 key 是 SimpleKey []，可讀性差
	@Cacheable(cacheNames = CacheNames.ALL_USERS, key = "'all'")
	public List<UserDto> findAll() {
		log.info("從資料庫查詢所有用戶");
		return userRepository.findAll().stream().map(UserDto::from).toList();
	}

	// 新增後，「全部使用者」的快取就過時了
	@Transactional
	@CacheEvict(cacheNames = CacheNames.ALL_USERS, allEntries = true)
	public UserDto create(UserRequest request) {
		log.info("創建新用戶: {}", request.name());
		User user = new User();
		request.applyTo(user);
		return UserDto.from(userRepository.save(user));
	}

	/**
	 * 修改：更新 id 快取（@CachePut），清除 email 快取與全部快取。
	 * email 快取用 allEntries 清除：修改時 email 可能也改了，舊 email 對應的快取無法用註解指定 key。
	 * 不存在時拋出 UserNotFoundException（404），例外發生時快取註解都不會執行。
	 */
	@Transactional
	@Caching(
			put = @CachePut(cacheNames = CacheNames.USERS, key = "#id"),
			evict = {
					@CacheEvict(cacheNames = CacheNames.USERS_BY_EMAIL, allEntries = true),
					@CacheEvict(cacheNames = CacheNames.ALL_USERS, allEntries = true) })
	public UserDto update(Long id, UserRequest request) {
		log.info("更新用戶並刷新快取: {}", id);
		User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
		request.applyTo(user);
		return UserDto.from(userRepository.save(user));
	}

	@Transactional
	@Caching(evict = {
			@CacheEvict(cacheNames = CacheNames.USERS, key = "#id"),
			@CacheEvict(cacheNames = CacheNames.USERS_BY_EMAIL, allEntries = true),
			@CacheEvict(cacheNames = CacheNames.ALL_USERS, allEntries = true) })
	public void delete(Long id) {
		log.info("刪除用戶並清除快取: {}", id);
		if (!userRepository.existsById(id)) {
			throw new UserNotFoundException(id);
		}
		userRepository.deleteById(id);
	}

	@Caching(evict = {
			@CacheEvict(cacheNames = CacheNames.USERS, allEntries = true),
			@CacheEvict(cacheNames = CacheNames.USERS_BY_EMAIL, allEntries = true),
			@CacheEvict(cacheNames = CacheNames.ALL_USERS, allEntries = true) })
	public void clearAllCaches() {
		log.info("清除所有用戶快取");
	}
}
