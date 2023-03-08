package org.example.CarbonStorehouseBot.model;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "roll")
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

    @ManyToMany(mappedBy = "rolls", fetch = FetchType.EAGER)
    private Set<Fabric> fabrics = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Roll roll = (Roll) o;
        return numberRoll == roll.numberRoll && rollMetric == roll.rollMetric && Objects.equals(id, roll.id) && Objects.equals(remark, roll.remark) && Objects.equals(dateFulfilment, roll.dateFulfilment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, numberRoll, rollMetric, remark, dateFulfilment);
    }
}
