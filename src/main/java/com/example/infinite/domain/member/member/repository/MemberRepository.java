package com.example.infinite.domain.member.member.repository;

import com.example.infinite.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    boolean existsByEmail(String email);

   Optional<Member> findByEmail(String email);
}
