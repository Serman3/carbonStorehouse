package org.example.CarbonStorehouseBot.model;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "roll", uniqueConstraints = {@UniqueConstraint(name = "numberRollAndFabric_id", columnNames = {"number_roll", "fabric_id"})})
public class Roll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "number_roll", nullable = false)
    private int numberRoll;

    @Column(name = "roll_metric", nullable = false)
    private int rollMetric;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "date_fulfilment", columnDefinition = "DATETIME", nullable = false)
    private LocalDateTime dateFulfilment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_roll", columnDefinition = "ENUM('READY','NOT_READY', 'AT_WORK', 'SOLD_OUT')", nullable = false)
    private StatusRoll statusRoll;

    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.EAGER)
    @JoinColumn(name = "fabric_id", referencedColumnName = "id", nullable = false)
    private Fabric fabric;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Roll roll = (Roll) o;
        return numberRoll == roll.numberRoll && rollMetric == roll.rollMetric && Objects.equals(id, roll.id) && Objects.equals(remark, roll.remark) && Objects.equals(dateFulfilment, roll.dateFulfilment) && Objects.equals(fabric, roll.fabric);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, numberRoll, rollMetric, remark, dateFulfilment, fabric);
    }
}
