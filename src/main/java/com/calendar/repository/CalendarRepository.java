package com.calendar.repository;

import com.calendar.model.CalendarDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CalendarRepository extends JpaRepository<CalendarDo, Long> {
    
    @Query("SELECT c.syncToken FROM CalendarDo c WHERE c.externalId = ?1")
    String findSyncTokenByExternalId(String externalId);
    
    Optional<CalendarDo> findByExternalId(String externalId);
} 