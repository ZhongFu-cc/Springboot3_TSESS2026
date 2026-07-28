package tw.org.tsess.validation.constraint;

import tw.org.tsess.enums.CommonStatusEnum;

public interface HasLoginAndMultipleSubmissionRules {

	public CommonStatusEnum getRequireLogin();
	public CommonStatusEnum getAllowMultipleSubmissions();
	
}
