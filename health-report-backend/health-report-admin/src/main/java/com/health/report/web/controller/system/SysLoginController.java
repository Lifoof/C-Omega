package com.health.report.web.controller.system;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.health.report.common.core.domain.model.SmsLoginBody;
import com.health.report.common.core.redis.RedisCache;
import com.health.report.common.exception.ServiceException;
import com.health.report.common.utils.*;
import com.health.report.common.utils.ip.IpUtils;
import com.health.report.framework.manager.AsyncManager;
import com.health.report.framework.manager.factory.AsyncFactory;
import com.health.report.system.service.ISysUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.health.report.common.constant.Constants;
import com.health.report.common.core.domain.AjaxResult;
import com.health.report.common.core.domain.entity.SysMenu;
import com.health.report.common.core.domain.entity.SysUser;
import com.health.report.common.core.domain.model.LoginBody;
import com.health.report.common.core.domain.model.LoginUser;
import com.health.report.common.core.text.Convert;
import com.health.report.framework.web.service.SysLoginService;
import com.health.report.framework.web.service.SysPermissionService;
import com.health.report.framework.web.service.TokenService;
import com.health.report.system.service.ISysConfigService;
import com.health.report.system.service.ISysMenuService;

import static com.health.report.common.constant.Constants.EMAIL_REDIS_KEY_PRE;
import static com.health.report.common.core.domain.AjaxResult.error;
import static com.health.report.common.core.domain.AjaxResult.success;

/**
 * 登录验证
 *
 * @author ruoyi
 */
@RestController
public class SysLoginController
{
    private static final Logger logger = LoggerFactory.getLogger(SysLoginController.class);

    @Autowired
    private SysLoginService loginService;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private DyEmailHelper dyEmailHelper;

    @Autowired
    private DySmsHelper dySmsHelper;

    /**
     * 登录方法
     *
     * @param loginBody 登录信息
     * @return 结果
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody)
    {
        AjaxResult ajax = AjaxResult.success();
        // 生成令牌
        Map<String, Object> map = loginService.login(loginBody.getUsername(), loginBody.getPassword());
        ajax.put(Constants.TOKEN, map.get(Constants.TOKEN));
        SysUser user = userService.selectUserById((Long) map.get("userId"));
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        ajax.put("roles", roles);
        ajax.put("timeStop", System.currentTimeMillis());
        return ajax;
    }

    /**
     * 手机号 + 短信验证码登录
     */
    @PostMapping("/smsLogin")
    public AjaxResult smsLogin(@RequestBody SmsLoginBody smsLoginBody)
    {
        AjaxResult ajax = AjaxResult.success();
        // 生成令牌
        Map<String, Object> map = loginService.smsLogin(smsLoginBody.getPhone(), smsLoginBody.getSmsCode());
        ajax.put(Constants.TOKEN, map.get(Constants.TOKEN));
        SysUser user = userService.selectUserById((Long) map.get("userId"));
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        ajax.put("roles", roles);
        ajax.put("timeStop", System.currentTimeMillis());
        return ajax;
    }

    @PostMapping("/userRegister")
    public AjaxResult userRegister(@RequestBody Map<String, String> param) {
        String type = param.get("registerType");
        String code = param.get("code");
        String password = param.get("password");
        String username = param.get("username");
        String nickName = param.get("nickName");

        String redisKey = null;
        if ("phone".equals(type)) {
            redisKey = Constants.PHONE_REDIS_KEY_PRE + param.get("phone");
        } else if ("email".equals(type)) {
            redisKey = EMAIL_REDIS_KEY_PRE + param.get("email");
        }

        String cacheCode = redisCache.getCacheObject(redisKey);
        if (cacheCode == null || !cacheCode.equals(code)) {
            return error("验证码错误");
        }

        if ("phone".equals(type)) {
            SysUser sysUser = new SysUser();
            sysUser.setPhonenumber(param.get("phone"));
            // 判断用户是否已存在
            if (!userService.checkPhoneUnique(sysUser)) {
                return error("账号已存在");
            }
        } else if ("email".equals(type)) {
            SysUser sysUser = new SysUser();
            sysUser.setEmail(param.get("email"));
            // 判断用户是否已存在
            if (!userService.checkEmailUnique(sysUser)) {
                return error("账号已存在");
            }
        }

        // 构建用户
        SysUser user = new SysUser();
        user.setUserName(username);
        user.setNickName(nickName);
        user.setPassword(SecurityUtils.encryptPassword(password));
        user.setPhonenumber(param.get("phone"));
        user.setEmail(param.get("email"));
        user.setCountry(param.get("country"));
        user.setCity(param.get("city"));
        user.setDistrict(param.get("district"));
        user.setAddress(param.get("address"));
        user.setRegisterType("register");
        user.setCreateTime(new Date());

        // 注册默认普通角色
        user.setRoleIds(new Long[]{2L});
        final int insert = userService.insertUser(user);
        if (insert > 0) {
            redisCache.deleteObject(redisKey);
            return success("注册成功");
        }
        return error("注册失败");
    }

    /**
     * 获取短信验证码（登录/注册/找回密码）
     */
    @PostMapping("/sendSmsCode")
    public AjaxResult sendSmsCode(@RequestBody Map<String, String> param, HttpServletRequest request) {
        String clientIp = IpUtils.getIpAddr(request);
        String mobile = param.get("mobile");
        String smsmode = param.get("smsmode"); // 0登录 1注册 2忘记密码

        // 校验手机号
        if (mobile == null || !mobile.matches("^1[3-9]\\d{9}$")) {
            return error("手机号格式不正确");
        }

        // Redis KEY 和你原来完全一样
        String redisKey = Constants.PHONE_REDIS_KEY_PRE + mobile;
        if (redisCache.getCacheObject(redisKey) != null) {
            return error("验证码5分钟内仍然有效，请勿重复发送");
        }

        // IP 防刷（直接用你原来的 DySmsLimit 即可）
        if (!DySmsLimit.canSendSms(clientIp)) {
            return error("短信请求过于频繁，请稍后再试");
        }

        // 生成6位验证码
        String captcha = dySmsHelper.generateCode();

        try {
            boolean b = false;

            // 注册模式
            if (Constants.SMS_TPL_TYPE_1.equals(smsmode)) {
                SysUser sysUser = userService.selectUserByPhone(mobile);
                if (sysUser != null) {
                    return error("手机号已经注册，请直接登录");
                }
                b = dySmsHelper.sendRegisterCode(mobile, captcha);
            }

            // 登录模式 / 忘记密码
            else {
                SysUser sysUser = userService.selectUserByPhone(mobile);
                if (sysUser == null) {
                    return error("该用户不存在或未绑定手机号");
                }

                // 登录模板
                if (Constants.SMS_TPL_TYPE_0.equals(smsmode)) {
                    b = dySmsHelper.sendLoginCode(mobile, captcha);
                }
                // 忘记密码模板
                else if (Constants.SMS_TPL_TYPE_2.equals(smsmode)) {
                    b = dySmsHelper.sendResetCode(mobile, captcha);
                }
            }

            if (!b) {
                return error("短信发送失败，请稍后重试");
            }

            // 存入Redis 5分钟
            redisCache.setCacheObject(redisKey, captcha, 5, TimeUnit.MINUTES);

            return success("短信发送成功");

        } catch (Exception e) {
            logger.error("短信发送异常", e);
            return error("短信接口未配置，请联系管理员");
        }
    }

    @PostMapping("/sendEmailCode")
    public AjaxResult sendEmailCode(@RequestBody Map<String, String> param) {
        String email = param.get("email");
        String type = param.get("type"); // register:注册, reset:找回密码

        if (email == null || !email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
            return error("邮箱格式错误");
        }

        // 根据场景类型进行不同的校验
        SysUser user = userService.selectUserByEmail(email);
        if ("register".equals(type)) {
            // 注册场景：邮箱必须未注册
            if (user != null) {
                return error("该邮箱已被注册");
            }
        } else {
            // 找回密码场景：邮箱必须已注册
            if (user == null) {
                return error("邮箱未注册");
            }
        }

        // 检查是否已发送过验证码（5分钟内不重复发送）
        String redisKey = Constants.EMAIL_REDIS_KEY_PRE + email;
        if (redisCache.getCacheObject(redisKey) != null) {
            return error("验证码5分钟内仍然有效，请勿重复发送");
        }

        String code = String.valueOf((int)((Math.random() * 9 + 1) * 100000));

        try {
            // 使用阿里云邮箱服务发送验证码，根据类型发送不同内容
            boolean success = dyEmailHelper.sendVerifyCode(email, code, type);

            if (success) {
                // 存入Redis 5分钟
                redisCache.setCacheObject(redisKey, code, 5, TimeUnit.MINUTES);
                return success("发送成功");
            } else {
                return error("邮件发送失败，请稍后重试");
            }
        } catch (Exception e) {
            logger.error("邮件发送异常", e);
            return error("邮件接口未配置，请联系管理员");
        }
    }

    /**
     * 验证验证码（找回密码时使用）
     */
    @PostMapping("/verifyCode")
    public AjaxResult verifyCode(@RequestBody Map<String, String> param) {
        String target = param.get("target");
        String code = param.get("code");
        String type = param.get("type");

        String key = type.equals("phone") ? Constants.PHONE_REDIS_KEY_PRE + target : Constants.EMAIL_REDIS_KEY_PRE + target;
        String cacheCode = redisCache.getCacheObject(key);

        if (cacheCode == null) {
            return error("验证码已过期");
        }

        if (!cacheCode.equals(code)) {
            return error("验证码错误");
        }

        return success("验证成功");
    }


    @PostMapping("/resetPassword")
    public AjaxResult resetPassword(@RequestBody Map<String, String> param) {
        String target = param.get("target");
        String code = param.get("code");
        String newPassword = param.get("newPassword");
        String type = param.get("type");

        String key = type.equals("phone") ? Constants.PHONE_REDIS_KEY_PRE + target : Constants.EMAIL_REDIS_KEY_PRE + target;
        String cacheCode = redisCache.getCacheObject(key);

        if (cacheCode == null || !cacheCode.equals(code)) {
            return error("验证码错误");
        }

        SysUser user = type.equals("phone")
                ? userService.selectUserByPhone(target)
                : userService.selectUserByEmail(target);

        if (user == null) return error("用户不存在");

        user.setPassword(SecurityUtils.encryptPassword(newPassword));
        userService.updateUserProfile(user);

        redisCache.deleteObject(key);
        return success("重置成功");
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        // 检查权限是否发生变化（处理 permissions 为 null 的情况）
        if (loginUser.getPermissions() == null || !loginUser.getPermissions().equals(permissions))
        {
            loginUser.setPermissions(permissions);
            tokenService.refreshToken(loginUser);
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        ajax.put("isDefaultModifyPwd", initPasswordIsModify(user.getPwdUpdateDate()));
        ajax.put("isPasswordExpired", passwordIsExpiration(user.getPwdUpdateDate()));
        return ajax;
    }

    /**
     * 获取路由信息
     *
     * @return 路由信息
     */
    @GetMapping("getRouters")
    public AjaxResult getRouters()
    {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(menuService.buildMenus(menus));
    }

    // 检查初始密码是否提醒修改
    public boolean initPasswordIsModify(Date pwdUpdateDate)
    {
        Integer initPasswordModify = Convert.toInt(configService.selectConfigByKey("sys.account.initPasswordModify"));
        return initPasswordModify != null && initPasswordModify == 1 && pwdUpdateDate == null;
    }

    // 检查密码是否过期
    public boolean passwordIsExpiration(Date pwdUpdateDate)
    {
        Integer passwordValidateDays = Convert.toInt(configService.selectConfigByKey("sys.account.passwordValidateDays"));
        if (passwordValidateDays != null && passwordValidateDays > 0)
        {
            if (StringUtils.isNull(pwdUpdateDate))
            {
                // 如果从未修改过初始密码，直接提醒过期
                return true;
            }
            Date nowDate = DateUtils.getNowDate();
            return DateUtils.differentDaysByMillisecond(nowDate, pwdUpdateDate) > passwordValidateDays;
        }
        return false;
    }
}
