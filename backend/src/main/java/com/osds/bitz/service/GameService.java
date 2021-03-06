package com.osds.bitz.service;

import com.osds.bitz.model.RecordTable;
import com.osds.bitz.model.entity.account.business.BusinessProfile;
import com.osds.bitz.model.entity.account.user.Manner;
import com.osds.bitz.model.entity.account.user.Skill;
import com.osds.bitz.model.entity.account.user.UserAuth;
import com.osds.bitz.model.entity.account.user.UserProfile;
import com.osds.bitz.model.entity.game.Game;
import com.osds.bitz.model.entity.game.GameParticipant;
import com.osds.bitz.model.entity.game.GameRecord;
import com.osds.bitz.model.entity.gym.Gym;
import com.osds.bitz.model.entity.gym.GymReview;
import com.osds.bitz.model.entity.log.GameRecordLog;
import com.osds.bitz.model.enumclass.UserState;
import com.osds.bitz.model.network.request.RecordRequest;
import com.osds.bitz.model.network.request.ReviewRequest;
import com.osds.bitz.model.network.request.gym.GameRequest;
import com.osds.bitz.model.network.response.game.GameDetailResponse;
import com.osds.bitz.model.network.response.game.GameListResponse;
import com.osds.bitz.model.network.response.game.GameReserveResponse;
import com.osds.bitz.model.network.response.game.GameResultResponse;
import com.osds.bitz.repository.account.business.BusinessProfileRepository;
import com.osds.bitz.repository.account.user.MannerRepository;
import com.osds.bitz.repository.account.user.SkillRepository;
import com.osds.bitz.repository.account.user.UserAuthRepository;
import com.osds.bitz.repository.account.user.UserProfileRepository;
import com.osds.bitz.repository.game.GameParticipantRepository;
import com.osds.bitz.repository.game.GameRecordRepository;
import com.osds.bitz.repository.game.GameRepository;
import com.osds.bitz.repository.gym.GymRepository;
import com.osds.bitz.repository.gym.GymReviewRepository;
import com.osds.bitz.repository.log.GameRecordLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

@Service
@Slf4j
public class GameService {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameRecordRepository gameRecordRepository;

    @Autowired
    private GameParticipantRepository gameParticipantRepository;

    @Autowired
    private GymRepository gymRepository;

    @Autowired
    private GymReviewRepository gymReviewRepository;

    @Autowired
    private UserAuthRepository userAuthRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private MannerRepository mannerRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private GameRecordLogRepository gameRecordLogRepository;

    class UserSkill implements Comparable<UserSkill> {
        String userId;
        double skillScore;

        public UserSkill(String userId, double skillScore) {
            this.userId = userId;
            this.skillScore = skillScore;
        }

        @Override
        public int compareTo(UserSkill o) {
            return (int) (o.skillScore - this.skillScore);
        }
    }

    /**
     * ?????? ??????
     */
    public Game createGame(GameRequest gameRequest) {

        // ????????? ???????????? Gym????????? ????????????
        Gym gym = this.gymRepository.getGymByName(gameRequest.getGymName());

        // game????????? ?????? ????????????
        Game game = Game.builder()
                .gym(gym)
                .date(gameRequest.getDate())
                .startTime(gameRequest.getStartTime())
                .endTime(gameRequest.getEndTime())
                .minPeople(gameRequest.getMinPeople())
                .maxPeople(gameRequest.getMaxPeople())
                .participationFee(gameRequest.getParticipationFee())
                .build();
        return this.gameRepository.save(game); // ?????????
    }

    /**
     * ?????? ??????
     */
    public GameDetailResponse getGameDetail(long gameId) {
        Game game = this.gameRepository.getGameById(gameId);
        ArrayList<GameParticipant> gameParticipantList = gameParticipantRepository.getGameParticipantsByGameId(gameId);
        BusinessProfile businessProfile = businessProfileRepository.getBusinessProfileByBusinessAuth(game.getGym().getBusinessAuth());
        businessProfile.setBusinessAuth(null);

        for (int i = 0; i < gameParticipantList.size(); i++) // ?????? ?????? ??????
            gameParticipantList.get(i).getUserAuth().setPassword(null);
        return new GameDetailResponse(gameParticipantList, game, businessProfile);
    }

    /**
     * ?????? ??????
     */
    public Game updateGame(GameRequest gameRequest) {
        Game updateGame = gameRepository.getGameById(gameRequest.getGameId());
        Gym gym = updateGame.getGym();

        updateGame.builder()
                .id(gameRequest.getGameId())
                .gym(gym)
                .date(gameRequest.getDate())
                .startTime(gameRequest.getStartTime())
                .endTime(gameRequest.getEndTime())
                .minPeople(gameRequest.getMinPeople())
                .maxPeople(gameRequest.getMaxPeople())
                .participationFee(gameRequest.getParticipationFee())
                .build();

        return this.gameRepository.save(updateGame);
    }

    /**
     * ?????? ??????
     */
    public void deleteGame(long gameId) {
        gameParticipantRepository.deleteAllByGameId(gameId);
        gameRepository.deleteAllById(gameId);
    }

    /**
     * ?????? ??????
     */
    public ArrayList<GameListResponse> getGameList(Date date, String sido) {
        ArrayList<Game> gameList = this.gameRepository.getGamesByDate(date); // ?????? ????????? ??????

        ArrayList<GameListResponse> result = new ArrayList<GameListResponse>();

        for (int i = 0; i < gameList.size(); i++) { // ?????? ????????? ?????? ??? ????????? ??????
            Game game = gameList.get(i);
            Gym gym = game.getGym();
            result.add(new GameListResponse(game, gym));
        }
        return result;
    }

    /**
     * ????????? ?????? ????????? ??????
     */
    public ArrayList<GameReserveResponse> getMyGameList(String userEmail) {
        UserAuth userAuth = userAuthRepository.getUserAuthByEmail(userEmail);
        ArrayList<GameParticipant> gameParticipants = gameParticipantRepository.getGameParticipantsByUserAuth(userAuth);
        ArrayList<GameReserveResponse> result = new ArrayList<>();

        for (int i = 0; i < gameParticipants.size(); i++) {
            Game game = gameRepository.getGameById(gameParticipants.get(i).getGameId());
            GameParticipant gameParticipant = gameParticipantRepository.getGameParticipantByUserAuthAndGameId(userAuth, game.getId());

            if (game.getDate().toString().compareTo(new Date(System.currentTimeMillis()).toString()) >= 0) {
                game.getGym().getBusinessAuth().setPassword(null);
                result.add(new GameReserveResponse(game, gameParticipant));
            }
        }

        return result;
    }

    /**
     * ?????? ??????
     */
    public GameParticipant reserveGame(String userEmail, Long gameId) {
        UserAuth userAuth = userAuthRepository.getUserAuthByEmail(userEmail);
        if (gameParticipantRepository.getGameParticipantByUserAuthAndGameId(userAuth, gameId) != null) {
            GameParticipant gameParticipant = gameParticipantRepository.getGameParticipantByUserAuthAndGameId(userAuth, gameId);
            if(gameParticipant.getState().equals(UserState.ON_DEPOSIT))
                return gameParticipant;
            else
                return null;
        }

        GameParticipant newGameParticipant =
                new GameParticipant().builder()
                        .userAuth(userAuth)
                        .gameId(gameId)
                        .team(0)
                        .state(UserState.ON_DEPOSIT)
                        .build();
        return gameParticipantRepository.save(newGameParticipant);
    }

    /**
     * ?????? ?????? ?????? (ON_DEPOSIT > WAITING)
     */
    public GameParticipant payGame(String userEmail, Long gameId) {
        UserAuth userAuth = userAuthRepository.getUserAuthByEmail(userEmail);
        GameParticipant updateGameParticipant = gameParticipantRepository.getGameParticipantByUserAuthAndGameId(userAuth, gameId);
        updateGameParticipant = updateGameParticipant.builder()
                .id(updateGameParticipant.getId())
                .gameId(gameId)
                .userAuth(userAuth)
                .state(UserState.WAITING)
                .team(updateGameParticipant.getTeam())
                .build();
        return gameParticipantRepository.save(updateGameParticipant);
    }

    /**
     * ????????? ?????? (WAITING > COMPLETE)
     **/
    public Game confirmGame(String userEmail, Long gameId) {
        UserAuth userAuth = userAuthRepository.getUserAuthByEmail(userEmail);

        GameParticipant updateGameParticipant = gameParticipantRepository.getGameParticipantByUserAuthAndGameId(userAuth, gameId);
        updateGameParticipant = updateGameParticipant.builder()
                .id(updateGameParticipant.getId())
                .gameId(gameId)
                .userAuth(userAuth)
                .state(UserState.COMPLETE)
                .team(updateGameParticipant.getTeam())
                .build();
        gameParticipantRepository.save(updateGameParticipant);

        Game game = gameRepository.getGameById(gameId);
        game.setParticipant(game.getParticipant() + 1);
        return gameRepository.save(game);
    }

    /**
     * ????????? ??????
     */
    public void deleteGameParticipant(String userEmail, Long gameId) {
        UserAuth userAuth = userAuthRepository.getUserAuthByEmail(userEmail);

        gameParticipantRepository.deleteGameParticipantByUserAuthAndGameId(userAuth, gameId);
    }

    /**
     * ??? ??????
     */
    public Game createTeaming(Long gameId) {
        ArrayList<GameParticipant> participants = this.gameParticipantRepository.getGameParticipantsByGameId(gameId);

        // 1. ????????? ?????? ?????? ?????? ?????? ????????? ??????
        int[] numOfTeam = getNumOfTeam(participants.size());

        // 2. ??? ??????????????? ?????? ??????(+ ???)??? ??????
        // UserSKill(String userId, double skill)
        ArrayList<UserSkill> userSkills = new ArrayList<>();
        for (GameParticipant participant : participants) {
            UserAuth userAuth = participant.getUserAuth();
            UserProfile userProfile = userProfileRepository.getUserProfileByUserAuth(userAuth);
            Skill skill = this.skillRepository.getSkillByUserAuth(userAuth);

            // ?????? ?????? ??????
            double total = getSkillScore(skill) + userProfile.getHeight();

            // ????????? ????????? + ????????????
            userSkills.add(new UserSkill(userAuth.getId(), total));
        }

        // 3. ??????????????? ?????? ???????????? ??????
        Collections.sort(userSkills);

        // 4. ??? ??????
        ArrayList<UserSkill[]> team = setTeamBySkillScore(numOfTeam, userSkills);

        // 5. DB??? ??????
        for (int i = 0; i < team.size(); i++) {
            for (int j = 0; j < team.get(i).length; j++) {
                UserSkill userSkill = team.get(i)[j];
                UserAuth userAuth = this.userAuthRepository.getUserAuthById(userSkill.userId);

                GameParticipant gameParticipant = this.gameParticipantRepository.getGameParticipantByUserAuthAndGameId(userAuth, gameId);
                gameParticipant.setId(gameParticipant.getId());
                gameParticipant.setTeam(i + 1);
                this.gameParticipantRepository.save(gameParticipant);
            }
        }

        // 6. ??? ?????? ????????????
        Game game = gameRepository.getGameById(gameId);
        game.setTeamCnt(team.size());
        return gameRepository.save(game);

    }

    /**
     * createTeaming() - ????????? ?????? ?????? ?????? ????????? ??????
     */
    public int[] getNumOfTeam(int numOfParticipant) {
        int[][] teams = {
                {6, 6, -1},
                {7, 6, -1},
                {7, 7, -1},
                {5, 5, 5},
                {8, 8, -1},
                {5, 6, 6},
                {6, 6, 6}
        };

        return teams[numOfParticipant - 12];
    }

    /**
     * createTeaming() - ?????? ?????? ??????
     */
    public double getSkillScore(Skill skill) {
        return (skill.getWinCnt() * 1.2) - (skill.getLoseCnt() * 1.0) + (skill.getTieCnt() * 0.2) + (skill.getMvpCnt() * 0.2);
    }

    /**
     * createTeaming() - ?????? ????????????
     */
    public ArrayList<UserSkill[]> setTeamBySkillScore(int[] numOfTeam, ArrayList<UserSkill> userSkills) {

        // ??????????????? ?????? team ?????? ????????? ????????????
        int teamCnt = numOfTeam[2] == -1 ? 2 : 3;

        ArrayList<UserSkill[]> team = new ArrayList<>();
        for (int i = 0; i < teamCnt; i++) {
            team.add(new UserSkill[numOfTeam[i]]);
        }

        // skillScore??? ?????? ??? ????????????
        /*
            teamIdx: team??? ???????????? index
            peopleIdx: ?????? ????????? ???????????? ???????????? index
            direction: ????????? ???????????? ??????
        */
        int teamIdx = 0, peopleIdx = 0;
        int direction = 1;
        for (int i = 0; i < userSkills.size(); i++) {
            UserSkill userSkill = userSkills.get(i);
            team.get(teamIdx)[peopleIdx] = userSkill;
            teamIdx = teamIdx + direction;

            if (teamIdx == -1 || teamIdx == teamCnt) {
                peopleIdx++;
                direction = direction * -1;
                teamIdx = teamIdx + direction;
            }
        }
        return team;
    }

    /**
     * ?????? ?????? ??????
     */
    public GameRecord createRecord(RecordRequest recordRequest) {

        UserAuth userAuth = this.userAuthRepository.getUserAuthByEmail(recordRequest.getUserEmail());

        GameRecord gameRecord = GameRecord.builder()
                .team(recordRequest.getTeam())
                .quarter(recordRequest.getQuarter())
                .score(recordRequest.getScore())
                .recordTime(LocalDateTime.now())
                .userAuth(userAuth)
                .gameId(recordRequest.getGameId())
                .build();
        GameRecord response = gameRecordRepository.save(gameRecord);

        // ?????? ???????????? ?????? ????????? 0.2??? ????????? (??? ????????? 2?????? ????????? ??????????????? 0.1??? ???)
        Manner manner = Manner.builder()
                .userAuth(userAuth)
                .score(1)
                .date(LocalDateTime.now())
                .build();
        mannerRepository.save(manner);

        return response;
    }

    /**
     * ?????? ?????? ?????? ??????
     */
    public RecordTable[] readRecord(Long gameId) {

        Game game = gameRepository.getGameById(gameId);
        ArrayList<GameRecord> gameRecordList = gameRecordRepository.getGameRecordsByGameId(gameId);

        // gameRecord ??????
        Comparator<GameRecord> comparator = new Comparator<GameRecord>() {
            @Override
            public int compare(GameRecord a, GameRecord b) {
                if (a.getQuarter() == b.getQuarter())
                    return a.getTeam() - b.getTeam();
                return a.getQuarter() - b.getQuarter();
            }
        };
        Collections.sort(gameRecordList, comparator);

        int teamCnt = game.getTeamCnt();    // 2 or 3
        int tableCnt = teamCnt; // 1 or 3
        RecordTable[] recordTableList = new RecordTable[tableCnt];

        // recordTableList ??????
        if (teamCnt == 2) {
            for (int i = 1; i <= tableCnt; i++) {
                recordTableList[i - 1] = new RecordTable();
                recordTableList[i - 1].setTeamA(i == teamCnt ? i - 1 : i);
                recordTableList[i - 1].setTeamB(i == teamCnt ? i : i + 1);
                recordTableList[i - 1].setTeamAScoreList(new ArrayList<>());
                recordTableList[i - 1].setTeamBScoreList(new ArrayList<>());
                recordTableList[i - 1].setRecorderList(new ArrayList<>());
            }
        } else {
            for (int i = 1; i <= tableCnt; i++) {
                recordTableList[i - 1] = new RecordTable();
                recordTableList[i - 1].setTeamA(i == teamCnt ? i / teamCnt : i);
                recordTableList[i - 1].setTeamB(i == teamCnt ? i : i + 1);
                recordTableList[i - 1].setTeamAScoreList(new ArrayList<>());
                recordTableList[i - 1].setTeamBScoreList(new ArrayList<>());
                recordTableList[i - 1].setRecorderList(new ArrayList<>());
            }
        }


        if (teamCnt == 2) {
            int peopleIdx = 0;
            int colIdx = 0;
            int tableIdx = 0;
            for (int i = 0; i < gameRecordList.size(); i++) {
                GameRecord gameRecord = gameRecordList.get(i);
                if (peopleIdx % 2 == 0) {
                    recordTableList[tableIdx].getTeamAScoreList().add(gameRecord.getScore());
                    UserAuth recorder = gameRecord.getUserAuth();
                    UserProfile userProfile = userProfileRepository.getUserProfileByUserAuth(recorder);
                    recordTableList[tableIdx].getRecorderList().add(userProfile.getName());
                } else {
                    recordTableList[tableIdx].getTeamBScoreList().add(gameRecord.getScore());
                }
                if (++peopleIdx == 2) { // 2??? ????????? ?????? ??????
                    peopleIdx = 0;
                    if (++colIdx == 4) { // 4????????? ???????????? ?????? ????????????
                        colIdx = 0;
                        tableIdx++;
                    }
                }
            }

        } else {
            int peopleIdx = 0;
            int tableIdx = 0;
            for (int i = 0; i < gameRecordList.size(); i++) {
                GameRecord gameRecord = gameRecordList.get(i);
                if (peopleIdx % 2 == 0) {
                    recordTableList[tableIdx].getTeamAScoreList().add(gameRecord.getScore());
                    UserAuth recorder = gameRecord.getUserAuth();
                    UserProfile userProfile = userProfileRepository.getUserProfileByUserAuth(recorder);
                    recordTableList[tableIdx].getRecorderList().add(userProfile.getName());
                } else {
                    recordTableList[tableIdx].getTeamBScoreList().add(gameRecord.getScore());
                }

                if (++peopleIdx == 2) {
                    peopleIdx = 0;
                    if (++tableIdx == tableCnt) {
                        tableIdx = 0;
                    }
                }
            }
        }

        return recordTableList;
    }

    /**
     * ?????? ?????? ??????
     */
    public GameResultResponse completeGame(Long gameId) {
        gameRecordLogRepository.save(GameRecordLog.builder().gameId(gameId).build());

        GameResultResponse gameResultResponse = new GameResultResponse();
        Game game = gameRepository.getGameById(gameId);
        int teamCnt = game.getTeamCnt(); // ??? ??????
        log.info("{}", teamCnt);
        ArrayList<int[]>[] team = (ArrayList<int[]>[]) new ArrayList[teamCnt + 1];

        for (int i = 1; i <= teamCnt; i++)
            team[i] = new ArrayList<>();

        RecordTable[] recordTableList = readRecord(gameId);
        log.info("{}", recordTableList);
        int[][] gameResult = new int[teamCnt][3];
        int[][] gameScoreTable = new int[teamCnt][4];


        for (int i = 0; i < recordTableList.length; i++) {
            RecordTable recordTable = recordTableList[i];
            int teamA = recordTableList[i].getTeamA();
            int teamB = recordTableList[i].getTeamB();

            int scoreA = recordTable.getTeamAScoreList().get(recordTable.getTeamAScoreList().size() - 1);
            int scoreB = recordTable.getTeamBScoreList().get(recordTable.getTeamBScoreList().size() - 1);

            gameScoreTable[i][0] = teamA;
            gameScoreTable[i][1] = teamB;
            gameScoreTable[i][2] = scoreA;
            gameScoreTable[i][3] = scoreB;

            ArrayList<GameParticipant> aTeam = gameParticipantRepository.getGameParticipantsByGameIdAndTeam(gameId, teamA);
            ArrayList<GameParticipant> bTeam = gameParticipantRepository.getGameParticipantsByGameIdAndTeam(gameId, teamB);

            if (scoreA > scoreB) {
                gameResult[teamA - 1][0]++;
                gameResult[teamB - 1][1]++;

                updateSkillScore(aTeam, bTeam, false);
            } else if (scoreA < scoreB) {
                gameResult[teamA - 1][1]++;
                gameResult[teamB - 1][0]++;

                updateSkillScore(bTeam, aTeam, false);
            } else { // ???????????? ??????
                gameResult[teamA - 1][2]++;
                gameResult[teamB - 1][2]++;

                updateSkillScore(aTeam, bTeam, true);
            }
        }

        gameResultResponse.setGameResult(gameResult);
        gameResultResponse.setGameScoreTable(gameScoreTable);
        log.info("{}", gameResultResponse);
        return gameResultResponse;
    }

    /**
     * ?????? ?????? ?????? ??????
     */
    public boolean checkGameResult(Long gameId) {
        if(gameRecordLogRepository.getGameRecordLogByGameId(gameId) != null)
            return true;

        return false;
    }
    /**
     * ?????? ?????? ????????????
     */
    public void updateSkillScore(ArrayList<GameParticipant> winners, ArrayList<GameParticipant> losers, boolean isTie) {

        if (!isTie) {
            for (GameParticipant winner : winners) {
                Skill skill = skillRepository.getSkillByUserAuth(winner.getUserAuth());
                skill.setWinCnt(skill.getWinCnt() + 1);
                skillRepository.save(skill);
            }
            for (GameParticipant loser : losers) {
                Skill skill = skillRepository.getSkillByUserAuth(loser.getUserAuth());
                skill.setLoseCnt(skill.getLoseCnt() + 1);
                skillRepository.save(skill);
            }
        } else {
            for (GameParticipant tier : winners) {
                Skill skill = skillRepository.getSkillByUserAuth(tier.getUserAuth());
                skill.setTieCnt(skill.getTieCnt() + 1);
                skillRepository.save(skill);
            }
            for (GameParticipant tier : losers) {
                Skill skill = skillRepository.getSkillByUserAuth(tier.getUserAuth());
                skill.setTieCnt(skill.getTieCnt() + 1);
                skillRepository.save(skill);
            }
        }
    }

    /**
     * ?????? ?????? ??????
     */
    public void createReview(ReviewRequest reviewRequest) {

        // 1. ????????? ?????? ??????
        GymReview gymReview = GymReview.builder()
                .gymId(reviewRequest.getGymId())        // ????????? ID
                .userId(userAuthRepository.getUserAuthByEmail(reviewRequest.getEmail()).getId())       // ?????? ID
                .rate(reviewRequest.getRate())          // ??????
                .date(LocalDateTime.now())              // ????????????
                .build();

        this.gymReviewRepository.save(gymReview);

        // 2. ????????? ?????? ??????
        // 2-1. MVP
        UserAuth mvpUser = this.userAuthRepository.getUserAuthByEmail(reviewRequest.getMvp());
        Skill skill = this.skillRepository.getSkillByUserAuth(mvpUser);

        int mvpCnt = this.skillRepository.getSkillById(skill.getId()).getMvpCnt();

        // Update MVP Count
        skill.setMvpCnt(mvpCnt + 1);
        this.skillRepository.save(skill);

        // 2-2. Manner
        // 2-2-1. Good
        for (String userEmail : reviewRequest.getGoodPeople()) {
            UserAuth user = this.userAuthRepository.getUserAuthByEmail(userEmail);
            Manner manner = Manner.builder()
                    .userAuth(user)
                    .score(10)
                    .date(LocalDateTime.now())
                    .build();
            this.mannerRepository.save(manner);
        }

        // 2-2-2. Bad
        for (String userEmail : reviewRequest.getBadPeople()) {
            UserAuth user = this.userAuthRepository.getUserAuthByEmail(userEmail);

            Manner manner = Manner.builder()
                    .userAuth(user)
                    .score(-10)
                    .date(LocalDateTime.now())
                    .build();
            this.mannerRepository.save(manner);
        }

    }

    /**
     * ?????? ?????? ?????? ?????? ??????
     */
    public boolean readReview(String userEmail, Long gameId) {
        String userId = userAuthRepository.getUserAuthByEmail(userEmail).getId();
        long gymId = gameRepository.getGameById(gameId).getGym().getId();

        if (gymReviewRepository.getGymReviewsByUserIdAndGymId(userId, gymId).size() > 0)
            return true;

        return false;
    }

}
