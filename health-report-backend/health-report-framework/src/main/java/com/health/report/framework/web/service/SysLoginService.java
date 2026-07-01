package com.health.report.framework.web.service;

import com.health.report.common.core.domain.entity.SysUser;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.health.report.common.constant.CacheConstants;
import com.health.report.common.constant.Constants;
import com.health.report.common.constant.UserConstants;
import com.health.report.common.core.domain.model.LoginUser;
import com.health.report.common.core.redis.RedisCache;
import com.health.report.common.exception.ServiceException;
import com.health.report.common.exception.user.BlackListException;
import com.health.report.common.exception.user.CaptchaException;
import com.health.report.common.exception.user.CaptchaExpireException;
import com.health.report.common.exception.user.UserNotExistsException;
import com.health.report.common.exception.user.UserPasswordNotMatchException;
import com.health.report.common.utils.DateUtils;
import com.health.report.common.utils.MessageUtils;
import com.health.report.common.utils.StringUtils;
import com.health.report.common.utils.ip.IpUtils;
import com.health.report.framework.manager.AsyncManager;
import com.health.report.framework.manager.factory.AsyncFactory;
import com.health.report.framework.security.context.AuthenticationContextHolder;
import com.health.report.system.service.ISysConfigService;
import com.health.report.system.service.ISysUserService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录校验方法
 *
 * @author ruoyi
 */
@Component
public class SysLoginService
{
    @Autowired
    private TokenService tokenService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private SysPermissionService permissionService;

    /**
     * 登录验证
     *
     * @param username 用户名
     * @param password 密码
     * @return 结果
     */
    public Map<String, Object> login(String username, String password)
    {
        // 查询真正的user_name
        String realUserName = findRealUserName(username);

        // 登录前置校验（用真实的 user_name）
        loginPreCheck(realUserName, password);

        // 用户验证
        Authentication authentication = null;
        try
        {
            // 关键：这里必须传真实的 user_name，否则SpringSecurity查不到
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(realUserName, password);

            AuthenticationContextHolder.setContext(authenticationToken);
            authentication = authenticationManager.authenticate(authenticationToken);
        }
        catch (Exception e)
        {
            if (e instanceof BadCredentialsException)
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
                throw new UserPasswordNotMatchException();
            }
            else
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage()));
                throw new ServiceException(e.getMessage());
            }
        }
        finally
        {
            AuthenticationContextHolder.clearContext();
        }

        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        recordLoginInfo(loginUser.getUserId());
        // 【关键修改】构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", tokenService.createToken(loginUser));
        result.put("userId", loginUser.getUserId());

        return result;
    }

    /**
     * 手机号 + 短信验证码登录
     */
    public Map<String, Object> smsLogin(String phone, String smsCode)
    {
        // ==============================
        // 1. 使用你项目的 REDIS KEY（和发送短信一致）
        // ==============================
        String redisKey = Constants.PHONE_REDIS_KEY_PRE + phone;

        // ==============================
        // 2. 从 Redis 取验证码（发送时已存入）
        // ==============================
        String cacheCode = redisCache.getCacheObject(redisKey);
        if (cacheCode == null) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(phone, Constants.LOGIN_FAIL, "验证码已过期"));
            throw new ServiceException("验证码已过期");
        }

        // ==============================
        // 3. 校验验证码是否正确
        // ==============================
        if (!cacheCode.equals(smsCode)) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(phone, Constants.LOGIN_FAIL, "验证码错误"));
            throw new ServiceException("验证码错误");
        }

        // ==============================
        // 4. 查询用户
        // ==============================
        SysUser user = userService.selectUserByPhone(phone);
        if (user == null) {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(phone, Constants.LOGIN_FAIL, "用户不存在"));
            throw new ServiceException("该用户不存在或未绑定手机号");
        }

        // ==============================
        // 5. 检查用户状态
        // ==============================
        loginPreUserNameCheck(user.getUserName());

        // ==============================
        // 6. 免密登录
        // ==============================
        // 获取用户权限信息
        Set<String> permissions = permissionService.getMenuPermission(user);
        // 创建 LoginUser 并设置完整信息
        LoginUser loginUser = new LoginUser(user.getUserId(), user.getDeptId(), user, permissions);
        // 设置认证信息到 SecurityContext
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        // ==============================
        // 7. 验证成功，删除验证码
        // ==============================
        redisCache.deleteObject(redisKey);

        // ==============================
        // 8. 记录日志 & 返回token
        // ==============================
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(phone, Constants.LOGIN_SUCCESS, "短信登录成功"));
        recordLoginInfo(loginUser.getUser().getUserId());

        // 【关键修改】构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", tokenService.createToken(loginUser));
        result.put("userId", loginUser.getUser().getUserId());

        return result;
    }

    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public void validateCaptcha(String username, String code, String uuid)
    {
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
            String captcha = redisCache.getCacheObject(verifyKey);
            if (captcha == null)
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire")));
                throw new CaptchaExpireException();
            }
            redisCache.deleteObject(verifyKey);
            if (!code.equalsIgnoreCase(captcha))
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error")));
                throw new CaptchaException();
            }
        }
    }

    /**
     * 登录前置校验
     * @param username 用户名
     */
    public void loginPreUserNameCheck(String username)
    {
        // 用户名为空 错误
        if (StringUtils.isEmpty(username))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("not.null")));
            throw new UserNotExistsException();
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // IP黑名单校验
        String blackStr = configService.selectConfigByKey("sys.login.blackIPList");
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("login.blocked")));
            throw new BlackListException();
        }
    }

    /**
     * 登录前置校验
     * @param username 用户名
     * @param password 用户密码
     */
    public void loginPreCheck(String username, String password)
    {
        // 用户名或密码为空 错误
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("not.null")));
            throw new UserNotExistsException();
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // IP黑名单校验
        String blackStr = configService.selectConfigByKey("sys.login.blackIPList");
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("login.blocked")));
            throw new BlackListException();
        }
    }

    /**
     * 根据 用户输入的账号（手机号/邮箱/用户名）自动获取真实的 user_name
     */
    private String findRealUserName(String account) {
        // 1. 如果是手机号
        if (account.matches("^1[3-9]\\d{9}$")) {
            SysUser user = userService.selectUserByPhone(account);
            if (user == null) {
                throw new ServiceException("用户不存在");
            }
            return user.getUserName();
        }
        // 2. 如果是邮箱
        else if (account.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
            SysUser user = userService.selectUserByEmail(account);
            if (user == null) {
                throw new ServiceException("用户不存在");
            }
            return user.getUserName();
        }
        // 3. 否则直接当作用户名
        return account;
    }

    /**
     * 记录登录信息
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId)
    {
        userService.updateLoginInfo(userId, IpUtils.getIpAddr(), DateUtils.getNowDate());
    }
}
