package com.pet.walkthroughserver.modules.template.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateRepository extends JpaRepository<TemplateEntity, UUID> {

    List<TemplateEntity> findByIsBuiltinTrueOrderByNameAsc();

    List<TemplateEntity> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    @Query("""
            SELECT t FROM TemplateEntity t
            WHERE t.isBuiltin = true
               OR t.userId = :userId
            ORDER BY t.isBuiltin DESC, t.updatedAt DESC
            """)
    List<TemplateEntity> findVisibleToUser(@Param("userId") UUID userId);

    @Query("""
            SELECT t FROM TemplateEntity t
            WHERE (t.isBuiltin = true OR t.userId = :userId)
              AND t.prType = :prType
            ORDER BY t.isBuiltin DESC, t.updatedAt DESC
            """)
    List<TemplateEntity> findVisibleToUserByPrType(@Param("userId") UUID userId,
                                                    @Param("prType") TemplatePrType prType);

    @Query("""
            SELECT t FROM TemplateEntity t
            WHERE t.isBuiltin = true
            ORDER BY t.duplicateCount DESC, t.name ASC
            """)
    List<TemplateEntity> findTopBuiltinsByDuplicateCount(Pageable pageable);
}
