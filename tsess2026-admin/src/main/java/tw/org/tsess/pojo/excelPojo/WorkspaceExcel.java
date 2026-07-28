package tw.org.tsess.pojo.excelPojo;

import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

@Data
public class WorkspaceExcel {

    @ExcelProperty("中文姓名")
    private String chineseName;

    @ExcelProperty("英文名字")
    private String firstName;

    @ExcelProperty("英文姓氏")
    private String lastName;

    @ExcelProperty("E-Mail")
    private String email;
    
    @ExcelProperty("電話號碼")
    private String phone;

    @ExcelProperty("付款狀態")
    private String status;
    
	@ExcelProperty("單位")
	private String affiliation;

	@ExcelProperty("職稱")
	private String jobTitle;
	
}
