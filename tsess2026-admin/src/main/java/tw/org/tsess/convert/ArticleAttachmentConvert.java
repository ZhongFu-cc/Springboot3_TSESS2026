package tw.org.tsess.convert;

import org.mapstruct.Mapper;

import tw.org.tsess.pojo.DTO.addEntityDTO.AddArticleAttachmentDTO;
import tw.org.tsess.pojo.entity.ArticleAttachment;

@Mapper(componentModel = "spring")
public interface ArticleAttachmentConvert {
	ArticleAttachment addDTOToEntity(AddArticleAttachmentDTO addArticleAttachmentDTO);
}
