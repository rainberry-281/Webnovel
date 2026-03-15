package com.bn.berrynovel.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "Library")
public class Bookshelf {

    @EmbeddedId
    private BookshelfId id;

    @ManyToOne
    @MapsId("userId")
    private User user;

    @ManyToOne
    @MapsId("novelId")
    private Novel novel;

    @Column(name = "saved_at")
    private LocalDateTime savedAt;

    public BookshelfId getId() {
        return id;
    }

    public void setId(BookshelfId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Novel getNovel() {
        return novel;
    }

    public void setNovel(Novel novel) {
        this.novel = novel;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.savedAt == null) {
            this.savedAt = LocalDateTime.now();
        }
    }
}
