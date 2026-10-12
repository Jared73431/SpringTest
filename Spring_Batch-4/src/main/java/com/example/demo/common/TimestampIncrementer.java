package com.example.demo.common;

import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.job.parameters.JobParametersIncrementer;

/**
 * 每次啟動加上 timestamp 參數：每次都是新的 JobInstance，所以可以重複執行。
 *
 * <p>
 * 只用在<b>冪等</b>的 Job：重做幾次結果都一樣（例如報表、只處理「待處理」資料的更新）。
 * 不能做兩次的 Job（例如套用一個價格更新檔）改用業務參數識別，見 PriceUpdateJobConfig。
 */
public class TimestampIncrementer implements JobParametersIncrementer {

	public static final String KEY = "timestamp";

	@Override
	public JobParameters getNext(JobParameters parameters) {
		return new JobParametersBuilder(parameters == null ? new JobParameters() : parameters)
				.addLong(KEY, System.currentTimeMillis())
				.toJobParameters();
	}
}
