package com.poweramp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReverbPresetRepository extends JpaRepository<ReverbPresetEntity, Long> {
}
