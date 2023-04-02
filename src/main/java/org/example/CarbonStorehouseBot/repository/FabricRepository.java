package org.example.CarbonStorehouseBot.repository;

import org.example.CarbonStorehouseBot.model.Fabric;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Repository
public interface FabricRepository extends CrudRepository<Fabric, String> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM `fabric` WHERE id = :fabricId", nativeQuery = true)
    void deleteFabric(@Param("fabricId") String fabricId);

    @Query(value = """
            SELECT f.id, f.name_fabric, f.status_fabric, f.metric_area_batch, f.date_manufacture, sum(r.roll_metric) AS actual_metric
            FROM carbon_storehouse.fabric f
            JOIN carbon_storehouse.roll r ON f.id = r.fabric_id
            WHERE r.fabric_id = :fabricId
            """, nativeQuery = true)
    List<Object[]> allInfoFabricAndSumMetric(@Param("fabricId") String fabricId);

    @Query(value = """
           SELECT *
           FROM fabric f
           JOIN roll r ON f.id = r.fabric_id
           WHERE f.status_fabric = :statusFabric
           AND r.status_roll = :statusRoll
            """, nativeQuery = true)
    Set<Fabric>findByStatusFabricReadyAndStatusRollSoldOut(@Param("statusFabric") String statusFabric, @Param("statusRoll") String statusRoll);

    @Query(value = """
            SELECT * FROM `fabric` f WHERE f.status_fabric = :statusFabric
            """, nativeQuery = true)
    List<Fabric> findByAllStatusFabric(@Param("statusFabric")String statusFabric);

    @Query(value = "SELECT * FROM `fabric` f WHERE f.name_fabric = :nameFabric", nativeQuery = true)
    List<Fabric> findByNameFabric(String nameFabric);

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE fabric
            SET status_fabric = :statusFabric, date_manufacture = :date
            WHERE id = :id
            """, nativeQuery = true)
    void updateStatusFabricSoldOut(@Param("statusFabric") String statusFabric, @Param("id") String id, @Param("date") LocalDate date);

    @Query(value = "SELECT name_fabric FROM fabric WHERE NOT status_fabric = :statusFabric", nativeQuery = true)
    Set<String> findAllNameFabricAndStatusSoldOut(@Param("statusFabric") String statusFabric);

    @Query(value = """
            SELECT f.*
            FROM fabric f
            JOIN roll r ON f.id = r.fabric_id
            WHERE YEAR(date_manufacture) = YEAR(NOW())
            AND MONTH(date_manufacture) = MONTH(NOW())
            AND r.status_roll = "SOLD_OUT"
            """, nativeQuery = true)
    Set<Fabric> findAllByStatusSoldOutCurrentMonth();

    @Query(value = """
            SELECT f.*
            FROM carbon_storehouse.fabric f
            JOIN roll r ON f.id = r.fabric_id
            WHERE f.date_manufacture >= DATE_SUB(CURDATE(), INTERVAL :countMonths MONTH)
            AND r.status_roll = "SOLD_OUT"
            """, nativeQuery = true)
    Set<Fabric> findByAllByStatusSoldOutLastCountMonths(@Param("countMonths") int countMonths);

    /*@Query(value = """
            SELECT *
            FROM fabric f
            WHERE date_manufacture >= DATE_SUB(CURDATE(), INTERVAL :countMonths MONTH)
            AND status_fabric = :statusFabric
            """, nativeQuery = true)
    List<Fabric> findByAllByStatusSoldOutLastCountMonths(@Param("statusFabric") String statusFabric, @Param("countMonths") int countMonths);*/

    @Query(value = """
            SELECT f.*
            FROM fabric f
            JOIN roll r ON r.fabric_id = f.id
            WHERE YEAR(date_manufacture) = YEAR(NOW())
            AND status_roll = "SOLD_OUT"
            """,nativeQuery = true)
    Set<Fabric> findByAllByStatusSoldOutInYear();
}
