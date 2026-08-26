package tw.org.tsess.pojo.VO;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tw.org.tsess.pojo.BO.MembershipDueYearBO;

@Data
public class OrdersVO {

	@Schema(description = "主鍵ID")
	private Long ordersId;

	@Schema(description = "自然鍵")
	private byte[] natureId;

	@Schema(description = "這筆訂單商品的統稱")
	private String itemsSummary;

	@Schema(description = "訂單總金額, 即各明細小計加總")
	private BigDecimal totalAmount;

	@Schema(description = "訂單狀態 0為未付款 ; 1為已付款-待審核 ; 2為付款成功 ; 3為付款失敗")
	private Integer status;

	@Schema(description = "其中的報名費, 個人與團體報名都算在這裡")
	private BigDecimal registrationFee;

	@Schema(description = "其中的補繳常年會費, 為各年度加總")
	private BigDecimal membershipDue;

	@Schema(description = "補繳常年會費的分年明細, 這張訂單沒有任何會費項目時為空陣列")
	private List<MembershipDueYearBO> membershipDueDetails;

	@Schema(description = "訂單明細, 例如註冊費、補繳常年會費")
	private List<OrdersItemVO> ordersItemList;

}
