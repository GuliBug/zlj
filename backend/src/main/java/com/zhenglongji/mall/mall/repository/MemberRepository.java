package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByPhone(String phone);
}
