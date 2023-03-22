package org.example.CarbonStorehouseBot.repository;

import org.example.CarbonStorehouseBot.model.Fabric;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
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
    List<Object[]> allInfoFabricAndSumMetricArea(@Param("fabricId") String fabricId);

    @Query(value = """
            SELECT * FROM `fabric` f WHERE f.status_fabric = :statusFabric
            """, nativeQuery = true)
    List<Fabric> findByAllStatusFabricReady(@Param("statusFabric")String statusFabric);

    @Query(value = "SELECT * FROM `fabric` f WHERE f.name_fabric = :nameFabric", nativeQuery = true)
    List<Fabric> findByNameFabric(String nameFabric);

    @Transactional
    @Modifying
    @Query(value = "UPDATE fabric f SET f.status_fabric = :statusFabric WHERE f.id = :id", nativeQuery = true)
    void updateStatusFabric(@Param("statusFabric") String statusFabric, @Param("id") String id);

    @Query(value = "SELECT name_fabric FROM fabric", nativeQuery = true)
    Set<String> findAllNameFabric();
}
