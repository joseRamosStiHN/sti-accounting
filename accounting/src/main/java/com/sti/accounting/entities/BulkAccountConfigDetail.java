package com.sti.accounting.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sti.accounting.utils.BulkDetailType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "bulk_account_detail")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkAccountConfigDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "title", columnDefinition = "VARCHAR(255)")
    private String title;

    @Column(name = "col_index", columnDefinition = "INTEGER")
    private Integer colIndex;

    @Column(name = "detail_type")
    @Enumerated(EnumType.STRING)
    private BulkDetailType detailType;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name ="operation")
    private String operation; // DEBE/HABER


    @Column(name = "field")
    private String field;

    @ManyToOne
    @JoinColumn(name = "bulk_account_config_id", referencedColumnName = "id")
    @JsonIgnore
    private BulkAccountConfig bulkAccountConfig;

}
