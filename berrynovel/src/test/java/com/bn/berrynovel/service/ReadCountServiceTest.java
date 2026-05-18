package com.bn.berrynovel.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.bn.berrynovel.domain.ChapterReadLog;
import com.bn.berrynovel.domain.NovelHot;
import com.bn.berrynovel.repository.ChapterReadLogRepository;
import com.bn.berrynovel.repository.ChapterRepository;
import com.bn.berrynovel.repository.NovelRepository;

@ExtendWith(MockitoExtension.class)
class ReadCountServiceTest {

    @Mock
    private ChapterReadLogRepository logRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private NovelRepository novelRepository;

    @Mock
    private UserService userService;

    private ReadCountService readCountService;

    @BeforeEach
    void setUp() {
        this.readCountService = new ReadCountService(
                this.logRepository,
                this.chapterRepository,
                this.novelRepository,
                this.userService);
        ReflectionTestUtils.setField(this.readCountService, "cooldownMinutes", 30);
        ReflectionTestUtils.setField(this.readCountService, "cooldownSeconds", 10);
        ReflectionTestUtils.setField(this.readCountService, "hotReadThreshold", 10L);
    }

    @Test
    void validAnonymousReadIncrementsCountsAndSyncsHotStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(this.logRepository.findFirstByChapterIdAndSessionIdAndReadAtAfter(
                eq(7L), anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        this.readCountService.recordChapterRead(3L, 7L, null, request);

        verify(this.chapterRepository).incrementReadCount(7L);
        verify(this.novelRepository).incrementTotalReadCount(3L);
        verify(this.novelRepository).markHotIfReadThresholdReached(3L, 10L, NovelHot.HOT);
        verify(this.logRepository).save(any(ChapterReadLog.class));
    }

    @Test
    void cooldownHitDoesNotIncrementCountsOrSyncHotStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(this.logRepository.findFirstByChapterIdAndSessionIdAndReadAtAfter(
                eq(7L), anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(new ChapterReadLog()));

        this.readCountService.recordChapterRead(3L, 7L, null, request);

        verify(this.chapterRepository, never()).incrementReadCount(any());
        verify(this.novelRepository, never()).incrementTotalReadCount(any());
        verify(this.novelRepository, never()).markHotIfReadThresholdReached(any(), any(), any());
        verify(this.logRepository, never()).save(any());
    }
}
