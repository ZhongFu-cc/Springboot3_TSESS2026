package tw.org.tsess.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.org.tsess.pojo.DTO.addEntityDTO.AddPublishFileDTO;
import tw.org.tsess.pojo.DTO.putEntityDTO.PutPublishFileDTO;
import tw.org.tsess.pojo.entity.PublishFile;

/**
 * <p>
 * 發佈檔案表 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
public interface PublishFileService extends IService<PublishFile> {

	List<PublishFile> getFileListByGroupAndType(String group, String type);

	IPage<PublishFile> getFilePageByGroup(String group, Page<PublishFile> pageInfo);

	void addPublishFile(MultipartFile file, MultipartFile imgFile, AddPublishFileDTO addPublishFileDTO);

	void putPublishFile(MultipartFile file, MultipartFile imgFile, PutPublishFileDTO putPublishFileDTO);

	void deletePublishFile(Long publishFileId);

	void deletePublishFile(List<Long> publishFileIdList);

}
