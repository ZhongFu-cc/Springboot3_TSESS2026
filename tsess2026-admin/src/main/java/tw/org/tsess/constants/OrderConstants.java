package tw.org.tsess.constants;

public final class OrderConstants {
	
	// 私有化構造函數,禁止被 new
	private OrderConstants() {}

	public static final String ITEMS_SUMMARY_REGISTRATION = "Registration Fee";
	public static final String GROUP_ITEMS_SUMMARY_REGISTRATION = "Group Registration Fee";

	/**
	 * 補繳常年會費的明細類型<br>
	 * 只會出現在 orders_item.product_type , 不會寫進 orders.items_summary ,
	 * 因為 items_summary 被當成「這是不是註冊費訂單」的查詢條件使用
	 */
	public static final String ITEMS_TYPE_MEMBERSHIP_DUE = "Annual Membership Dues";

	/** 補繳常年會費的明細品名 , Invoice 為英文版面 */
	public static final String MEMBERSHIP_DUE_PRODUCT_NAME = "Annual Membership Dues (2024-2026)";
}
