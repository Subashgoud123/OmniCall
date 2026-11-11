package com.omnicall.language;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LanguageRepository extends JpaRepository<LanguageDefinition, String> {

    List<LanguageDefinition> findAllByOrderBySortOrderAsc();

    List<LanguageDefinition> findByEnabledTrueOrderBySortOrderAsc();
}
