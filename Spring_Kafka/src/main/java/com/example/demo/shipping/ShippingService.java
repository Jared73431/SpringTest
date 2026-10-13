package com.example.demo.shipping;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.demo.order.OrderEvent;

/**
 * 出貨：訂單建立 → 待出貨；訂單取消 → 取消出貨。
 *
 * <p>
 * <b>冪等</b>（第 4 課）：Kafka 保證「至少送達一次」，同一個事件可能被處理兩次（例如處理完、還沒 commit offset 就當機）。
 * 用 eventId 記住處理過的事件，第二次直接略過，結果和只處理一次相同。
 *
 * <p>
 * 練習用：狀態存在記憶體中。真實系統存在資料庫，並且在<b>同一個交易</b>中記錄「處理過的 eventId」與出貨資料。
 */
@Service
public class ShippingService {

	private static final Logger log = LoggerFactory.getLogger(ShippingService.class);

	/** 物流一次最多出貨的數量：超過的訂單不能出貨（業務規則，重試也沒用） */
	public static final int MAX_QUANTITY = 10;

	public enum Status {
		WAITING_FOR_SHIPMENT, CANCELLED
	}

	private final Map<String, Status> shipments = new ConcurrentHashMap<>();

	private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

	/** 實際處理（不含重複的事件）了幾次：讓測試觀察冪等 */
	private final AtomicInteger handled = new AtomicInteger();

	public void handle(OrderEvent event) {
		if (event.type() == OrderEvent.Type.PLACED && event.quantity() > MAX_QUANTITY) {
			throw new ShippingRejectedException(
					"Order " + event.orderId() + ": quantity " + event.quantity() + " exceeds " + MAX_QUANTITY);
		}
		if (!processedEventIds.add(event.eventId())) {
			log.info("Event {} already processed, skipped", event.eventId());
			return;
		}
		switch (event.type()) {
			case PLACED -> shipments.put(event.orderId(), Status.WAITING_FOR_SHIPMENT);
			case CANCELLED -> shipments.put(event.orderId(), Status.CANCELLED);
		}
		handled.incrementAndGet();
		log.info("Order {} -> {}", event.orderId(), shipments.get(event.orderId()));
	}

	public Optional<Status> status(String orderId) {
		return Optional.ofNullable(shipments.get(orderId));
	}

	public int handledCount() {
		return handled.get();
	}
}
