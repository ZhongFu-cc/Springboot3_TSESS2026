package tw.org.tsess.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 常年會費欠繳名單表<br>
 * 由學會提供的 Excel 名單一次性匯入, 報名時以 id_card 比對, 有欠費則併入註冊費訂單
 * </p>
 *
 * @author Joey
 */
@Getter
@Setter
@TableName("membership_fee_due")
@Schema(name = "MembershipFeeDue", description = "常年會費欠繳名單表")
public class MembershipFeeDue implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主鍵ID")
	@TableId("membership_fee_due_id")
	private Long membershipFeeDueId;

	@Schema(description = "身分證字號, 與 member.id_card 比對")
	@TableField("id_card")
	private String idCard;

	@Schema(description = "學會會員編號, 供人工核對")
	@TableField("member_no")
	private String memberNo;

	@Schema(description = "中文姓名")
	@TableField("chinese_name")
	private String chineseName;

	@Schema(description = "會員類別: 正式會員 / 準會員 / 榮譽會員")
	@TableField("member_category")
	private String memberCategory;

	@Schema(description = "113年度應補繳常年會費")
	@TableField("due_113")
	private BigDecimal due113;

	@Schema(description = "114年度應補繳常年會費")
	@TableField("due_114")
	private BigDecimal due114;

	@Schema(description = "115年度應補繳常年會費")
	@TableField("due_115")
	private BigDecimal due115;

	/**
	 * 學會提供的加總值, 僅供人工對帳<br>
	 * 程式一律以 due_113 + due_114 + due_115 為準, 不要讀這個欄位
	 */
	@Schema(description = "學會提供的加總值, 僅供對帳, 程式請改用三個年度欄位相加")
	@TableField("total_due")
	private BigDecimal totalDue;

	@Schema(description = "創建者")
	@TableField(value = "create_by", fill = FieldFill.INSERT)
	private String createBy;

	@Schema(description = "創建時間")
	@TableField(value = "create_date", fill = FieldFill.INSERT)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createDate;

	@Schema(description = "最後修改者")
	@TableField(value = "update_by", fill = FieldFill.UPDATE)
	private String updateBy;

	@Schema(description = "最後修改時間")
	@TableField(value = "update_date", fill = FieldFill.UPDATE)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updateDate;

	@Schema(description = "邏輯刪除,預設為0活耀,1為刪除")
	@TableField("is_deleted")
	@TableLogic
	private Integer isDeleted;
}
