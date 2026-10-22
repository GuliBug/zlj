package com.zhenglongji.mall.mall.repository;

import com.zhenglongji.mall.mall.domain.BannedWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BannedWordRepository extends JpaRepository<BannedWord, Long> {}
