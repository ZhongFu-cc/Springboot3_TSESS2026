package tw.org.tsess.utils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import tw.org.tsess.pojo.entity.OrdersItem;

/**
 * 訂單明細的共用計算<br>
 * 訂單總額現在可能同時含報名費與各年度的補繳常年會費, 拆帳邏輯集中在這裡, 避免各處各寫一份
 */
public final class OrdersItemUtil {

	// 私有化構造函數,禁止被 new
	private OrdersItemUtil() {}

	/**
	 * 加總訂單明細中, 產品類型符合的小計<br>
	 * 沒有符合的明細時回傳 BigDecimal.ZERO , 不會回 null
	 *
	 * @param ordersItemList
	 * @param productTypes
	 * @return
	 */
	public static BigDecimal sumByProductType(List<OrdersItem> ordersItemList, String... productTypes) {

		if (ordersItemList == null || ordersItemList.isEmpty()) {
			return BigDecimal.ZERO;
		}

		Set<String> targetTypes = Set.of(productTypes);
		return ordersItemList.stream()
				.filter(ordersItem -> targetTypes.contains(ordersItem.getProductType()))
				.map(OrdersItem::getSubtotal)
				.filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

}
