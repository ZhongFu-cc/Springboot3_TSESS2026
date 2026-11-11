package tw.org.tsess.utils;

import java.time.LocalDate;
import java.time.chrono.MinguoDate;
import java.time.temporal.ChronoField;

/**
 * 民國 (中華民國) 紀年的共用轉換<br>
 * 學會收據上的日期是「中華民國 ___ 年 __ 月 __ 日」, 年月日是三個獨立空格,
 * 因此這裡只提供拆成三格的取法, 不做整串格式化
 */
public final class RocDateUtil {

	// 私有化構造函數,禁止被 new
	private RocDateUtil() {}

	/** 民國元年為西元 1912 年, 民國年 = 西元年 - 1911 */
	private static final int ROC_OFFSET = 1911;

	/**
	 * 西元年轉民國年<br>
	 * 民國前 (西元 1912 年以前) 不在業務範圍內, 不做特別處理
	 *
	 * @param adYear 西元年
	 * @return 民國年
	 */
	public static int toRocYear(int adYear) {
		return adYear - ROC_OFFSET;
	}

	/**
	 * 民國年轉西元年
	 *
	 * @param rocYear 民國年
	 * @return 西元年
	 */
	public static int toAdYear(int rocYear) {
		return rocYear + ROC_OFFSET;
	}

	/**
	 * 取得民國年, date 為 null 時回傳 null
	 *
	 * @param date
	 * @return 例如 115
	 */
	public static Integer getRocYear(LocalDate date) {
		if (date == null) {
			return null;
		}
		// 用 JDK 內建的民國曆法換算, 避免自己算錯跨年邊界
		return MinguoDate.from(date).get(ChronoField.YEAR_OF_ERA);
	}

	/**
	 * 拆出民國的年月日數字字串, 例如 "115" / "03" / "05"<br>
	 * 月與日補零到兩位, 模板直接印不用再設 pattern
	 *
	 * @param date
	 * @return 長度為 3 的陣列 [年, 月, 日] , date 為 null 時三格皆為空字串
	 */
	public static String[] splitRocDate(LocalDate date) {
		if (date == null) {
			return new String[] { "", "", "" };
		}
		return new String[] { String.valueOf(getRocYear(date)), String.format("%02d", date.getMonthValue()),
				String.format("%02d", date.getDayOfMonth()) };
	}

}
