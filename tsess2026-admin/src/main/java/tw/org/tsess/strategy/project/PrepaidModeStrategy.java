package tw.org.tsess.strategy.project;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.org.tsess.constants.I18nMessageKey;
import tw.org.tsess.enums.OrderStatusEnum;
import tw.org.tsess.exception.PaperClosedException;
import tw.org.tsess.helper.MessageHelper;
import tw.org.tsess.helper.TagAssignmentHelper;
import tw.org.tsess.manager.RegistrationOrderCalculator;
import tw.org.tsess.pojo.BO.OrderDraftBO;
import tw.org.tsess.pojo.DTO.EmailBodyContent;
import tw.org.tsess.pojo.entity.Member;
import tw.org.tsess.pojo.entity.Orders;
import tw.org.tsess.service.AsyncService;
import tw.org.tsess.service.MemberTagService;
import tw.org.tsess.service.NotificationService;
import tw.org.tsess.service.OrdersService;
import tw.org.tsess.service.TagService;

@Component
@RequiredArgsConstructor
public class PrepaidModeStrategy implements ProjectModeStrategy {

	@Value("${project.name}")
	private String PROJECT_NAME;

	@Value("${project.banner-url}")
	private String BANNER_PHOTO_URL;

	@Value("${project.group-size}")
	private int GROUP_SIZE;

	// 註冊費與常年會費的計算已抽到 RegistrationOrderCalculator , 與 PostpaidModeStrategy 共用
	private final RegistrationOrderCalculator registrationOrderCalculator;

	private final MessageHelper messageHelper;
	private final TagAssignmentHelper tagAssignmentHelper;
	private final MemberTagService memberTagService;
	private final TagService tagService;
	private final OrdersService ordersService;
	private final NotificationService notificationService;
	private final AsyncService asyncService;

	/**
	 * 預付款模式<br>
	 * 計算會員的註冊費，建立訂單，並寄信通知
	 * 
	 */
	@Override
	public void handleRegistration(Member member) {

		// 1.計算註冊費, 並比對常年會費欠繳名單, 產生訂單草稿(含明細)
		OrderDraftBO draft = registrationOrderCalculator.calculate(member);

		// 2.總額為0才是真正免費 , 註冊費0但有欠繳常年會費者仍需付款
		if (draft.isFree()) {
			ordersService.createFreeRegistrationOrder(member);
		} else {
			// 創建付費註冊費訂單
			ordersService.createRegistrationOrder(draft, member);
			// 獲取當下「未付款」的Member群體的Index，賦予「未繳費」標籤
			tagAssignmentHelper.assignTag(member.getMemberId(), ordersService::getNotPaidRegistrationOrderGroupIndex,
					tagService::getOrCreateNotPaidGroupTag, memberTagService::addMemberTag);
		}

		// 3.創建註冊成功通知信件內容
		EmailBodyContent registrationSuccessContent = notificationService.generateRegistrationSuccessContent(member,
				BANNER_PHOTO_URL);

		// 4.異步寄送信件
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
		//「先付費」 模式,需要先判斷他有沒有繳錢

		// 1.查詢註冊費訂單,個人和團體報名都算
		Orders registrationOrder = ordersService.getRegistrationOrderByMemberId(memberId);

		// 2.如果這個訂單 沒有付款成功, 那直接報錯,請他先去付費
		if (!OrderStatusEnum.PAYMENT_SUCCESS.getValue().equals(registrationOrder.getStatus())) {
			throw new PaperClosedException(messageHelper.get(I18nMessageKey.Paper.PREPAID));
		}

	}

}
