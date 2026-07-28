package tw.org.tsess.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import jakarta.servlet.http.HttpServletResponse;
import tw.org.tsess.pojo.DTO.ECPayDTO.ECPayResponseDTO;
import tw.org.tsess.pojo.DTO.addEntityDTO.AddTempWorkspaceDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutTempWorkspaceDTO;
import tw.org.tsess.pojo.entity.TempWorkspace;

/**
 * <p>
 * TICBCS 臨時表 , 收集工作坊資料 服务类
 * </p>
 *
 * @author Joey
 * @since 2026-03-05
 */
public interface TempWorkspaceService extends IService<TempWorkspace> {

	/**
	 * 查詢工作坊報名者
	 * @param tempWorkspaceId
	 * @return
	 */
	TempWorkspace searchRegistrant(Long tempWorkspaceId);
	
	/**
	 * 查詢工作坊報名者 (分頁)
	 * @param page
	 * @return
	 */
	IPage<TempWorkspace> searchRegistrantPage(Page<TempWorkspace> page);
	
	/**
	 * 工作坊報名 , 狀態為未付款
	 * @param addTempWorkspace
	 */
	String add(AddTempWorkspaceDTO addTempWorkspace);

	
	/**
	 * 工作坊報名 , 資訊修改 
	 * @param putTempWorkspaceDTO
	 */
	void modify(PutTempWorkspaceDTO putTempWorkspaceDTO);
	
	/**
	 * 刪除報名者
	 * @param tempWorkspaceId
	 */
	void remove(Long tempWorkspaceId);
	
	/**
	 * 下載報名者Excel
	 * @param response
	 * @throws UnsupportedEncodingException 
	 * @throws IOException 
	 */
	void downloadExcel(HttpServletResponse response) throws UnsupportedEncodingException, IOException;
	
	/**
	 * 處理綠界刷付
	 * @param ECPayResponseDTO
	 */
	void handleEcpayCallback(ECPayResponseDTO ECPayResponseDTO);
	
	
}
