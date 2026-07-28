package tw.org.tsess.service.impl;

import java.util.List;
import java.util.Map;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.org.tsess.convert.EmailTemplateConvert;
import tw.org.tsess.enums.TagTypeEnum;
import tw.org.tsess.mapper.EmailTemplateMapper;
import tw.org.tsess.pojo.DTO.SendEmailDTO;
import tw.org.tsess.pojo.DTO.addEntityDTO.AddEmailTemplateDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutEmailTemplateDTO;
import tw.org.tsess.pojo.entity.EmailTemplate;
import tw.org.tsess.service.EmailTemplateService;
import tw.org.tsess.service.ScheduleEmailTaskService;
import tw.org.tsess.service.TagService;
import tw.org.tsess.strategy.mail.MailStrategy;

/**
 * <p>
 * 信件模板表 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-01-16
 */
@Service
@RequiredArgsConstructor
public class EmailTemplateServiceImpl extends ServiceImpl<EmailTemplateMapper, EmailTemplate>
		implements EmailTemplateService {

	private final Map<String, MailStrategy> strategyMap;

	private static final String DAILY_EMAIL_QUOTA_KEY = "email:dailyQuota";

	private final TagService tagService;
	private final EmailTemplateConvert emailTemplateConvert;
	private final ScheduleEmailTaskService scheduleEmailTaskService;


	//redLockClient01  businessRedissonClient
	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

	@Override
	public List<EmailTemplate> getAllEmailTemplate() {
		List<EmailTemplate> emailTemplateList = baseMapper.selectList(null);
		return emailTemplateList;
	}

	@Override
	public IPage<EmailTemplate> getAllEmailTemplate(Page<EmailTemplate> page) {
		Page<EmailTemplate> emailTemplatePage = baseMapper.selectPage(page, null);
		return emailTemplatePage;
	}

	@Override
	public EmailTemplate getEmailTemplate(Long emailTemplateId) {
		EmailTemplate emailTemplate = baseMapper.selectById(emailTemplateId);
		return emailTemplate;
	}

	@Override
	public Long insertEmailTemplate(AddEmailTemplateDTO insertEmailTemplateDTO) {
		EmailTemplate emailTemplate = emailTemplateConvert.insertDTOToEntity(insertEmailTemplateDTO);
		baseMapper.insert(emailTemplate);
		return emailTemplate.getEmailTemplateId();
	}

	@Override
	public void updateEmailTemplate(PutEmailTemplateDTO updateEmailTemplateDTO) {
		EmailTemplate emailTemplate = emailTemplateConvert.updateDTOToEntity(updateEmailTemplateDTO);
		baseMapper.updateById(emailTemplate);
	}

	@Override
	public void deleteEmailTemplate(Long emailTemplateId) {
		baseMapper.deleteById(emailTemplateId);
	}

	@Override
	public void deleteEmailTemplate(List<Long> emailTemplateIdList) {
		for (Long emailTemplateId : emailTemplateIdList) {
			deleteEmailTemplate(emailTemplateId);
		}

	}
	

	@Override
	public Long getDailyEmailQuota() {
		// 1.從Redis中查看本日信件餘額
		RAtomicLong quota = redissonClient.getAtomicLong(DAILY_EMAIL_QUOTA_KEY);
		long currentQuota = quota.get();

		// 2.查詢本日的排程信件
		int pendingExpectedEmailVolumeByToday = scheduleEmailTaskService.getPendingExpectedEmailVolumeByToday();

		// 3.本日信件餘額 - 本日排程信件 ， 才是今日真實餘額
		return currentQuota - pendingExpectedEmailVolumeByToday;
	}

	@Override
	public void scheduleEmail(List<Long> tagIdList, SendEmailDTO sendEmailDTO) {

		// 1.校驗tagId列表,拿到tagType
		TagTypeEnum tagTypeEnum = tagService.validateAndGetTagType(tagIdList);

		// 2.根據tagType拿到MailStrategy
		MailStrategy mailStrategy = strategyMap.get(tagTypeEnum.getMailStrategy());

		// 3.使用符合的策略去 排程寄信
		mailStrategy.scheduleEmail(tagIdList, sendEmailDTO);
	}

	@Override
	public void sendEmail(List<Long> tagIdList, SendEmailDTO sendEmailDTO) {

		// 1.校驗tagId列表,拿到tagType
		TagTypeEnum tagTypeEnum = tagService.validateAndGetTagType(tagIdList);

		// 2.根據tagType拿到MailStrategy
		MailStrategy mailStrategy = strategyMap.get(tagTypeEnum.getMailStrategy());

		// 3.使用符合的策略去 立刻寄信
		mailStrategy.batchSendEmail(tagIdList, sendEmailDTO);
	}



}
