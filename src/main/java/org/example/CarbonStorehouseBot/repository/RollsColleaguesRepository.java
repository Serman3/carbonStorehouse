package org.example.CarbonStorehouseBot.repository;

import org.example.CarbonStorehouseBot.model.RollsColleaguesTable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface RollsColleaguesRepository extends CrudRepository<RollsColleaguesTable, Long> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM `rolls_colleagues_table` r WHERE r.roll_id = :id", nativeQuery = true)
    void deleteFromRollAndColleague(@Param("id") long id);

    @Transactional
    @Modifying
    void deleteByNumberFabric(String numberFabric);

    @Query(value = """
            SELECT c.first_name, SUM(metric_colleague)
            FROM rolls_colleagues_table rct
            JOIN colleague c ON c.id = rct.colleague_id
            WHERE YEAR(rct.date_working_shift) = YEAR(NOW())
            AND MONTH(rct.date_working_shift) = MONTH(NOW())
            AND rct.colleague_id = :colleagueId
            AND rct.number_fabric = :numberFabric;
            """, nativeQuery = true)
    Object[] findByColleagueIdAndNumberFabricCurrentMonth(@Param("colleagueId") Long colleagueId, @Param("numberFabric") String numberFabric);
}
