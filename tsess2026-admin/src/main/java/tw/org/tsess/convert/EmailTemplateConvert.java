package tw.org.tsess.convert;

import org.mapstruct.Mapper;

import tw.org.tsess.pojo.DTO.addEntityDTO.AddEmailTemplateDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutEmailTemplateDTO;
import tw.org.tsess.pojo.entity.EmailTemplate;

@Mapper(componentModel = "spring")
public interface EmailTemplateConvert {

	EmailTemplate insertDTOToEntity(AddEmailTemplateDTO addArticleDTO);

	EmailTemplate updateDTOToEntity(PutEmailTemplateDTO updateArticleDTO);
	
}
