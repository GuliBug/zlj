package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.Address;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByMemberIdOrderByIdDesc(Long memberId);
}
