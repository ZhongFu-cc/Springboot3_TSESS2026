package tw.org.tsess.convert;

import org.mapstruct.Mapper;

import tw.org.tsess.pojo.DTO.addEntityDTO.AddTagDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutTagDTO;
import tw.org.tsess.pojo.entity.Tag;

@Mapper(componentModel = "spring")
public interface TagConvert {

	Tag addDTOToEntity(AddTagDTO addTagDTO);
	
	Tag putDTOToEntity(PutTagDTO updateTagDTO);
	
}
