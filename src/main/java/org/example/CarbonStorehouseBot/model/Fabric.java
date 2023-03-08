package org.example.CarbonStorehouseBot.model;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "fabric")
public class Fabric implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('A40', 'A60', 'A80', 'A120', 'A160')", nullable = false)
    private NameFabrics nameFabric;

    @Column(name = "batch_number", columnDefinition = "TEXT", nullable = false)
    private String batchNumber;

    @Column(name = "metric_area_batch", nullable = false)
    private int metricAreaBatch;

    @Column(name = "date_manufacture", columnDefinition = "DATETIME", nullable = false)
    private LocalDateTime dateManufacture;

    @ManyToMany(cascade = {CascadeType.MERGE})
    @JoinTable(name = "fabrics_rolls_table",
            joinColumns = {@JoinColumn(name = "fabric_id", referencedColumnName = "id")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "roll_id", referencedColumnName = "id")
            })
    private Set<Roll> rolls = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.MERGE})
    @JoinTable(name = "fabrics_colleagues_table",
    joinColumns = {@JoinColumn(name = "fabric_id", referencedColumnName = "id")
    },
    inverseJoinColumns = {
            @JoinColumn(name = "colleague_id", referencedColumnName = "id")
    })
    private Set<Colleague> colleagues = new HashSet<>();

    public void addColleague(Colleague colleague) {
        colleagues.add(colleague);
        colleague.getFabrics().add(this);
    }

    public void removeColleague(Colleague colleague) {
        colleagues.remove(colleague);
        colleague.getFabrics().remove(this);
    }

    public void addRoll(Roll roll) {
        rolls.add(roll);
        roll.getFabrics().add(this);
    }

    public void removeRoll(Roll roll) {
        rolls.remove(roll);
        roll.getFabrics().remove(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Fabric fabric = (Fabric) o;
        return metricAreaBatch == fabric.metricAreaBatch && Objects.equals(id, fabric.id) && nameFabric == fabric.nameFabric && Objects.equals(batchNumber, fabric.batchNumber) && Objects.equals(dateManufacture, fabric.dateManufacture);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nameFabric, batchNumber, metricAreaBatch, dateManufacture);
    }
}
