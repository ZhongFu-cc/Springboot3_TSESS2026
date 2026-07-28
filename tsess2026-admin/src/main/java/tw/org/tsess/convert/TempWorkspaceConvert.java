package tw.org.tsess.convert;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import tw.org.tsess.pojo.DTO.addEntityDTO.AddTempWorkspaceDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutTempWorkspaceDTO;
import tw.org.tsess.pojo.entity.TempWorkspace;
import tw.org.tsess.pojo.excelPojo.WorkspaceExcel;

@Mapper(componentModel = "spring")
public interface TempWorkspaceConvert {

	TempWorkspace addDTOToEntity(AddTempWorkspaceDTO addTempWorkspaceDTO);

	TempWorkspace putDTOToEntity(PutTempWorkspaceDTO putTempWorkspaceDTO);

	@Mapping(target = "status", source = "status", qualifiedByName = "convertStatus")
	WorkspaceExcel entityToExcel(TempWorkspace tempWorkspace);

	@Named("convertStatus")
	default String convertStatus(Integer status) {
		if (status == 0) {
			return "未付款";
		} else if (status == 1) {
			return "已付款";
		} else {
			return "未知狀態";
		}

	}

}
