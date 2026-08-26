package tw.org.tsess.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.org.tsess.pojo.BO.OrderDraftBO;
import tw.org.tsess.pojo.DTO.addEntityDTO.AddOrdersDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutOrdersDTO;
import tw.org.tsess.pojo.VO.OrdersVO;
import tw.org.tsess.pojo.entity.Member;
import tw.org.tsess.pojo.entity.Orders;

public interface OrdersService extends IService<Orders> {

	/**
	 * 拿到 註冊費未付款 團體標籤的index
	 * 
	 * @param groupSize 一組的數量(人數)
	 * @return
	 */
	int getNotPaidRegistrationOrderGroupIndex(int groupSize);

	/**
	 * 根據繳費狀態,查詢符合的註冊費訂單(註冊費 和 團體註冊費)分頁對象
	 * 
	 * @param page
	 * @param status
	 * @return
	 */
	Page<Orders> getRegistrationOrderPageByStatus(Page<Orders> page, Integer status);

	/**
	 * 根據繳費狀態,查詢符合的註冊費訂單(註冊費 和 團體註冊費)列表
	 * 
	 * @param status
	 * @return
	 */
	List<Orders> getRegistrationOrderListByStatus(Integer status);

	/**
	 * 找到會員的註冊費訂單
	 * 
	 * @param memberId
	 * @return
	 */
	Orders getRegistrationOrderByMemberId(Long memberId);

	/**
	 * 查詢拿到所有 會員-註冊費訂單映射對象
	 * 
	 * @return 拿到以 memberId 為key , Order 為value 的Map對象
	 */
	Map<Long, Orders> getRegistrationOrderMapByMemberId();

	/**
	 * 根據memberIds,查詢拿到範圍內的 會員-註冊費訂單映射對象
	 * 
	 * @param memberIds
	 * @return 拿到以 memberId 為key , Order 為value 的Map對象
	 */
	Map<Long, Orders> getRegistrationOrderMapByMemberId(Collection<Long> memberIds);

	/**
	 * 根據memberList,查詢拿到範圍內的 會員-註冊費訂單映射對象
	 * 
	 * @param memberList
	 * @return 拿到以 memberId 為key , Order 為value 的Map對象
	 */
	Map<Long, Orders> getRegistrationOrderMapByMemberId(List<Member> memberList);

	/**
	 * For Taiwan本國籍的快速搜索 (外國團體報名不在此限)
	 * 查詢尚未付款，ItemSummary為註冊費的訂單資料；
	 *
	 * @return
	 */
	List<Orders> getUnpaidRegistrationOrderList();

	/**
	 * For Taiwan本國籍的快速審核繳費狀態 (外國團體報名/訂單不在此限)
	 * 修改註冊費繳款狀態 為 付款成功
	 * 
	 * @param memberId
	 * @return
	 */
	void approveUnpaidMember(Long memberId);

	/**
	 * 獲得註冊費訂單(包含註冊費 和 團體註冊費)列表
	 * 
	 * @return
	 */
	List<Orders> getRegistrationOrderListForExcel();

	/**
	 * 創建註冊費訂單<br>
	 * 付款狀態為 「未付款」<br>
	 * 訂單總額與明細皆來自草稿 , 可能包含註冊費以外的補繳常年會費
	 *
	 * @param draft
	 * @param member
	 */
	void createRegistrationOrder(OrderDraftBO draft, Member member);

	/**
	 * 創建 「免費」 註冊費訂單<br>
	 * 付款狀態為 「已付款」<br>
	 * 主要適用於MVP、Speaker、Moderator
	 * 
	 * @param member
	 */
	void createFreeRegistrationOrder(Member member);

	/**
	 * 創建 團體報名 註冊費訂單<br>
	 * 付款狀態為 「未付款」
	 * 
	 * @param amount
	 * @param member
	 */
	void createGroupRegistrationOrder(BigDecimal amount, Member member);

	/**
	 * 創建 「免費」 團體報名註冊費訂單<br>
	 * 付款狀態為 「未付款」
	 * 
	 * @param member
	 */
	void createFreeGroupRegistrationOrder(Member member);

	/**
	 * 創建 「免費」 團體報名註冊費訂單<br>
	 * 付款狀態為 「已付款」
	 * 
	 * @param member
	 */
	void createFreeGroupRegistrationPaidOrder(Member member);

	Orders getOrders(Long OrdersId);

	Orders getOrders(Long memberId, Long OrdersId);

	List<Orders> getOrdersList();

	List<Orders> getOrdersList(Long memberId);

	IPage<Orders> getOrdersPage(Page<Orders> page);

	/**
	 * 將訂單組裝成帶明細的VO<br>
	 * 明細一次查完後分組塞入 , 不會逐筆查詢
	 *
	 * @param ordersList
	 * @return
	 */
	List<OrdersVO> toOrdersVOList(List<Orders> ordersList);

	/**
	 * 查詢單一訂單, 帶明細
	 *
	 * @param ordersId
	 * @return
	 */
	OrdersVO getOrdersVO(Long ordersId);

	/**
	 * 查詢全部訂單, 帶明細
	 *
	 * @return
	 */
	List<OrdersVO> getOrdersVOList();

	/**
	 * 查詢會員自己全部的訂單, 帶明細
	 *
	 * @param memberId
	 * @return
	 */
	List<OrdersVO> getOrdersVOList(Long memberId);

	/**
	 * 查詢全部訂單的分頁對象, 帶明細
	 *
	 * @param page
	 * @return
	 */
	IPage<OrdersVO> getOrdersVOPage(Page<Orders> page);

	Long addOrders(AddOrdersDTO addOrdersDTO);

	void updateOrders(PutOrdersDTO putOrdersDTO);

	void updateOrders(Long memberId, PutOrdersDTO putOrdersDTO);

	void deleteOrders(Long ordersId);

	void deleteOrders(Long memberId, Long ordersId);

	void deleteOrdersList(List<Long> OrdersIds);

	/**
	 * 同步更新子報名者訂單的付款狀態
	 * 
	 * @param slaveMemberId 子報名者 memberId
	 * @param currentStatus 當前付款狀態 (通常來自主報名者訂單)
	 */
	void syncSlaveMemberOrderStatus(Long slaveMemberId, Integer currentStatus);

}
