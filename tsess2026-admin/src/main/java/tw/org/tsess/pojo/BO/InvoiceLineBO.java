package tw.org.tsess.pojo.BO;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Invoice(繳費證明) 的明細資料<br>
 * 金額皆已由台幣換算成美金, 可直接餵給 Jasper 子報表
 */
@Data
public class InvoiceLineBO {

	@Schema(description = "產品名稱")
	private String productName;

	@Schema(description = "產品類型")
	private String productType;

	@Schema(description = "數量")
	private Integer quantity;

	@Schema(description = "單價 (USD)")
	private BigDecimal unitPrice;

	@Schema(description = "小計 (USD)")
	private BigDecimal subtotal;

}
