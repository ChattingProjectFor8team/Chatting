package com.example.infinite.global.auth;


import com.example.infinite.domain.user.entity.User;
import com.example.infinite.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB에서 사용자 정보를 조회하여 시큐리티 인증 객체로 변환하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("사용자 인증 정보 조회 시작: {}", email);

        // 1. DB에서 이메일로 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("인증 실패 - 존재하지 않는 사용자: {}", email);
                    return new UsernameNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다: " + email);
                });

        // 2. 조회된 엔티티를 신분증(UserDetailsImpl)에 담아 반환
        return new UserDetailsImpl(user);
    }
}
