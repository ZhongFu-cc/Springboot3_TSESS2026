package tw.org.tsess.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * TICBCS 臨時表 , 收集工作坊資料
 * </p>
 *
 * @author Joey
 * @since 2026-03-05
 */
@Getter
@Setter
@TableName("temp_workspace")
@Schema(name = "TempWorkspace", description = "TICBCS 臨時表 , 收集工作坊資料")
public class TempWorkspace implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主鍵ID")
    @TableId("temp_workspace_id")
    private Long tempWorkspaceId;

    @Schema(description = "中文姓名，外國人非必填，台灣人必填")
    @TableField("chinese_name")
    private String chineseName;

    @Schema(description = "名字, 華人的名在後  , 外國人的名在前")
    @TableField("first_name")
    private String firstName;

    @Schema(description = "姓氏, 華人的姓氏在前, 外國人的姓氏在後")
    @TableField("last_name")
    private String lastName;

    @Schema(description = "E-Mail")
    @TableField("email")
    private String email;
    
    @Schema(description = "電話號碼,這邊要使用 國碼-號碼")
    @TableField("phone")
    private String phone;

    @Schema(description = "狀態,0為未付款,1為已付款")
    @TableField("status")
    private Integer status;
    
	@Schema(description = "單位(所屬的機構)")
	@TableField("affiliation")
	private String affiliation;

	@Schema(description = "職稱")
	@TableField("job_title")
	private String jobTitle;

    @Schema(description = "創建者")
    @TableField("create_by")
    private String createBy;

    @Schema(description = "創建時間")
    @TableField(value = "create_date", fill = FieldFill.INSERT)
    private LocalDateTime createDate;

    @Schema(description = "最後修改者")
    @TableField("update_by")
    private String updateBy;

    @Schema(description = "最後修改時間")
    @TableField(value = "update_date", fill = FieldFill.UPDATE)
    private LocalDateTime updateDate;

    @Schema(description = "邏輯刪除,預設為0活耀,1為刪除")
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
