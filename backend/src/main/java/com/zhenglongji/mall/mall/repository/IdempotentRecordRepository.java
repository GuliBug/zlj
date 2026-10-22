package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.IdempotentRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotentRecordRepository extends JpaRepository<IdempotentRecord, Long> {

    Optional<IdempotentRecord> findByIdemKey(String idemKey);
}
