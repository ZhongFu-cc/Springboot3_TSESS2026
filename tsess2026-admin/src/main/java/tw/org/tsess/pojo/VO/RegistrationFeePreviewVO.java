package tw.org.tsess.pojo.VO;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tw.org.tsess.pojo.BO.MembershipDueYearBO;

/**
 * 註冊前的費用試算結果<br>
 * 與正式註冊時建立的訂單使用同一份計算邏輯, 但不落庫
 */
@Data
public class RegistrationFeePreviewVO {

	@Schema(description = "報名費")
	private BigDecimal registrationFee;

	@Schema(description = "補繳的常年會費, 為各年度加總, 不在欠繳名單中則為 0")
	private BigDecimal membershipDue;

	@Schema(description = "補繳常年會費的分年明細, 含金額為 0 的年度; 不在欠繳名單中則為空陣列")
	private List<MembershipDueYearBO> membershipDueDetails;

	@Schema(description = "本次應付總金額, 即報名費 + 補繳常年會費")
	private BigDecimal totalAmount;

	@Schema(description = "是否完全免費, 免費時註冊當下不會產生付款流程")
	private Boolean free;

}
