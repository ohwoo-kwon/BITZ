package com.osds.bitz.controller;

import com.osds.bitz.model.entity.account.business.BusinessAuth;
import com.osds.bitz.model.entity.game.Game;
import com.osds.bitz.model.network.request.account.*;
import com.osds.bitz.model.network.response.account.BusinessAuthResponse;
import com.osds.bitz.model.network.response.account.BusinessResponse;
import com.osds.bitz.service.BusinessService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;

@CrossOrigin("*")
@AllArgsConstructor
@RestController
@RequestMapping("/account/business")
@Api("Account 컨트롤러 API V1")
@Slf4j
public class BusinessController {

    private BusinessService businessService;

    @PostMapping("/businessauth")
    @ApiOperation(value = "회원가입", notes = "회원의 정보를 DB에 저장합니다.")
    public ResponseEntity<BusinessAuthResponse> createBusiness(@RequestBody @ApiParam(value = "회원 정보") BusinessAuthRequest businessAuthRequest) throws IOException {
        businessService.createBusiness(businessAuthRequest);
        return new ResponseEntity(HttpStatus.CREATED);
    }

    @PostMapping("/businessauth/email")
    @ApiOperation(value = "이메일 중복체크", notes = "이메일 중복체크를 시행합니다.")
    public ResponseEntity<BusinessAuthResponse> isDuplicatedEmail(@RequestBody @ApiParam(value = "이메일") BusinessProfileRequest businessProfileRequest) {
        if (businessService.isDuplicatedEmail(businessProfileRequest.getEmail()))
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        return new ResponseEntity(HttpStatus.OK);
    }

    @PostMapping("/businessauth/login")
    @ApiOperation(value = "로그인", notes = "회원의 정보를 통해 로그인 처리를 합니다.")
    public ResponseEntity<BusinessAuthResponse> readBusiness(@RequestBody @ApiParam(value = "아이디, 비밀번호") ReadAuthRequest readAuthRequest) {
        BusinessAuthResponse response = new BusinessAuthResponse(businessService.readBusiness(readAuthRequest));
        if (response == null)
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        return new ResponseEntity(HttpStatus.OK);
    }

    @PostMapping("/loginlog")
    @ApiOperation(value = "최초로그인 확인", notes = "최초로그인인지 DB에서 확인 후 결과를 반환합니다.")
    public ResponseEntity<BusinessAuthResponse> readLoginLog(@RequestBody @ApiParam(value = "이메일") ReadAuthRequest readAuthRequest) {
        if (!businessService.readLoginLog(readAuthRequest.getEmail()))  // 최초로그인이 아닌 경우
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        return new ResponseEntity(HttpStatus.OK);                       // 최초로그인인 경우
    }

    @GetMapping("/businessprofile")
    @ApiOperation(value = "마이페이지 정보 조회", notes = "회원의 마이페이지 정보를 조회합니다.")
    public ResponseEntity<BusinessResponse> readProfile(@RequestParam(value = "email") String email) {
        BusinessResponse businessResponse = businessService.readProfile(email);
        if (businessResponse == null)
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        return ResponseEntity.status(HttpStatus.OK).body(businessResponse);
    }

    @PutMapping("/businessprofile")
    @ApiOperation(value = "마이페이지 정보 수정", notes = "회원의 마이페이지 정보를 수정합니다.")
    public ResponseEntity<BusinessResponse> updateProfile(@RequestBody @ApiParam(value = "회원 정보") BusinessRequest businessRequest) {
        businessService.updateProfile(businessRequest);
        return new ResponseEntity(HttpStatus.OK);
    }

    @GetMapping("/game/list")
    @ApiOperation(value = "메인 페이지 조회", notes = "자신이 올린 게임 리스트를 조회합니다.")
    public ResponseEntity<ArrayList<Game>> readGameList(@RequestParam(value = "email") String email, @RequestParam(value = "date") Date date) {
        ArrayList<Game> response = businessService.readGameList(email, date);
        if (response == null)
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/businessauth/password/change")
    @ApiOperation(value = "비밀번호 변경", notes = "회원의 비밀번호를 DB에서 수정합니다.")
    public ResponseEntity<BusinessAuthResponse> updatePassword(@RequestBody @ApiParam(value = "회원 정보") UpdatePasswordRequest updatePasswordRequest) {
        BusinessAuth businessAuth = businessService.updatePassword(updatePasswordRequest);
        if (businessAuth == null)
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        return new ResponseEntity(HttpStatus.OK);
    }

    @PutMapping("/businessauth/password/reset")
    @ApiOperation(value = "비밀번호 찾기", notes = "회원의 이메일에 비밀번호를 찾아서 임시 비밀번호를 이메일로 전송합니다.")
    public ResponseEntity<BusinessAuthResponse> resetPassword(@RequestBody @ApiParam(value = "이메일") BusinessProfileRequest businessProfileRequest) {
        BusinessAuth businessAuth = businessService.resetPassword(businessProfileRequest.getEmail());
        if (businessAuth == null)
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        return new ResponseEntity(HttpStatus.OK);
    }

    @DeleteMapping("/businessauth")
    @ApiOperation(value = "회원탈퇴", notes = "회원의 계정을 DB에서 삭제합니다.")
    public ResponseEntity deleteBusiness(@RequestBody @ApiParam(value = "회원 정보") ReadAuthRequest readAuthRequest) {
        businessService.deleteBusiness(readAuthRequest);
        return new ResponseEntity(HttpStatus.OK);
    }

}