package com.example.demo.price;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;

/**
 * 記錄被跳過的行與原因：跳過不是忽略，事後要能知道哪幾行沒有套用。
 */
public class PriceUpdateSkipListener implements SkipListener<PriceUpdate, PriceUpdate> {

	private static final Logger log = LoggerFactory.getLogger(PriceUpdateSkipListener.class);

	@Override
	public void onSkipInRead(Throwable t) {
		if (t instanceof FlatFileParseException e) {
			log.warn("Skipped line {} [{}]: {}", e.getLineNumber(), e.getInput(), e.getMessage());
		}
		else {
			log.warn("Skipped a line: {}", t.getMessage());
		}
	}

	@Override
	public void onSkipInProcess(PriceUpdate item, Throwable t) {
		log.warn("Skipped {}: {}", item, t.getMessage());
	}
}
