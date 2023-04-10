package org.example.CarbonStorehouseBot.repository;

import org.example.CarbonStorehouseBot.model.RollsColleaguesTable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

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
            """, nativeQuery = true)
    List<Object[]> findByColleagueIdCurrentMonth(@Param("colleagueId") Long colleagueId);

    @Query(value = """
            SELECT *
            FROM rolls_colleagues_table
            WHERE YEAR(date_working_shift) = YEAR(NOW())
            AND MONTH(date_working_shift) = MONTH(NOW())
            AND colleague_id = :colleagueId
            ORDER BY date_working_shift DESC
            """, nativeQuery = true)
    List<RollsColleaguesTable> allDataColleagueCurrentMonth(@Param("colleagueId") Long colleagueId);

    @Query(value = """
            SELECT c.first_name, SUM(metric_colleague)
            FROM rolls_colleagues_table rct
            JOIN colleague c ON c.id = rct.colleague_id
            WHERE rct.date_working_shift >= CONCAT(YEAR(CURRENT_DATE() - INTERVAL 1 MONTH),'-',MONTH(CURRENT_DATE() - INTERVAL 1 MONTH),'-01')
            AND rct.date_working_shift <= LAST_DAY(CURRENT_DATE() - INTERVAL 1 MONTH)
            AND rct.colleague_id = :colleagueId
            """, nativeQuery = true)
    List<Object[]> findByColleagueIdPreviousMonth(@Param("colleagueId") Long colleagueId);

    @Query(value = """
            SELECT *
            FROM rolls_colleagues_table
            WHERE date_working_shift >= CONCAT(YEAR(CURRENT_DATE() - INTERVAL 1 MONTH),'-',MONTH(CURRENT_DATE() - INTERVAL 1 MONTH),'-01')
            AND date_working_shift <= LAST_DAY(CURRENT_DATE() - INTERVAL 1 MONTH)
            AND colleague_id = :colleagueId
            ORDER BY date_working_shift DESC;
            """, nativeQuery = true)
    List<RollsColleaguesTable> allDataColleaguePreviousMonth(@Param("colleagueId") Long colleagueId);
}
