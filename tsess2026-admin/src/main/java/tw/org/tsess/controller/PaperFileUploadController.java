package tw.org.tsess.controller;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import tw.org.tsess.pojo.entity.PaperReviewerFile;
import tw.org.tsess.service.PaperFileUploadService;
import tw.org.tsess.utils.R;

@Tag(name = "稿件-公文附件上傳API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/paper-file-upload")
public class PaperFileUploadController {

	private final PaperFileUploadService paperFileUploadService;

	@PostMapping(value = "official-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "新增單一稿件「公文」附件", description = "請使用formData包裝,兩個key <br>"
			+ "1.paperId(value = paperId(string)<br>" + "2.檔案 file(value = binary)<br>"
			+ "knife4j Web 文檔顯示有問題, 真實傳輸方式為 「multipart/form-data」<br>"
			+ "請用 http://localhost:8080/swagger-ui/index.html 測試 ")
	@SaCheckRole("super-admin")
	public R<PaperReviewerFile> saveOfficalDocument(@RequestPart("file") @NotNull @Valid MultipartFile file,
			@RequestPart("paperId") @NotNull @Valid String paperId) {
		Long value = Long.valueOf(paperId);
		paperFileUploadService.addOfficialDocument(file, value);
		return R.ok();
	}

	@PutMapping(value = "official-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "修改單一稿件「公文」附件", description = "請使用formData包裝,兩個key <br>"
			+ "1.paperFileId(value = paperFileId(string)<br>" + "2.檔案 file(value = binary)<br>"
			+ "knife4j Web 文檔顯示有問題, 真實傳輸方式為 「multipart/form-data」<br>"
			+ "請用 http://localhost:8080/swagger-ui/index.html 測試 ")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<PaperReviewerFile> updateOfficalDocument(@RequestPart("file") @NotNull @Valid MultipartFile file,
			@RequestPart("paperFileId") @NotNull @Valid String paperFileId) {

		Long value = Long.valueOf(paperFileId);
		paperFileUploadService.updateOfficialDocument(file, value);
		return R.ok();
	}

	@DeleteMapping("official-document/{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "刪除稿件「公文」附件")
	@SaCheckRole("super-admin")
	public R<PaperReviewerFile> deleteOfficalDocument(@PathVariable("id") Long paperFileId) {
		paperFileUploadService.removeOfficialDocument(paperFileId);
		return R.ok();
	}

}
