package tw.org.tsess.strategy.project;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.org.tsess.config.RegistrationFeeConfig;
import tw.org.tsess.enums.MemberCategoryEnum;
import tw.org.tsess.enums.RegistrationPhaseEnum;
import tw.org.tsess.helper.TagAssignmentHelper;
import tw.org.tsess.pojo.DTO.EmailBodyContent;
import tw.org.tsess.pojo.entity.Member;
import tw.org.tsess.service.AsyncService;
import tw.org.tsess.service.MemberTagService;
import tw.org.tsess.service.NotificationService;
import tw.org.tsess.service.OrdersService;
import tw.org.tsess.service.SettingService;
import tw.org.tsess.service.TagService;
import tw.org.tsess.utils.CountryUtil;

@Component
@RequiredArgsConstructor
public class PostpaidModeStrategy implements ProjectModeStrategy {

	@Value("${project.name}")
	private String PROJECT_NAME;

	@Value("${project.banner-url}")
	private String BANNER_PHOTO_URL;

	@Value("${project.group-size}")
	private int GROUP_SIZE;

	private final RegistrationFeeConfig registrationFeeConfig;
	private final TagAssignmentHelper tagAssignmentHelper;
	private final MemberTagService memberTagService;
	private final TagService tagService;
	private final OrdersService ordersService;
	private final SettingService settingService;
	private final NotificationService notificationService;
	private final AsyncService asyncService;

	@Override
	public void handleRegistration(Member member) {
		// 1.拿到配置設定,知道處於哪個註冊階段
		RegistrationPhaseEnum registrationPhaseEnum = settingService.getRegistrationPhaseEnum();

		// 2.透過Country 拿到國籍 , 只分國內國外,	
		String country = CountryUtil.getTaiwanOrForeign(member.getCountry());

		// 3.拿到身分
		MemberCategoryEnum memberCategoryEnum = MemberCategoryEnum.fromValue(member.getCategory());

		// 4.透過階段、國籍、身分，得到金額
		BigDecimal membershipFee = registrationFeeConfig.getFee(registrationPhaseEnum.getValue(), country,
				memberCategoryEnum.getConfigKey());

		// 5.如果註冊費金額為0 , 創建免費註冊費訂單 , 會自動為繳費完畢的情況
		if (membershipFee.compareTo(BigDecimal.ZERO) == 0) {
			ordersService.createFreeRegistrationOrder(member);
		} else {
			// 創建付費註冊費訂單
			ordersService.createRegistrationOrder(membershipFee, member);
			// 獲取當下「未付款」的Member群體的Index，賦予「未繳費」標籤
			tagAssignmentHelper.assignTag(member.getMemberId(), ordersService::getNotPaidRegistrationOrderGroupIndex,
					tagService::getOrCreateNotPaidGroupTag, memberTagService::addMemberTag);
		}

		// 6.創建註冊成功通知信件內容
		EmailBodyContent registrationSuccessContent = notificationService.generateRegistrationSuccessContent(member,
				BANNER_PHOTO_URL);

		// 7.異步寄送信件
		asyncService.sendCommonEmail(member.getEmail(), PROJECT_NAME + " Registration Successful",
				registrationSuccessContent.getHtmlContent(), registrationSuccessContent.getPlainTextContent());

	}

	@Override
	public void handleGroupRegistration(Member member, boolean isMaster, BigDecimal totalFee) {
		if (isMaster) {
			// Master 負責付錢
			ordersService.createGroupRegistrationOrder(totalFee, member);
			// 獲取當下「未付款」的Member群體的Index，賦予「未繳費」標籤
			tagAssignmentHelper.assignTag(member.getMemberId(), ordersService::getNotPaidRegistrationOrderGroupIndex,
					tagService::getOrCreateNotPaidGroupTag, memberTagService::addMemberTag);
		} else {
			// Slave 不付錢，0元訂單，未付款
			ordersService.createFreeGroupRegistrationOrder(member);
			// 獲取當下「未付款」的Member群體的Index，賦予「未繳費」標籤
			tagAssignmentHelper.assignTag(member.getMemberId(), ordersService::getNotPaidRegistrationOrderGroupIndex,
					tagService::getOrCreateNotPaidGroupTag, memberTagService::addMemberTag);
		}

		// 2.產生系統團體報名通知信
		EmailBodyContent groupRegistrationSuccessContent = notificationService
				.generateGroupRegistrationSuccessContent(member, BANNER_PHOTO_URL);

		// 3.寄信個別通知會員，團體報名成功
		asyncService.sendCommonEmail(member.getEmail(), PROJECT_NAME + " GROUP Registration Successful",
				groupRegistrationSuccessContent.getHtmlContent(),
				groupRegistrationSuccessContent.getPlainTextContent());

	}

	@Override
	public void handlePaperSubmission(Long memberId) {
		// 「後付費」 模式,不用去攔截他投稿，但是注意最終是否能發表則是看有沒有繳註冊費

	}

}
