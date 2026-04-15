package com.example.infinite.global.auth;



import com.example.infinite.domain.member.entity.Member;
import com.example.infinite.domain.member.repository.MemberRepository;
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
public class MemberDetailsServiceImpl implements UserDetailsService {

    private final MemberRepository memberRepository; // 💡 UserRepository -> MemberRepository

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return memberRepository.findByEmail(email)
                .map(MemberDetailsImpl::new) // 💡 MemberDetailsImpl로 변환
                .orElseThrow(() -> new UsernameNotFoundException("이메일을 찾을 수 없습니다."));
    }
}
