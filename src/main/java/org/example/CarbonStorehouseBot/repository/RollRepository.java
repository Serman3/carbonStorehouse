package org.example.CarbonStorehouseBot.repository;

import org.example.CarbonStorehouseBot.model.Roll;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RollRepository extends CrudRepository<Roll, Long> {

   @Transactional
   @Modifying
   @Query(value = "DELETE FROM `roll` WHERE `fabric_id` = :fabricId", nativeQuery = true)
   void deleteAllByFabricId(@Param("fabricId") String fabricId);

   @Transactional
   @Modifying
   @Query(value = "DELETE FROM `roll` WHERE `fabric_id` = :fabricId AND `number_roll` = :numberRoll", nativeQuery = true)
   void deleteByFabricIdAndNumberRoll(@Param("fabricId") String fabricId, @Param("numberRoll") int numberRoll);

   @Query(value = "SELECT * FROM `roll` WHERE `fabric_id` = :fabricId AND `number_roll` = :numberRoll", nativeQuery = true)
   Optional<Roll> findByNumberRollAndFabricId(@Param("numberRoll") int numberRoll, @Param("fabricId") String fabricId);

   @Query(value = "SELECT * FROM `roll` WHERE `fabric_id` = :fabricId", nativeQuery = true)
   List<Roll> findByFabricId(@Param("fabricId") String fabricId);

   @Query(value = """
            SELECT r.*
            FROM roll r
            JOIN fabric f ON f.id = r.fabric_id
            WHERE r.status_roll = :statusRoll AND f.status_fabric = :statusFabric
            """, nativeQuery = true)
   List<Roll> getAllSoldOutRollInReadyFabric(@Param("statusRoll") String statusRoll, @Param("statusFabric") String statusFabric);

   @Transactional
   @Modifying
   @Query(value = """
           UPDATE roll
           SET status_roll = :statusRoll, date_fulfilment = :date
           WHERE fabric_id = :fabricId
           """, nativeQuery = true)
   void updateAllStatusRoll(@Param("statusRoll") String statusRoll, @Param("fabricId") String fabricId, @Param("date") LocalDateTime date);

   @Transactional
   @Modifying
   @Query(value = """
            UPDATE roll
            SET status_roll = :statusRoll, date_fulfilment = :date
            WHERE fabric_id = :fabricId AND number_roll = :numberRoll
           """, nativeQuery = true)
   void updateByStatusRoll(@Param("statusRoll") String statusRoll, @Param("fabricId") String fabricId, @Param("numberRoll") int numberRoll, @Param("date") LocalDateTime date);

 /*  @Transactional
   @Modifying
   @Query(value = "DELETE FROM `rolls_colleagues_table` r WHERE r.roll_id = :id", nativeQuery = true)
   void deleteFromRollAndColleague(@Param("id") long id);*/
}
