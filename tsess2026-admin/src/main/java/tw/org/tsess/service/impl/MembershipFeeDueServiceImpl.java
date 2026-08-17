package tw.org.tsess.service.impl;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.org.tsess.mapper.MembershipFeeDueMapper;
import tw.org.tsess.pojo.entity.MembershipFeeDue;
import tw.org.tsess.service.MembershipFeeDueService;

@Service
@RequiredArgsConstructor
public class MembershipFeeDueServiceImpl extends ServiceImpl<MembershipFeeDueMapper, MembershipFeeDue>
		implements MembershipFeeDueService {

	@Override
	public BigDecimal getTotalDueByIdCard(String idCard) {

		// 1.查無此人 或 沒有登記金額 , 一律視為無欠費
		MembershipFeeDue membershipFeeDue = this.getByIdCard(idCard);
		if (membershipFeeDue == null || membershipFeeDue.getTotalDue() == null) {
			return BigDecimal.ZERO;
		}

		return membershipFeeDue.getTotalDue();
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
