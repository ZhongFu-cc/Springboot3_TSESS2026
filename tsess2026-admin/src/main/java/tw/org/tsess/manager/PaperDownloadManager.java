package tw.org.tsess.manager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.alibaba.excel.EasyExcel;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tw.org.tsess.constants.PaperFileConstants;
import tw.org.tsess.convert.PaperConvert;
import tw.org.tsess.enums.OrderStatusEnum;
import tw.org.tsess.enums.ReviewStageEnum;
import tw.org.tsess.pojo.entity.Orders;
import tw.org.tsess.pojo.entity.Paper;
import tw.org.tsess.pojo.entity.PaperAndPaperReviewer;
import tw.org.tsess.pojo.entity.PaperFileUpload;
import tw.org.tsess.pojo.excelPojo.PaperScoreExcel;
import tw.org.tsess.service.OrdersService;
import tw.org.tsess.service.PaperAndPaperReviewerService;
import tw.org.tsess.service.PaperFileUploadService;
import tw.org.tsess.service.PaperService;
import tw.org.tsess.utils.S3Util;

/**
 * 處理稿件相關檔案下載(Excel , 摘要附件 , slide附件)
 */
@Component
@RequiredArgsConstructor
@Validated
public class PaperDownloadManager {

	// 預設存储桶名称
	@Value("${spring.cloud.aws.s3.bucketName}")
	private String bucketName;

	private final OrdersService ordersService;
	private final PaperService paperService;
	private final PaperFileUploadService paperFileUploadService;
	private final PaperAndPaperReviewerService paperAndPaperReviewerService;
	private final PaperConvert paperConvert;
	private final S3Util s3Util;

	/**
	 * -------------------------- 後台下載相關Excel --------------------------------
	 */

	/**
	 * 下載對應審核階段的稿件評分
	 * 
	 * @param response
	 * @param reviewStage 審核階段
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public void downloadScoreExcel(HttpServletResponse response, String reviewStage)
			throws UnsupportedEncodingException, IOException {

		// 1.初始設定
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setCharacterEncoding("utf-8");
		String label = ReviewStageEnum.fromValue(reviewStage).getLabel();
		String fileName = URLEncoder.encode(label + "稿件分數", "UTF-8").replaceAll("\\+", "%20");
		response.setHeader("Content-disposition", "attachment;filename*=" + fileName + ".xlsx");

		// 2.查詢所有稿件
		List<Paper> paperList = paperService.getPapersEfficiently();

		// 3.抽取memberIds , 為了之後拿取繳費狀態
		Set<Long> memberIds = paperList.stream().map(Paper::getMemberId).collect(Collectors.toSet());

		// 4.獲得以paperId為key , 關聯紀錄List的映射對象
		Map<Long, List<PaperAndPaperReviewer>> paperReviewersMap = paperAndPaperReviewerService
				.groupPaperReviewersByPaperId(reviewStage);

		// 5. 拿到memberId 與 註冊費訂單的映射
		Map<Long, Orders> registrationOrderMapByMemberId = ordersService.getRegistrationOrderMapByMemberId(memberIds);

		// 6.開始遍歷並組裝成Excel對象
		List<PaperScoreExcel> excelData = paperList.stream().map(paper -> {

			PaperScoreExcel paperScoreExcel = paperConvert.entityToExcel(paper);

			// 透過paperId, 獲得他有的所有關聯 (評審 和 分數)
			List<PaperAndPaperReviewer> list = paperReviewersMap.getOrDefault(paper.getPaperId(),
					Collections.emptyList());

			// 新增全部審核人
			String allReviewers = list.stream()
					.map(PaperAndPaperReviewer::getReviewerName)
					.collect(Collectors.joining(","));
			paperScoreExcel.setAllReviewers(allReviewers);

			// 新增有評分的審核人
			String scorers = list.stream()
					.filter(papersReviewers -> papersReviewers.getScore() != null)
					.map(PaperAndPaperReviewer::getReviewerName)
					.collect(Collectors.joining(","));
			paperScoreExcel.setScorers(scorers);

			// 新增所有分數
			String allScores = list.stream()
					.filter(papersReviewers -> papersReviewers.getScore() != null) // 過濾掉 null 的分數
					.map(PaperAndPaperReviewer::getScore) // 取得 Integer 分數
					.map(String::valueOf) // 將 Integer 轉成 String
					.collect(Collectors.joining(",")); // 用逗號連接
			paperScoreExcel.setAllScores(allScores);

			// 新增平均分數
			Double averageScore = list.stream()
					.filter(papersReviewers -> papersReviewers.getScore() != null) // 過濾掉 null 的分數
					.mapToInt(PaperAndPaperReviewer::getScore) // 轉換成 IntStream
					.average() // 計算平均值，回傳 OptionalDouble
					.orElse(0.0); // 如果沒有分數，預設為 0.0
			paperScoreExcel.setAverageScore(averageScore);

			// 拿到會員繳費狀態塞進vo
			Orders orders = registrationOrderMapByMemberId.get(paper.getMemberId());
			OrderStatusEnum orderStatusEnum = OrderStatusEnum.fromValue(orders.getStatus());
			paperScoreExcel.setMemberPaymentStatus(orderStatusEnum.getLabelZh());

			return paperScoreExcel;

		}).collect(Collectors.toList());

		// 5.輸出Excel
		EasyExcel.write(response.getOutputStream(), PaperScoreExcel.class).sheet("稿件分數列表").doWrite(excelData);

	};

	/**
	 * 更新稿件流水號，並流式下載所有 摘要 檔案
	 * 
	 * @return
	 */
	public ResponseEntity<StreamingResponseBody> downloadAbstracts() {

		// 1.查詢所有稿件
		List<Paper> paperList = paperService.getPapersEfficiently();

		// 2.命名規則應設
		Map<String, String> renameRules = new HashMap<>();

		// 3.初始化計數器
		AtomicInteger counter = new AtomicInteger(1);

		// 4.拿到稿件ID 和 稿件 列表的映射對象
		Map<Long, List<PaperFileUpload>> filesMapByPaperId = paperFileUploadService.getFilesMapByPaperId(paperList);

		// 5.遍歷稿件,重設流水號
		for (Paper paper : paperList) {
			// 5-1 重設流水號,避免斷號
			paper.setSequenceNo(counter.getAndIncrement());

			// 5-2 根據映射關係,拿到稿件的所有附件
			List<PaperFileUpload> paperFiles = filesMapByPaperId.get(paper.getPaperId());

			// 5-3 遍歷附件,為重命名規則做準備
			for (PaperFileUpload paperFile : paperFiles) {
				// 5-3-1 先拿到檔案原路徑
				String fullPath = paperFile.getPath();
				String s3key = s3Util.extractS3PathInDbUrl(bucketName, fullPath);

				// 5-3-2 獲取副檔名
				String ext = s3Util.getFileExtension(s3key);
				// 5-3-3格式化流水號三位數
				String seqStr = String.format("%03d", paper.getSequenceNo());
				// 5-3-4 獲取 類別 與 主講者
				String absType = paper.getAbsType();
				String speaker = paper.getSpeaker();

				// 5-3-5生成新檔名,例:001_oral_林家仰.pdf
				String newFileName = seqStr + "_" + absType + "_" + speaker + ext;

				// 5-3-6 取得原始路徑的資料夾path部分（去掉檔名,包含/線）
				int lastSlash = s3key.lastIndexOf('/');
				String folderPath = lastSlash != -1 ? s3key.substring(0, lastSlash + 1) : "";

				// 5-3-7 組成 ZIP 內的完整相對路徑
				String finalRelativePath = folderPath + newFileName;
				// 5-3-8 舊檔案路徑,映射成新檔案路徑 + 名稱
				renameRules.put(s3key, finalRelativePath);
			}

		}

		// 6.批量更新稿件流水號
		paperService.updateBatchById(paperList);

		// 7.開始生成ZIP檔
		return s3Util.downloadFolderZipByStream(PaperFileConstants.ABSTRACT_BASE_PATH, renameRules);

	}

	/**
	 * 根據 paper 和附件生成完整 ZIP 檔名，並加入 renameRules
	 */
	private void addRenameRuleForSlide(Paper paper, PaperFileUpload file, Map<String, String> renameRules) {
		String speaker = paper.getSpeaker();
		String fullPath = file.getPath();
		String s3Key = s3Util.extractS3PathInDbUrl(bucketName, fullPath);
		String ext = s3Util.getFileExtension(s3Key);

		// 處理檔名
		String newFileName;
		// 如果有發表編號
		if (StringUtils.isNotBlank(paper.getPublicationNumber())) {
			newFileName = paper.getPublicationNumber() + "_" + speaker + ext;
		} else {
			// 沒有發表編號則用流水號
			String seqStr = String.format("%03d", paper.getSequenceNo());
			newFileName = seqStr + "_" + speaker + ext;
		}

		// 處理檔案路徑
		int lastSlash = s3Key.lastIndexOf('/');
		String folderPath = lastSlash != -1 ? s3Key.substring(0, lastSlash + 1) : "";

		String newPath;

		if (folderPath.startsWith(PaperFileConstants.SLIDE_BASE_PATH)) {
			String publicationGroup = paper.getPublicationGroup();

			// 取出 second-stage 之後的子路徑，例如/Poster Presentation/pdf
			String subPath = folderPath.substring(PaperFileConstants.SLIDE_BASE_PATH.length());

			StringBuilder pathBuilder = new StringBuilder(PaperFileConstants.SLIDE_BASE_PATH);

			if (StringUtils.isNotBlank(publicationGroup)) {
				// 有 → 覆蓋原本分類
				pathBuilder.append("/").append(publicationGroup);
			} else {
				// 沒有 → 保留原本分類
				pathBuilder.append(subPath);
			}

			pathBuilder.append("/").append(newFileName);
			newPath = pathBuilder.toString();
		} else {
			// 這邊處理非二階段的稿件path , 不處理也沒差 , 不會用到
			newPath = folderPath + newFileName;
		}

		System.out.println("slide組裝後的newPath: " + newPath);

		renameRules.put(s3Key, newPath);
	}

	/**
	 * 下載所有 第二階段 上傳的檔案(Slide/Video/Poster)
	 * 
	 * @return
	 */
	public ResponseEntity<StreamingResponseBody> downloadSlides() {

		// 1.查詢所有稿件
		List<Paper> paperList = paperService.getPapersEfficiently();

		// 2.命名規則應設
		Map<String, String> renameRules = new HashMap<>();

		// 3.初始化計數器
		AtomicInteger counter = new AtomicInteger(1);

		// 4.拿到稿件ID 和 稿件 列表的映射對象
		Map<Long, List<PaperFileUpload>> filesMapByPaperId = paperFileUploadService.getFilesMapByPaperId(paperList);

		// 5.遍歷稿件
		for (Paper paper : paperList) {

			// 5-1 根據映射關係,拿到稿件的所有附件
			List<PaperFileUpload> paperFiles = filesMapByPaperId.get(paper.getPaperId());

			// 5-2 重設流水號,避免斷號
			paper.setSequenceNo(counter.getAndIncrement());

			// 5-3遍歷附件,為重命名規則做準備
			for (PaperFileUpload paperFile : paperFiles) {
				// 對附件原檔名進行重命名的映射
				this.addRenameRuleForSlide(paper, paperFile, renameRules);
			}
		}

		// 6.批量更新稿件流水號
		paperService.updateBatchById(paperList);

		// 7.開始生成ZIP檔
		return s3Util.downloadFolderZipByStream(PaperFileConstants.SLIDE_BASE_PATH, renameRules);

	}

}
