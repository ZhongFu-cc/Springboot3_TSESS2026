package tw.org.tsess.convert;

import java.util.List;

import org.mapstruct.Mapper;

import tw.org.tsess.pojo.DTO.addEntityDTO.AddCheckinRecordDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutCheckinRecordDTO;
import tw.org.tsess.pojo.VO.CheckinRecordVO;
import tw.org.tsess.pojo.entity.CheckinRecord;
import tw.org.tsess.pojo.excelPojo.AttendeesExcel;
import tw.org.tsess.pojo.excelPojo.CheckinRecordExcel;

@Mapper(componentModel = "spring")
public interface CheckinRecordConvert {

	CheckinRecord addDTOToEntity(AddCheckinRecordDTO addCheckinRecordDTO);

	CheckinRecord putDTOToEntity(PutCheckinRecordDTO putCheckinRecordDTO);

	CheckinRecordVO entityToVO(CheckinRecord checkinRecord);

	List<CheckinRecordVO> entityListToVOList(List<CheckinRecord> checkinRecordList);

	CheckinRecordExcel attendeesExcelToCheckinRecordExcel(AttendeesExcel attendeesExcel);
	
}
