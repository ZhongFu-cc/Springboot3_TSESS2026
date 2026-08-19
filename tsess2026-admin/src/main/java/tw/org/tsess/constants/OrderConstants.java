package tw.org.tsess.constants;

public final class OrderConstants {

	// 私有化構造函數,禁止被 new
	private OrderConstants() {}

	public static final String ITEMS_SUMMARY_REGISTRATION = "Registration Fee";
	public static final String GROUP_ITEMS_SUMMARY_REGISTRATION = "Group Registration Fee";

	/** 補繳常年會費的明細類型與品名前綴 , Invoice 為英文版面 */
	private static final String MEMBERSHIP_DUE = "Annual Membership Dues";

	/**
	 * 補繳常年會費的明細類型, 逐年一種<br>
	 * 例如 2024 年度為 "Annual Membership Dues 2024"<br>
	 * 只會出現在 orders_item.product_type , 不會寫進 orders.items_summary ,
	 * 因為 items_summary 被當成「這是不是註冊費訂單」的查詢條件使用
	 *
	 * @param adYear 西元年
	 * @return
	 */
	public static String membershipDueProductType(Integer adYear) {
		return MEMBERSHIP_DUE + " " + adYear;
	}

	/**
	 * 補繳常年會費的明細品名, 逐年一筆<br>
	 * 例如 2024 年度為 "Annual Membership Dues (2024)"
	 *
	 * @param adYear 西元年
	 * @return
	 */
	public static String membershipDueProductName(Integer adYear) {
		return MEMBERSHIP_DUE + " (" + adYear + ")";
	}
}
