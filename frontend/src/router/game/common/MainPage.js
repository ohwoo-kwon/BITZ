import React, { useContext, useEffect, useState } from "react"; // useContext import
import "./MainPage.css";
import DateCarousel from "components/game/player/Main__DateCarousel.js";
import PreferredArea from "components/game/player/Main__PreferredArea.js";
import GameList from "components/game/player/Main__GameList.js";
import { store } from 'store/store.js'; // store import
import gameListDummy from "store/gameListDummy.js"
import UserApi from "api/UserApi";
import FirstLogin from "components/user/player/FirstLogin";

function MainPage() {
  const [isFirstLogin, setIsFirstLogin] = useState(false)

  // (1) store에서 가져온 store Context를 globalState 변수에 집어넣음
  const globalState = useContext(store);
  // (2) globalState에서 전역 변수 value를 업데이트하는 dispatch만 가져오기
  const { dispatch } = globalState;

  useEffect(()=>{
    // (3) 위에서 가져온 dipatch를 이용해서 reducer 실행 => 전역 변수 value 업데이트
    /* useEffect를 사용하는 이유
    dispatch로 value를 업데이트하면 관련 컴포넌트 Rerendering
    => MainPage 함수가 다시 실행되면서 useEffect 바깥에 있는 코드가 전부 재실행됨
    이 때 dispatch가 바깥에 있으면. dispatch 재귀적 무한 실행 & 무한 렌더링으로 에러 발생 */
    dispatch({ type: 'TEST' })
    dispatch({ type: 'GET_GAME_LIST', value: gameListDummy })
    const data = {
      email : globalState.value.isLogin,
      password : null,
    }
    UserApi.firstLogin(data,
      () => {
        setIsFirstLogin(true)
      },
      (err) => {
        console.log(err)
      }
    )
  },[])
  return(
    <div className="main">
      {isFirstLogin ? <FirstLogin /> :
        <store.Provider value={globalState}>
          <div className="main__dates"><DateCarousel /></div>
          <div className="main__areas"><PreferredArea /></div>
          <div className="main__games"><GameList /></div>
        </store.Provider>
      }
    </div>
  );
}

export default MainPage;
