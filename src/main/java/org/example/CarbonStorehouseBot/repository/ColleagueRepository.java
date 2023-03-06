package org.example.CarbonStorehouseBot.repository;

import org.example.CarbonStorehouseBot.model.Colleague;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ColleagueRepository extends CrudRepository<Colleague, Long> {

}
