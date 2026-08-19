package tw.org.tsess.manager;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tw.org.tsess.constants.OrderConstants;
import tw.org.tsess.convert.MemberConvert;
import tw.org.tsess.enums.MembershipDueYearEnum;
import tw.org.tsess.enums.OrderStatusEnum;
import tw.org.tsess.enums.TagTypeEnum;
import tw.org.tsess.helper.TagAssignmentHelper;
import tw.org.tsess.pojo.BO.MemberExcelRaw;
import tw.org.tsess.pojo.DTO.OfflineTransferDTO;
import tw.org.tsess.pojo.VO.MemberOrderVO;
import tw.org.tsess.pojo.VO.MemberTagVO;
import tw.org.tsess.pojo.VO.MemberVO;
import tw.org.tsess.pojo.entity.Attendees;
import tw.org.tsess.pojo.entity.Member;
import tw.org.tsess.pojo.entity.Orders;
import tw.org.tsess.pojo.entity.OrdersItem;
import tw.org.tsess.pojo.excelPojo.MemberExcel;
import tw.org.tsess.service.AttendeesService;
import tw.org.tsess.service.AttendeesTagService;
import tw.org.tsess.service.MemberService;
import tw.org.tsess.service.MemberTagService;
import tw.org.tsess.service.OrdersItemService;
import tw.org.tsess.service.OrdersService;
import tw.org.tsess.service.TagService;
import tw.org.tsess.utils.OrdersItemUtil;

/**
 * 管理會員 和 訂單的需求,<br>
 * 以及成為與會者流程組裝
 */
@Component
@RequiredArgsConstructor
public class MemberOrderManager {

	private final TagAssignmentHelper tagAssignmentHelper;
	private final MemberConvert memberConvert;
	private final MemberService memberService;
	private final MemberTagService memberTagService;
	private final OrdersService ordersService;
	private final OrdersItemService ordersItemService;
	private final AttendeesService attendeesService;
	private final AttendeesTagService attendeesTagService;
	private final TagService tagService;

	// --------------------------- 查詢相關 ---------------------------------------

	/**
	 * 拿到帶有註冊費繳費狀態的VO對象
	 * 
	 * @param memberId
	 * @return
	 */
	public MemberVO getMemberVO(Long memberId) {

		Member member = memberService.getMember(memberId);
		MemberVO vo = memberConvert.entityToVO(member);

		Orders registrationOrder = ordersService.getRegistrationOrderByMemberId(memberId);
		vo.setStatus(registrationOrder.getStatus());
		return vo;

	}

	/**
	 * 獲得訂單狀態的會員人數
	 * 
	 * @param status
	 * @return
	 */
	public Integer getMemberOrderCount(Integer status) {

		// 1.查找符合訂單狀態的訂單
		List<Orders> registrationOrderList = ordersService.getRegistrationOrderListByStatus(status);

		// 2.返回當前訂單狀態的會員總人數
		return memberService.getMemberOrderCount(registrationOrderList);

	}

	/**
	 * 獲得會員及其訂單的VO對象
	 * 
	 * @param page
	 * @param status
	 * @param queryText
	 * @return
	 */
	public IPage<MemberOrderVO> getMemberOrderVO(Page<Orders> page, Integer status, String queryText) {
		// 1.根據分頁 和 訂單狀態, 拿到分頁對象
		Page<Orders> orderPage = ordersService.getRegistrationOrderPageByStatus(page, status);

		// 2.再把訂單分頁 和 會員的查詢條件放入,拿到VO對象並返回
		IPage<MemberOrderVO> memberOrderVO = memberService.getMemberOrderVO(orderPage, status, queryText);
		return memberOrderVO;
	}

	/**
	 * 適用於不使用金流,人工審核<br>
	 * 獲得未付款的 會員及其訂單的VO對象
	 * 
	 * @param page
	 * @param queryText
	 * @return
	 */
	public IPage<MemberTagVO> getUnpaidMemberPage(Page<Member> page, String country, String queryText) {

		// 1.獲取未付款的個人訂單 (外國團體報名不在此限)
		List<Orders> unpaidRegistrationOrderList = ordersService.getUnpaidRegistrationOrderList();

		// 2.獲取未付款的分頁對象
		IPage<MemberTagVO> unpaidMemberPage = memberService.getUnpaidMemberPage(page, unpaidRegistrationOrderList,
				country, queryText);
		return unpaidMemberPage;
	}

	/**
	 * 離線/人工 匯款<br>
	 * 使用者送出確認，等待管理員審核
	 * 
	 * @param offlineTransferDTO
	 */
	public void offlineTransfer(OfflineTransferDTO offlineTransferDTO) {

		Orders order = ordersService.getOrders(offlineTransferDTO.getOrderId());
		Member member = memberService.getMember(order.getMemberId());

		// 修改會員卡號末五碼，不管新舊，理論上就是以這個為準
		member.setRemitAccountLast5(offlineTransferDTO.getRemitAccountLast5());
		memberService.updateById(member);

		// 不管狀態為何,觸發則將訂單狀態改為 付款-待確認
		order.setStatus(OrderStatusEnum.PENDING_CONFIRMATION.getValue());

		// 更新訂單狀態 , 改為已付款-確認中
		ordersService.updateById(order);

	}

	/**
	 * 管理者手動更改付款狀態<br>
	 * 適用於非系統金流收款的狀態<br>
	 * 變更成付款狀態時,新增進與會者名單,並配置Tag
	 * 
	 * @param memberId
	 */
	public void approveUnpaidMember(Long memberId) {
		// 1.新會員的註冊費訂單狀態 => 已付款
		ordersService.approveUnpaidMember(memberId);

		// 2.拿到Member資訊
		Member member = memberService.getMember(memberId);

		// 3.由後台新增的Member , 自動付款完成，新增進與會者名單
		Attendees attendees = attendeesService.addAttendees(member);

		// 4.獲取當下與會者群體的Index,進行與會者標籤分組
		tagAssignmentHelper.assignTag(attendees.getAttendeesId(), attendeesService::getAttendeesGroupIndex,
				tagService::getOrCreateAttendeesGroupTag, attendeesTagService::addAttendeesTag);

		// 5.移除會員 註冊費未付款 Tag
		tagAssignmentHelper.removeGroupTagsByPattern(member.getMemberId(), TagTypeEnum.MEMBER.getType(), "註冊費未付款",
				tagService::getTagIdsByTypeAndNamePattern, memberTagService::removeTagsFromMember);

	}

	/**
	 * 下載所有會員列表, 其中包含他們當前的付款狀態
	 * 
	 * @param response
	 * @throws IOException
	 */
	public void downloadExcel(HttpServletResponse response) throws IOException {
		// 1.設置Excel 檔案資訊
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setCharacterEncoding("utf-8");
		// 这里URLEncoder.encode可以防止中文乱码 ， 和easyexcel没有关系
		String fileName = URLEncoder.encode("會員名單", "UTF-8").replaceAll("\\+", "%20");
		response.setHeader("Content-disposition", "attachment;filename*=" + fileName + ".xlsx");

		// 2.獲取 會員ID-註冊費訂單 的映射對象
		Map<Long, Orders> ordersMap = ordersService.getRegistrationOrderMapByMemberId();

		// 3.高效率獲取所有會員資料
		List<Member> memberList = memberService.getMembersEfficiently();

		// 4.訂單總額現在可能含補繳常年會費, 所以一次撈完明細, 用來拆出註冊費 / 常年會費兩欄
		List<Long> ordersIds = ordersMap.values().stream().map(Orders::getOrdersId).toList();
		Map<Long, List<OrdersItem>> ordersItemMap = ordersItemService.getOrdersItemsByOrderIds(ordersIds);

		// 5.遍歷會員資料,組裝excelVO對象
		List<MemberExcel> excelData = memberList.stream().map(member -> {
			// 5-1 獲取該會員的訂單
			Orders orders = ordersMap.get(member.getMemberId());

			// 5-2 依明細的產品類型, 分別加總註冊費 與 各年度的補繳常年會費
			List<OrdersItem> ordersItemList = ordersItemMap.getOrDefault(orders.getOrdersId(), List.of());
			BigDecimal registrationFee = OrdersItemUtil.sumByProductType(ordersItemList,
					OrderConstants.ITEMS_SUMMARY_REGISTRATION, OrderConstants.GROUP_ITEMS_SUMMARY_REGISTRATION);
			BigDecimal membershipDue113 = OrdersItemUtil.sumByProductType(ordersItemList,
					OrderConstants.membershipDueProductType(MembershipDueYearEnum.ROC_113.getAdYear()));
			BigDecimal membershipDue114 = OrdersItemUtil.sumByProductType(ordersItemList,
					OrderConstants.membershipDueProductType(MembershipDueYearEnum.ROC_114.getAdYear()));
			BigDecimal membershipDue115 = OrdersItemUtil.sumByProductType(ordersItemList,
					OrderConstants.membershipDueProductType(MembershipDueYearEnum.ROC_115.getAdYear()));

			// 5-3 轉換設置資料
			MemberExcelRaw memberExcelRaw = memberConvert.entityToExcelRaw(member);
			memberExcelRaw.setStatus(orders.getStatus());
			memberExcelRaw.setRegistrationFee(registrationFee);
			memberExcelRaw.setMembershipDue113(membershipDue113);
			memberExcelRaw.setMembershipDue114(membershipDue114);
			memberExcelRaw.setMembershipDue115(membershipDue115);
			memberExcelRaw.setMembershipDue(membershipDue113.add(membershipDue114).add(membershipDue115));
			memberExcelRaw.setTotalAmount(orders.getTotalAmount());
			MemberExcel memberExcel = memberConvert.memberExcelRawToExcel(memberExcelRaw);

			return memberExcel;

		}).toList();

		// 6.輸出成Excel
		EasyExcel.write(response.getOutputStream(), MemberExcel.class).sheet("會員列表").doWrite(excelData);

	}

}
