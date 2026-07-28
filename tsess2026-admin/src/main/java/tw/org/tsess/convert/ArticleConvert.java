package tw.org.tsess.convert;

import org.mapstruct.Mapper;

import tw.org.tsess.pojo.DTO.addEntityDTO.AddArticleDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutArticleDTO;
import tw.org.tsess.pojo.entity.Article;

@Mapper(componentModel = "spring")
public interface ArticleConvert {

	Article addDTOToEntity(AddArticleDTO insertArticleDTO);

	Article putDTOToEntity(PutArticleDTO updateArticleDTO);
	
	Article copyEntity(Article article);
	
}
