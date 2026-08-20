package tw.org.tsess.pojo.BO;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 註冊費訂單的計算草稿<br>
 * 尚未落庫, 描述「這位會員這次報名要付哪些錢」
 */
@Data
public class OrderDraftBO {

	@Schema(description = "訂單明細, 金額為 0 的項目不會產生明細")
	private List<OrderLineBO> lines;

	@Schema(description = "訂單總金額, 即各明細小計加總")
	private BigDecimal totalAmount;

	@Schema(description = "其中的註冊費金額, 供 Excel 匯出拆欄使用")
	private BigDecimal registrationFee;

	@Schema(description = "其中的常年會費補繳金額, 為各年度加總, 供 Excel 匯出拆欄使用")
	private BigDecimal membershipDue;

	@Schema(description = "常年會費補繳的分年明細, 名單中查無此人時為空 List")
	private List<MembershipDueYearBO> membershipDueYears;

	/**
	 * 這次報名是否完全免費<br>
	 * 注意: 註冊費為 0 但有欠繳常年會費時, 這裡會是 false, 訂單仍需付款
	 *
	 * @return
	 */
	public boolean isFree() {
		return totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) == 0;
	}

}
