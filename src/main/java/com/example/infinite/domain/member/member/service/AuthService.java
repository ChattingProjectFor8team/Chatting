package com.example.infinite.domain.member.member.service;



import com.example.infinite.domain.member.member.dto.request.LoginRequest;
import com.example.infinite.domain.member.member.dto.request.SignUpRequest;
import com.example.infinite.domain.member.member.dto.response.TokenResponse;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.repository.MemberRepository;
import com.example.infinite.global.auth.JwtTokenProvider;
import com.example.infinite.global.error.ErrorCode;
import com.example.infinite.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 💡 회원가입
    public void signUp(SignUpRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL); // 이미 있는 이메일 체크
        }

        // 전화번호도 유니크 정책으로 관리해 동일 번호 재가입을 막는다.
        if (memberRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new com.example.infinite.domain.member.member.error.MemberException(
                    com.example.infinite.domain.member.member.error.MemberErrorCode.MEMBER_DUPLICATE_PHONE_NUMBER
            );
        }

        if (memberRepository.existsByNickname(request.nickname())) {
            throw new com.example.infinite.domain.member.member.error.MemberException(
                    com.example.infinite.domain.member.member.error.MemberErrorCode.MEMBER_DUPLICATE_NICKNAME
            );
        }

        Member member = Member.createNewMember(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname(),
                request.phoneNumber()
        );
        memberRepository.save(member);
    }

    // 💡 로그인
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 토큰 생성 (Subject로 이메일 사용)
        String accessToken = jwtTokenProvider.createToken(member.getEmail(), member.getRole().name(), member.getId());
        return new TokenResponse(accessToken, "Bearer");
    }
}
