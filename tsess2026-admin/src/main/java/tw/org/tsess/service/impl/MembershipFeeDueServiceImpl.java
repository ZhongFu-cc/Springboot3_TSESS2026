package tw.org.tsess.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.org.tsess.enums.MembershipDueYearEnum;
import tw.org.tsess.mapper.MembershipFeeDueMapper;
import tw.org.tsess.pojo.BO.MembershipDueYearBO;
import tw.org.tsess.pojo.entity.MembershipFeeDue;
import tw.org.tsess.service.MembershipFeeDueService;

@Service
@RequiredArgsConstructor
public class MembershipFeeDueServiceImpl extends ServiceImpl<MembershipFeeDueMapper, MembershipFeeDue>
		implements MembershipFeeDueService {

	@Override
	public List<MembershipDueYearBO> getYearlyDueByIdCard(String idCard) {

		// 1.查無此人一律視為無欠費 , 回傳空 List 讓呼叫端不會產生任何明細
		MembershipFeeDue membershipFeeDue = this.getByIdCard(idCard);
		if (membershipFeeDue == null) {
			return List.of();
		}

		// 2.分年金額是唯一真實來源 , 依 enum 宣告的順序 (113 -> 115) 組出結果
		return List.of(
				MembershipDueYearBO.of(MembershipDueYearEnum.ROC_113, membershipFeeDue.getDue113()),
				MembershipDueYearBO.of(MembershipDueYearEnum.ROC_114, membershipFeeDue.getDue114()),
				MembershipDueYearBO.of(MembershipDueYearEnum.ROC_115, membershipFeeDue.getDue115()));
	}

	@Override
	public BigDecimal getTotalDueByIdCard(String idCard) {

		// 總額一律由各年度加總得出 , 不讀 total_due 欄位 , 避免兩份數字對不起來
		return this.getYearlyDueByIdCard(idCard)
				.stream()
				.map(MembershipDueYearBO::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	@Override
	public MembershipFeeDue getByIdCard(String idCard) {

		// 1.身分證字號為比對的唯一依據 , 空值直接視為對不到
		// member.id_card 由使用者自行輸入 , 這邊統一去空白並轉大寫再比對
		String normalizedIdCard = StringUtils.upperCase(StringUtils.trimToNull(idCard));
		if (normalizedIdCard == null) {
			return null;
		}

		// 2.名單中 id_card 具唯一索引 , 直接 selectOne
		LambdaQueryWrapper<MembershipFeeDue> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(MembershipFeeDue::getIdCard, normalizedIdCard);

		return baseMapper.selectOne(queryWrapper);
	}

}
