package com.altacod.favorites.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "sync_state")
public class SyncState {

    @Id
    private Long id = 1L;

    @Column(name = "last_update_id", nullable = false)
    private Long lastUpdateId = 0L;

    @Column(name = "last_user_message_id", nullable = false)
    private Long lastUserMessageId = 0L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLastUpdateId() {
        return lastUpdateId;
    }

    public void setLastUpdateId(Long lastUpdateId) {
        this.lastUpdateId = lastUpdateId;
    }

    public Long getLastUserMessageId() {
        return lastUserMessageId;
    }

    public void setLastUserMessageId(Long lastUserMessageId) {
        this.lastUserMessageId = lastUserMessageId;
    }
}
