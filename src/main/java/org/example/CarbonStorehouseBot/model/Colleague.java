package org.example.CarbonStorehouseBot.model;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "colleague")
public class Colleague implements Serializable {

    @Id
    @Column(name = "id",nullable = false)
    private Long chatId;

    @Column(name = "first_name",columnDefinition = "VARCHAR(255)", nullable = false)
    private String firstName;

    @Column(name = "last_name",columnDefinition = "VARCHAR(255)")
    private String lastName;

    @Column(name = "user_name",columnDefinition = "VARCHAR(255)")
    private String userName;

    @Column(name = "date_register", columnDefinition = "DATETIME", nullable = false)
    private LocalDate dateRegister;

    @ManyToMany(mappedBy = "colleagues", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private Set<Fabric> fabrics = new HashSet<>();

    @ManyToMany(mappedBy = "colleagueSet", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private Set<Roll> rolls = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Colleague colleague = (Colleague) o;
        return Objects.equals(chatId, colleague.chatId) && Objects.equals(firstName, colleague.firstName) && Objects.equals(lastName, colleague.lastName) && Objects.equals(userName, colleague.userName) && Objects.equals(dateRegister, colleague.dateRegister);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatId, firstName, lastName, userName, dateRegister);
    }
}
