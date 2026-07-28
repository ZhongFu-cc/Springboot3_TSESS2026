package tw.org.tsess.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OfflineTransferDTO {

	@NotNull
	@Schema(description = "訂單ID")
	private Long orderId;
	
	@NotBlank
	@Schema(description = "匯款帳號-後五碼  台灣會員使用")
	private String remitAccountLast5;
	
}
