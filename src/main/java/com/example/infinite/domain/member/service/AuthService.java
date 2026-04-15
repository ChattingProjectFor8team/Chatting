package com.example.infinite.domain.member.service;



import com.example.infinite.domain.member.dto.request.LoginRequest;
import com.example.infinite.domain.member.dto.request.SignUpRequest;
import com.example.infinite.domain.member.dto.response.TokenResponse;
import com.example.infinite.domain.member.entity.Member;
import com.example.infinite.domain.member.enums.MemberRole;
import com.example.infinite.domain.member.repository.MemberRepository;
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

        Member member = Member.createNewMember(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.phoneNumber(),
                request.nickname(),
                MemberRole.USER
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
        String accessToken = jwtTokenProvider.createToken(member.getEmail(), member.getRole().name());
        return new TokenResponse(accessToken, "Bearer");
    }
}
