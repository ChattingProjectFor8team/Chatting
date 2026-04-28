package com.example.infinite.global.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreRemove;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
// 생성일, 수정일, 삭제일 같은 공통 컬럼을 모든 엔티티에 상속한다.
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PreRemove
    protected void preRemove() {
        // @SQLDelete는 DB 행만 갱신하므로, 현재 영속성 컨텍스트 안의 엔티티 상태도 함께 맞춘다.
        this.deletedAt = LocalDateTime.now();
    }

    public void delete() {
        // 서비스 계층에서 명시적으로 soft delete 상태로 전환할 때 사용한다.
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        // soft delete 여부를 서비스 계층에서 빠르게 판단할 때 사용한다.
        return this.deletedAt != null;
    }
}
