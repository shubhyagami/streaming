package com.poweramp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EqPresetRepository extends JpaRepository<EqPresetEntity, Long> {
    List<EqPresetEntity> findByCategory(String category);
    List<EqPresetEntity> findByNameContainingIgnoreCase(String name);
}
