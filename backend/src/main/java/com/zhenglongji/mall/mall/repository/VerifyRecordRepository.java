package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.VerifyRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerifyRecordRepository extends JpaRepository<VerifyRecord, Long> {

    List<VerifyRecord> findByStoreIdOrderByIdDesc(Long storeId);
}
