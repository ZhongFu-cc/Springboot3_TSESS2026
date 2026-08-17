package tw.org.tsess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import tw.org.tsess.config.RegistrationFeeConfig;
import tw.org.tsess.constants.OrderConstants;
import tw.org.tsess.enums.MemberCategoryEnum;
import tw.org.tsess.enums.RegistrationPhaseEnum;
import tw.org.tsess.manager.RegistrationOrderCalculator;
import tw.org.tsess.pojo.BO.OrderDraftBO;
import tw.org.tsess.pojo.BO.OrderLineBO;
import tw.org.tsess.pojo.entity.Member;
import tw.org.tsess.service.MembershipFeeDueService;
import tw.org.tsess.service.SettingService;

/**
 * 註冊費 + 補繳常年會費 的計算<br>
 * 純 Mockito 單元測試, 不啟動 Spring 容器, 因此不需要 DB / Redis
 */
@ExtendWith(MockitoExtension.class)
public class RegistrationOrderCalculatorTest {

	private static final String PROJECT_NAME = "TSESS 2026";
	private static final String ID_CARD = "A123456789";

	@Mock
	private RegistrationFeeConfig registrationFeeConfig;

	@Mock
	private SettingService settingService;

	@Mock
	private MembershipFeeDueService membershipFeeDueService;

	@InjectMocks
	private RegistrationOrderCalculator registrationOrderCalculator;

	private Member member;

	@BeforeEach
	void setUp() {
		// @Value 注入的欄位在單元測試中要自行塞入
		ReflectionTestUtils.setField(registrationOrderCalculator, "PROJECT_NAME", PROJECT_NAME);

		member = new Member();
		member.setMemberId(1L);
		member.setIdCard(ID_CARD);
		member.setCountry("Taiwan");
		member.setCategory(MemberCategoryEnum.MEMBER.getValue());

		when(settingService.getRegistrationPhaseEnum()).thenReturn(RegistrationPhaseEnum.REGULAR);
	}

	/** 設定這次報名算出來的註冊費 與 名單上的欠費 */
	private void given(BigDecimal registrationFee, BigDecimal membershipDue) {
		when(registrationFeeConfig.getFee(anyString(), anyString(), anyString())).thenReturn(registrationFee);
		when(membershipFeeDueService.getTotalDueByIdCard(ID_CARD)).thenReturn(membershipDue);
	}

	/** 從明細中找出指定產品類型的那一筆 */
	private OrderLineBO lineOf(OrderDraftBO draft, String productType) {
		return draft.getLines()
				.stream()
				.filter(line -> productType.equals(line.getProductType()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("找不到產品類型為 " + productType + " 的明細"));
	}

	@Test
	@DisplayName("註冊費>0 且有欠費 → 兩筆明細, 總額為兩者相加")
	void feeAndDue() {

		given(BigDecimal.valueOf(1000), BigDecimal.valueOf(7000));

		OrderDraftBO draft = registrationOrderCalculator.calculate(member);

		assertEquals(2, draft.getLines().size());
		assertEquals(0, BigDecimal.valueOf(8000).compareTo(draft.getTotalAmount()));
		assertEquals(0, BigDecimal.valueOf(1000).compareTo(draft.getRegistrationFee()));
		assertEquals(0, BigDecimal.valueOf(7000).compareTo(draft.getMembershipDue()));
		assertFalse(draft.isFree());

		OrderLineBO registrationLine = lineOf(draft, OrderConstants.ITEMS_SUMMARY_REGISTRATION);
		assertEquals(PROJECT_NAME + " " + OrderConstants.ITEMS_SUMMARY_REGISTRATION, registrationLine.getProductName());
		assertEquals(1, registrationLine.getQuantity());
		assertEquals(0, BigDecimal.valueOf(1000).compareTo(registrationLine.getSubtotal()));

		OrderLineBO dueLine = lineOf(draft, OrderConstants.ITEMS_TYPE_MEMBERSHIP_DUE);
		assertEquals(OrderConstants.MEMBERSHIP_DUE_PRODUCT_NAME, dueLine.getProductName());
		assertEquals(0, BigDecimal.valueOf(7000).compareTo(dueLine.getSubtotal()));
	}

	@Test
	@DisplayName("註冊費>0 但無欠費 → 只有一筆註冊費明細")
	void feeOnly() {

		given(BigDecimal.valueOf(4000), BigDecimal.ZERO);

		OrderDraftBO draft = registrationOrderCalculator.calculate(member);

		assertEquals(1, draft.getLines().size());
		assertEquals(OrderConstants.ITEMS_SUMMARY_REGISTRATION, draft.getLines().get(0).getProductType());
		assertEquals(0, BigDecimal.valueOf(4000).compareTo(draft.getTotalAmount()));
		assertFalse(draft.isFree());
	}

	@Test
	@DisplayName("註冊費=0 但有欠費 → 只有一筆會費明細, 且訂單不是免費的")
	void dueOnly() {

		// speaker / staff 這類身分註冊費為 0 , 但仍可能欠常年會費
		member.setCategory(MemberCategoryEnum.SPEAKER.getValue());
		given(BigDecimal.ZERO, BigDecimal.valueOf(3000));

		OrderDraftBO draft = registrationOrderCalculator.calculate(member);

		assertEquals(1, draft.getLines().size());
		assertEquals(OrderConstants.ITEMS_TYPE_MEMBERSHIP_DUE, draft.getLines().get(0).getProductType());
		assertEquals(0, BigDecimal.valueOf(3000).compareTo(draft.getTotalAmount()));

		// 這是最關鍵的分支: 註冊費為0不代表免費, 訂單必須是要付款的
		assertFalse(draft.isFree());
	}

	@Test
	@DisplayName("註冊費=0 且無欠費 → 0元訂單, 仍保留一筆0元明細")
	void allFree() {

		member.setCategory(MemberCategoryEnum.STAFF.getValue());
		given(BigDecimal.ZERO, BigDecimal.ZERO);

		OrderDraftBO draft = registrationOrderCalculator.calculate(member);

		assertEquals(1, draft.getLines().size());
		assertEquals(OrderConstants.ITEMS_SUMMARY_REGISTRATION, draft.getLines().get(0).getProductType());
		assertEquals(0, BigDecimal.ZERO.compareTo(draft.getTotalAmount()));
		assertTrue(draft.isFree());
	}

	@Test
	@DisplayName("名單中查無此人 → 視為無欠費")
	void notInDueList() {

		member.setIdCard("NOT_IN_LIST");
		when(registrationFeeConfig.getFee(anyString(), anyString(), anyString())).thenReturn(BigDecimal.valueOf(1000));
		when(membershipFeeDueService.getTotalDueByIdCard("NOT_IN_LIST")).thenReturn(BigDecimal.ZERO);

		OrderDraftBO draft = registrationOrderCalculator.calculate(member);

		assertEquals(1, draft.getLines().size());
		assertEquals(0, BigDecimal.valueOf(1000).compareTo(draft.getTotalAmount()));
		assertEquals(0, BigDecimal.ZERO.compareTo(draft.getMembershipDue()));
	}

	@Test
	@DisplayName("訂單總額必定等於各明細小計加總")
	void totalAlwaysMatchesLines() {

		given(BigDecimal.valueOf(1000), BigDecimal.valueOf(5500));

		OrderDraftBO draft = registrationOrderCalculator.calculate(member);

		BigDecimal sumOfLines = draft.getLines()
				.stream()
				.map(OrderLineBO::getSubtotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		assertEquals(0, sumOfLines.compareTo(draft.getTotalAmount()));
	}

}
