package com.example.infinite.domain.dm.entity;

import com.example.infinite.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "dm_rooms", indexes = {
        @Index(name = "uk_dm_room_user_artist", columnList = "user_id, artist_id", unique = true),
        @Index(name = "idx_dm_room_artist", columnList = "artist_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE dm_rooms SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class DmRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "artist_id", nullable = false)
    private Long artistId;

    @Builder
    private DmRoom(Long userId, Long artistId) {
        this.userId = userId;
        this.artistId = artistId;
    }
}