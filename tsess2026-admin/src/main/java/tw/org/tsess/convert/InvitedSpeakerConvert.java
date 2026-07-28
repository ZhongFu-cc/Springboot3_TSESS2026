package tw.org.tsess.convert;

import org.mapstruct.Mapper;

import tw.org.tsess.pojo.DTO.addEntityDTO.AddInvitedSpeakerDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutInvitedSpeakerDTO;
import tw.org.tsess.pojo.entity.InvitedSpeaker;

@Mapper(componentModel = "spring")
public interface InvitedSpeakerConvert {

	InvitedSpeaker addDTOToEntity(AddInvitedSpeakerDTO addInvitedSpeakerDTO);

	InvitedSpeaker putDTOToEntity(PutInvitedSpeakerDTO putInvitedSpeakerDTO);
	
	
}
