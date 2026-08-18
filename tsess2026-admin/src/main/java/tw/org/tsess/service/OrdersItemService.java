package tw.org.tsess.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.org.tsess.pojo.BO.OrderLineBO;
import tw.org.tsess.pojo.DTO.addEntityDTO.AddOrdersItemDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutOrdersItemDTO;
import tw.org.tsess.pojo.entity.Orders;
import tw.org.tsess.pojo.entity.OrdersItem;

public interface OrdersItemService extends IService<OrdersItem> {

	/**
	 * 現場註冊者, 立刻產生他的免費訂單
	 * 
	 * @param orderId
	 * @param amount
	 */
	void addRegistrationOrderItem(Long orderId, BigDecimal amount);

	/**
	 * 創建 註冊費訂單的 訂單細項<br>
	 * 一張註冊費訂單可以有多筆明細 (註冊費 + 補繳常年會費)
	 *
	 * @param orderId
	 * @param lines
	 */
	void createRegistrationOrderItems(Long orderId, List<OrderLineBO> lines);

	/**
	 * 創建 團體報名 註冊費訂單的 訂單細項
	 * @param order
	 */
	void createGroupRegistrationOrderItem(Orders order);

	OrdersItem getOrdersItem(Long oredersItemId);

	List<OrdersItem> getOrdersItemList();

	/**
	 * 根據訂單ID , 查詢其訂單細項
	 *
	 * @param orderId
	 * @return
	 */
	List<OrdersItem> getOrdersItemsByOrderId(Long orderId);

	/**
	 * 根據多筆訂單ID , 一次查完所有訂單細項<br>
	 * 供列表/分頁組裝 VO 使用 , 避免 N+1
	 *
	 * @param orderIds
	 * @return 以 ordersId 為key , 該訂單的細項列表為value 的Map對象
	 */
	Map<Long, List<OrdersItem>> getOrdersItemsByOrderIds(Collection<Long> orderIds);

	IPage<OrdersItem> getOrdersItemPage(Page<OrdersItem> page);

	void addOrdersItem(AddOrdersItemDTO addOrdersItemDTO);

	void updateOrdersItem(PutOrdersItemDTO putOrdersItemDTO);
	
	void deleteOrdersItem(Long oredersItemId);

	void deleteOrdersItemList(List<Long> oredersItemIds);
	
	/**
	 * 根據訂單ID , 刪除其訂單細項
	 * 
	 * @param orderId
	 */
	void deleteOrdersItemByOrderId(Long orderId);

}
