package tw.org.tsess.convert;

import org.mapstruct.Mapper;

import tw.org.tsess.enums.CommonStatusEnum;
import tw.org.tsess.pojo.DTO.addEntityDTO.AddFormDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutFormDTO;
import tw.org.tsess.pojo.VO.FormVO;
import tw.org.tsess.pojo.entity.Form;

@Mapper(componentModel = "spring")
public interface FormConvert {

    // 宣告默認映射 , 告訴 MapStruct 如何把 CommonStatusEnum → Integer
    default Integer commonStatusEnumMapToInteger(CommonStatusEnum status) {
        return status == null ? null : status.getValue();
    }
	
	Form addDTOToEntity(AddFormDTO addFormDTO);
	
	Form putDTOToEntity(PutFormDTO putFormDTO);
	
	FormVO entityToVO(Form form);
	
}
