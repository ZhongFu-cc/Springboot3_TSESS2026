package tw.org.tsess.convert;

import java.util.List;

import org.mapstruct.Mapper;

import tw.org.tsess.pojo.DTO.addEntityDTO.AddOrdersDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutOrdersDTO;
import tw.org.tsess.pojo.VO.OrdersVO;
import tw.org.tsess.pojo.entity.Orders;

@Mapper(componentModel = "spring")
public interface OrdersConvert {

	Orders addDTOToEntity(AddOrdersDTO addOrdersDTO);

	Orders putDTOToEntity(PutOrdersDTO putOrdersDTO);
	
	OrdersVO entityToVO(Orders orders);
	
	List<OrdersVO> entityListToVOList(List<Orders> ordersList);
	
}
