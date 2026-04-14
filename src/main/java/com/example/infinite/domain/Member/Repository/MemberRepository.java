package com.example.infinite.domain.Member.Repository;

import com.example.infinite.domain.Member.Entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
}
