package tw.org.tsess.constants;

public final class OrderConstants {

	// 私有化構造函數,禁止被 new
	private OrderConstants() {}

	public static final String ITEMS_SUMMARY_REGISTRATION = "Registration Fee";
	public static final String GROUP_ITEMS_SUMMARY_REGISTRATION = "Group Registration Fee";

	/** 補繳常年會費的明細類型與品名前綴 , Invoice 為英文版面 */
	private static final String MEMBERSHIP_DUE = "常年會費";

	/**
	 * 補繳常年會費的明細類型, 逐年一種<br>
	 * 例如 113 年度為 "常年會費 113"<br>
	 * 只會出現在 orders_item.product_type , 不會寫進 orders.items_summary ,
	 * 因為 items_summary 被當成「這是不是註冊費訂單」的查詢條件使用
	 *
	 * @param rocYear 民國年
	 * @return
	 */
	public static String membershipDueProductType(Integer rocYear) {
		return MEMBERSHIP_DUE + " " + rocYear;
	}

	/**
	 * 補繳常年會費的明細品名, 逐年一筆<br>
	 * 例如 113 年度為 "常年會費 (113)"
	 *
	 * @param rocYear 民國年
	 * @return
	 */
	public static String membershipDueProductName(Integer rocYear) {
		return MEMBERSHIP_DUE + " (" + rocYear + ")";
	}

	/**
	 * 判斷此明細的產品類型是否為「補繳常年會費」<br>
	 * 例如 "常年會費 113"
	 *
	 * @param productType
	 * @return
	 */
	public static boolean isMembershipDueProductType(String productType) {
		return productType != null && productType.startsWith(MEMBERSHIP_DUE + " ");
	}

	/**
	 * 從補繳常年會費的產品類型解析出民國年<br>
	 * 例如 "常年會費 113" 會得到 113 , 格式不符時回傳 null
	 *
	 * @param productType
	 * @return
	 */
	public static Integer parseMembershipDueRocYear(String productType) {
		if (!isMembershipDueProductType(productType)) {
			return null;
		}
		try {
			return Integer.valueOf(productType.substring((MEMBERSHIP_DUE + " ").length()).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
