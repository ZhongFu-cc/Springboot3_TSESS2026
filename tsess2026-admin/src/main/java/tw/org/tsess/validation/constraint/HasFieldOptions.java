package tw.org.tsess.validation.constraint;

import tw.org.tsess.enums.FormFieldTypeEnum;
import tw.org.tsess.pojo.DTO.FormFieldOptionDTO;

public interface HasFieldOptions {

	public FormFieldTypeEnum getFieldType();
	
	public FormFieldOptionDTO getOptions();
	
}
