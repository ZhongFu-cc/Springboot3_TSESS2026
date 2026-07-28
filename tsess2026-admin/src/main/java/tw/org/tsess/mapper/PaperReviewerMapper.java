package tw.org.tsess.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import tw.org.tsess.pojo.entity.PaperReviewer;

/**
 * <p>
 * 稿件評審資料表 Mapper 接口
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
public interface PaperReviewerMapper extends BaseMapper<PaperReviewer> {

	@Select("SELECT * FROM paper_reviewer WHERE is_deleted = 0")
	List<PaperReviewer> selectReviewers();
	
}
