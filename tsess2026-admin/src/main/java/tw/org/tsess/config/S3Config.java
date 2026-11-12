package tw.org.tsess.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.awspring.cloud.autoconfigure.s3.S3ClientCustomizer;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;

/**
 * S3 (MinIO) 客戶端設定。
 *
 * AWS SDK v2 自 2.30 起，對 DeleteObjects 等「協議強制要求 checksum」的操作，
 * 預設改送 x-amz-checksum-crc32，而不再送 Content-MD5。
 * MinIO 等 S3 相容服務在部分版本仍只認 Content-MD5，
 * 會回 400 Missing required header for this request: Content-Md5。
 *
 * 將 requestChecksumCalculation 設為 WHEN_REQUIRED，
 * SDK 會退回 2.30 之前的行為(改送 Content-MD5)，以相容 MinIO。
 *
 * 注意:
 * 1. 只影響「請求端是否自動計算 checksum」，不影響簽章與資料完整性
 *    (SigV4 本身已含 x-amz-content-sha256)。
 * 2. 未變更 responseChecksumValidation，故既有檔案的下載驗證行為不變。
 * 3. 分片上傳本專案走純 ETag 流程，不受影響。
 */
@Configuration
public class S3Config {

	@Bean
	S3ClientCustomizer s3ChecksumCompatibilityCustomizer() {
		return builder -> builder.requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED);
	}

}
