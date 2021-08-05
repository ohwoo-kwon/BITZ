package com.osds.bitz.model.network.response.game;

import com.osds.bitz.model.entity.game.Game;
import com.osds.bitz.model.entity.game.GameParticipant;
import com.osds.bitz.model.entity.gym.Gym;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameDetailResponse { // 게임상세 페이지 보여주기 객체

    ArrayList<GameParticipant> gameParticipantList; // 게임 참여자들 정보

    Game gameInfo; // 게임정보

}
