package com.bn.berrynovel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.bn.berrynovel.domain.Novel;
import com.bn.berrynovel.domain.NovelHot;
import com.bn.berrynovel.repository.ChapterRepository;
import com.bn.berrynovel.repository.GenreRepository;
import com.bn.berrynovel.repository.NovelRepository;
import com.turkraft.springfilter.converter.FilterSpecificationConverter;

class NovelServiceTest {

    @Test
    void isHotUsesPersistedHotStatusOnly() {
        NovelService novelService = new NovelService(
                mock(NovelRepository.class),
                mock(GenreRepository.class),
                mock(ImageService.class),
                mock(ChapterRepository.class),
                mock(FilterSpecificationConverter.class));

        Novel belowThresholdButSavedHot = new Novel();
        belowThresholdButSavedHot.setHot(NovelHot.HOT);
        belowThresholdButSavedHot.setTotalReadCount(0L);

        Novel aboveThresholdButNotSavedHot = new Novel();
        aboveThresholdButNotSavedHot.setHot(NovelHot.NOT_HOT);
        aboveThresholdButNotSavedHot.setTotalReadCount(1_000L);

        Novel nullHot = new Novel();
        nullHot.setHot(null);
        nullHot.setTotalReadCount(null);

        assertThat(novelService.isHot(belowThresholdButSavedHot)).isTrue();
        assertThat(novelService.isHot(aboveThresholdButNotSavedHot)).isFalse();
        assertThat(novelService.isHot(nullHot)).isFalse();
        assertThat(novelService.isHot(null)).isFalse();
    }
}
