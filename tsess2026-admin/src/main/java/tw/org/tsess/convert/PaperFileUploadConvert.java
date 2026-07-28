package tw.org.tsess.convert;

import org.mapstruct.Mapper;

import tw.org.tsess.pojo.DTO.addEntityDTO.AddPaperFileUploadDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutPaperFileUploadDTO;
import tw.org.tsess.pojo.entity.PaperFileUpload;

@Mapper(componentModel = "spring")
public interface PaperFileUploadConvert {

	PaperFileUpload addDTOToEntity(AddPaperFileUploadDTO addPaperFileUploadDTO);

	PaperFileUpload putDTOToEntity(PutPaperFileUploadDTO putPaperFileUploadDTO);
	
	
	
}
