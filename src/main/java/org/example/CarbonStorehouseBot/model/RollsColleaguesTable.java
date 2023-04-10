package org.example.CarbonStorehouseBot.model;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "rolls_colleagues_table")
public class RollsColleaguesTable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roll_colleagues_id")
    private Long rollColleaguesId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roll_id")
    private Roll roll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colleague_id")
    private Colleague colleague;

    @Column(name = "number_roll", nullable = false)
    private int numberRoll;

    @Column(name = "metric_colleague", nullable = false)
    private Double metricColleague;

    @Column(name = "date_working_shift", nullable = false)
    private LocalDate dateWorkingShift;

    @Column(name = "number_fabric", nullable = false)
    private String numberFabric;
}
