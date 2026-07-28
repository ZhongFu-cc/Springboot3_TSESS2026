package tw.org.tsess.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import ecpay.payment.integration.AllInOne;
import ecpay.payment.integration.domain.AioCheckOutOneTime;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tw.org.tsess.convert.TempWorkspaceConvert;
import tw.org.tsess.enums.ECpayRtnCodeEnum;
import tw.org.tsess.mapper.TempWorkspaceMapper;
import tw.org.tsess.pojo.DTO.ECPayDTO.ECPayResponseDTO;
import tw.org.tsess.pojo.DTO.addEntityDTO.AddTempWorkspaceDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutTempWorkspaceDTO;
import tw.org.tsess.pojo.entity.Payment;
import tw.org.tsess.pojo.entity.TempWorkspace;
import tw.org.tsess.pojo.excelPojo.WorkspaceExcel;
import tw.org.tsess.service.AsyncService;
import tw.org.tsess.service.PaymentService;
import tw.org.tsess.service.TempWorkspaceService;

/**
 * <p>
 * TICBCS 臨時表 , 收集工作坊資料 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2026-03-05
 */
@Service
@RequiredArgsConstructor
public class TempWorkspaceServiceImpl extends ServiceImpl<TempWorkspaceMapper, TempWorkspace>
		implements TempWorkspaceService {

	@Value("${project.banner-url}")
	private String BANNER_PHOTO_URL; 

	private final TempWorkspaceConvert tempWorkspaceConvert;
	private final PaymentService paymentService;
	private final AsyncService asyncService;
	private static final AtomicInteger SEQ = new AtomicInteger(0);
	private static volatile long lastMillis = -1L;

	@Override
	public TempWorkspace searchRegistrant(Long tempWorkspaceId) {
		return baseMapper.selectById(tempWorkspaceId);
	}

	@Override
	public IPage<TempWorkspace> searchRegistrantPage(Page<TempWorkspace> page) {
		return baseMapper.selectPage(page, null);
	}
	
	
	@Override
	public String add(AddTempWorkspaceDTO addTempWorkspace) {
		System.out.println("觸發工作坊報名");

		// 轉換資料並儲存
		TempWorkspace entity = tempWorkspaceConvert.addDTOToEntity(addTempWorkspace);
		// 報名時一定沒付款
		entity.setStatus(0);
		baseMapper.insert(entity);

		// ---------------------------

		// 1.創建綠界全方位金流對象
		AllInOne allInOne = new AllInOne("");

		// 2.創建信用卡一次付清模式
		AioCheckOutOneTime aioCheckOutOneTime = new AioCheckOutOneTime();

		// 5.獲取當前時間並格式化，為了填充交易時間
		LocalDateTime now = LocalDateTime.now();
		String nowFormat = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));

		// 訂單交易編號,僅接受20位長度，編號不可重複，使用自定義生成function 處理
		aioCheckOutOneTime.setMerchantTradeNo(this.generateTradeNo());
		// 設定交易日期
		aioCheckOutOneTime.setMerchantTradeDate(nowFormat);
		// 綠界金流 僅接受新台幣 以及整數的金額，所以BigDecimal 要進行去掉無意義的0以及轉換成String
		aioCheckOutOneTime.setTotalAmount(BigDecimal.valueOf(5000L).stripTrailingZeros().toPlainString());
		// 設定交易描述
		aioCheckOutOneTime.setTradeDesc("This payment page only displays the total amount.");
		// 設定交易產品名稱概要,他沒有辦法一個item對應一個amount , 但可以透過#將item分段顯示
		// 例如: item01#item02#item03
		aioCheckOutOneTime.setItemName("乳癌遺傳性基因檢測工作坊報名費");
		// 設定付款完成後，返回的前端網址，這邊讓他回到官網
		aioCheckOutOneTime.setClientBackURL("https://ticbcs.org.tw/workshop/finish");
		// 設定付款完成通知的網址,應該可以直接設定成後端API，實證有效
		aioCheckOutOneTime.setReturnURL("https://ticbcs.org.tw/prod-api/temp-workspace/payment");
		// 這邊不需要他回傳額外付款資料
		aioCheckOutOneTime.setNeedExtraPaidInfo("N");
		// 設定英文介面，不特別設定為 繁體中文
		//		aioCheckOutOneTime.setLanguage("ENG");

		// 這邊使用他預留的客製化欄位,填入我們的訂單ID,當他透過return URL 觸發我們API時會回傳
		// 這邊因為還是只能String , 所以要將Long 類型做轉換
		aioCheckOutOneTime.setCustomField1(String.valueOf(entity.getTempWorkspaceId()));

		// 6.前述設定完成,放入全方位金流對象
		String form = allInOne.aioCheckOut(aioCheckOutOneTime, null);
		System.out.println("產生的form " + form);
		return form;

	}

	/**
	 * 使用 project.payment.prefix + <br>
	 * 時間戳轉Base36 ,減少長度 + <br>
	 * 同毫秒內的三位數序列號 <br>
	 * 產生廠商訂單編號
	 * 
	 * @return
	 */
	private String generateTradeNo() {

		// 1.拿到配置文件的payment 前墜
		String prefix = "workshop";

		// 2.prefix 最多 9 碼 
		if (prefix.length() > 9) {
			prefix = prefix.substring(0, 9);
		}

		// 3.拿到當下毫秒級的時間戳
		long now = System.currentTimeMillis();

		// 4.同毫秒內 sequence 控制
		if (now == lastMillis) {
			int seq = SEQ.incrementAndGet();
			if (seq >= 1000) {
				// 同毫秒超過999則交易等待下一毫秒
				while (System.currentTimeMillis() == now) {
					// 讓出 CPU，允許 JVM 排程其他 thread 執行
					Thread.yield();
				}
				now = System.currentTimeMillis();
				SEQ.set(0);
			}
		} else {
			SEQ.set(0);
			lastMillis = now;
		}

		// 5.Base36 壓縮時間戳
		String base36Time = Long.toString(now, 36).toUpperCase();

		// 6.格式化 3位數的 sequence , 
		String seqPart = String.format("%03d", SEQ.get());

		// 7.組裝TradeNo
		System.out.println(prefix + base36Time + seqPart);

		return prefix + base36Time + seqPart;

	}

	@Override
	public void handleEcpayCallback(ECPayResponseDTO ECPayResponseDTO) {

		// 1.新增此筆交易明細
		Payment payment = paymentService.addPayment(ECPayResponseDTO);

		
		// 2.獲取此筆工作坊報名紀錄
		TempWorkspace currentTempWorkspace = baseMapper.selectById(payment.getCustomField1());

		// 3.付款成功，更新 工作坊報名者 的付款狀態
		if (ECpayRtnCodeEnum.SUCCESS.getCode().equals(payment.getRtnCode())) {

			// 如果當前訂單狀態不是 '付款成功' 則變更狀態
			if (!currentTempWorkspace.getStatus().equals(1)) {

				// 4-1更新這筆訂單資料
				currentTempWorkspace.setStatus(1);
				baseMapper.updateById(currentTempWorkspace);

				// 4-2 處理信件內容

				// 組合英文姓名
				String englishName = String
						.format("%s %s", Optional.ofNullable(currentTempWorkspace.getFirstName()).orElse(""),
								Optional.ofNullable(currentTempWorkspace.getLastName()).orElse(""))
						.trim();

				String chineseName = Optional.ofNullable(currentTempWorkspace.getChineseName()).orElse("-");
				String phone = Optional.ofNullable(currentTempWorkspace.getPhone()).orElse("-");
				String affiliation = Optional.ofNullable(currentTempWorkspace.getAffiliation()).orElse("-");
				String jobTitle = Optional.ofNullable(currentTempWorkspace.getJobTitle()).orElse("-");

				// HTML 內容
				String htmlContent = "<!DOCTYPE html>" + "<html>" + "<head>" + "<meta charset='UTF-8'>"
						+ "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
						+ "<title>已確認繳費 - 乳癌遺傳性基因檢測工作坊</title>" + "</head>" +

						"<body style='margin:0;padding:20px;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;font-size:16px;line-height:1.8;color:#333333;'>"
						+

						"<table width='100%' cellpadding='0' cellspacing='0' border='0' align='center' "
						+ "style='max-width:720px;margin:0 auto;background-color:#ffffff;border:1px solid #dddddd;border-radius:6px;overflow:hidden;'>"
						+

						// ===== Banner Image Row =====
						"<tr>" + "<td style='padding:0;margin:0;'>" + "<img src='" + BANNER_PHOTO_URL
						+ "' alt='Workshop Banner' width='720' "
						+ "style='display:block;width:100%;max-width:720px;height:auto;border:0;outline:none;text-decoration:none;'>"
						+ "</td>" + "</tr>" +

						// ===== Content Container Row =====
						"<tr>" + "<td style='padding:30px;'>" +

						"<div style='font-size:22px;font-weight:bold;color:#2c3e50;padding-bottom:15px;'>"
						+ "乳癌遺傳性基因檢測工作坊<br/>"
						+ "<span style='font-size:16px;font-weight:normal;'>Hereditary Breast Cancer Genetic Testing Workshop</span>"
						+ "</div>" +

						"<div style='padding-bottom:15px;'>您好：</div>" +

						"<div style='padding-bottom:15px;'>" + "我們已確認您完成本次工作坊之繳費，感謝您的參與。" + "</div>" +

						"<div style='padding-bottom:10px;font-weight:bold;'>以下為您當時填寫的報名資訊：</div>" +

						"<table width='100%' cellpadding='0' cellspacing='0' border='0' style='border-top:1px solid #eeeeee;margin-top:10px;font-size:15px;'>"
						+

						"<tr>" + "<td style='width:160px;padding:8px 0;color:#555555;'>中文姓名</td>"
						+ "<td style='padding:8px 0;'>" + chineseName + "</td>" + "</tr>" +

						"<tr>" + "<td style='padding:8px 0;color:#555555;'>英文姓名</td>" + "<td style='padding:8px 0;'>"
						+ englishName + "</td>" + "</tr>" +

						"<tr>" + "<td style='padding:8px 0;color:#555555;'>電話</td>" + "<td style='padding:8px 0;'>"
						+ phone + "</td>" + "</tr>" +

						"<tr>" + "<td style='padding:8px 0;color:#555555;'>單位</td>" + "<td style='padding:8px 0;'>"
						+ affiliation + "</td>" + "</tr>" +

						"<tr>" + "<td style='padding:8px 0;color:#555555;'>職稱</td>" + "<td style='padding:8px 0;'>"
						+ jobTitle + "</td>" + "</tr>" +

						"</table>" +

						"<div style='padding-top:25px;padding-bottom:10px;'>" + "若報名資料有誤，請儘速與主辦單位聯繫。" + "</div>" +

						"<div style='padding-top:15px;color:#777777;font-size:14px;border-top:1px solid #eeeeee;'>"
						+ "此為系統自動發送信件，請勿直接回覆。" + "</div>" +

						"</td>" + "</tr>" +

						"</table>" + "</body>" + "</html>";

				// Plain Text 內容
				String plainContent = "您好：\n\n" + "您報名之 乳癌遺傳性基因檢測工作坊\n"
						+ "Hereditary Breast Cancer Genetic Testing Workshop\n" + "已確認完成繳費。\n\n" + "以下為您當時填寫的報名資訊：\n"
						+ "----------------------------------\n" + "中文姓名: " + chineseName + "\n" + "英文姓名: "
						+ englishName + "\n" + "電話: " + phone + "\n" + "單位: " + affiliation + "\n" + "職稱: " + jobTitle
						+ "\n" + "----------------------------------\n\n" + "若報名資訊有誤，請儘速與我們聯繫。\n\n"
						+ "感謝您的參與，期待於課程當日與您見面。\n\n" + "（此為系統自動發送信件，請勿直接回覆）";

				// 4-3寄信告知用戶已經付款 兼 報名成功
				asyncService.sendCommonEmail(currentTempWorkspace.getEmail(),
						"已確認繳費｜乳癌遺傳性基因檢測工作坊 Hereditary Breast Cancer Genetic Testing Workshop", htmlContent,
						plainContent);

			}

			// 5.付款失敗，更新 工作坊報名者 的付款狀態
		} else {
			log.warn(currentTempWorkspace.getTempWorkspaceId() + " 工作坊付款失敗");
		}
	}

	@Override
	public void modify(PutTempWorkspaceDTO putTempWorkspaceDTO) {
		TempWorkspace entity = tempWorkspaceConvert.putDTOToEntity(putTempWorkspaceDTO);
		baseMapper.updateById(entity);
	}

	@Override
	public void remove(Long tempWorkspaceId) {
		baseMapper.deleteById(tempWorkspaceId);
	}

	@Override
	public void downloadExcel(HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
		
		List<TempWorkspace> list = baseMapper.selectList(null);
		
		List<WorkspaceExcel> excelData = list.stream().map(workspace -> {
			 WorkspaceExcel workspaceExcel = tempWorkspaceConvert.entityToExcel(workspace);

			return workspaceExcel;
		}).toList();
		
		// 7. 設置 Http Header（檔名部分不變）
		String rawFileName = "工作坊報名資料";
		String encodedFileName = URLEncoder.encode(rawFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setCharacterEncoding("utf-8");
		response.setHeader("Content-Disposition",
				"attachment; filename=\"" + URLEncoder.encode(rawFileName, "ISO-8859-1") + ".xlsx\"; "
						+ "filename*=UTF-8''" + encodedFileName + ".xlsx");

		// 8. 使用 EasyExcel 輸出
		EasyExcel.write(response.getOutputStream(), WorkspaceExcel.class)
				.sheet("回覆結果")
				.doWrite(excelData);
		
	}




}
