package org.example.CarbonStorehouseBot.repository;

import org.example.CarbonStorehouseBot.model.Roll;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface RollRepository extends CrudRepository<Roll, Long> {

   @Transactional
   @Modifying
   @Query(value = "DELETE FROM `roll` WHERE `fabric_id` = :fabricId", nativeQuery = true)
   void deleteAllByFabricId(@Param("fabricId") String fabricId);

   @Query(value = "SELECT * FROM `roll` WHERE `fabric_id` = :fabricId AND `number_roll` = :numberRoll", nativeQuery = true)
   Optional<Roll> findByNumberRollAndFabricId(@Param("numberRoll") int numberRoll, @Param("fabricId") String fabricId);
}
