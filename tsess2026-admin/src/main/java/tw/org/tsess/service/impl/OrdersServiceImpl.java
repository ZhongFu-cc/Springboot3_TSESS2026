package tw.org.tsess.service.impl;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.org.tsess.constants.OrderConstants;
import tw.org.tsess.convert.OrdersConvert;
import tw.org.tsess.convert.OrdersItemConvert;
import tw.org.tsess.enums.OrderStatusEnum;
import tw.org.tsess.mapper.OrdersMapper;
import tw.org.tsess.pojo.BO.OrderDraftBO;
import tw.org.tsess.pojo.BO.OrderLineBO;
import tw.org.tsess.pojo.DTO.addEntityDTO.AddOrdersDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutOrdersDTO;
import tw.org.tsess.pojo.VO.OrdersVO;
import tw.org.tsess.pojo.entity.Member;
import tw.org.tsess.pojo.entity.Orders;
import tw.org.tsess.pojo.entity.OrdersItem;
import tw.org.tsess.service.OrdersItemService;
import tw.org.tsess.service.OrdersService;

@Service
@RequiredArgsConstructor
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

	@Value("${project.name}")
	private String PROJECT_NAME;

	private final OrdersConvert ordersConvert;
	private final OrdersItemConvert ordersItemConvert;
	private final OrdersItemService ordersItemService;

	@Override
	public int getNotPaidRegistrationOrderGroupIndex(int groupSize) {
		LambdaQueryWrapper<Orders> orderQueryWrapper = new LambdaQueryWrapper<>();
		// 註冊費未繳費的 (包含繳費失敗、未繳費)
		orderQueryWrapper.ne(Orders::getStatus, OrderStatusEnum.PAYMENT_SUCCESS.getValue()).and(wrapper -> {
			wrapper.eq(Orders::getItemsSummary, OrderConstants.ITEMS_SUMMARY_REGISTRATION)
					.or()
					.eq(Orders::getItemsSummary, OrderConstants.GROUP_ITEMS_SUMMARY_REGISTRATION);
		});

		Long notPaidCount = baseMapper.selectCount(orderQueryWrapper);

		return (int) Math.ceil(notPaidCount / (double) groupSize);

	}

	@Override
	public Page<Orders> getRegistrationOrderPageByStatus(Page<Orders> page, Integer status) {
		LambdaQueryWrapper<Orders> orderQueryWrapper = new LambdaQueryWrapper<>();
		orderQueryWrapper.eq(status != null, Orders::getStatus, status).and(wrapper -> {
			wrapper.eq(Orders::getItemsSummary, OrderConstants.ITEMS_SUMMARY_REGISTRATION)
					.or()
					.eq(Orders::getItemsSummary, OrderConstants.GROUP_ITEMS_SUMMARY_REGISTRATION);
		});

		Page<Orders> ordersPage = baseMapper.selectPage(page, orderQueryWrapper);

		return ordersPage;
	}

	@Override
	public List<Orders> getRegistrationOrderListByStatus(Integer status) {
		// 查找itemsSummary 為 註冊費 , 以及符合status 的member數量
		LambdaQueryWrapper<Orders> orderQueryWrapper = new LambdaQueryWrapper<>();
		orderQueryWrapper.eq(status != null, Orders::getStatus, status).and(wrapper -> {
			wrapper.eq(Orders::getItemsSummary, OrderConstants.ITEMS_SUMMARY_REGISTRATION)
					.or()
					.eq(Orders::getItemsSummary, OrderConstants.GROUP_ITEMS_SUMMARY_REGISTRATION);
		});

		List<Orders> orderList = baseMapper.selectList(orderQueryWrapper);
		return orderList;
	}

	@Override
	public Orders getRegistrationOrderByMemberId(Long memberId) {
		// 找到items_summary 符合 Registration Fee 以及 訂單會員ID與 會員相符的資料
		LambdaQueryWrapper<Orders> orderQueryWrapper = new LambdaQueryWrapper<>();
		orderQueryWrapper.eq(Orders::getMemberId, memberId).and(wrapper -> {
			wrapper.eq(Orders::getItemsSummary, OrderConstants.ITEMS_SUMMARY_REGISTRATION)
					.or()
					.eq(Orders::getItemsSummary, OrderConstants.GROUP_ITEMS_SUMMARY_REGISTRATION);
		});

		Orders orders = baseMapper.selectOne(orderQueryWrapper);
		return orders;
	}

	@Override
	public List<Orders> getRegistrationOrderListForExcel() {
		// 查詢所有沒被刪除 且 items_summary為 註冊費 或者 團體註冊費 訂單
		// 這種名稱在註冊費訂單中只會出現一種，不會同時出現，
		// 也就是註冊費訂單的items_summary 只有 ITEMS_SUMMARY_REGISTRATION 和 GROUP_ITEMS_SUMMARY_REGISTRATION 的選項
		List<Orders> orderList = baseMapper.selectOrders(OrderConstants.ITEMS_SUMMARY_REGISTRATION,
				OrderConstants.GROUP_ITEMS_SUMMARY_REGISTRATION);

		return orderList;
	}

	private Map<Long, Orders> baseGetRegistrationOrderMapByMemberId(Collection<Long> memberIds) {
		// 1.沒有關聯直接返回空映射
		if (memberIds.isEmpty()) {
			return Collections.emptyMap();
		}

		// 2.找到items_summary 符合 Registration Fee 以及 訂單會員ID與 會員相符的資料
		LambdaQueryWrapper<Orders> orderQueryWrapper = new LambdaQueryWrapper<>();
		orderQueryWrapper.in(Orders::getMemberId, memberIds).and(wrapper -> {
			wrapper.eq(Orders::getItemsSummary, OrderConstants.ITEMS_SUMMARY_REGISTRATION)
					.or()
					.eq(Orders::getItemsSummary, OrderConstants.GROUP_ITEMS_SUMMARY_REGISTRATION);
		});
		List<Orders> orderList = baseMapper.selectList(orderQueryWrapper);

		//3.拿到以memberId為key , Order為value的Map對象
		return orderList.stream().collect(Collectors.toMap(Orders::getMemberId, Function.identity()));

	}

	@Override
	public Map<Long, Orders> getRegistrationOrderMapByMemberId() {
		List<Orders> orderList = this.getRegistrationOrderListForExcel();
		return orderList.stream().collect(Collectors.toMap(Orders::getMemberId, Function.identity()));
	}

	@Override
	public Map<Long, Orders> getRegistrationOrderMapByMemberId(List<Member> memberList) {
		Set<Long> memberIds = memberList.stream().map(Member::getMemberId).collect(Collectors.toSet());
		return this.baseGetRegistrationOrderMapByMemberId(memberIds);
	}

	@Override
	public Map<Long, Orders> getRegistrationOrderMapByMemberId(Collection<Long> memberIds) {
		return this.baseGetRegistrationOrderMapByMemberId(memberIds);
	}

	@Override
	public List<Orders> getUnpaidRegistrationOrderList() {
		LambdaQueryWrapper<Orders> ordersWrapper = new LambdaQueryWrapper<>();
		ordersWrapper
				.in(Orders::getStatus, OrderStatusEnum.UNPAID.getValue(),
						OrderStatusEnum.PENDING_CONFIRMATION.getValue())
				.eq(Orders::getItemsSummary, OrderConstants.ITEMS_SUMMARY_REGISTRATION);
		List<Orders> ordersList = baseMapper.selectList(ordersWrapper);
		return ordersList;
	}
	
	@Override
	public void approveUnpaidMember(Long memberId) {
		// 在訂單表查詢,memberId符合,且ItemSummary 也符合註冊費的訂單
		LambdaQueryWrapper<Orders> ordersWrapper = new LambdaQueryWrapper<>();
		ordersWrapper.eq(Orders::getMemberId, memberId)
				.eq(Orders::getItemsSummary, OrderConstants.ITEMS_SUMMARY_REGISTRATION);
		Orders orders = baseMapper.selectOne(ordersWrapper);

		// 更新訂單付款狀態為 已付款
		orders.setStatus(OrderStatusEnum.PAYMENT_SUCCESS.getValue());

		// 更新進資料庫
		baseMapper.updateById(orders);
	}

	@Override
	public void createRegistrationOrder(OrderDraftBO draft, Member member) {
		// 1.新建 註冊費 訂單
		Orders order = new Orders();
		// 2.設定會員ID
		order.setMemberId(member.getMemberId());
		// 3.設定這筆訂單商品的統稱
		// 注意: items_summary 固定為 Registration Fee , 它被當成「這是不是註冊費訂單」的查詢條件,
		// 即使這張訂單含補繳常年會費也不能改, 品項差異看 orders_item
		order.setItemsSummary(OrderConstants.ITEMS_SUMMARY_REGISTRATION);
		// 4.設定繳費狀態為 未繳費(0)
		order.setStatus(OrderStatusEnum.UNPAID.getValue());
		// 5.設定金額, 為各明細小計加總
		order.setTotalAmount(draft.getTotalAmount());
		// 6.透過訂單服務 新增訂單
		baseMapper.insert(order);

		// 7.創建註冊費訂單細項 (註冊費 + 可能的補繳常年會費)
		ordersItemService.createRegistrationOrderItems(order.getOrdersId(), draft.getLines());

	}

	@Override
	public void createFreeRegistrationOrder(Member member) {
		// 1.新建 免註冊費 訂單
		Orders order = new Orders();
		// 2.設定會員ID
		order.setMemberId(member.getMemberId());
		// 3.設定這筆訂單商品的統稱
		order.setItemsSummary(OrderConstants.ITEMS_SUMMARY_REGISTRATION);
		// 4.設定繳費狀態為 已繳費(2)
		order.setStatus(OrderStatusEnum.PAYMENT_SUCCESS.getValue());
		// 5.設定金額
		order.setTotalAmount(BigDecimal.ZERO);
		// 6.透過訂單服務 新增訂單
		baseMapper.insert(order);

		// 7.創建註冊費訂單細項 , 免費訂單仍留一筆 0 元明細
		ordersItemService.createRegistrationOrderItems(order.getOrdersId(),
				List.of(OrderLineBO.ofSingle(OrderConstants.ITEMS_SUMMARY_REGISTRATION,
						PROJECT_NAME + " " + OrderConstants.ITEMS_SUMMARY_REGISTRATION, BigDecimal.ZERO)));

	}

	@Override
	public void createGroupRegistrationOrder(BigDecimal amount, Member member) {
		// 1.新建 團體報名註冊費 訂單
		Orders order = new Orders();
		// 2.設定會員ID
		order.setMemberId(member.getMemberId());
		// 3.設定這筆訂單商品的統稱
		order.setItemsSummary(OrderConstants.GROUP_ITEMS_SUMMARY_REGISTRATION);
		// 4.設定繳費狀態為 未繳費(0)
		order.setStatus(OrderStatusEnum.UNPAID.getValue());
		// 5.設定金額
		order.setTotalAmount(amount);
		// 6.透過訂單服務 新增訂單
		baseMapper.insert(order);

		// 7.創建註冊費訂單細項
		ordersItemService.createGroupRegistrationOrderItem(order);

	}

	@Override
	public void createFreeGroupRegistrationOrder(Member member) {
		// 1.新建 免註冊費 訂單
		Orders order = new Orders();
		// 2.設定會員ID
		order.setMemberId(member.getMemberId());
		// 3.設定這筆訂單商品的統稱
		order.setItemsSummary(OrderConstants.GROUP_ITEMS_SUMMARY_REGISTRATION);
		// 4.設定繳費狀態為 未繳費
		order.setStatus(OrderStatusEnum.UNPAID.getValue());
		// 5.設定金額
		order.setTotalAmount(BigDecimal.ZERO);
		// 6.透過訂單服務 新增訂單
		baseMapper.insert(order);

		// 7.創建註冊費訂單細項
		ordersItemService.createGroupRegistrationOrderItem(order);

	}

	@Override
	public void createFreeGroupRegistrationPaidOrder(Member member) {
		// 1.新建 免註冊費 訂單
		Orders order = new Orders();
		// 2.設定會員ID
		order.setMemberId(member.getMemberId());
		// 3.設定這筆訂單商品的統稱
		order.setItemsSummary(OrderConstants.GROUP_ITEMS_SUMMARY_REGISTRATION);
		// 4.設定繳費狀態為 已繳費
		order.setStatus(OrderStatusEnum.PAYMENT_SUCCESS.getValue());
		// 5.設定金額
		order.setTotalAmount(BigDecimal.ZERO);
		// 6.透過訂單服務 新增訂單
		baseMapper.insert(order);

		// 7.創建註冊費訂單細項
		ordersItemService.createGroupRegistrationOrderItem(order);
	}

	@Override
	public Orders getOrders(Long ordersId) {
		Orders orders = baseMapper.selectById(ordersId);
		return orders;
	}

	@Override
	public Orders getOrders(Long memberId, Long ordersId) {
		LambdaQueryWrapper<Orders> ordersQueryWrapper = new LambdaQueryWrapper<>();
		ordersQueryWrapper.eq(Orders::getMemberId, memberId).eq(Orders::getOrdersId, ordersId);

		Orders orders = baseMapper.selectOne(ordersQueryWrapper);

		return orders;
	}

	@Override
	public List<Orders> getOrdersList() {
		return baseMapper.selectList(null);
	}

	@Override
	public List<Orders> getOrdersList(Long memberId) {
		LambdaQueryWrapper<Orders> ordersQueryWrapper = new LambdaQueryWrapper<>();
		ordersQueryWrapper.eq(Orders::getMemberId, memberId);
		return baseMapper.selectList(ordersQueryWrapper);
	}

	@Override
	public IPage<Orders> getOrdersPage(Page<Orders> page) {
		Page<Orders> ordersPage = baseMapper.selectPage(page, null);
		return ordersPage;
	}

	@Override
	public List<OrdersVO> toOrdersVOList(List<Orders> ordersList) {

		// 1.沒有訂單直接返回空列表
		if (ordersList == null || ordersList.isEmpty()) {
			return Collections.emptyList();
		}

		// 2.先轉出訂單本身的VO
		List<OrdersVO> ordersVOList = ordersConvert.entityListToVOList(ordersList);

		// 3.一次撈完這批訂單的所有明細, 避免逐筆查詢造成 N+1
		List<Long> ordersIds = ordersList.stream().map(Orders::getOrdersId).toList();
		Map<Long, List<OrdersItem>> ordersItemMap = ordersItemService.getOrdersItemsByOrderIds(ordersIds);

		// 4.依 ordersId 把明細塞回對應的VO, 沒有明細的訂單給空列表而非null
		for (OrdersVO ordersVO : ordersVOList) {
			List<OrdersItem> ordersItemList = ordersItemMap.getOrDefault(ordersVO.getOrdersId(), Collections.emptyList());
			ordersVO.setOrdersItemList(ordersItemConvert.entityListToVOList(ordersItemList));
		}

		return ordersVOList;
	}

	@Override
	public OrdersVO getOrdersVO(Long ordersId) {
		Orders orders = baseMapper.selectById(ordersId);
		if (orders == null) {
			return null;
		}
		return this.toOrdersVOList(List.of(orders)).get(0);
	}

	@Override
	public List<OrdersVO> getOrdersVOList() {
		return this.toOrdersVOList(this.getOrdersList());
	}

	@Override
	public List<OrdersVO> getOrdersVOList(Long memberId) {
		return this.toOrdersVOList(this.getOrdersList(memberId));
	}

	@Override
	public IPage<OrdersVO> getOrdersVOPage(Page<Orders> page) {

		// 1.先拿到訂單本身的分頁對象
		Page<Orders> ordersPage = baseMapper.selectPage(page, null);

		// 2.保留分頁資訊, 只替換掉records
		Page<OrdersVO> ordersVOPage = new Page<>(ordersPage.getCurrent(), ordersPage.getSize(), ordersPage.getTotal());
		ordersVOPage.setRecords(this.toOrdersVOList(ordersPage.getRecords()));

		return ordersVOPage;
	}

	@Override
	@Transactional
	public Long addOrders(AddOrdersDTO addOrdersDTO) {
		// 新增訂單本身
		Orders orders = ordersConvert.addDTOToEntity(addOrdersDTO);
		baseMapper.insert(orders);

		return orders.getOrdersId();
	}

	@Override
	public void updateOrders(PutOrdersDTO putOrdersDTO) {
		Orders orders = ordersConvert.putDTOToEntity(putOrdersDTO);
		baseMapper.updateById(orders);
	}

	@Override
	public void updateOrders(Long memberId, PutOrdersDTO putOrdersDTO) {
		Orders orders = ordersConvert.putDTOToEntity(putOrdersDTO);

		LambdaQueryWrapper<Orders> ordersQueryWrapper = new LambdaQueryWrapper<>();
		ordersQueryWrapper.eq(Orders::getMemberId, memberId).eq(Orders::getOrdersId, orders.getOrdersId());
		baseMapper.update(orders, ordersQueryWrapper);
	}

	@Override
	public void deleteOrders(Long ordersId) {
		// 1.刪除訂單的細項
		ordersItemService.deleteOrdersItemByOrderId(ordersId);
		// 2.刪除訂單
		baseMapper.deleteById(ordersId);
	}

	@Override
	public void deleteOrders(Long memberId, Long ordersId) {
		// 1.查詢memberId 和 orderId符合的訂單
		LambdaQueryWrapper<Orders> ordersQueryWrapper = new LambdaQueryWrapper<>();
		ordersQueryWrapper.eq(Orders::getMemberId, memberId).eq(Orders::getOrdersId, ordersId);
		Orders order = baseMapper.selectOne(ordersQueryWrapper);

		// 2.刪除訂單及其細項
		this.deleteOrders(order.getOrdersId());

	}

	@Override
	public void deleteOrdersList(List<Long> ordersIds) {
		for (Long orderId : ordersIds) {
			this.deleteOrders(orderId);
		}
	}

	@Override
	public void syncSlaveMemberOrderStatus(Long slaveMemberId, Integer currentStatus) {
		// 1.查詢子報名者當前的 團體報名訂單狀態
		LambdaQueryWrapper<Orders> query = new LambdaQueryWrapper<>();
		query.eq(Orders::getMemberId, slaveMemberId)
				.eq(Orders::getItemsSummary, OrderConstants.GROUP_ITEMS_SUMMARY_REGISTRATION);
		Orders slaveOrder = baseMapper.selectOne(query);

		// 2.沒有找到就略過
		if (slaveOrder == null) {
			return;
		}

		// 3.如果子報名者訂單狀態不是「付款成功 (2)」，才允許更新，避免成功付款仍再付一次
		if (!OrderStatusEnum.PAYMENT_SUCCESS.getValue().equals(slaveOrder.getStatus())) {
			slaveOrder.setStatus(currentStatus);
			baseMapper.updateById(slaveOrder);
		}
	}


}
