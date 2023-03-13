package org.example.CarbonStorehouseBot.repository;

import org.example.CarbonStorehouseBot.model.Fabric;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface FabricRepository extends CrudRepository<Fabric, String> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM `fabric` WHERE id = :fabricId", nativeQuery = true)
    void deleteFabric(@Param("fabricId") String fabricId);

    @Query(value = """
            SELECT f.name_fabric, f.status_fabric, f.metric_area_batch, f.date_manufacture, sum(r.roll_metric) AS all_sum_metric
            FROM carbon_storehouse.fabric f
            JOIN carbon_storehouse.roll r ON f.id = r.fabric_id
            WHERE r.fabric_id = :fabricId
            """, nativeQuery = true)
    String allInfoFabricAndSumMetricArea(@Param("fabricId") String fabricId);
}
