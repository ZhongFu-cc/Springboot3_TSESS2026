package tw.org.tsess.pojo.DTO.putEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PutTempWorkspaceDTO {

    @Schema(description = "主鍵ID")
    private Long tempWorkspaceId;
	
    @NotBlank
    @Schema(description = "中文姓名，外國人非必填，台灣人必填")
    private String chineseName;

    @NotBlank
    @Schema(description = "名字, 華人的名在後  , 外國人的名在前")
    private String firstName;

    @NotBlank
    @Schema(description = "姓氏, 華人的姓氏在前, 外國人的姓氏在後")
    private String lastName;

    @NotBlank
    @Schema(description = "E-Mail")
    private String email;
    
    @NotBlank
    @Schema(description = "電話號碼")
    private String phone;
    
    @NotBlank
	@Schema(description = "單位(所屬的機構)")
	private String affiliation;

    @NotBlank
	@Schema(description = "職稱")
	private String jobTitle;
	
}
