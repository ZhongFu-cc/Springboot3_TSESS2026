package tw.org.tsess.convert;

import org.mapstruct.Mapper;

import tw.org.tsess.pojo.DTO.addEntityDTO.AddScheduleEmailRecordDTO;
import tw.org.tsess.pojo.entity.ScheduleEmailRecord;

@Mapper(componentModel = "spring")
public interface ScheduleEmailRecordConvert {

	ScheduleEmailRecord addDTOToEntity(AddScheduleEmailRecordDTO addScheduleEmailRecordDTO);

	ScheduleEmailRecord copyEntity(ScheduleEmailRecord scheduleEmailRecord);
	
}
