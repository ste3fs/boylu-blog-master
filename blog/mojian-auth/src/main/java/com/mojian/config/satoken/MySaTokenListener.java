package com.mojian.config.satoken;

import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.hutool.json.JSONUtil;
import com.mojian.common.RedisConstants;
import com.mojian.entity.SysUser;
import com.mojian.mapper.SysUserMapper;
import com.mojian.utils.IpUtil;
import com.mojian.utils.RedisUtil;
import com.mojian.utils.UserAgentUtil;
import com.mojian.vo.user.OnlineUserVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 自定义侦听器的实现
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MySaTokenListener implements SaTokenListener {

    private final SysUserMapper userMapper;

    private final HttpServletRequest request;

    private final RedisUtil redisUtil;

    @Value("${sa-token.timeout}")
    private Integer timeout;

    /** 每次登录时触发 */
    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginModel loginModel) {

        String ip = IpUtil.getIp();
        SysUser user = userMapper.selectById((Integer) loginId);
        // 更新登录信息
        String userAgent = request.getHeader("User-Agent");
        user.setLastLoginTime(LocalDateTime.now());
        user.setIp(ip);
        user.setIpLocation(IpUtil.getIp2region(ip));
        user.setOs(UserAgentUtil.getOs(userAgent));
        user.setBrowser(UserAgentUtil.getBrowser(userAgent));
        userMapper.updateById(user);

        OnlineUserVo onlineUserVo = new OnlineUserVo();
        BeanUtils.copyProperties(user, onlineUserVo);
        onlineUserVo.setTokenValue(tokenValue);
        onlineUserVo.setPassword("");
        redisUtil.set(RedisConstants.LOGIN_TOKEN + tokenValue, JSONUtil.toJsonStr(onlineUserVo),timeout, TimeUnit.SECONDS);
        log.debug("Sa-Token login event, loginId={}", loginId);
    }

    /** 每次注销时触发 */
    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        redisUtil.delete(RedisConstants.LOGIN_TOKEN + tokenValue);
        log.debug("Sa-Token logout event, loginId={}", loginId);
    }

    /** 每次被踢下线时触发 */
    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        redisUtil.delete(RedisConstants.LOGIN_TOKEN + tokenValue);
        log.debug("Sa-Token kickout event, loginId={}", loginId);
    }

    /** 每次被顶下线时触发 */
    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        redisUtil.delete(RedisConstants.LOGIN_TOKEN + tokenValue);
        log.debug("Sa-Token replaced event, loginId={}", loginId);
    }

    /** 每次被封禁时触发 */
    @Override
    public void doDisable(String loginType, Object loginId, String service, int level, long disableTime) {
        log.debug("Sa-Token disable event, loginId={}, service={}", loginId, service);
    }

    /** 每次被解封时触发 */
    @Override
    public void doUntieDisable(String loginType, Object loginId, String service) {
        log.debug("Sa-Token untie disable event, loginId={}, service={}", loginId, service);
    }

    /** 每次二级认证时触发 */
    @Override
    public void doOpenSafe(String loginType, String tokenValue, String service, long safeTime) {
        log.debug("Sa-Token open safe event, service={}", service);
    }

    /** 每次退出二级认证时触发 */
    @Override
    public void doCloseSafe(String loginType, String tokenValue, String service) {
        log.debug("Sa-Token close safe event, service={}", service);
    }

    /** 每次创建Session时触发 */
    @Override
    public void doCreateSession(String id) {
        log.debug("Sa-Token create session event, sessionId={}", id);
    }

    /** 每次注销Session时触发 */
    @Override
    public void doLogoutSession(String id) {
        log.debug("Sa-Token logout session event, sessionId={}", id);
    }

    /** 每次Token续期时触发 */
    @Override
    public void doRenewTimeout(String tokenValue, Object loginId, long timeout) {
        log.debug("Sa-Token renew timeout event, loginId={}", loginId);
    }
}
