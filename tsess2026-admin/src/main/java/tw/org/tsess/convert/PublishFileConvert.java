package tw.org.tsess.convert;

import org.mapstruct.Mapper;

import tw.org.tsess.pojo.DTO.addEntityDTO.AddPublishFileDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutPublishFileDTO;
import tw.org.tsess.pojo.entity.PublishFile;

@Mapper(componentModel = "spring")
public interface PublishFileConvert {

	PublishFile addDTOToEntity(AddPublishFileDTO addPublishFileDTO);

	PublishFile putDTOToEntity(PutPublishFileDTO putPublishFileDTO);

}
