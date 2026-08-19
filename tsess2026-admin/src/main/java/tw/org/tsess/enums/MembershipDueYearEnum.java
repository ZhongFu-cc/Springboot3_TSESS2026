package tw.org.tsess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 常年會費的補繳年度<br>
 * 對標 membership_fee_due table 的 due_113 / due_114 / due_115 三個欄位<br>
 * 民國年供後台顯示, 西元年供 Invoice 等英文版面使用
 */
@Getter
@AllArgsConstructor
public enum MembershipDueYearEnum {
	ROC_113(113, 2024),
	ROC_114(114, 2025),
	ROC_115(115, 2026);

	/** 民國年 */
	private final Integer rocYear;

	/** 西元年 */
	private final Integer adYear;

	public static MembershipDueYearEnum fromRocYear(Integer rocYear) {
		for (MembershipDueYearEnum type : values()) {
			if (type.rocYear.equals(rocYear))
				return type;
		}
		throw new IllegalArgumentException("無效的常年會費年度: " + rocYear);
	}

}
