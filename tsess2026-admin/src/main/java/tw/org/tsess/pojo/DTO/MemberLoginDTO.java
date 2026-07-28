package tw.org.tsess.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MemberLoginDTO {

	@NotBlank
	@Schema(description = "外國人使用「信箱」，台灣人使用「身分證字號」")
	private String account;

	@NotBlank
	@Schema(description = "密碼")
	private String password;
	
	@NotBlank
	@Schema(description = "驗證碼key")
	private String verificationKey;
	
	@NotBlank
	@Schema(description = "用戶輸入的驗證碼")
	private String verificationCode;
	
	
}
