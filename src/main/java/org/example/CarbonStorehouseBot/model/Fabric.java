package org.example.CarbonStorehouseBot.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@Entity
@Table(name = "fabric")
public class Fabric implements Serializable {

    @Id
    @Column(name = "id", nullable = false)
    private String batchNumberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_fabric", columnDefinition = "ENUM('READY','NOT_READY', 'AT_WORK')", nullable = false)
    private StatusFabric statusFabric;

    @Column(name = "name_fabric", columnDefinition = "TEXT", nullable = false)
    private String nameFabric;

    @Column(name = "metric_area_batch", nullable = false)
    private int metricAreaBatch;

    @Column(name = "date_manufacture", columnDefinition = "DATETIME", nullable = false)
    private LocalDateTime dateManufacture;

    @OneToMany(mappedBy = "fabric", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @BatchSize(size = 2)
    private List<Roll> rolls;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @OnDelete(action = OnDeleteAction.CASCADE)
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

    public void remove(){
        for(Colleague colleague : new ArrayList<>(colleagues)){
            removeColleague(colleague);
        }
    }

}
