package tw.org.tsess.pojo.BO;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tw.org.tsess.enums.MembershipDueYearEnum;

/**
 * 單一年度的常年會費補繳金額<br>
 * 分年金額是唯一真實來源, 應繳總額一律由各年度加總得出
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembershipDueYearBO {

	@Schema(description = "民國年, 例如 113")
	private Integer rocYear;

	@Schema(description = "西元年, 例如 2024")
	private Integer adYear;

	@Schema(description = "該年度應補繳金額, 未欠繳則為 0")
	private BigDecimal amount;

	/**
	 * 金額為 null 時視為 0, 讓呼叫端不必再判空
	 *
	 * @param membershipDueYearEnum
	 * @param amount
	 * @return
	 */
	public static MembershipDueYearBO of(MembershipDueYearEnum membershipDueYearEnum, BigDecimal amount) {
		return new MembershipDueYearBO(membershipDueYearEnum.getRocYear(), membershipDueYearEnum.getAdYear(),
				amount == null ? BigDecimal.ZERO : amount);
	}

}
