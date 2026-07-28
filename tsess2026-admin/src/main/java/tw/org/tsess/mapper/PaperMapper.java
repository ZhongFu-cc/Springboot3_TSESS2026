package tw.org.tsess.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import tw.org.tsess.pojo.entity.Paper;

/**
 * <p>
 * 投稿資料表 Mapper 接口
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
public interface PaperMapper extends BaseMapper<Paper> {

	@Select("SELECT * FROM paper WHERE is_deleted = 0")
	List<Paper> selectPapers();
	
	@Select("SELECT * FROM paper WHERE status = #{status} AND is_deleted = 0")
	List<Paper> selectPapersByStatus(@Param("status") Integer status);
}
