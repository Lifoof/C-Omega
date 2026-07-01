package com.health.report.web.controller.system;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.excel.EasyExcel;
import com.health.report.common.annotation.Log;
import com.health.report.common.core.controller.BaseController;
import com.health.report.common.core.domain.AjaxResult;
import com.health.report.common.core.domain.entity.SysDept;
import com.health.report.common.core.domain.entity.SysRole;
import com.health.report.common.core.domain.entity.SysUser;
import com.health.report.common.core.page.TableDataInfo;
import com.health.report.common.enums.BusinessType;
import com.health.report.common.utils.SecurityUtils;
import com.health.report.common.utils.StringUtils;
import com.health.report.common.utils.poi.ExcelUtil;
import com.health.report.system.service.ISysDeptService;
import com.health.report.system.service.ISysPostService;
import com.health.report.system.service.ISysRoleService;
import com.health.report.system.service.ISysUserService;

/**
 * 用户信息
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    private ISysPostService postService;

    /**
     * 获取用户列表
     */
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysUser user)
    {
        startPage();
        List<SysUser> list = userService.selectUserList(user);
        return getDataTable(list);
    }

    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:user:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysUser user,
                       @RequestParam(defaultValue = "zh") String language)
    {
        List<SysUser> list = userService.selectUserList(user);

        boolean isEnglish = "en".equalsIgnoreCase(language);
        String sheetName = isEnglish ? "User Data" : "用户数据";

        // 使用 EasyExcel 导出，支持动态表头
        List<List<String>> headers = new ArrayList<>();
        if (isEnglish) {
            headers.add(List.of("User ID"));
            headers.add(List.of("Name"));
            headers.add(List.of("Phone Number"));
            headers.add(List.of("Email"));
            headers.add(List.of("Country"));
            headers.add(List.of("City"));
            headers.add(List.of("District"));
            headers.add(List.of("Address"));
            headers.add(List.of("Status"));
        } else {
            headers.add(List.of("用户编号"));
            headers.add(List.of("姓名"));
            headers.add(List.of("手机号码"));
            headers.add(List.of("邮箱"));
            headers.add(List.of("国家"));
            headers.add(List.of("城市"));
            headers.add(List.of("区/县"));
            headers.add(List.of("地址"));
            headers.add(List.of("状态"));
        }

        // 准备数据
        List<List<Object>> data = new ArrayList<>();
        for (SysUser sysUser : list) {
            List<Object> row = new ArrayList<>();
            row.add(sysUser.getUserId());
            row.add(sysUser.getNickName());
            row.add(sysUser.getPhonenumber());
            row.add(sysUser.getEmail());
            row.add(convertCountry(sysUser.getCountry(), isEnglish));
            row.add(convertCity(sysUser.getCity(), isEnglish));
            row.add(convertDistrict(sysUser.getDistrict(), isEnglish));
            row.add(sysUser.getAddress());
            row.add(convertStatus(sysUser.getStatus(), isEnglish));
            data.add(row);
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = isEnglish
                    ? "User_Data_" + System.currentTimeMillis() + ".xlsx"
                    : "用户数据_" + System.currentTimeMillis() + ".xlsx";
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

            EasyExcel.write(response.getOutputStream())
                    .head(headers)
                    .sheet(sheetName)
                    .doWrite(data);
        } catch (Exception e) {
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    /**
     * 转换国家
     */
    private String convertCountry(String country, boolean isEnglish) {
        if (StringUtils.isEmpty(country)) {
            return "";
        }
        if (isEnglish) {
            return switch (country) {
                case "CN" -> "China";
                case "US" -> "United States";
                case "GB" -> "United Kingdom";
                default -> country;
            };
        }
        return switch (country) {
            case "CN" -> "中国";
            case "US" -> "美国";
            case "GB" -> "英国";
            default -> country;
        };
    }

    /**
     * 转换城市
     */
    private String convertCity(String city, boolean isEnglish) {
        if (StringUtils.isEmpty(city)) {
            return "";
        }
        if (isEnglish) {
            return switch (city) {
                case "北京" -> "Beijing";
                case "上海" -> "Shanghai";
                case "广州" -> "Guangzhou";
                case "深圳" -> "Shenzhen";
                default -> city;
            };
        }
        return city;
    }

    /**
     * 转换区县
     */
    private String convertDistrict(String district, boolean isEnglish) {
        if (StringUtils.isEmpty(district)) {
            return "";
        }
        if (isEnglish) {
            return switch (district) {
                case "朝阳区" -> "Chaoyang District";
                case "海淀区" -> "Haidian District";
                case "东城区" -> "Dongcheng District";
                case "西城区" -> "Xicheng District";
                case "丰台区" -> "Fengtai District";
                default -> district;
            };
        }
        return district;
    }

    /**
     * 转换状态
     */
    private String convertStatus(String status, boolean isEnglish) {
        if (StringUtils.isEmpty(status)) {
            return "";
        }
        if ("0".equals(status)) {
            return isEnglish ? "Normal" : "正常";
        } else {
            return isEnglish ? "Disabled" : "停用";
        }
    }

    @Log(title = "用户管理", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('system:user:import')")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        List<SysUser> userList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = userService.importUser(userList, updateSupport, operName);
        return success(message);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.importTemplateExcel(response, "用户数据");
    }

    /**
     * 根据用户编号获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping(value = { "/", "/{userId}" })
    public AjaxResult getInfo(@PathVariable(value = "userId", required = false) Long userId)
    {
        AjaxResult ajax = AjaxResult.success();
        if (StringUtils.isNotNull(userId))
        {
            userService.checkUserDataScope(userId);
            SysUser sysUser = userService.selectUserById(userId);
            ajax.put(AjaxResult.DATA_TAG, sysUser);
            ajax.put("postIds", postService.selectPostListByUserId(userId));
            ajax.put("roleIds", sysUser.getRoles().stream().map(SysRole::getRoleId).collect(Collectors.toList()));
        }
        List<SysRole> roles = roleService.selectRoleAll();
        ajax.put("roles", SecurityUtils.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        ajax.put("posts", postService.selectPostAll());
        return ajax;
    }

    /**
     * 新增用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysUser user)
    {
        //默认用户名称就是手机号
        user.setUserName(user.getPhonenumber());
        deptService.checkDeptDataScope(user.getDeptId());
        roleService.checkRoleDataScope(user.getRoleIds());
        if (!userService.checkUserNameUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setCreateBy(getUsername());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        return toAjax(userService.insertUser(user));
    }

    /**
     * 修改用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysUser user)
    {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        deptService.checkDeptDataScope(user.getDeptId());
        roleService.checkRoleDataScope(user.getRoleIds());
        if (!userService.checkUserNameUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setUpdateBy(getUsername());
        return toAjax(userService.updateUser(user));
    }

    /**
     * 删除用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds)
    {
        if (ArrayUtils.contains(userIds, getUserId()))
        {
            return error("当前用户不能删除");
        }
        return toAjax(userService.deleteUserByIds(userIds));
    }

    /**
     * 重置密码
     */
    @PreAuthorize("@ss.hasPermi('system:user:resetPwd')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody SysUser user)
    {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        user.setUpdateBy(getUsername());
        return toAjax(userService.resetPwd(user));
    }

    /**
     * 状态修改
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysUser user)
    {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setUpdateBy(getUsername());
        return toAjax(userService.updateUserStatus(user));
    }

    /**
     * 根据用户编号获取授权角色
     */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/authRole/{userId}")
    public AjaxResult authRole(@PathVariable("userId") Long userId)
    {
        AjaxResult ajax = AjaxResult.success();
        SysUser user = userService.selectUserById(userId);
        List<SysRole> roles = roleService.selectRolesByUserId(userId);
        ajax.put("user", user);
        ajax.put("roles", SecurityUtils.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        return ajax;
    }

    /**
     * 用户授权角色
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.GRANT)
    @PutMapping("/authRole")
    public AjaxResult insertAuthRole(Long userId, Long[] roleIds)
    {
        userService.checkUserDataScope(userId);
        roleService.checkRoleDataScope(roleIds);
        userService.insertUserAuth(userId, roleIds);
        return success();
    }

    /**
     * 获取部门树列表
     */
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/deptTree")
    public AjaxResult deptTree(SysDept dept)
    {
        return success(deptService.selectDeptTreeList(dept));
    }
}
