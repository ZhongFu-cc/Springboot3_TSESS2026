package tw.org.tsess.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 註冊前的費用試算條件<br>
 * 只收計費用得到的欄位, 對應 AddMemberDTO 的同名屬性
 */
@Data
public class RegistrationFeePreviewDTO {

	@Schema(description = "同時作為護照號碼 和 台灣身分證字號使用, 用於比對常年會費欠繳名單, 未填則視為無欠費")
	private String idCard;

	@NotBlank
	@Schema(description = "國家")
	private String country;

	@NotNull
	@Schema(description = "用於分類會員資格, 1為 Member，2為 Others，3為 Non-Member，4為 MVP，5為 Speaker，6為 Moderator，7為 Staff")
	private Integer category;

}
