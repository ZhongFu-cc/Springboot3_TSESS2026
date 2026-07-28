package tw.org.tsess.convert;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import tw.org.tsess.pojo.DTO.ECPayDTO.ECPayResponseDTO;
import tw.org.tsess.pojo.DTO.addEntityDTO.AddPaymentDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutPaymentDTO;
import tw.org.tsess.pojo.VO.PaymentVO;
import tw.org.tsess.pojo.entity.Payment;

@Mapper(componentModel = "spring")
public interface PaymentConvert {

	@Mapping(target = "ordersId", source = "customField1")
	@Mapping(target = "merchantId", source = "merchantID")
	@Mapping(target = "merchantTradeNumber", source = "merchantTradeNo")
	@Mapping(target = "storeId", source = "storeID")
	@Mapping(target = "rtnCode", source = "rtnCode")
	@Mapping(target = "rtnMsg", source = "rtnMsg")
	@Mapping(target = "tradeNumber", source = "tradeNo")
	@Mapping(target = "tradeAmt", source = "tradeAmt")
	@Mapping(target = "paymentDate", source = "paymentDate", qualifiedByName = "stringToLocalDateTime")
	@Mapping(target = "paymentType", source = "paymentType")
	@Mapping(target = "paymentTypeChargeFee", source = "paymentTypeChargeFee")
	@Mapping(target = "tradeDate", source = "tradeDate", qualifiedByName = "stringToLocalDateTime")
	@Mapping(target = "platformId", source = "platformID")
	@Mapping(target = "simulatePaid", source = "simulatePaid")
	@Mapping(target = "checkMacValue", source = "checkMacValue")
	Payment officialDataToEntity(ECPayResponseDTO ecPayResponseDTO);

	Payment addDTOToEntity(AddPaymentDTO addPaymentDTO);

	Payment putDTOToEntity(PutPaymentDTO putPaymentDTO);

	PaymentVO entityToVO(Payment payment);

	List<PaymentVO> entityListToVOList(List<Payment> paymentList);

	@Named("stringToLocalDateTime")
	default LocalDateTime stringToLocalDateTime(String date) {
		if (date == null || date.isEmpty()) {
			return null;
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		return LocalDateTime.parse(date, formatter);

	}

}
