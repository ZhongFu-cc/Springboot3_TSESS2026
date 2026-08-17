package tw.org.tsess.pojo.VO;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

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

	@Schema(description = "訂單狀態 0為未付款 1為已付款 2為付款失敗")
	private Integer status;

	@Schema(description = "訂單明細, 例如註冊費、補繳常年會費")
	private List<OrdersItemVO> ordersItemList;

}
