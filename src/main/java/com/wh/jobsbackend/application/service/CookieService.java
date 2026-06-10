package com.wh.jobsbackend.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.wh.jobsbackend.application.entity.CookieEntity;
import com.wh.jobsbackend.application.mapper.CookieMapper;
import com.wh.jobsbackend.application.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CookieService {

    private final CookieMapper cookieMapper;
    private final CurrentUserService currentUserService;

    public CookieEntity getCookieByPlatform(String platform) {
        return getCookieByPlatform(currentUserService.requireUserId(), platform);
    }

    public CookieEntity getCookieByPlatform(Long userId, String platform) {
        LambdaQueryWrapper<CookieEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CookieEntity::getUserId, userId)
                .eq(CookieEntity::getPlatform, platform)
                .orderByDesc(CookieEntity::getUpdatedAt)
                .last("LIMIT 1");
        return cookieMapper.selectOne(wrapper);
    }

    public boolean saveOrUpdateCookie(String platform, String cookieValue, String remark) {
        return saveOrUpdateCookie(currentUserService.requireUserId(), platform, cookieValue, remark);
    }

    public boolean saveOrUpdateCookie(Long userId, String platform, String cookieValue, String remark) {
        CookieEntity existingCookie = getCookieByPlatform(userId, platform);

        if (existingCookie != null) {
            existingCookie.setCookieValue(cookieValue);
            existingCookie.setRemark(remark);
            existingCookie.setUpdatedAt(LocalDateTime.now());
            return cookieMapper.updateById(existingCookie) > 0;
        }

        CookieEntity newCookie = new CookieEntity();
        newCookie.setUserId(userId);
        newCookie.setPlatform(platform);
        newCookie.setCookieValue(cookieValue);
        newCookie.setRemark(remark);
        newCookie.setCreatedAt(LocalDateTime.now());
        newCookie.setUpdatedAt(LocalDateTime.now());
        return cookieMapper.insert(newCookie) > 0;
    }

    public boolean clearCookieByPlatform(String platform, String remark) {
        return clearCookieByPlatform(currentUserService.requireUserId(), platform, remark);
    }

    public boolean clearCookieByPlatform(Long userId, String platform, String remark) {
        UpdateWrapper<CookieEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", userId)
                .eq("platform", platform)
                .set("cookie_value", "")
                .set("remark", remark)
                .set("updated_at", LocalDateTime.now());
        return cookieMapper.update(null, updateWrapper) > 0;
    }

    public boolean deleteCookie(String platform) {
        return deleteCookie(currentUserService.requireUserId(), platform);
    }

    public boolean deleteCookie(Long userId, String platform) {
        LambdaQueryWrapper<CookieEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CookieEntity::getUserId, userId)
                .eq(CookieEntity::getPlatform, platform);
        return cookieMapper.delete(wrapper) > 0;
    }

    public List<CookieEntity> getAllCookies() {
        LambdaQueryWrapper<CookieEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CookieEntity::getUserId, currentUserService.requireUserId());
        return cookieMapper.selectList(wrapper);
    }
}
