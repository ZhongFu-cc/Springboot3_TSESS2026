package tw.org.tsess.convert;

import org.mapstruct.Mapper;

import tw.org.tsess.pojo.DTO.addEntityDTO.AddFormResponseDTO;
import tw.org.tsess.pojo.VO.FormResponseVO;
import tw.org.tsess.pojo.entity.FormResponse;

@Mapper(componentModel = "spring")
public interface FormResponseConvert {

    // 宣告默認映射 , 告訴 MapStruct 如何把 CommonStatusEnum → Integer
//    default Integer commonStatusEnumMapToInteger(CommonStatusEnum status) {
//        return status == null ? null : status.getValue();
//    }
	
	FormResponse addDTOToEntity(AddFormResponseDTO formResponseDTO);
	
	FormResponseVO entityToVO(FormResponse formResponse);
	
}
