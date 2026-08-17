package tw.org.tsess.pojo.BO;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 訂單明細的計算結果<br>
 * 尚未落庫, 由 {@link tw.org.tsess.manager.RegistrationOrderCalculator} 產生,
 * 再由 OrdersItemService 寫進 orders_item
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderLineBO {

	@Schema(description = "產品類型")
	private String productType;

	@Schema(description = "產品名稱")
	private String productName;

	@Schema(description = "單價")
	private BigDecimal unitPrice;

	@Schema(description = "數量")
	private Integer quantity;

	@Schema(description = "小計")
	private BigDecimal subtotal;

	/**
	 * 數量固定為 1 的明細, 小計即單價
	 *
	 * @param productType
	 * @param productName
	 * @param amount
	 * @return
	 */
	public static OrderLineBO ofSingle(String productType, String productName, BigDecimal amount) {
		return new OrderLineBO(productType, productName, amount, 1, amount);
	}

}
