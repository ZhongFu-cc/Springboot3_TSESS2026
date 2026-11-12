package tw.org.tsess.manager;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import tw.org.tsess.constants.OrderConstants;
import tw.org.tsess.enums.OrderStatusEnum;
import tw.org.tsess.exception.MemberException;
import tw.org.tsess.pojo.BO.MembershipDueYearBO;
import tw.org.tsess.pojo.BO.ReceiptBO;
import tw.org.tsess.pojo.entity.Attendees;
import tw.org.tsess.pojo.entity.Member;
import tw.org.tsess.pojo.entity.MembershipFeeDue;
import tw.org.tsess.pojo.entity.Orders;
import tw.org.tsess.pojo.entity.OrdersItem;
import tw.org.tsess.pojo.entity.Payment;
import tw.org.tsess.service.AttendeesService;
import tw.org.tsess.service.MemberService;
import tw.org.tsess.service.MembershipFeeDueService;
import tw.org.tsess.service.OrdersItemService;
import tw.org.tsess.service.OrdersService;
import tw.org.tsess.service.PaymentService;
import tw.org.tsess.utils.RocDateUtil;

/**
 * 學會收據 (存根聯 / 收執聯) 的產生<br>
 * 負責把 orders / orders_item 翻譯成收據上的每一個空格, 再交給 Jasper 套版輸出 PDF
 * <p>
 * 收據與 Invoice 的差別:<br>
 * 1. 收據是中文、新台幣, 不做匯率換算<br>
 * 2. 收據是逐「項目」分列 (報名費 / 常年會費), 不是逐筆明細列表, 所以沒有子報表<br>
 * 3. 收據的日期是民國年, 取訂單完成 (付款成功) 的日期
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MemberReceiptManager {

	// 學會收據 Template Path
	private final String RECEIPT_TEMPLATE_PATH = "jasperTemplate/TSESS_invoice_Counterfoil.jasper";

	/**
	 * 收據底版圖的 classpath 位置<br>
	 * 模板的 $P{BG_IMAGE} 宣告成 String , 不是 InputStream , 所以這裡傳「位置字串」,
	 * Jasper 會依序以 檔案 → URL → classpath 去找, 打包成 jar 之後仍讀得到
	 */
	private final String RECEIPT_TEMPLATE_BG_PATH = "jasperTemplate/TSESS_Receipt_Customer-Copy.png";

	private final MemberService memberService;
	private final AttendeesService attendeesService;
	private final OrdersService ordersService;
	private final OrdersItemService ordersItemService;
	private final MembershipFeeDueService membershipFeeDueService;
	private final PaymentService paymentService;

	/**
	 * 產生學會收據 PDF<br>
	 * 底版圖是整張 A4 的掃描圖 (存根聯 + 收執聯各半), 上下兩聯內容相同,
	 * 模板那邊同一組 $P{} 擺兩次即可, 這裡不需要為了兩聯多準備資料
	 *
	 * @param response
	 * @param memberId
	 * @throws IOException
	 */
	public void generateReceipt(HttpServletResponse response, Long memberId) throws IOException {

		// 1.引入收據的 Jasper文件(模板)
		Resource resource = new ClassPathResource(RECEIPT_TEMPLATE_PATH);
		InputStream mainInputStream = resource.getInputStream();

		// 2.透過response得到響應輸出流,不做設置直接響應
		ServletOutputStream outputStream = response.getOutputStream();

		// 3.準備資料,製作收據 PDF
		try {

			// 3-1 組出收據的全部欄位, key 即為模板的 $P{} 名稱
			Map<String, Object> parameters = this.buildReceiptParameters(memberId);

			// 3-2 底版圖片, 模板的 $P{BG_IMAGE} 是 String , 傳位置字串即可, 不是串流
			parameters.put("BG_IMAGE", RECEIPT_TEMPLATE_BG_PATH);

			/**
			 * 3-3 填充報表
			 *
			 * 務必!!要以三個參數來創建,儘管第三個參數數據源為空,不填寫編譯時也不會報錯,但最終PDF數據都會為空
			 * 第一個參數為: 文件輸入流 InputStream , 準確來說是 主報表 .jasper文件
			 * 第二個參數為: Map對象 向模板中輸入的參數 $P{} ,
			 * 通常是String、InputStream、List、Set這類的,SubReport常搭配List、Set使用
			 * 第三個參數為: JasperDataSource 數據源(和Mysql數據源不同,這代表的是要填入的數據) , $F{}
			 * 收據全部欄位都走 $P{} , 沒有逐筆明細, 所以用 JREmptyDataSource()
			 *
			 */
			JasperPrint print = JasperFillManager.fillReport(mainInputStream, parameters, new JREmptyDataSource());

			// 3-4.將JasperPrint以PDF形式輸出
			// 透過JasperExportManager工具類使用exportReportToPdfStream
			// 傳遞第一個參數JasperPrint對象
			// 傳遞第二個參數outputStream
			JasperExportManager.exportReportToPdfStream(print, outputStream);

		} catch (JRException e) {
			log.error(e.getMessage());
			e.printStackTrace();

		} finally {
			// 最終關閉這個響應輸出流 , 以及輸入流
			outputStream.close();
			mainInputStream.close();
		}

	}

	/**
	 * 組出一張收據所需的全部資料<br>
	 * 以會員的「註冊費訂單」為來源, 未付款的訂單不給收據
	 *
	 * @param memberId
	 * @return
	 */
	public ReceiptBO buildReceipt(Long memberId) {

		// 1.拿到會員的註冊費訂單, 並確認已付款完成, 未付款不給收據
		Orders order = ordersService.getRegistrationOrderByMemberId(memberId);
		if (order == null) {
			throw new MemberException("查無此會員的註冊費訂單 , 不給予收據");
		}
		if (!OrderStatusEnum.PAYMENT_SUCCESS.getValue().equals(order.getStatus())) {
			throw new MemberException("會員尚未完成付款 , 不給予收據");
		}

		// 2.拿到會員本人資料
		Member member = memberService.getMember(memberId);

		// 3.拿到這張訂單的所有明細
		List<OrdersItem> ordersItemList = ordersItemService.getOrdersItemsByOrderId(order.getOrdersId());

		ReceiptBO receipt = new ReceiptBO();
		receipt.setOrdersId(order.getOrdersId());

		// 4.收據號碼取自與會者資料, 由學會用 Excel 模板批量匯入,
		// 還沒匯入的人這格會留白
		Attendees attendees = attendeesService.getAttendeesByMemberId(memberId);

		receipt.setReceiptNo(attendees == null ? null : StringUtils.trimToNull(attendees.getReceiptNo()));

		// 5.繳款人姓名, 收據是中文版面, 優先用中文姓名, 外籍會員沒有中文名才退回英文姓名
		receipt.setPayerName(this.resolvePayerName(member));

		// 6.會員編號取自學會匯入的常年會費欠繳名單, 系統的 member table 本身沒有這個欄位
		MembershipFeeDue membershipFeeDue = membershipFeeDueService.getByIdCard(member.getIdCard());
		receipt.setMemberNo(membershipFeeDue == null ? null : membershipFeeDue.getMemberNo());

		// 7.統一編號 與 收據抬頭, 會員報名時自行填寫, 沒填就留白
		receipt.setTaxId(StringUtils.trimToNull(member.getTaxId()));
		receipt.setReceiptTitle(StringUtils.trimToNull(member.getReceipt()));

		// 8.課程/活動報名費, 個人報名與團體報名的明細都算在這一列
		receipt.setRegistrationFee(this.sumRegistrationFee(ordersItemList));

		// 9.常年會費, 逐年一筆, 收據上的「____年度常年會費」只有一格,
		// 因此同時給「逐年明細」與「年度字串 + 總額」, 模板要列幾行自己決定
		List<MembershipDueYearBO> membershipDueYears = this.resolveMembershipDueYears(ordersItemList);
		receipt.setMembershipDueYears(membershipDueYears);
		receipt.setMembershipDueYearText(this.toMembershipDueYearText(membershipDueYears));
		receipt.setMembershipDueAmount(this.sumMembershipDue(membershipDueYears));

		// 10.共計, 一律由全部明細的小計加總, 不直接讀 orders.total_amount,
		// 注意這是「整張訂單」的總額, 若日後出現報名費與常年會費以外的品項,
		// 收據上沒有欄位可放, 但仍然會被算進共計裡
		BigDecimal totalAmount = this.sumSubtotal(ordersItemList);
		receipt.setTotalAmount(totalAmount == null ? BigDecimal.ZERO : totalAmount);

		// 11.訂單完成日期 → 中華民國 XXX 年 XX 月 XX 日 , 年月日各自一格
		LocalDate paidDate = this.resolvePaidDate(order);
		receipt.setPaidDate(paidDate);

		String[] rocDateParts = RocDateUtil.splitRocDate(paidDate);
		receipt.setPaidRocYear(rocDateParts[0]);
		receipt.setPaidMonth(rocDateParts[1]);
		receipt.setPaidDay(rocDateParts[2]);

		return receipt;
	}

	/**
	 * 收據的 Jasper 參數 Map<br>
	 * key 就是模板裡的 $P{} 名稱, 一律大寫底線, 模板只要宣告同名參數即可,
	 * 不需要資料源 (用 JREmptyDataSource)
	 * <p>
	 * 可用的 key:<br>
	 * RECEIPT_NO / PAYER_NAME / MEMBER_NO / TAX_ID<br>
	 * REGISTRATION_FEE / MEMBERSHIP_DUE_YEAR_TEXT / MEMBERSHIP_DUE_AMOUNT /
	 * TOTAL_AMOUNT<br>
	 * PAID_ROC_YEAR / PAID_MONTH / PAID_DAY
	 * <p>
	 * 收據上沒有「入會費」「其他 (Other)」「逐年常年會費」的位置, 因此不提供對應參數,
	 * 多年度一律併成 MEMBERSHIP_DUE_YEAR_TEXT 一格;<br>
	 * 日期是年月日三個獨立空格, 底版上「中華民國」四個字已經印好, 不需要整串的參數
	 * <p>
	 * 底版圖 BG 與子報表 SUB_REPORT 是 InputStream , 由產生 PDF 的方法自己 put ,
	 * 這裡不處理, 避免資料組裝跟串流開關混在一起
	 *
	 * @param memberId
	 * @return
	 */
	public Map<String, Object> buildReceiptParameters(Long memberId) {

		ReceiptBO receipt = this.buildReceipt(memberId);

		Map<String, Object> parameters = new HashMap<>();

		// 字串欄位一律以空字串代替 null , 模板的 textField 沒設 blankWhenNull 時,
		// 傳 null 會在收據上印出 "null" 四個字
		parameters.put("RECEIPT_NO", this.blankIfNull(receipt.getReceiptNo()));
		parameters.put("PAYER_NAME", this.blankIfNull(receipt.getPayerName()));
		parameters.put("MEMBER_NO", this.blankIfNull(receipt.getMemberNo()));
		parameters.put("TAX_ID", this.blankIfNull(receipt.getTaxId()));
		parameters.put("MEMBERSHIP_DUE_YEAR_TEXT", this.blankIfNull(receipt.getMembershipDueYearText()));

		// 金額一律去掉小數位, 收據上不印 .00
		parameters.put("REGISTRATION_FEE", this.toWholeAmount(receipt.getRegistrationFee()));
		parameters.put("MEMBERSHIP_DUE_AMOUNT", this.toWholeAmount(receipt.getMembershipDueAmount()));
		parameters.put("TOTAL_AMOUNT", this.toWholeAmount(receipt.getTotalAmount()));

		parameters.put("PAID_ROC_YEAR", this.blankIfNull(receipt.getPaidRocYear()));
		parameters.put("PAID_MONTH", this.blankIfNull(receipt.getPaidMonth()));
		parameters.put("PAID_DAY", this.blankIfNull(receipt.getPaidDay()));

		return parameters;
	}

	/**
	 * null 轉成空字串, 讓收據上該格留白而不是印出 "null"
	 *
	 * @param value
	 * @return
	 */
	private String blankIfNull(String value) {
		return value == null ? "" : value;
	}

	/**
	 * 金額去掉小數位<br>
	 * DB 的金額欄位是 decimal , 取出來會是 4000.00 , 收據上只要整數,
	 * 學會的費用都是整百整千, 不會有實際的小數, 這裡用四捨五入保險
	 * <p>
	 * 金額為 null 時回傳 null , 代表該項沒有費用, 模板需設 blankWhenNull 才不會印出 "null"
	 *
	 * @param amount
	 * @return
	 */
	private BigDecimal toWholeAmount(BigDecimal amount) {
		return amount == null ? null : amount.setScale(0, RoundingMode.HALF_UP);
	}

	/**
	 * 繳款人姓名<br>
	 * 收據是中文版面, 優先中文姓名, 沒有才用 firstName + lastName
	 *
	 * @param member
	 * @return
	 */
	private String resolvePayerName(Member member) {

		String chineseName = StringUtils.trimToEmpty(member.getChineseName());
		if (StringUtils.isNotBlank(chineseName)) {
			return chineseName;
		}

		return Stream.of(StringUtils.trimToEmpty(member.getFirstName()), StringUtils.trimToEmpty(member.getLastName()))
				.filter(StringUtils::isNotBlank)
				.collect(Collectors.joining(" "));
	}

	/**
	 * 課程/活動報名費, 只認 product_type 為 "Registration Fee" 的明細<br>
	 * 團體報名的 "Group Registration Fee" 不算在這一列<br>
	 * 訂單中沒有這類明細時回傳 null , 回 null 而不是 0 ,
	 * 是為了讓模板能判斷「這一列要不要打勾」
	 *
	 * @param ordersItemList
	 * @return
	 */
	private BigDecimal sumRegistrationFee(List<OrdersItem> ordersItemList) {
		return this.sumSubtotal(ordersItemList.stream()
				.filter(ordersItem -> OrderConstants.ITEMS_SUMMARY_REGISTRATION.equals(ordersItem.getProductType()))
				.toList());
	}

	/**
	 * 從訂單明細還原出補繳的各年度常年會費<br>
	 * 以 orders_item.product_type (例如 "常年會費 113") 解析年度, 只保留金額大於 0 的年度,
	 * 年度由小到大排序
	 *
	 * @param ordersItemList
	 * @return 沒有補繳時回傳空 List
	 */
	private List<MembershipDueYearBO> resolveMembershipDueYears(List<OrdersItem> ordersItemList) {

		List<MembershipDueYearBO> membershipDueYears = new ArrayList<>();

		for (OrdersItem ordersItem : ordersItemList) {

			Integer rocYear = OrderConstants.parseMembershipDueRocYear(ordersItem.getProductType());
			if (rocYear == null) {
				continue;
			}

			BigDecimal subtotal = ordersItem.getSubtotal();
			if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}

			membershipDueYears.add(new MembershipDueYearBO(rocYear, RocDateUtil.toAdYear(rocYear), subtotal));
		}

		membershipDueYears.sort((a, b) -> a.getRocYear().compareTo(b.getRocYear()));

		return membershipDueYears;
	}

	/**
	 * 常年會費的年度字串<br>
	 * 收據上「____年度常年會費」只有一格, 多個年度就以頓號相連, 例如 "113、114"
	 *
	 * @param membershipDueYears
	 * @return 沒有補繳時回傳空字串
	 */
	private String toMembershipDueYearText(List<MembershipDueYearBO> membershipDueYears) {
		return membershipDueYears.stream()
				.map(membershipDueYear -> String.valueOf(membershipDueYear.getRocYear()))
				.collect(Collectors.joining("、"));
	}

	/**
	 * 常年會費總額, 為各年度加總, 沒有補繳時回傳 null
	 *
	 * @param membershipDueYears
	 * @return
	 */
	private BigDecimal sumMembershipDue(List<MembershipDueYearBO> membershipDueYears) {

		if (membershipDueYears.isEmpty()) {
			return null;
		}

		return membershipDueYears.stream()
				.map(MembershipDueYearBO::getAmount)
				.filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/**
	 * 明細小計加總, 清單為空時回傳 null<br>
	 * 回 null 的用意同 {@link #sumRegistrationFee(java.util.List)} , 讓模板判斷要不要印
	 *
	 * @param ordersItemList
	 * @return
	 */
	private BigDecimal sumSubtotal(List<OrdersItem> ordersItemList) {

		if (ordersItemList == null || ordersItemList.isEmpty()) {
			return null;
		}

		return ordersItemList.stream()
				.map(OrdersItem::getSubtotal)
				.filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/**
	 * 訂單完成的日期<br>
	 * 1. 優先取綠界的付款日期 (payment.payment_date) , 這是真正的收款日<br>
	 * 2. 人工確認收款 (匯款) 沒有綠界紀錄, 退而用訂單最後修改時間, 也就是狀態被改成付款完成的時間<br>
	 * 3. 兩者都沒有才用訂單建立時間
	 *
	 * @param order
	 * @return
	 */
	private LocalDate resolvePaidDate(Orders order) {

		Payment payment = paymentService.getLatestSuccessPaymentByOrdersId(order.getOrdersId());
		if (payment != null && payment.getPaymentDate() != null) {
			return payment.getPaymentDate().toLocalDate();
		}

		if (order.getUpdateDate() != null) {
			return order.getUpdateDate().toLocalDate();
		}

		if (order.getCreateDate() != null) {
			return order.getCreateDate().toLocalDate();
		}

		log.warn("訂單 {} 無法取得完成日期 , 收據日期將留白", order.getOrdersId());
		return null;
	}

}
