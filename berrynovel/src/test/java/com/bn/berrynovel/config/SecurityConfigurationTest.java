package com.bn.berrynovel.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

class SecurityConfigurationTest {

    @Test
    void slugGetMatchersAllowQueryString() {
        assertThat(matches(SecurityConfiguration.NOVEL_SLUG_URL_PATTERN, HttpMethod.GET,
                "/42-toan-thu-than-nong-dan", null)).isTrue();
        assertThat(matches(SecurityConfiguration.NOVEL_SLUG_URL_PATTERN, HttpMethod.GET,
                "/42-toan-thu-than-nong-dan", "from=home")).isTrue();
        assertThat(matches(SecurityConfiguration.READER_SLUG_URL_PATTERN, HttpMethod.GET,
                "/42-toan-thu-than-nong-dan/c101-chuong-1-khai-dau", "from=bookmark")).isTrue();
    }

    @Test
    void slugMatchersDoNotPermitNonGetOrNonSlugPaths() {
        assertThat(matches(SecurityConfiguration.NOVEL_SLUG_URL_PATTERN, HttpMethod.POST,
                "/42-toan-thu-than-nong-dan", "from=home")).isFalse();
        assertThat(matches(SecurityConfiguration.READER_SLUG_URL_PATTERN, HttpMethod.POST,
                "/42-toan-thu-than-nong-dan/c101-chuong-1-khai-dau", null)).isFalse();
        assertThat(matches(SecurityConfiguration.NOVEL_SLUG_URL_PATTERN, HttpMethod.GET,
                "/bookshelf", null)).isFalse();
    }

    private boolean matches(String pattern, HttpMethod method, String servletPath, String queryString) {
        RequestMatcher matcher = RegexRequestMatcher.regexMatcher(HttpMethod.GET, pattern);
        MockHttpServletRequest request = new MockHttpServletRequest(method.name(), servletPath);
        request.setServletPath(servletPath);
        request.setQueryString(queryString);
        return matcher.matches(request);
    }
}
