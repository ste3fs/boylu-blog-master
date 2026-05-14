package com.boylu.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boylu.common.Constants;
import com.boylu.common.RedisConstants;
import com.boylu.config.properties.*;
import com.boylu.dto.Captcha;
import com.boylu.dto.EmailRegisterDto;
import com.boylu.dto.LoginDTO;
import com.boylu.dto.user.LoginUserInfo;
import com.boylu.entity.SysConfig;
import com.boylu.entity.SysRole;
import com.boylu.entity.SysUserOauthBinding;
import com.boylu.enums.LoginTypeEnum;
import com.boylu.mapper.SysConfigMapper;
import com.boylu.service.AuthService;
import com.boylu.entity.SysUser;
import com.boylu.enums.MenuTypeEnum;
import com.boylu.exception.ServiceException;
import com.boylu.mapper.SysMenuMapper;
import com.boylu.mapper.SysRoleMapper;
import com.boylu.mapper.SysUserOauthBindingMapper;
import com.boylu.mapper.SysUserMapper;
import com.boylu.utils.*;
import com.boylu.vo.user.OauthBindingVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.*;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Set<String> SUPPORTED_OAUTH_SOURCES = new HashSet<>(Arrays.asList("gitee", "qq", "weibo", "github"));
    private static final Set<String> LEGACY_LOGIN_SOURCES = new HashSet<>(Arrays.asList("wechat", "weixin", "wx", "applet"));

    private final SysUserMapper userMapper;

    private final SysRoleMapper roleMapper;

    private final SysMenuMapper menuMapper;

    private final EmailUtil emailUtil;

    private final RedisUtil redisUtil;

    private final SysUserMapper sysUserMapper;

    private final SysUserOauthBindingMapper oauthBindingMapper;

    private final String[] avatarList = {
            "https://api.dicebear.com/6.x/pixel-art/svg?seed=Raccoon",
            "https://api.dicebear.com/6.x/pixel-art/svg?seed=Kitty",
            "https://api.dicebear.com/6.x/pixel-art/svg?seed=Puppy",
            "https://api.dicebear.com/6.x/pixel-art/svg?seed=Bunny",
            "https://api.dicebear.com/6.x/pixel-art/svg?seed=Fox"
    };
    private final SysRoleMapper sysRoleMapper;

    private final GiteeConfigProperties giteeConfigProperties;

    private final GithubConfigProperties githubConfigProperties;

    private final QqConfigProperties qqConfigProperties;

    private final WeiboConfigProperties weiboConfigProperties;

    private final WechatProperties wechatProperties;

    private final SysConfigMapper sysConfigMapper;

    @Value("${site.frontend-url:${app.frontend-url:https://boylu.cn}}")
    private String frontendUrl;


    @Override
    public LoginUserInfo login(LoginDTO loginDTO) {

        SysConfig verifySwitch = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, "slider_verify_switch"));
        if (verifySwitch != null && verifySwitch.getConfigValue().equals("Y")) {
            //校验验证码
            CaptchaUtil.checkImageCode(loginDTO.getNonceStr(), loginDTO.getValue());
        }


        // 查询用户
        SysUser user = userMapper.selectByUsername(loginDTO.getUsername());

        //校验是否能够登录
        validateLogin(loginDTO, user);

        // 执行登录
        StpUtil.login(user.getId());
        String tokenValue = StpUtil.getTokenValue();

        // 返回用户信息
        LoginUserInfo loginUserInfo = BeanCopyUtil.copyObj(user, LoginUserInfo.class);
        loginUserInfo.setToken(tokenValue);

        StpUtil.getSession().set(Constants.CURRENT_USER, loginUserInfo);
        return loginUserInfo;
    }

    private static void validateLogin(LoginDTO loginDTO, SysUser user) {
        if (user == null) {
            throw new ServiceException("登录用户不存在");
        }

        // 验证密码
        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new ServiceException("用户名或密码错误");
        }

        // 验证状态
        if (user.getStatus() != 1) {
            throw new ServiceException("账号已被禁用");
        }

        if (user.getUsername().equals(Constants.TEST) && loginDTO.getSource().equalsIgnoreCase("PC")) {
            throw new ServiceException("演示用户不允许门户登录！");
        }
    }

    @Override
    public LoginUserInfo getLoginUserInfo(String source) {
        // 获取当前登录用户ID
        Integer userId = StpUtil.getLoginIdAsInt();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        LoginUserInfo loginUserInfo = BeanCopyUtil.copyObj(user, LoginUserInfo.class);

        //获取菜单权限列表
        if (source.equalsIgnoreCase(Constants.ADMIN)) {
            List<String> permissions;
            List<String> roles = roleMapper.selectRolesCodeByUserId(userId);
            if (roles.contains(Constants.ADMIN)) {
                permissions = menuMapper.getPermissionList(MenuTypeEnum.BUTTON.getCode());
            } else {
                permissions = menuMapper.getPermissionListByUserId(userId, MenuTypeEnum.BUTTON.getCode());
            }
            loginUserInfo.setRoles(roles);
            loginUserInfo.setPermissions(permissions);
        }

        return loginUserInfo;
    }

    @Override
    public Boolean sendEmailCode(String email) throws MessagingException {
        emailUtil.sendCode(email);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean register(EmailRegisterDto dto) {

        validateEmailCode(dto);

        SysUser sysUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getEmail()));
        if (sysUser != null) {
            throw new ServiceException("当前邮箱已注册，请前往登录");
        }

        //获取随机头像
        String avatar = avatarList[(int) (Math.random() * avatarList.length)];
        sysUser = SysUser.builder()
                .username(dto.getEmail())
                .password(BCrypt.hashpw(dto.getPassword()))
                .nickname(dto.getNickname())
                .email(dto.getEmail())
                .avatar(avatar)
                .status(Constants.YES)
                .build();
        sysUserMapper.insert(sysUser);

        //添加用户角色信息
        insertRole(sysUser);

        redisUtil.delete(RedisConstants.CAPTCHA_CODE_KEY + dto.getEmail());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean forgot(EmailRegisterDto dto) {
        validateEmailCode(dto);
        SysUser sysUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getEmail()));
        if (sysUser == null) {
            throw new ServiceException("当前邮箱未注册，请前往注册");
        }
        sysUser.setPassword(BCrypt.hashpw(dto.getPassword()));
        sysUserMapper.updateById(sysUser);
        redisUtil.delete(RedisConstants.CAPTCHA_CODE_KEY + dto.getEmail());
        return true;
    }

    @Override
    public String getWechatLoginCode() {
        //随机获取4位数字
        String code = "DL" + (int) ((Math.random() * 9 + 1) * 1000);
        redisUtil.set(RedisConstants.WX_LOGIN_USER_CODE + code, "NOT-LOGIN", RedisConstants.FIVE_MINUTES_EXPIRE, TimeUnit.SECONDS);
        return code;
    }

    @Override
    public LoginUserInfo getWechatIsLogin(String loginCode) {
        Object value = redisUtil.get(RedisConstants.WX_LOGIN_USER + loginCode);

        if (value == null) {
            throw new ServiceException("登录失败");
        }

        LoginUserInfo loginUserInfo = JSONUtil.toBean(JSONUtil.parseObj(value), LoginUserInfo.class);

        StpUtil.login(loginUserInfo.getId());
        loginUserInfo.setToken(StpUtil.getTokenValue());

        return loginUserInfo;
    }

    @Override
    public String wechatLogin(WxMpXmlMessage message) {
        String code = message.getContent().toUpperCase();
        //先判断登录码是否已过期
        Object e = redisUtil.hasKey(RedisConstants.WX_LOGIN_USER_CODE + code);
        if (e == null) {
            return "验证码已过期";
        }
        LoginUserInfo loginUserInfo = wechatLogin(message.getFromUser());
        //修改redis缓存 以便监听是否已经授权成功
        redisUtil.set(RedisConstants.WX_LOGIN_USER + code, JSONUtil.toJsonStr(loginUserInfo), RedisConstants.FIVE_MINUTES_EXPIRE, TimeUnit.SECONDS);
        return "网站登录成功！(若页面长时间未跳转请刷新验证码)";
    }

    @Override
    public String renderAuth(String source) {
        AuthRequest authRequest = getAuthRequest(normalizeOauthSource(source));
        return authRequest.authorize(AuthStateUtils.createState());
    }

    @Override
    public String renderBindAuth(String source) {
        StpUtil.checkLogin();
        String normalizedSource = normalizeOauthSource(source);
        if (!SUPPORTED_OAUTH_SOURCES.contains(normalizedSource)) {
            throw new ServiceException("当前第三方账号暂不支持网页绑定");
        }

        String state = "bind:" + UUID.randomUUID();
        redisUtil.set(RedisConstants.OAUTH_BIND_STATE + state, StpUtil.getLoginIdAsLong(), RedisConstants.FIVE_MINUTES_EXPIRE, TimeUnit.SECONDS);
        return getAuthRequest(normalizedSource).authorize(state);
    }

    @Override
    public List<OauthBindingVo> listOauthBindings() {
        StpUtil.checkLogin();
        Long loginUserId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(loginUserId);
        if (user != null) {
            ensureLegacyBinding(user);
        }

        List<SysUserOauthBinding> bindings = oauthBindingMapper.selectList(new LambdaQueryWrapper<SysUserOauthBinding>()
                .eq(SysUserOauthBinding::getUserId, loginUserId));
        List<OauthBindingVo> result = new ArrayList<>();
        for (SysUserOauthBinding binding : bindings) {
            OauthBindingVo vo = new OauthBindingVo();
            vo.setSource(binding.getSource());
            vo.setUsername(maskOpenId(binding.getUsername()));
            vo.setNickname(StringUtils.defaultIfBlank(binding.getNickname(), binding.getUsername()));
            vo.setAvatar(binding.getAvatar());
            vo.setBindable(SUPPORTED_OAUTH_SOURCES.contains(binding.getSource()));
            vo.setBindTime(binding.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unbindOauth(String source) {
        StpUtil.checkLogin();
        String normalizedSource = normalizeOauthSource(source);
        Long loginUserId = StpUtil.getLoginIdAsLong();
        SysUserOauthBinding binding = oauthBindingMapper.selectOne(new LambdaQueryWrapper<SysUserOauthBinding>()
                .eq(SysUserOauthBinding::getUserId, loginUserId)
                .eq(SysUserOauthBinding::getSource, normalizedSource));
        if (binding == null) {
            return true;
        }

        SysUser currentUser = userMapper.selectById(loginUserId);
        if (currentUser != null
                && normalizedSource.equalsIgnoreCase(StringUtils.defaultString(currentUser.getLoginType()))
                && binding.getOpenId().equals(currentUser.getUsername())) {
            throw new ServiceException("当前账号的主要登录方式不能直接解绑，请先绑定邮箱账号或其他登录方式");
        }

        oauthBindingMapper.deleteById(binding.getId());
        return true;
    }


    @Override
    public void authLogin(AuthCallback callback,String source, HttpServletResponse httpServletResponse) throws IOException {
        String normalizedSource = normalizeOauthSource(source);
        AuthRequest authRequest = getAuthRequest(normalizedSource);
        AuthResponse<AuthUser> response = authRequest.login(callback);
        String callbackState = callback.getState();

        if (response.getData() == null) {
            log.info("用户取消了 {} 第三方登录", normalizedSource);
            if (isBindCallbackState(callbackState)) {
                clearBindState(callbackState);
                httpServletResponse.sendRedirect(buildFrontendRedirect("/user/profile", "tab=binding&bind=cancelled&source=" + encode(normalizedSource)));
            } else {
                httpServletResponse.sendRedirect(buildFrontendRedirect("/login", "oauth=cancelled&source=" + encode(normalizedSource)));
            }
            return;
        }
        String result = com.alibaba.fastjson.JSONObject.toJSONString(response.getData());
        log.info("第三方登录验证结果:{}", result);

        com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(result);
        Object uuid = jsonObject.get("uuid");
        if (uuid == null || StringUtils.isBlank(uuid.toString())) {
            httpServletResponse.sendRedirect(buildFrontendRedirect("/login", "oauth=failed&message=" + encode("第三方账号缺少唯一标识")));
            return;
        }

        if (isBindCallbackState(callbackState)) {
            try {
                bindOauthAccount(callbackState, normalizedSource, jsonObject);
                httpServletResponse.sendRedirect(buildFrontendRedirect("/user/profile", "tab=binding&bind=success&source=" + encode(normalizedSource)));
            } catch (ServiceException e) {
                httpServletResponse.sendRedirect(buildFrontendRedirect("/user/profile", "tab=binding&bind=failed&source=" + encode(normalizedSource) + "&message=" + encode(e.getMessage())));
            }
            return;
        }

        // 获取用户ip信息
        String ipAddress = IpUtil.getIp();
        String ipSource = IpUtil.getIp2region(ipAddress);
        // 判断是否已注册
        SysUserOauthBinding binding = oauthBindingMapper.selectOne(new LambdaQueryWrapper<SysUserOauthBinding>()
                .eq(SysUserOauthBinding::getSource, normalizedSource)
                .eq(SysUserOauthBinding::getOpenId, uuid.toString()));
        SysUser user = binding == null ? null : userMapper.selectById(binding.getUserId());
        if (user == null) {
            user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, uuid.toString()));
        }
        if (ObjectUtils.isEmpty(user)) {
            // 保存账号信息
            user = SysUser.builder()
                    .username(uuid.toString())
                    .password(UUID.randomUUID().toString())
                    .loginType(normalizedSource)
                    .lastLoginTime(LocalDateTime.now())
                    .ipLocation(ipAddress)
                    .ip(ipSource)
                    .status(Constants.YES)
                    .nickname(normalizedSource + "-" +getRandomString(6))
                    .avatar(StringUtils.defaultIfBlank(jsonObject.getString("avatar"), avatarList[(int) (Math.random() * avatarList.length)]))
                    .build();
            userMapper.insert(user);
            //添加角色
            insertRole(user);
        } else if (user.getStatus() == Constants.NO) {
            httpServletResponse.sendRedirect(buildFrontendRedirect("/login", "oauth=disabled&message=" + encode("账号已被禁用")));
            return;
        }

        upsertOauthBinding(user.getId().longValue(), normalizedSource, jsonObject);

        StpUtil.login(user.getId());
        httpServletResponse.sendRedirect(buildFrontendRedirect("/", "token=" + encode(StpUtil.getTokenValue())));
    }

    @Override
    public LoginUserInfo appletLogin(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + wechatProperties.getAppletAppId()
                + "&secret=" + wechatProperties.getAppletSecret() + "&js_code=" + code + "&grant_type=authorization_code";
        String result = HttpUtil.get(url);
        com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(result);
        Object openid = jsonObject.get("openid");
        if (openid == null) {
            throw new ServiceException("登录失败");
        }

        // 查询用户
        SysUser user = userMapper.selectByUsername(openid.toString());

        if (user == null) {
            String ip = IpUtil.getIp();
            String avatar = avatarList[(int) (Math.random() * avatarList.length)];
            user = SysUser.builder()
                    .username(openid.toString())
                    .password(UUID.randomUUID().toString())
                    .loginType(LoginTypeEnum.APPLET.getType())
                    .lastLoginTime(LocalDateTime.now())
                    .ipLocation(IpUtil.getIp2region(ip))
                    .ip(ip)
                    .status(Constants.YES)
                    .nickname("applet-" + getRandomString(6))
                    .avatar(avatar)
                    .build();
            userMapper.insert(user);
            //添加用户角色信息
            this.insertRole(user);
        }else {
            if (user.getStatus() == Constants.NO) {
                throw new ServiceException("账号已被禁用，请联系管理员");
            }
        }

        LoginUserInfo loginUserInfo = BeanCopyUtil.copyObj(user, LoginUserInfo.class);

        StpUtil.login(loginUserInfo.getId());
        loginUserInfo.setToken(StpUtil.getTokenValue());

        return loginUserInfo;
    }

    @Override
    public Captcha getCaptcha() {
        Captcha captcha = new Captcha();
        CaptchaUtil.getCaptcha(captcha);
        return captcha;
    }

    private void validateEmailCode(EmailRegisterDto dto) {
        Object code = redisUtil.get(RedisConstants.CAPTCHA_CODE_KEY + dto.getEmail());
        if (code == null || !code.equals(dto.getCode())) {
            throw new ServiceException("验证码已过期或输入错误");
        }
    }

    private LoginUserInfo wechatLogin(String openId) {

        SysUser user = userMapper.selectByUsername(openId);
        if (ObjectUtils.isEmpty(user)) {
            String ip = IpUtil.getIp();
            String ipSource = IpUtil.getIp2region(ip);

            // 保存账号信息
            user = SysUser.builder()
                    .username(openId)
                    .password(BCrypt.hashpw(openId))
                    .nickname("WECHAT-" + getRandomString(6))
                    .avatar(avatarList[(int) (Math.random() * avatarList.length)])
                    .loginType(LoginTypeEnum.WECHAT.getType())
                    .lastLoginTime(LocalDateTime.now())
                    .ip(ip)
                    .ipLocation(ipSource)
                    .status(Constants.YES)
                    .build();
            userMapper.insert(user);

            //添加用户角色信息
            this.insertRole(user);
        }

        return BeanCopyUtil.copyObj(user, LoginUserInfo.class);
    }

    /**
     * 添加用户角色信息
     * @param user
     */
    private void insertRole(SysUser user) {
        SysRole sysRole = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, Constants.USER));
        sysRoleMapper.addRoleUser(user.getId(), Collections.singletonList(sysRole.getId()));
    }

    /**
     * 随机生成6位数的字符串
     */
    public static String getRandomString(int length) {
        String str = "QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(str.length());
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    private void bindOauthAccount(String state, String source, com.alibaba.fastjson.JSONObject authUser) {
        Object userIdValue = redisUtil.get(RedisConstants.OAUTH_BIND_STATE + state);
        redisUtil.delete(RedisConstants.OAUTH_BIND_STATE + state);
        if (userIdValue == null) {
            throw new ServiceException("绑定状态已过期，请重新发起绑定");
        }

        Long userId = Long.valueOf(userIdValue.toString());
        SysUser currentUser = userMapper.selectById(userId);
        if (currentUser == null) {
            throw new ServiceException("当前用户不存在");
        }

        String openId = authUser.getString("uuid");
        SysUser legacyUser = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, openId));
        if (legacyUser != null && legacyUser.getId().longValue() != userId.longValue()) {
            throw new ServiceException("该第三方账号已作为其他站内账号使用，不能直接绑定");
        }

        upsertOauthBinding(userId, source, authUser);
    }

    private boolean isBindCallbackState(String state) {
        return StringUtils.isNotBlank(state) && state.startsWith("bind:");
    }

    private void clearBindState(String state) {
        if (isBindCallbackState(state)) {
            redisUtil.delete(RedisConstants.OAUTH_BIND_STATE + state);
        }
    }

    private void upsertOauthBinding(Long userId, String source, com.alibaba.fastjson.JSONObject authUser) {
        String openId = authUser.getString("uuid");
        if (StringUtils.isBlank(openId)) {
            throw new ServiceException("第三方账号缺少唯一标识");
        }

        SysUserOauthBinding byOpenId = oauthBindingMapper.selectOne(new LambdaQueryWrapper<SysUserOauthBinding>()
                .eq(SysUserOauthBinding::getSource, source)
                .eq(SysUserOauthBinding::getOpenId, openId));
        if (byOpenId != null && !byOpenId.getUserId().equals(userId)) {
            throw new ServiceException("该第三方账号已绑定其他用户");
        }

        SysUserOauthBinding byUserSource = oauthBindingMapper.selectOne(new LambdaQueryWrapper<SysUserOauthBinding>()
                .eq(SysUserOauthBinding::getUserId, userId)
                .eq(SysUserOauthBinding::getSource, source));
        if (byUserSource != null && !byUserSource.getOpenId().equals(openId)) {
            throw new ServiceException("该来源已绑定其他第三方账号，请先解绑");
        }

        SysUserOauthBinding binding = byOpenId != null ? byOpenId : byUserSource;
        if (binding == null) {
            binding = new SysUserOauthBinding();
            binding.setUserId(userId);
            binding.setSource(source);
            binding.setOpenId(openId);
        }

        binding.setUsername(StringUtils.defaultIfBlank(authUser.getString("username"), openId));
        binding.setNickname(StringUtils.defaultIfBlank(authUser.getString("nickname"), binding.getUsername()));
        binding.setAvatar(authUser.getString("avatar"));

        if (binding.getId() == null) {
            oauthBindingMapper.insert(binding);
        } else {
            oauthBindingMapper.updateById(binding);
        }
    }

    private void ensureLegacyBinding(SysUser user) {
        String source = normalizeOauthSource(user.getLoginType());
        if (StringUtils.isBlank(source) || StringUtils.isBlank(user.getUsername())) {
            return;
        }
        if (!SUPPORTED_OAUTH_SOURCES.contains(source) && !LEGACY_LOGIN_SOURCES.contains(source)) {
            return;
        }

        SysUserOauthBinding exists = oauthBindingMapper.selectOne(new LambdaQueryWrapper<SysUserOauthBinding>()
                .eq(SysUserOauthBinding::getSource, source)
                .eq(SysUserOauthBinding::getOpenId, user.getUsername()));
        if (exists != null) {
            return;
        }

        SysUserOauthBinding userSourceExists = oauthBindingMapper.selectOne(new LambdaQueryWrapper<SysUserOauthBinding>()
                .eq(SysUserOauthBinding::getUserId, user.getId())
                .eq(SysUserOauthBinding::getSource, source));
        if (userSourceExists != null) {
            return;
        }

        SysUserOauthBinding binding = new SysUserOauthBinding();
        binding.setUserId(user.getId().longValue());
        binding.setSource(source);
        binding.setOpenId(user.getUsername());
        binding.setUsername(user.getUsername());
        binding.setNickname(StringUtils.defaultIfBlank(user.getNickname(), user.getUsername()));
        binding.setAvatar(user.getAvatar());
        oauthBindingMapper.insert(binding);
    }

    private String buildFrontendRedirect(String path, String query) {
        String base = StringUtils.defaultIfBlank(frontendUrl, "https://boylu.cn").replaceAll("/+$", "");
        String normalizedPath = StringUtils.defaultIfBlank(path, "/");
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        String url = base + normalizedPath;
        if (StringUtils.isNotBlank(query)) {
            url += "?" + query;
        }
        return url;
    }

    private String normalizeOauthSource(String source) {
        String normalized = StringUtils.lowerCase(StringUtils.trimToEmpty(source));
        if ("wx".equals(normalized) || "weixin".equals(normalized) || "applet".equals(normalized)) {
            return "wechat";
        }
        return normalized;
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            throw new IllegalStateException("UTF-8 encoding is not available", ex);
        }
    }

    private String maskOpenId(String value) {
        if (StringUtils.isBlank(value) || value.length() <= 8) {
            return value;
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private @NotNull AuthRequest getAuthRequest(String source) {
        AuthRequest authRequest = null;
        switch (source) {
            case "gitee":
                authRequest = new AuthGiteeRequest(AuthConfig.builder()
                        .clientId(giteeConfigProperties.getAppId())
                        .clientSecret(giteeConfigProperties.getAppSecret())
                        .redirectUri(giteeConfigProperties.getRedirectUrl())
                        .build());
                break;
            case "qq":
                authRequest = new AuthQqRequest(AuthConfig.builder()
                        .clientId(qqConfigProperties.getAppId())
                        .clientSecret(qqConfigProperties.getAppSecret())
                        .redirectUri(qqConfigProperties.getRedirectUrl())
                        .build());
                break;
            case "weibo":
                authRequest = new AuthWeiboRequest(AuthConfig.builder()
                        .clientId(weiboConfigProperties.getAppId())
                        .clientSecret(weiboConfigProperties.getAppSecret())
                        .redirectUri(weiboConfigProperties.getRedirectUrl())
                        .build());
                break;
            case "github":
                authRequest = new AuthGithubRequest(AuthConfig.builder()
                        .clientId(githubConfigProperties.getAppId())
                        .clientSecret(githubConfigProperties.getAppSecret())
                        .redirectUri(githubConfigProperties.getRedirectUrl())
                        .build());
                break;
            default:
                break;
        }
        if (null == authRequest) {
            throw new AuthException("授权地址无效");
        }
        return authRequest;
    }

}
