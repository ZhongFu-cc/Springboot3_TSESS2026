package tw.org.tsess.pojo.VO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewStatsVO {

	// 應審核數量
	Integer toBeReviewedCount;
	
	// 已審核數量
	Integer reviewedCount;
	
	// 未審核數量
	Integer notReviewedCount;

}
