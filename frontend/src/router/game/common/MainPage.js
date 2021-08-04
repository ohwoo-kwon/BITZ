import React, { useContext } from "react"; // useContext import
import "./MainPage.css";
import MainPlayerCommon from "components/game/player/Main__Player_Common";
import MainBusiness from "components/game/player/Main__Business";
import { store } from 'store/store.js';

function MainPage() {
  const globalState = useContext(store);
  const { value } = globalState;

  return(
    <div className="main">
      {
        value.isLogin && value.userKind === 'business' ? (
          <MainBusiness />
        ) : (
          <MainPlayerCommon />
        )
      }
    </div>
  ) 
}

export default MainPage;
