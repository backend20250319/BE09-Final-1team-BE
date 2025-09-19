package com.newnormallist.tooltipservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "term_definition")
@Getter
@NoArgsConstructor
public class TermDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "term_definition_id")
    private Long id;

    // 용어 정의는 텍스트가 길 수 있으므로 TEXT 타입으로 매핑합니다.
    @Column(name = "definition", nullable = false)
    private String definition;

    @Column(name = "pos", length = 50)
    private String pos; // 품사 (예: 명사, 동사)

    @Column(name = "is_primary")
    private Boolean isPrimary; // 대표 뜻 여부

    @Column(name = "display_order")
    private Integer displayOrder; // 표시 순서

    // TermDefinition은 하나의 VocabularyTerm에 속합니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_term_id")
    private VocabularyTerm vocabularyTerm;
}
