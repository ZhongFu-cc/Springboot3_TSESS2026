package tw.org.tsess.controller;

import java.io.IOException;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.org.tsess.pojo.DTO.ECPayDTO.ECPayResponseDTO;
import tw.org.tsess.pojo.DTO.addEntityDTO.AddTempWorkspaceDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutTempWorkspaceDTO;
import tw.org.tsess.pojo.entity.Member;
import tw.org.tsess.pojo.entity.TempWorkspace;
import tw.org.tsess.service.TempWorkspaceService;
import tw.org.tsess.utils.R;

/**
 * <p>
 * TICBCS 臨時表 , 收集工作坊資料 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2026-03-05
 */
@Tag(name = "工作坊表單API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/temp-workspace")
public class TempWorkspaceController {
	
	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;
	private final TempWorkspaceService tempWorkspaceService;
	
	@GetMapping("{id}")
	@Operation(summary = "查詢單一工作坊報名者For管理者")
	@SaCheckRole("super-admin")
	public R<TempWorkspace> getMember(@PathVariable("id") Long tempWorkspaceId) {
		TempWorkspace Registrant = tempWorkspaceService.searchRegistrant(tempWorkspaceId);
		return R.ok(Registrant);
	}
	
	@GetMapping("pagination")
	@Operation(summary = "查詢全部工作坊報名者(分頁)")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<IPage<TempWorkspace>> getTempWorkspacePage(@RequestParam Integer page, @RequestParam Integer size) {
		Page<TempWorkspace> pageable = new Page<TempWorkspace>(page, size);
		IPage<TempWorkspace> registrantPage = tempWorkspaceService.searchRegistrantPage(pageable);
		return R.ok(registrantPage);
	}
	
	
	@PostMapping
	@Operation(summary = "工作坊報名")
	public R<String> save(@RequestBody @Valid AddTempWorkspaceDTO addTempWorkspaceDTO){

		// 透過key 獲取redis中的驗證碼
		String redisCode = redissonClient.<String>getBucket(addTempWorkspaceDTO.getVerificationKey()).get();
		String userVerificationCode = addTempWorkspaceDTO.getVerificationCode();

		// 判斷驗證碼是否正確,如果不正確就直接返回前端,不做後續的業務處理
		if (userVerificationCode == null || redisCode == null
				|| !redisCode.equals(userVerificationCode.trim().toLowerCase())) {
			return R.fail("Verification code is incorrect");
		}
		
		// 驗證通過,刪除key 並往後執行添加操作
		redissonClient.getBucket(addTempWorkspaceDTO.getVerificationKey()).delete();

		// 進行新增 , 並把附件檔案寄送給財務 , 本身不留信用卡授權書
		String form = tempWorkspaceService.add(addTempWorkspaceDTO);

		return R.ok("操作成功",form);
	}
	
	@PostMapping(value = "payment",consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@Operation(summary = "接收綠界回傳資料，修改工作坊報名狀態")
	public String savePayment(@ModelAttribute @Valid ECPayResponseDTO ECPayResponseDTO) {
		System.out.println(ECPayResponseDTO);
		tempWorkspaceService.handleEcpayCallback(ECPayResponseDTO);
		return "1|OK";
	}
	
	/**
	 * 測試用
	 * @param ECPayResponseDTO
	 * @return
	 */
//	@PostMapping(value = "payment")
//	@Operation(summary = "接收綠界回傳資料，修改工作坊報名狀態")
//	public String savePayment(@RequestBody @Valid ECPayResponseDTO ECPayResponseDTO) {
//		tempWorkspaceService.handleEcpayCallback(ECPayResponseDTO);
//		return "1|OK";
//	}
	
	@PutMapping
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "修改工作坊報名者資料For管理者")
	@SaCheckRole("super-admin")
	public R<Member> updateTempWorkspace(@RequestBody @Valid PutTempWorkspaceDTO putTempWorkspaceDTO) {
		// 直接更新工作坊報名者資料
		tempWorkspaceService.modify(putTempWorkspaceDTO);
		return R.ok();
	}
	
	@DeleteMapping("{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@Operation(summary = "刪除工作坊報名者")
	public R<Member> deleteTempWorkspacer(@PathVariable("id") Long tempWorkspaceId) {
		tempWorkspaceService.remove(tempWorkspaceId);
		return R.ok();
	}
	
	@Operation(summary = "下載 工作坊報名者 excel列表")
	@SaCheckRole("super-admin")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@GetMapping("/download-excel")
	public void downloadExcel(HttpServletResponse response) throws IOException {
		tempWorkspaceService.downloadExcel(response);
	}


}
