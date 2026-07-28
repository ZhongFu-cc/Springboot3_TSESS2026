package tw.org.tsess.convert;

import org.mapstruct.Mapper;

import tw.org.tsess.pojo.DTO.PutPaperReviewDTO;
import tw.org.tsess.pojo.VO.AssignedReviewersVO;
import tw.org.tsess.pojo.VO.ReviewerScoreStatsVO;
import tw.org.tsess.pojo.entity.PaperAndPaperReviewer;

@Mapper(componentModel = "spring")
public interface PaperAndPaperReviewerConvert {


	PaperAndPaperReviewer putDTOToEntity(PutPaperReviewDTO putPaperReviewDTO);

	AssignedReviewersVO entityToAssignedReviewersVO(PaperAndPaperReviewer paperAndPaperReviewer);

	ReviewerScoreStatsVO entityToReviewerScoreStatsVO(PaperAndPaperReviewer paperAndPaperReviewer);
}
