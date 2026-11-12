package tw.org.tsess.pojo.BO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 學會收據 (存根聯 / 收執聯) 的資料<br>
 * 對應紙本收據上的每一個空格, 一個欄位一個空格, Jasper 模板只要把底版圖當背景, 再把這些值擺上去即可
 * <p>
 * 金額一律為新台幣, 不做任何匯率換算 (Invoice 才需要美金)<br>
 * 收據上沒有勾到的項目金額會是 null , 模板可以用 null 判斷那一列要不要打勾、要不要印金額
 */
@Data
public class ReceiptBO {

	@Schema(description = "收據號碼, 取自 attendees.receipt_no (學會以 Excel 批量匯入), 未匯入則為 null")
	private String receiptNo;

	@Schema(description = "訂單ID, 供後台對帳用, 不印在收據上")
	private Long ordersId;

	@Schema(description = "繳款人姓名, 印在「茲收到 ____ 醫師」, 優先中文姓名, 沒有才用英文姓名")
	private String payerName;

	@Schema(description = "會員編號, 取自常年會費欠繳名單 (membership_fee_due.member_no), 查無則為 null")
	private String memberNo;

	@Schema(description = "統一編號 (抬頭統編), 取自 member.tax_id, 未填則為 null")
	private String taxId;

	@Schema(description = "收據抬頭, 取自 member.receipt, 未填則為 null")
	private String receiptTitle;

	@Schema(description = "課程/活動報名費, 訂單中沒有這筆明細時為 null")
	private BigDecimal registrationFee;

	@Schema(description = "常年會費各年度明細, 逐年一筆, 只放金額大於 0 的年度, 沒有則為空 List")
	private List<MembershipDueYearBO> membershipDueYears;

	@Schema(description = "常年會費年度字串, 多個年度以頓號相連, 例如 \"113、114\", 沒有則為空字串")
	private String membershipDueYearText;

	@Schema(description = "常年會費總額, 為各年度加總, 沒有補繳則為 null")
	private BigDecimal membershipDueAmount;

	@Schema(description = "共計金額 (新台幣), 為訂單明細小計總和")
	private BigDecimal totalAmount;

	@Schema(description = "訂單完成 (付款成功) 日期, 無法取得時為 null")
	private LocalDate paidDate;

	@Schema(description = "訂單完成日期的民國年, 字串, 例如 \"115\"")
	private String paidRocYear;

	@Schema(description = "訂單完成日期的月份, 兩位數字串, 例如 \"03\"")
	private String paidMonth;

	@Schema(description = "訂單完成日期的日, 兩位數字串, 例如 \"05\"")
	private String paidDay;

}
