package com.osds.bitz.service.account.business;

import com.osds.bitz.model.entity.account.business.BusinessAuth;
import com.osds.bitz.model.entity.account.business.BusinessProfile;
import com.osds.bitz.model.entity.log.LoginLog;
import com.osds.bitz.model.network.request.BusinessAuthRequest;
import com.osds.bitz.model.network.request.ReadAuthRequest;
import com.osds.bitz.model.network.request.UpdatePasswordRequest;
import com.osds.bitz.repository.account.business.BusinessAuthRepository;
import com.osds.bitz.repository.account.business.BusinessProfileRepository;
import com.osds.bitz.repository.log.LoginLogRepository;
import com.osds.bitz.service.account.BaseAuthService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Service
@Slf4j
public class BusinessAuthService extends BaseAuthService {

    @Autowired
    private BusinessAuthRepository businessAuthRepository;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private LoginLogRepository loginLogRepository;

    @Autowired
    private JavaMailSender mailSender;

    // 회원가입
    public BusinessAuth createBusiness(BusinessAuthRequest businessAuthRequest) throws IOException {

        // 이메일 중복체크
        if(this.businessAuthRepository.getBusinessAuthByEmail(businessAuthRequest.getEmail()) != null)
            return null;

        // businessId로 설정할 랜덤 값 생성
        String businessAuthId = generateRandomNumber(false);
        // businessId 중복 체크
        BusinessAuth duplicataionBusinessAuth = this.businessAuthRepository.getById(businessAuthId);
        while(businessAuthId.equals(duplicataionBusinessAuth.getId())){
            businessAuthId = generateRandomNumber(false);
        }

            // 파일 로컬에 저장
            File targetFile = new File("src/main/resources/static/imgs/" + businessAuthRequest.getBusinessRegistration().getOriginalFilename());
            try {
                InputStream fileStream = businessAuthRequest.getBusinessRegistration().getInputStream();
                FileUtils.copyInputStreamToFile(fileStream, targetFile);
            } catch (IOException e) {
            log.info("{}",e.getMessage());
        }


        BusinessAuth businessAuth = BusinessAuth.builder()
                .id(businessAuthId)
                .email(businessAuthRequest.getEmail())
                .password(businessAuthRequest.getPassword())
                .birth(businessAuthRequest.getBirth())
                .build();
        log.info("{}", businessAuth);

        BusinessProfile businessProfile = BusinessProfile.builder()
                .name(businessAuthRequest.getName())
                .phone(businessAuthRequest.getPhone())
                .bank(businessAuthRequest.getBank())
                .account(businessAuthRequest.getAccount())
                .businessRegistration(businessAuthRequest.getBusinessRegistration().getBytes())
                .businessAuth(businessAuth)
                .build();
        log.info("{}", businessProfile);

        BusinessAuth newBusinessAuth = this.businessAuthRepository.save(businessAuth);
        this.businessProfileRepository.save(businessProfile);

        return newBusinessAuth;
    }

    // 로그인
    public BusinessAuth readBusiness(ReadAuthRequest readAuthRequest) {
        // 이메일과 비밀번호로 객체 찾아오기
        return this.businessAuthRepository.findBusinessAuthByEmailAndPassword(readAuthRequest.getEmail(), readAuthRequest.getPassword());
    }

    // 첫 로그인인지 확인하기
    public BusinessAuth readFirstBusinessAuthRequest(ReadAuthRequest readAuthRequest){

        // 이메일로 로그인 로그 객체 찾아오기
        LoginLog loginLog = this.loginLogRepository.getLoginLogByUserEmailAndIsGeneral(readAuthRequest.getEmail(), false);

        if(loginLog == null){               // 최초 로그인시
            loginLog = LoginLog.builder()
                    .userEmail(readAuthRequest.getEmail())
                    .isGeneral(false)
                    .build();
            this.loginLogRepository.save(loginLog);
            return this.businessAuthRepository.getBusinessAuthByEmail(loginLog.getUserEmail());
        }
        return null;
    }

    // 비밀번호 변경하기
    public BusinessAuth updatePassword(UpdatePasswordRequest updatePasswordRequest) {
        // 이메일로 해당 객체 찾아오기
        BusinessAuth newBusinessAuth = new BusinessAuth();
        newBusinessAuth = this.businessAuthRepository.getBusinessAuthByEmail(updatePasswordRequest.getEmail());

        // 변경할 비밀번호 설정하기
        newBusinessAuth.setPassword(updatePasswordRequest.getNewPassword());
        return this.businessAuthRepository.save(newBusinessAuth);
    }

    // 비밀번호 찾기
    public BusinessAuth readPassword(BusinessAuthRequest businessAuthRequest) {
        // 이메일로 해당 객체 찾아오기
        BusinessAuth newBusinessAuth = this.businessAuthRepository.getBusinessAuthByEmail(businessAuthRequest.getEmail());

        // 임시 비밀번호 생성 및 메일 전송
        String code = "";
        try {
            code = generateRandomNumber();
            String msg = "<p><b> " + newBusinessAuth.getEmail() + " </b>님의 임시 비밀번호입니다.</p> <p style=color:red;> <h1>" + code + "</h1> </p>\n \n 로 새롭게 로그인 후 비밀번호를 변경해주세요!";
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("OSDS"); // 보내는 사람
            helper.setTo(newBusinessAuth.getEmail());
            helper.setText(msg);
            message.setContent(msg, "text/html; charset=UTF-8");
            helper.setSubject("[OSDS] 비밀번호 찾기 요청에 대한 임시 비밀번호를 보내드립니다.");
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 임시 비밀번호로 비밀번호 변경하기
        newBusinessAuth.setPassword(code);
        return this.businessAuthRepository.save(newBusinessAuth);
    }

}
