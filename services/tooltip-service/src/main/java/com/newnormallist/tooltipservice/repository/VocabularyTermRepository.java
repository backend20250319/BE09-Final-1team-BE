package com.newnormallist.tooltipservice.repository;

import com.newnormallist.tooltipservice.entity.VocabularyTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface VocabularyTermRepository extends JpaRepository<VocabularyTerm,Long> {

    @Query("SELECT vt.term FROM VocabularyTerm vt")
    Set<String> findAllTerms();

    // 단어(term) 문자열로 VocabularyTerm 엔티티를 찾는 메소드 (정확 일치)
    Optional<VocabularyTerm> findByTerm(String term);
    
    // 단어가 포함된 용어를 찾는 메소드 (부분 일치)
    @Query("SELECT vt FROM VocabularyTerm vt WHERE vt.term LIKE CONCAT(:term, '%') OR vt.term = :term ORDER BY LENGTH(vt.term) ASC")
    Optional<VocabularyTerm> findByTermStartingWith(@Param("term") String term);

}