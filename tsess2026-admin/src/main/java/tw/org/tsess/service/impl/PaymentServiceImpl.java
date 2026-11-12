package tw.org.tsess.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.org.tsess.convert.PaymentConvert;
import tw.org.tsess.mapper.PaymentMapper;
import tw.org.tsess.pojo.DTO.ECPayDTO.ECPayResponseDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutPaymentDTO;
import tw.org.tsess.pojo.entity.Payment;
import tw.org.tsess.service.PaymentService;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {

	private final PaymentConvert paymentConvert;

	@Override
	public Payment getPayment(Long paymentId) {
		Payment payment = baseMapper.selectById(paymentId);
		return payment;
	}

	@Override
	public List<Payment> getPaymentList() {
		List<Payment> paymentList = baseMapper.selectList(null);
		return paymentList;
	}

	@Override
	public IPage<Payment> getPaymentPage(Page<Payment> page) {
		Page<Payment> paymentPage = baseMapper.selectPage(page, null);
		return paymentPage;
	}

	@Override
	public void updatePayment(PutPaymentDTO putPaymentDTO) {
		Payment payment = paymentConvert.putDTOToEntity(putPaymentDTO);
		baseMapper.updateById(payment);

	}

	@Override
	public void deletePayment(Long paymentId) {
		baseMapper.deleteById(paymentId);

	}

	@Override
	public void deletePaymentList(List<Long> paymentIds) {
		baseMapper.deleteBatchIds(paymentIds);
	}

	@Override
	public Payment getLatestSuccessPaymentByOrdersId(Long ordersId) {

		if (ordersId == null) {
			return null;
		}

		// 交易狀態 rtn_code 為 "1" 才算成功, 同一張訂單理論上只會有一筆,
		// 仍以付款日期由新到舊取第一筆保險
		LambdaQueryWrapper<Payment> paymentWrapper = new LambdaQueryWrapper<>();
		paymentWrapper.eq(Payment::getOrdersId, ordersId)
				.eq(Payment::getRtnCode, "1")
				.orderByDesc(Payment::getPaymentDate)
				.last("LIMIT 1");

		return baseMapper.selectOne(paymentWrapper);
	}

	@Override
	public Payment addPayment(ECPayResponseDTO ECPayResponseDTO) {
		// 1.轉換綠界金流official Data 轉換 自己這邊的Entity
		Payment payment = paymentConvert.officialDataToEntity(ECPayResponseDTO);
		log.info("綠界金流回傳付款確認後資料：　" + payment);

		// 2.新增響應回來的交易紀錄
		baseMapper.insert(payment);

		return payment;
	}

}
