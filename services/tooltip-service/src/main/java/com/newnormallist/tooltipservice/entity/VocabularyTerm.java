package com.newnormallist.tooltipservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vocabulary_term")
@Getter
@NoArgsConstructor
public class VocabularyTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vocabulary_term_id")
    private Long id;

    @Column(name = "term", nullable = false, length = 100, unique = true)
    private String term;

    // VocabularyTerm 하나에 여러 TermDefinition이 연관
    @OneToMany(mappedBy = "vocabularyTerm", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TermDefinition> definitions = new ArrayList<>();


}
