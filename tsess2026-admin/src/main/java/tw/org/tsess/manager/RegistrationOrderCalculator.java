package tw.org.tsess.manager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.org.tsess.config.RegistrationFeeConfig;
import tw.org.tsess.constants.OrderConstants;
import tw.org.tsess.enums.MemberCategoryEnum;
import tw.org.tsess.enums.RegistrationPhaseEnum;
import tw.org.tsess.pojo.BO.OrderDraftBO;
import tw.org.tsess.pojo.BO.OrderLineBO;
import tw.org.tsess.pojo.entity.Member;
import tw.org.tsess.service.MembershipFeeDueService;
import tw.org.tsess.service.SettingService;
import tw.org.tsess.utils.CountryUtil;

/**
 * 計算個人報名要付的錢, 產生尚未落庫的訂單草稿<br>
 * 由 PrepaidModeStrategy 和 PostpaidModeStrategy 共用, 兩者的費用邏輯相同, 只差付款門檻
 */
@Component
@RequiredArgsConstructor
public class RegistrationOrderCalculator {

	@Value("${project.name}")
	private String PROJECT_NAME;

	private final RegistrationFeeConfig registrationFeeConfig;
	private final SettingService settingService;
	private final MembershipFeeDueService membershipFeeDueService;

	/**
	 * 計算註冊費, 並比對學會的常年會費欠繳名單, 有欠費則一併列入同一張訂單
	 *
	 * @param member
	 * @return
	 */
	public OrderDraftBO calculate(Member member) {

		// 1.拿到配置設定,知道處於哪個註冊階段
		RegistrationPhaseEnum registrationPhaseEnum = settingService.getRegistrationPhaseEnum();

		// 2.透過Country 拿到國籍 , 只分國內國外
		String country = CountryUtil.getTaiwanOrForeign(member.getCountry());

		// 3.拿到身分
		MemberCategoryEnum memberCategoryEnum = MemberCategoryEnum.fromValue(member.getCategory());

		// 4.透過階段、國籍、身分，得到註冊費金額
		BigDecimal registrationFee = registrationFeeConfig.getFee(registrationPhaseEnum.getValue(), country,
				memberCategoryEnum.getConfigKey());

		// 5.以身分證字號比對常年會費欠繳名單, 名單中查無此人則為 0
		BigDecimal membershipDue = membershipFeeDueService.getTotalDueByIdCard(member.getIdCard());

		// 6.組出訂單明細, 金額為 0 的項目不產生明細
		List<OrderLineBO> lines = new ArrayList<>();

		if (registrationFee.compareTo(BigDecimal.ZERO) > 0) {
			lines.add(OrderLineBO.ofSingle(OrderConstants.ITEMS_SUMMARY_REGISTRATION,
					PROJECT_NAME + " " + OrderConstants.ITEMS_SUMMARY_REGISTRATION, registrationFee));
		}

		if (membershipDue.compareTo(BigDecimal.ZERO) > 0) {
			lines.add(OrderLineBO.ofSingle(OrderConstants.ITEMS_TYPE_MEMBERSHIP_DUE,
					OrderConstants.MEMBERSHIP_DUE_PRODUCT_NAME, membershipDue));
		}

		// 7.完全免費時仍要留一筆 0 元註冊費明細, 讓每張訂單都至少有一筆細項
		if (lines.isEmpty()) {
			lines.add(OrderLineBO.ofSingle(OrderConstants.ITEMS_SUMMARY_REGISTRATION,
					PROJECT_NAME + " " + OrderConstants.ITEMS_SUMMARY_REGISTRATION, BigDecimal.ZERO));
		}

		// 8.總金額必為各明細小計加總, 避免表頭與明細對不起來
		BigDecimal totalAmount = lines.stream()
				.map(OrderLineBO::getSubtotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		OrderDraftBO draft = new OrderDraftBO();
		draft.setLines(lines);
		draft.setTotalAmount(totalAmount);
		draft.setRegistrationFee(registrationFee);
		draft.setMembershipDue(membershipDue);

		return draft;
	}

	/**
	 * 後台新增會員 / 現場註冊等免費情境使用的草稿<br>
	 * 不比對常年會費, 維持 0 元已付款訂單的既有行為
	 *
	 * @return
	 */
	public OrderDraftBO freeDraft() {

		List<OrderLineBO> lines = new ArrayList<>();
		lines.add(OrderLineBO.ofSingle(OrderConstants.ITEMS_SUMMARY_REGISTRATION,
				PROJECT_NAME + " " + OrderConstants.ITEMS_SUMMARY_REGISTRATION, BigDecimal.ZERO));

		OrderDraftBO draft = new OrderDraftBO();
		draft.setLines(lines);
		draft.setTotalAmount(BigDecimal.ZERO);
		draft.setRegistrationFee(BigDecimal.ZERO);
		draft.setMembershipDue(BigDecimal.ZERO);

		return draft;
	}

}
