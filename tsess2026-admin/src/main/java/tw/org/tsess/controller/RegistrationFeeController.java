package tw.org.tsess.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.org.tsess.manager.RegistrationOrderCalculator;
import tw.org.tsess.pojo.BO.OrderDraftBO;
import tw.org.tsess.pojo.DTO.RegistrationFeePreviewDTO;
import tw.org.tsess.pojo.VO.RegistrationFeePreviewVO;
import tw.org.tsess.utils.R;

@Tag(name = "註冊費用試算API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/registration-fee")
public class RegistrationFeeController {

	private final RegistrationOrderCalculator registrationOrderCalculator;

	/**
	 * 用 POST 而非 GET , 除了前端傳參方便 , idCard 屬於個資 , 放在 body 也不會被記進存取紀錄
	 */
	@PostMapping("preview")
	@Operation(summary = "註冊前試算本次應付金額, 報名費與補繳常年會費分開列出")
	public R<RegistrationFeePreviewVO> previewRegistrationFee(
			@RequestBody @Valid RegistrationFeePreviewDTO registrationFeePreviewDTO) {

		// 1.與正式註冊共用同一份計算邏輯, 只是不落庫, 確保試算與實際收費一致
		OrderDraftBO draft = registrationOrderCalculator.calculate(registrationFeePreviewDTO.getCountry(),
				registrationFeePreviewDTO.getCategory(), registrationFeePreviewDTO.getIdCard());

		RegistrationFeePreviewVO registrationFeePreviewVO = new RegistrationFeePreviewVO();
		registrationFeePreviewVO.setRegistrationFee(draft.getRegistrationFee());
		registrationFeePreviewVO.setMembershipDue(draft.getMembershipDue());
		registrationFeePreviewVO.setTotalAmount(draft.getTotalAmount());
		registrationFeePreviewVO.setFree(draft.isFree());

		return R.ok(registrationFeePreviewVO);
	}

}
