package tw.org.tsess.pojo.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SettingVO {

	@Schema(description = "註冊是否開啟;同時也是當下是否能繳費")
	private Boolean isRegistrationOpen;

	@Schema(description = "團體註冊是否開啟")
	private Boolean isGroupRegistrationOpen;

	@Schema(description = "投稿摘要是否開啟")
	private Boolean isAbstractSubmissionOpen;

	@Schema(description = "Slide上傳是否開啟")
	private Boolean isSlideUploadOpen;

}
