package tw.org.tsess.convert;

import java.util.List;

import org.mapstruct.Mapper;

import tw.org.tsess.pojo.DTO.addEntityDTO.AddSettingDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutSettingDTO;
import tw.org.tsess.pojo.VO.SettingVO;
import tw.org.tsess.pojo.entity.Setting;

@Mapper(componentModel = "spring")
public interface SettingConvert {

	Setting addDTOToEntity(AddSettingDTO addSettingDTO);

	Setting putDTOToEntity(PutSettingDTO putSettingDTO);
	
	SettingVO entityToVO(Setting setting);
	
	List<SettingVO> entityListToVOList(List<Setting> settingList);
	
}
