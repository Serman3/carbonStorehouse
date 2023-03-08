package org.example.CarbonStorehouseBot.repository;

import org.example.CarbonStorehouseBot.model.Roll;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RollRepository extends CrudRepository<Roll, Long> {

}
