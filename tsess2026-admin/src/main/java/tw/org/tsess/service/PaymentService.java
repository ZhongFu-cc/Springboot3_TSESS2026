package tw.org.tsess.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.org.tsess.pojo.DTO.ECPayDTO.ECPayResponseDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutPaymentDTO;
import tw.org.tsess.pojo.entity.Payment;

public interface PaymentService extends IService<Payment> {

	Payment getPayment(Long paymentId);

	List<Payment> getPaymentList();

	IPage<Payment> getPaymentPage(Page<Payment> pageable);
	
	Payment addPayment(ECPayResponseDTO ECPayResponseDTO);

	void updatePayment(PutPaymentDTO putPaymentDTO);

	void deletePayment(Long paymentId);

	/**
	 * 依訂單ID查詢付款紀錄, 取交易成功 (rtn_code = 1) 且付款日期最新的一筆<br>
	 * 查無 (例如人工確認收款, 沒有走綠界) 時回傳 null
	 *
	 * @param ordersId
	 * @return
	 */
	Payment getLatestSuccessPaymentByOrdersId(Long ordersId);

	void deletePaymentList(List<Long> paymentIds);

}
