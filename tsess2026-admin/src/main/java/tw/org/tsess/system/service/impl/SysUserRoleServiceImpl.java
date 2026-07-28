package tw.org.tsess.system.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import tw.org.tsess.system.mapper.SysUserRoleMapper;
import tw.org.tsess.system.pojo.entity.SysUserRole;
import tw.org.tsess.system.service.SysUserRoleService;

/**
 * <p>
 * 用戶與角色 - 多對多關聯表 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2024-05-10
 */
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {

}
