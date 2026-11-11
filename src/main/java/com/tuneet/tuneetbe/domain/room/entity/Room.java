package com.tuneet.tuneetbe.domain.room.entity;

import com.tuneet.tuneetbe.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @Column(nullable = false)
    private String roomTitle; // 방 이름

    private String roomPicture; // 방 이미지

    private String password; // 방 비밀번호 (4자리, 해시 저장)

    private String roomDescription; // 방 소개

    @Column(nullable = false, unique = true)
    private String inviteCode; // 초대 코드

    @Builder
    private Room(String roomTitle, String roomPicture, String password, String roomDescription, String inviteCode) {
        this.roomTitle = roomTitle;
        this.roomPicture = roomPicture;
        this.password = password;
        this.roomDescription = roomDescription;
        this.inviteCode = inviteCode;
    }
}
