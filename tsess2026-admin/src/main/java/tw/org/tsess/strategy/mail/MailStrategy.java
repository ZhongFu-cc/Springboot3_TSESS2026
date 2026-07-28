package tw.org.tsess.strategy.mail;

import java.util.List;

import tw.org.tsess.pojo.DTO.SendEmailDTO;

public interface MailStrategy {

	/**
	 * 立刻寄信
	 * 
	 * @param tagIdList
	 * @param sendEmailDTO
	 */
	void batchSendEmail(List<Long> tagIdList, SendEmailDTO sendEmailDTO);

	/**
	 * 排程寄信
	 * 
	 * @param tagIdList
	 * @param sendEmailDTO
	 */
	void scheduleEmail(List<Long> tagIdList, SendEmailDTO sendEmailDTO);

}
