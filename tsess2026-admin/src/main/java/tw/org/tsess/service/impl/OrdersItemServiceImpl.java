package tw.org.tsess.service.impl;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.org.tsess.constants.OrderConstants;
import tw.org.tsess.convert.OrdersItemConvert;
import tw.org.tsess.pojo.BO.OrderLineBO;
import tw.org.tsess.mapper.OrdersItemMapper;
import tw.org.tsess.pojo.DTO.addEntityDTO.AddOrdersItemDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutOrdersItemDTO;
import tw.org.tsess.pojo.entity.Orders;
import tw.org.tsess.pojo.entity.OrdersItem;
import tw.org.tsess.service.OrdersItemService;

@Service
@RequiredArgsConstructor
public class OrdersItemServiceImpl extends ServiceImpl<OrdersItemMapper, OrdersItem> implements OrdersItemService {

	@Value("${project.name}")
	private String PROJECT_NAME;

	private final OrdersItemConvert ordersItemConvert;

	@Override
	public void addRegistrationOrderItem(Long orderId, BigDecimal amount) {
		OrdersItem ordersItem = new OrdersItem();
		ordersItem.setOrdersId(orderId);
		ordersItem.setProductType(OrderConstants.ITEMS_SUMMARY_REGISTRATION);
		ordersItem.setProductName(PROJECT_NAME + " " + OrderConstants.ITEMS_SUMMARY_REGISTRATION);
		ordersItem.setUnitPrice(amount);
		ordersItem.setQuantity(1);
		ordersItem.setSubtotal(amount.multiply(BigDecimal.ONE));

		baseMapper.insert(ordersItem);

	}

	@Override
	public void createRegistrationOrderItems(Long orderId, List<OrderLineBO> lines) {

		// 1.註冊費訂單可以有多筆明細 (註冊費 + 補繳常年會費) , 逐筆落庫
		List<OrdersItem> ordersItemList = lines.stream().map(line -> {
			OrdersItem ordersItem = new OrdersItem();
			ordersItem.setOrdersId(orderId);
			ordersItem.setProductType(line.getProductType());
			ordersItem.setProductName(line.getProductName());
			ordersItem.setUnitPrice(line.getUnitPrice());
			ordersItem.setQuantity(line.getQuantity());
			ordersItem.setSubtotal(line.getSubtotal());
			return ordersItem;
		}).toList();

		// 2.批量新增訂單明細
		this.saveBatch(ordersItemList);

	}

	@Override
	public void createGroupRegistrationOrderItem(Orders order) {
		// 1.綁在註冊時的訂單產生，設定固定訂單的細節
		OrdersItem ordersItem = new OrdersItem();
		// 2.設定基本資料
		ordersItem.setOrdersId(order.getOrdersId());
		ordersItem.setProductType(OrderConstants.GROUP_ITEMS_SUMMARY_REGISTRATION);
		ordersItem.setProductName(PROJECT_NAME + " " + OrderConstants.GROUP_ITEMS_SUMMARY_REGISTRATION);
		// 3.設定單價、數量、小計
		ordersItem.setUnitPrice(order.getTotalAmount());
		ordersItem.setQuantity(1);
		ordersItem.setSubtotal(order.getTotalAmount().multiply(BigDecimal.ONE));

		// 4.新增訂單明細
		baseMapper.insert(ordersItem);

	}

	@Override
	public OrdersItem getOrdersItem(Long ordersItemId) {
		OrdersItem ordersItem = baseMapper.selectById(ordersItemId);
		return ordersItem;
	}

	@Override
	public List<OrdersItem> getOrdersItemList() {
		List<OrdersItem> ordersItemList = baseMapper.selectList(null);
		return ordersItemList;
	}

	@Override
	public List<OrdersItem> getOrdersItemsByOrderId(Long orderId) {
		LambdaQueryWrapper<OrdersItem> ordersItemWrapper = new LambdaQueryWrapper<>();
		ordersItemWrapper.eq(OrdersItem::getOrdersId, orderId).orderByAsc(OrdersItem::getOrdersItemId);
		return baseMapper.selectList(ordersItemWrapper);
	}

	@Override
	public Map<Long, List<OrdersItem>> getOrdersItemsByOrderIds(Collection<Long> orderIds) {

		// 1.沒有訂單直接返回空映射
		if (orderIds == null || orderIds.isEmpty()) {
			return Collections.emptyMap();
		}

		// 2.一次撈完範圍內所有明細 , 避免逐筆查詢造成 N+1
		LambdaQueryWrapper<OrdersItem> ordersItemWrapper = new LambdaQueryWrapper<>();
		ordersItemWrapper.in(OrdersItem::getOrdersId, orderIds).orderByAsc(OrdersItem::getOrdersItemId);
		List<OrdersItem> ordersItemList = baseMapper.selectList(ordersItemWrapper);

		// 3.以 ordersId 分組
		return ordersItemList.stream().collect(Collectors.groupingBy(OrdersItem::getOrdersId));
	}

	@Override
	public IPage<OrdersItem> getOrdersItemPage(Page<OrdersItem> page) {
		Page<OrdersItem> ordersItemPage = baseMapper.selectPage(page, null);
		return ordersItemPage;
	}

	@Override
	public void addOrdersItem(AddOrdersItemDTO addOrdersItemDTO) {
		OrdersItem ordersItem = ordersItemConvert.addDTOToEntity(addOrdersItemDTO);
		baseMapper.insert(ordersItem);
	}

	@Override
	public void updateOrdersItem(PutOrdersItemDTO putOrdersItemDTO) {
		OrdersItem ordersItem = ordersItemConvert.putDTOToEntity(putOrdersItemDTO);
		baseMapper.updateById(ordersItem);

	}

	@Override
	public void deleteOrdersItem(Long ordersItemId) {
		baseMapper.deleteById(ordersItemId);
	}

	@Override
	public void deleteOrdersItemList(List<Long> ordersItemIds) {
		baseMapper.deleteBatchIds(ordersItemIds);
	}

	@Override
	public void deleteOrdersItemByOrderId(Long orderId) {
		LambdaQueryWrapper<OrdersItem> ordersItemWrapper = new LambdaQueryWrapper<>();
		ordersItemWrapper.eq(OrdersItem::getOrdersId, orderId);
		baseMapper.delete(ordersItemWrapper);

	}

}
