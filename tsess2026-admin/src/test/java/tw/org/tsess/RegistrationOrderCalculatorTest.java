package tw.org.tsess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

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
import tw.org.tsess.enums.MembershipDueYearEnum;
import tw.org.tsess.enums.RegistrationPhaseEnum;
import tw.org.tsess.manager.RegistrationOrderCalculator;
import tw.org.tsess.pojo.BO.MembershipDueYearBO;
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

	/** 設定這次報名算出來的註冊費 與 名單上各年度的欠費 */
	private void given(BigDecimal registrationFee, long due113, long due114, long due115) {
		when(registrationFeeConfig.getFee(anyString(), anyString(), anyString())).thenReturn(registrationFee);
		when(membershipFeeDueService.getYearlyDueByIdCard(ID_CARD)).thenReturn(List.of(
				MembershipDueYearBO.of(MembershipDueYearEnum.ROC_113, BigDecimal.valueOf(due113)),
				MembershipDueYearBO.of(MembershipDueYearEnum.ROC_114, BigDecimal.valueOf(due114)),
				MembershipDueYearBO.of(MembershipDueYearEnum.ROC_115, BigDecimal.valueOf(due115))));
	}

	/** 從明細中找出指定產品類型的那一筆 */
	private OrderLineBO lineOf(OrderDraftBO draft, String productType) {
		return draft.getLines()
				.stream()
				.filter(line -> productType.equals(line.getProductType()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("找不到產品類型為 " + productType + " 的明細"));
	}

	/** 指定年度的會費明細 */
	private OrderLineBO dueLineOf(OrderDraftBO draft, MembershipDueYearEnum year) {
		return lineOf(draft, OrderConstants.membershipDueProductType(year.getAdYear()));
	}

	@Test
	@DisplayName("註冊費>0 且三個年度都有欠費 → 四筆明細, 總額為全部相加")
	void feeAndDue() {

		// 正式會員的實際費率: 113年 2000 / 114年 2000 / 115年 3000
		given(BigDecimal.valueOf(1000), 2000, 2000, 3000);

		OrderDraftBO draft = registrationOrderCalculator.calculate(member);

		assertEquals(4, draft.getLines().size());
		assertEquals(0, BigDecimal.valueOf(8000).compareTo(draft.getTotalAmount()));
		assertEquals(0, BigDecimal.valueOf(1000).compareTo(draft.getRegistrationFee()));
		assertEquals(0, BigDecimal.valueOf(7000).compareTo(draft.getMembershipDue()));
		assertFalse(draft.isFree());

		OrderLineBO registrationLine = lineOf(draft, OrderConstants.ITEMS_SUMMARY_REGISTRATION);
		assertEquals(PROJECT_NAME + " " + OrderConstants.ITEMS_SUMMARY_REGISTRATION, registrationLine.getProductName());
		assertEquals(1, registrationLine.getQuantity());
		assertEquals(0, BigDecimal.valueOf(1000).compareTo(registrationLine.getSubtotal()));

		// 每個年度各自一筆, 品名帶西元年
		assertEquals(0, BigDecimal.valueOf(2000).compareTo(dueLineOf(draft, MembershipDueYearEnum.ROC_113).getSubtotal()));
		assertEquals(0, BigDecimal.valueOf(2000).compareTo(dueLineOf(draft, MembershipDueYearEnum.ROC_114).getSubtotal()));
		assertEquals(0, BigDecimal.valueOf(3000).compareTo(dueLineOf(draft, MembershipDueYearEnum.ROC_115).getSubtotal()));
		assertEquals("Annual Membership Dues (2024)",
				dueLineOf(draft, MembershipDueYearEnum.ROC_113).getProductName());
		assertEquals("Annual Membership Dues (2026)",
				dueLineOf(draft, MembershipDueYearEnum.ROC_115).getProductName());
	}

	@Test
	@DisplayName("只欠其中一個年度 → 只產生該年度一筆會費明細")
	void singleYearDue() {

		// 準會員只欠 115 年度 500 元
		given(BigDecimal.valueOf(1000), 0, 0, 500);

		OrderDraftBO draft = registrationOrderCalculator.calculate(member);

		assertEquals(2, draft.getLines().size());
		assertEquals(0, BigDecimal.valueOf(1500).compareTo(draft.getTotalAmount()));
		assertEquals(0, BigDecimal.valueOf(500).compareTo(draft.getMembershipDue()));

		OrderLineBO dueLine = dueLineOf(draft, MembershipDueYearEnum.ROC_115);
		assertEquals("Annual Membership Dues (2026)", dueLine.getProductName());
		assertEquals(0, BigDecimal.valueOf(500).compareTo(dueLine.getSubtotal()));
	}

	@Test
	@DisplayName("註冊費>0 但無欠費 → 只有一筆註冊費明細")
	void feeOnly() {

		given(BigDecimal.valueOf(4000), 0, 0, 0);

		OrderDraftBO draft = registrationOrderCalculator.calculate(member);

		assertEquals(1, draft.getLines().size());
		assertEquals(OrderConstants.ITEMS_SUMMARY_REGISTRATION, draft.getLines().get(0).getProductType());
		assertEquals(0, BigDecimal.valueOf(4000).compareTo(draft.getTotalAmount()));
		assertFalse(draft.isFree());
	}

	@Test
	@DisplayName("註冊費=0 但有欠費 → 只有會費明細, 且訂單不是免費的")
	void dueOnly() {

		// speaker / staff 這類身分註冊費為 0 , 但仍可能欠常年會費
		member.setCategory(MemberCategoryEnum.SPEAKER.getValue());
		given(BigDecimal.ZERO, 0, 0, 3000);

		OrderDraftBO draft = registrationOrderCalculator.calculate(member);

		assertEquals(1, draft.getLines().size());
		assertEquals(OrderConstants.membershipDueProductType(MembershipDueYearEnum.ROC_115.getAdYear()),
				draft.getLines().get(0).getProductType());
		assertEquals(0, BigDecimal.valueOf(3000).compareTo(draft.getTotalAmount()));

		// 這是最關鍵的分支: 註冊費為0不代表免費, 訂單必須是要付款的
		assertFalse(draft.isFree());
	}

	@Test
	@DisplayName("註冊費=0 且無欠費 → 0元訂單, 仍保留一筆0元明細")
	void allFree() {

		member.setCategory(MemberCategoryEnum.STAFF.getValue());
		given(BigDecimal.ZERO, 0, 0, 0);

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
		when(membershipFeeDueService.getYearlyDueByIdCard("NOT_IN_LIST")).thenReturn(List.of());

		OrderDraftBO draft = registrationOrderCalculator.calculate(member);

		assertEquals(1, draft.getLines().size());
		assertEquals(0, BigDecimal.valueOf(1000).compareTo(draft.getTotalAmount()));
		assertEquals(0, BigDecimal.ZERO.compareTo(draft.getMembershipDue()));
		assertTrue(draft.getMembershipDueYears().isEmpty());
	}

	@Test
	@DisplayName("補繳總額必定等於各年度明細加總")
	void membershipDueMatchesYears() {

		given(BigDecimal.valueOf(1000), 2000, 500, 3000);

		OrderDraftBO draft = registrationOrderCalculator.calculate(member);

		BigDecimal sumOfYears = draft.getMembershipDueYears()
				.stream()
				.map(MembershipDueYearBO::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		assertEquals(0, sumOfYears.compareTo(draft.getMembershipDue()));
		assertEquals(0, BigDecimal.valueOf(5500).compareTo(draft.getMembershipDue()));
	}

	@Test
	@DisplayName("訂單總額必定等於各明細小計加總")
	void totalAlwaysMatchesLines() {

		given(BigDecimal.valueOf(1000), 2000, 500, 3000);

		OrderDraftBO draft = registrationOrderCalculator.calculate(member);

		BigDecimal sumOfLines = draft.getLines()
				.stream()
				.map(OrderLineBO::getSubtotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		assertEquals(0, sumOfLines.compareTo(draft.getTotalAmount()));
	}

}
