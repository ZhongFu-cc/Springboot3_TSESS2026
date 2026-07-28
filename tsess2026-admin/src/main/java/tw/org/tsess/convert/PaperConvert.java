package tw.org.tsess.convert;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import tw.org.tsess.enums.PaperStatusEnum;
import tw.org.tsess.pojo.DTO.PutPaperForAdminDTO;
import tw.org.tsess.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.org.tsess.pojo.VO.PaperTagVO;
import tw.org.tsess.pojo.VO.PaperVO;
import tw.org.tsess.pojo.VO.ReviewVO;
import tw.org.tsess.pojo.entity.Paper;
import tw.org.tsess.pojo.excelPojo.PaperScoreExcel;

@Mapper(componentModel = "spring")
public interface PaperConvert {

	Paper addDTOToEntity(AddPaperDTO addPaperDTO);

	Paper putDTOToEntity(PutPaperDTO putPaperDTO);

	Paper putForAdminDTOToEntity(PutPaperForAdminDTO putPaperForAdminDTO);


	/**
	 * 給投稿者的VO
	 * 
	 * @param paper
	 * @return
	 */
	PaperVO entityToVO(Paper paper);
	
	/**
	 * 給管理者的詳細VO
	 * 
	 * @param paper
	 * @return
	 */
	PaperTagVO entityToTagVO(Paper paper);

	ReviewVO entityToReviewVO(Paper paper);

	@Mapping(source = "paperId", target = "paperId", qualifiedByName = "convertLongToString")
	@Mapping(source = "memberId", target = "memberId", qualifiedByName = "convertLongToString")
	@Mapping(source = "status", target = "status", qualifiedByName = "convertStatusToString")
	PaperScoreExcel entityToExcel(Paper paper);
	
	@Mapping(source = "paperId", target = "paperId", qualifiedByName = "convertStringToLong")
	@Mapping(source = "status", target = "status", qualifiedByName = "convertStringToStatus")
	PutPaperForAdminDTO excelToUpdatePojo(PaperScoreExcel paperScoreExcel);

	@Named("convertLongToString")
	default String convertLongToString(Long id) {
		return id.toString();
	}
	
	@Named("convertStringToLong")
	default Long convertStringToLong(String value) {

	    if (value == null) {
	        return null;
	    }

	    String trimmed = value.trim();
	    if (trimmed.isEmpty()) {
	        return null;
	    }

	    try {
	        return Long.valueOf(trimmed);
	    } catch (NumberFormatException ex) {
	        throw new IllegalArgumentException(
	            "無法轉換為 Long: [" + value + "]", ex
	        );
	    }
	}

	@Named("convertStatusToString")
	default String convertStatusToString(Integer status) {
		return PaperStatusEnum.fromValue(status).getLabelZh();
	}
	
	@Named("convertStringToStatus")
	default Integer convertStringToStatus(String status) {
		return PaperStatusEnum.fromLabelZh(status).getValue();
	}

}
