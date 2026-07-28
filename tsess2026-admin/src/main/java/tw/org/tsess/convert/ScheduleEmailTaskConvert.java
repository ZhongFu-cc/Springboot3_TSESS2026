package tw.org.tsess.convert;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import tw.org.tsess.pojo.DTO.SendEmailDTO;
import tw.org.tsess.pojo.entity.ScheduleEmailTask;

@Mapper(componentModel = "spring")
public interface ScheduleEmailTaskConvert {

	// DTO 名稱不同的屬性名轉換
	@Mapping(source = "scheduleTime", target = "startTime")
	ScheduleEmailTask DTOToEntity(SendEmailDTO sendEmailDTO);

	ScheduleEmailTask copyEntity(ScheduleEmailTask scheduleEmailTask);

}
