package com.bn.berrynovel.domain;

import org.springframework.data.domain.Page;

public class PaginationQuery<T> {
    private int page;
    private Page<T> nvs;

    public PaginationQuery(int page, Page<T> nvs) {
        this.page = page;
        this.nvs = nvs;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public Page<T> getNvs() {
        return nvs;
    }

    public void setNvs(Page<T> nvs) {
        this.nvs = nvs;
    }

}
