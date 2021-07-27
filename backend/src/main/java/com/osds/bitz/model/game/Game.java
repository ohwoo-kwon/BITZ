//package com.osds.bitz.model.game;
//
//import lombok.*;
//
//import javax.persistence.*;
//
//import java.time.LocalDateTime;
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Entity(name="game")
//public class Game {
//
//    @EmbeddedId
//    private GamePK gamePK;
//
//    @Column
//    private String gameID; // 게임ID
//    private LocalDateTime date; // 날짜
//    private String gymID; // 체육관ID
//    private LocalDateTime startTime; // 시작시간
//    private LocalDateTime endTime; // 종료시간
//    private int minPeople; // 최소인원
//    private int maxPeople; // 최대인원
//    private int participant; // 참가인원
//    private int quarter; // 쿼터
//
//}