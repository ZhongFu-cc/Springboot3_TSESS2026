package tw.org.tsess.convert;

import java.util.List;

import org.mapstruct.Mapper;

import tw.org.tsess.pojo.DTO.addEntityDTO.AddOrdersItemDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutOrdersItemDTO;
import tw.org.tsess.pojo.VO.OrdersItemVO;
import tw.org.tsess.pojo.entity.OrdersItem;

@Mapper(componentModel = "spring")
public interface OrdersItemConvert {

	OrdersItem addDTOToEntity(AddOrdersItemDTO addOrdersItemDTO);

	OrdersItem putDTOToEntity(PutOrdersItemDTO putOrdersItemDTO);
	
	OrdersItemVO entityToVO(OrdersItem ordersItem);
	
	List<OrdersItemVO> entityListToVOList(List<OrdersItem> ordersItemList);
	
}
