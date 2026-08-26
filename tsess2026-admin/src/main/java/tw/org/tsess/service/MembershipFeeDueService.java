package tw.org.tsess.service;

import java.math.BigDecimal;
import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;

import tw.org.tsess.pojo.BO.MembershipDueYearBO;
import tw.org.tsess.pojo.entity.MembershipFeeDue;

public interface MembershipFeeDueService extends IService<MembershipFeeDue> {

	/**
	 * 依身分證字號查詢各年度應補繳的常年會費<br>
	 * 依年度由小到大排序, 金額為 0 的年度也會保留, 是否過濾由呼叫端決定<br>
	 * 名單中查無此人 (含 idCard 為空) 時回傳空 List
	 *
	 * @param idCard
	 * @return
	 */
	List<MembershipDueYearBO> getYearlyDueByIdCard(String idCard);

	/**
	 * 依身分證字號查詢應補繳的常年會費總額<br>
	 * 總額為各年度金額加總, 不是讀取 total_due 欄位<br>
	 * 名單中查無此人 (含 idCard 為空) 時回傳 BigDecimal.ZERO , 代表無欠費
	 *
	 * @param idCard
	 * @return
	 */
	BigDecimal getTotalDueByIdCard(String idCard);

	/**
	 * 依身分證字號查詢欠費名單資料 , 查無則回傳 null
	 *
	 * @param idCard
	 * @return
	 */
	MembershipFeeDue getByIdCard(String idCard);

}
