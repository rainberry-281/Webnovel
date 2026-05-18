package com.bn.berrynovel.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.bn.berrynovel.domain.ChapterReadLog;
import com.bn.berrynovel.domain.NovelHot;
import com.bn.berrynovel.domain.User;
import com.bn.berrynovel.repository.ChapterReadLogRepository;
import com.bn.berrynovel.repository.ChapterRepository;
import com.bn.berrynovel.repository.NovelRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ReadCountService {
    private static final Logger logger = LoggerFactory.getLogger(ReadCountService.class);

    private final ChapterReadLogRepository logRepository;
    private final ChapterRepository chapterRepository;
    private final NovelRepository novelRepository;
    private final UserService userService;

    @Value("${read.count.cooldown-minutes:30}")
    private int cooldownMinutes;

    @Value("${read.count.cooldown-seconds:0}")
    private int cooldownSeconds;

    @Value("${hot.novel.read-threshold:10}")
    private long hotReadThreshold;

    public ReadCountService(ChapterReadLogRepository logRepository, ChapterRepository chapterRepository,
            NovelRepository novelRepository, UserService userService) {
        this.logRepository = logRepository;
        this.chapterRepository = chapterRepository;
        this.novelRepository = novelRepository;
        this.userService = userService;
    }

    @Transactional
    public void recordChapterRead(Long novelId, Long chapterId, Authentication authentication,
            HttpServletRequest request) {
        try {
            LocalDateTime since = LocalDateTime.now().minusSeconds(resolveCooldownSeconds());

            if (isLoggedIn(authentication)) {
                Long userId = resolveUserId(authentication);
                if (userId != null && this.logRepository
                        .findFirstByChapterIdAndUserIdAndReadAtAfter(chapterId, userId, since)
                        .isPresent()) {
                    return;
                }

                incrementCounts(novelId, chapterId);
                saveLog(chapterId, novelId, userId, null, request);
                return;
            }

            String sessionId = request.getSession(true).getId();
            if (this.logRepository.findFirstByChapterIdAndSessionIdAndReadAtAfter(chapterId, sessionId, since)
                    .isPresent()) {
                return;
            }

            incrementCounts(novelId, chapterId);
            saveLog(chapterId, novelId, null, sessionId, request);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.warn("Failed to record chapter read for novelId={}, chapterId={}", novelId, chapterId, e);
        }
    }

    private void incrementCounts(Long novelId, Long chapterId) {
        this.chapterRepository.incrementReadCount(chapterId);
        this.novelRepository.incrementTotalReadCount(novelId);
        this.novelRepository.markHotIfReadThresholdReached(novelId, this.hotReadThreshold, NovelHot.HOT);
    }

    private long resolveCooldownSeconds() {
        if (cooldownSeconds > 0) {
            return cooldownSeconds;
        }
        return Math.max(cooldownMinutes, 0) * 60L;
    }

    private void saveLog(Long chapterId, Long novelId, Long userId, String sessionId, HttpServletRequest request) {
        ChapterReadLog log = new ChapterReadLog();
        log.setChapterId(chapterId);
        log.setNovelId(novelId);
        log.setUserId(userId);
        log.setSessionId(sessionId);
        log.setIpHash(hashString(getClientIp(request)));
        log.setUserAgentHash(hashString(request.getHeader("User-Agent")));
        log.setReadAt(LocalDateTime.now());
        this.logRepository.save(log);
    }

    private boolean isLoggedIn(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName());
    }

    private Long resolveUserId(Authentication authentication) {
        User user = this.userService.getUserByUsername(authentication.getName());
        return user == null ? null : user.getId();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String hashString(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return null;
        }
    }
}
