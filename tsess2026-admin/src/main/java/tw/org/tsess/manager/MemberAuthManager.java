package tw.org.tsess.manager;

import org.springframework.stereotype.Component;

import cn.dev33.satoken.stp.SaTokenInfo;
import lombok.RequiredArgsConstructor;
import tw.org.tsess.pojo.DTO.EmailBodyContent;
import tw.org.tsess.pojo.DTO.MemberEmailLogin;
import tw.org.tsess.pojo.DTO.MemberIdCardLogin;
import tw.org.tsess.pojo.DTO.MemberLoginDTO;
import tw.org.tsess.pojo.entity.Member;
import tw.org.tsess.service.AsyncService;
import tw.org.tsess.service.MemberService;
import tw.org.tsess.service.NotificationService;

@RequiredArgsConstructor
@Component
public class MemberAuthManager {

	private final MemberService memberService;
	private final NotificationService notificationService;
	private final AsyncService asyncService;

	/**
	 * 會員登入 - Email & Password
	 * 
	 * @param memberEmailLogin
	 * @return
	 */
	public SaTokenInfo login(MemberEmailLogin memberEmailLogin) {
		return memberService.login(memberEmailLogin);
	}
	
	/**
	 * 會員登入 - IdCard & Password
	 * 
	 * @param memberIdCardLogin
	 * @return
	 */
	public SaTokenInfo login(MemberIdCardLogin memberIdCardLogin) {
		return memberService.login(memberIdCardLogin);
	}
	
	/**
	 * 「外國人」登入 - Email & Password 綁定國籍「非」台灣 
	 * 
	 * @param memberLoginDTO
	 * @return
	 */
	public SaTokenInfo foreignLogin(MemberLoginDTO memberLoginDTO) {
		return memberService.foreignLogin(memberLoginDTO);
	}
	
	/**
	 * 「本國人」登入 - IdCard & Password 綁定國籍 台灣 
	 * 
	 * @param memberLoginDTO
	 * @return
	 */
	public SaTokenInfo localLogin(MemberLoginDTO memberLoginDTO) {
		return memberService.localLogin(memberLoginDTO);
	}
	
	

	/**
	 * 會員登出
	 * 
	 */
	public void logout() {
		memberService.logout();
	};

	/**
	 * 忘記密碼
	 * 
	 * @param email
	 */
	public void forgetPassword(String email) {

		// 1.先透過email查找是否為註冊過的會員
		Member member = memberService.getMemberByEmail(email);

		// 2.產生找回密碼的信件內容
		EmailBodyContent retrieveContent = notificationService.generateRetrieveContent(member.getPassword());

		// 3.將密碼寄送到信箱
		asyncService.sendCommonEmail(email, "Retrieve password", retrieveContent.getHtmlContent(),
				retrieveContent.getPlainTextContent());

	}
	
	public Member getMemberInfo() {
		return memberService.getMemberInfo();
	};
	
}
