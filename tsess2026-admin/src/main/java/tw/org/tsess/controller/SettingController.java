package tw.org.tsess.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutSettingDTO;
import tw.org.tsess.pojo.VO.SettingVO;
import tw.org.tsess.pojo.entity.Setting;
import tw.org.tsess.service.SettingService;
import tw.org.tsess.utils.R;

@Tag(name = "設定API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/setting")
public class SettingController {

	private final SettingService settingService;

	@GetMapping()
	@Operation(summary = "查詢設定")
	public R<Setting> getSetting() {
		Setting setting = settingService.getSetting();
		return R.ok(setting);
	}

	@GetMapping("front")
	@Operation(summary = "查詢各類功能是否開放")
	public R<SettingVO> getFrontSetting() {
		SettingVO frontSetting = settingService.getFrontSetting();
		return R.ok(frontSetting);
	}

	@PutMapping
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@Operation(summary = "修改設定")
	public R<Setting> updateSetting(@RequestBody @Valid PutSettingDTO putSettingDTO) {
		settingService.updateSetting(putSettingDTO);
		return R.ok();
	}

}
