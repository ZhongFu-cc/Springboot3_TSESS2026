package tw.org.tsess.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.org.tsess.enums.ScheduleEmailStatus;
import tw.org.tsess.pojo.DTO.SendEmailDTO;
import tw.org.tsess.pojo.entity.ScheduleEmailRecord;
import tw.org.tsess.pojo.entity.ScheduleEmailTask;
import tw.org.tsess.service.AsyncService;
import tw.org.tsess.service.ScheduleEmailRecordService;
import tw.org.tsess.utils.S3Util;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncServiceImpl implements AsyncService {

	private final JavaMailSender mailSender;
	private final ScheduleEmailRecordService scheduleEmailRecordService;
	private final S3Util s3Util;
	
	// 「預設」存储桶名称
	@Value("${spring.cloud.aws.s3.bucketName}") // 注意：这里的 Value key 可能需要对应您的配置
	private String bucketName;

	@Value("${project.email.from}")
	private String EMAIL_FROM;

	@Value("${project.email.from-name}")
	private String EMAIL_FROM_NAME;

	@Value("${project.email.reply-to}")
	private String EMAIL_REPLY_TO;


	@Override
	@Async("taskExecutor")
	public void sendCommonEmail(String to, String subject, String htmlContent, String plainTextContent) {
		sendCommonEmail(to, subject, htmlContent, plainTextContent, null);
	}

	@Override
	@Async("taskExecutor")
	public void sendCommonEmail(String to, String subject, String htmlContent, String plainTextContent,
			List<ByteArrayResource> attachments) {
		String[] recipients = parseEmailAddresses(to);
		sendInternal(recipients, subject, htmlContent, plainTextContent, attachments);
	}

	@Override
	@Async("taskExecutor")
	public void sendCommonEmail(List<String> to, String subject, String htmlContent, String plainTextContent) {
		// 呼叫處理 List 的版本
		String[] recipients = parseEmailAddresses(to);
		// 呼叫共同邏輯
		sendInternal(recipients, subject, htmlContent, plainTextContent, null);
	}

	@Override
	@Async("taskExecutor")
	public void sendCommonEmail(List<String> to, String subject, String htmlContent, String plainTextContent,
			List<ByteArrayResource> attachments) {
		// 呼叫處理 List 的版本
		String[] recipients = parseEmailAddresses(to);
		// 呼叫共同邏輯
		sendInternal(recipients, subject, htmlContent, plainTextContent, attachments);
	}


	/**
	 * 私有方法,核心寄信業務
	 * 
	 * @param recipients
	 * @param subject
	 * @param htmlContent
	 * @param plainTextContent
	 * @param attachments
	 */
	private void sendInternal(String[] recipients, String subject, String htmlContent, String plainTextContent,
			List<ByteArrayResource> attachments) {
		if (recipients.length == 0) {
			log.warn("收件人列表為空，取消發送。");
			return;
		}

		try {
			MimeMessage message = mailSender.createMimeMessage();
			// 🔥 關鍵：設定信件為「高重要性」
//			message.addHeader("X-Priority", "1"); // 1 = High, 3 = Normal, 5 = Low
//			message.addHeader("Importance", "High"); // Outlook / Exchange 會識別
//			message.addHeader("Priority", "urgent"); // 部分郵件用戶端使用這個標頭

			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			// MimeMessageHelper 支援 String[]
			helper.setTo(recipients);
			helper.setFrom(EMAIL_FROM, EMAIL_FROM_NAME);
//			helper.setReplyTo(EMAIL_REPLY_TO);
			helper.setSubject(subject);
			helper.setText(plainTextContent, htmlContent);

			// 有附件就新增附件
			if (attachments != null) {
				for (ByteArrayResource file : attachments) {
					helper.addAttachment(file.getFilename(), file);
				}
			}

			mailSender.send(message);
		} catch (Exception e) {
			System.err.println("發送郵件失敗: " + e.getMessage());
			log.error("發送郵件失敗", e);
		}
	}

	/**
	 * 私有方法
	 * 將傳入的單一字串（可能包含多個信箱）拆解成陣列
	 * 支援逗號 (,) 或分號 (;) 分隔
	 */
	private String[] parseEmailAddresses(String to) {
		if (to == null || to.isBlank()) {
			return new String[0];
		}
		// 使用正則表達式拆分，並去除多餘空格
		return Arrays.stream(to.split("[,;]"))
				.map(String::trim)
				.filter(email -> !email.isEmpty())
				.toArray(String[]::new);
	}

	/**
	 * 私有方法，重載
	 * 處理 List<String>（同樣支援元素內包含逗號、分號的情況）
	 */
	private String[] parseEmailAddresses(List<String> to) {
		if (to == null || to.isEmpty()) {
			return new String[0];
		}
		return to.stream()
				.filter(Objects::nonNull)
				// 核心重點：將每個元素丟進上面的 String 版本處理，再扁平化 (flatMap)
				.flatMap(s -> Arrays.stream(this.parseEmailAddresses(s)))
				.toArray(String[]::new);
	}

	@Override
	@Async("taskExecutor")
	public <T> void batchSendEmail(List<T> recipients, SendEmailDTO sendEmailDTO, Function<T, String> emailExtractor,
			BiFunction<String, T, String> contentReplacer

	) {
		int batchSize = 10; // 每批寄信數量
		long delayMs = 3000L; // 每批間隔

		// 使用 Guava partition 分批
		List<List<T>> batches = Lists.partition(recipients, batchSize);

		for (List<T> batch : batches) {
			for (T recipient : batch) {
				// 1. 個人化內容
				String htmlContent = contentReplacer.apply(sendEmailDTO.getHtmlContent(), recipient);
				String plainText = contentReplacer.apply(sendEmailDTO.getPlainText(), recipient);

				// 2. 測試信件 vs 真實收件者
				String email = sendEmailDTO.getIsTest() ? sendEmailDTO.getTestEmail() : emailExtractor.apply(recipient);

				// 3. 寄信
				this.sendCommonEmail(email, sendEmailDTO.getSubject(), htmlContent, plainText);
			}

			try {
				Thread.sleep(delayMs); // ✅ 控速，避免被信箱伺服器擋
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	@Async("taskExecutor")
	public <T> void batchSendEmail(List<T> recipients, SendEmailDTO sendEmailDTO, Function<T, String> emailExtractor,
			BiFunction<String, T, String> contentReplacer, Function<T, List<ByteArrayResource>> attachmentProvider) {
		int batchSize = 10;
		long delayMs = 3000L;

		List<List<T>> batches = Lists.partition(recipients, batchSize);

		for (List<T> batch : batches) {
			for (T recipient : batch) {

				// 1.個人化內容
				String htmlContent = contentReplacer.apply(sendEmailDTO.getHtmlContent(), recipient);
				String plainText = contentReplacer.apply(sendEmailDTO.getPlainText(), recipient);

				// 2.測試 vs 真實收件者
				String email = sendEmailDTO.getIsTest() ? sendEmailDTO.getTestEmail() : emailExtractor.apply(recipient);

				// 3. 查詢附件（判斷是否需要附件）
				List<ByteArrayResource> attachments = new ArrayList<>();
				if (sendEmailDTO.getIncludeOfficialAttachment() && attachmentProvider != null) {
					attachments = attachmentProvider.apply(recipient);
				}

				// 4.寄信
				this.sendCommonEmail(email, sendEmailDTO.getSubject(), htmlContent, plainText, attachments);
			}

			try {
				Thread.sleep(delayMs); // 控速
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	public void triggerSendEmail(ScheduleEmailTask scheduleEmailTask,
			List<ScheduleEmailRecord> scheduleEmailRecordList) {

		// 批量寄信數量
		int batchSize = 10;
		// 批量寄信間隔 3000 毫秒
		long delayMs = 3000L;

		/**
		 * 把一個 List<T> 拆成若干個小清單（subList），每組大小為 batchSize：
		 * List<String> names = Arrays.asList("A", "B", "C", "D", "E");
		 * List<List<String>> batches = Lists.partition(names, 2);
		 * 
		 * // 結果： [["A", "B"], ["C", "D"], ["E"]]
		 * 
		 */
		List<List<ScheduleEmailRecord>> batches = Lists.partition(scheduleEmailRecordList, batchSize);

		for (List<ScheduleEmailRecord> batch : batches) {
			for (ScheduleEmailRecord scheduleEmailRecord : batch) {

				// 初始化附件列表
				List<ByteArrayResource> attachments = new ArrayList<>();

				// 拿到記錄中的檔案列表
				List<String> paths = new ArrayList<>();

				try {

					// 如果附件Path 不為Null,則進行拆分,拿到所有附件路徑
					if (scheduleEmailRecord.getAttachmentsPath() != null) {
						paths = Arrays.stream(scheduleEmailRecord.getAttachmentsPath().split(","))
								.map(String::trim)
								.filter(str -> !str.isEmpty())
								.toList();
					}

					// 將檔案列表遍歷拿到真正的檔案
					for (String path : paths) {

						String s3Key = s3Util.extractS3PathInDbUrl(bucketName, path);
						
						// 獲取檔案位元組
						byte[] fileBytes = s3Util.getFileBytes(s3Key);

						if (fileBytes != null) {
							// 解析檔名
							String fileName = path.substring(path.lastIndexOf("/") + 1);

							ByteArrayResource resource = new ByteArrayResource(fileBytes) {
								@Override
								public String getFilename() {
									return fileName;
								}
							};

							attachments.add(resource);
						}

					}

					// 狀態變更為執行中，立即更新，避免保持狀態及時
					scheduleEmailRecord.setStatus(ScheduleEmailStatus.EXECUTE.getValue());
					scheduleEmailRecordService.updateById(scheduleEmailRecord);

					//					System.out.println("模擬寄信,等其他測試完成就打開它");
					this.sendCommonEmail(scheduleEmailRecord.getEmail(), scheduleEmailTask.getSubject(),
							scheduleEmailRecord.getHtmlContent(), scheduleEmailRecord.getPlainText(), attachments);

					scheduleEmailRecord.setStatus(ScheduleEmailStatus.FINISHED.getValue());

				} catch (Exception e) {
					log.error("taskRecordId: " + scheduleEmailRecord.getScheduleEmailRecordId()
							+ "執行上碰到問題，信件無法正常寄送，問題為: " + e.getMessage());
					scheduleEmailRecord.setStatus(ScheduleEmailStatus.FAILED.getValue());
				} finally {
					scheduleEmailRecordService.updateById(scheduleEmailRecord);
				}

			}

			// 每完成一個批次 , 停止3秒
			try {
				Thread.sleep(delayMs); // ✅ 控速，避免信箱被擋
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

		}

	}

}
