package com.bn.berrynovel.domain;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable
public class BookshelfId implements Serializable {
    private Long userId;

    private Long novelId;

    public BookshelfId() {
    }

    public BookshelfId(Long userId, Long novelId) {
        this.userId = userId;
        this.novelId = novelId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BookshelfId other = (BookshelfId) obj;
        return Objects.equals(this.userId, other.userId) && Objects.equals(this.novelId, other.novelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.userId, this.novelId);
    }

}
