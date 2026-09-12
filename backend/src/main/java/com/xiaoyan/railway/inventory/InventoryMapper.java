package com.xiaoyan.railway.inventory;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface InventoryMapper {

    /** Number of stops on a train; segments are 1 .. (count-1). Single-run path. */
    @Select("SELECT MAX(ts.stop_seq) FROM train_stop ts WHERE ts.train_id = #{trainId}")
    Integer segmentCount(@Param("trainId") Long trainId);

    /** Planned total per seat type for a run. Single-run path. */
    @Select("SELECT seat_type_id AS seatTypeId, total_count AS total FROM seat_inventory WHERE train_run_id = #{trainRunId}")
    List<Map<String, Object>> seatTotals(@Param("trainRunId") Long trainRunId);

    @Insert("""
            INSERT IGNORE INTO seat_inventory
                (id, train_run_id, seat_type_id, total_count, locked_count, sold_count, version)
            VALUES (#{id}, #{trainRunId}, #{seatTypeId}, #{totalCount}, 0, 0, 0)
            """)
    int insertSeatInventory(@Param("id") Long id,
                            @Param("trainRunId") Long trainRunId,
                            @Param("seatTypeId") Long seatTypeId,
                            @Param("totalCount") int totalCount);

    /** Stop counts for many trains in one query (batch reload, avoids N+1). */
    @Select("""
            <script>
            SELECT train_id AS trainId, MAX(stop_seq) AS segmentCount
            FROM train_stop
            WHERE train_id IN
            <foreach collection="trainIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY train_id
            </script>
            """)
    List<Map<String, Object>> segmentCounts(@Param("trainIds") List<Long> trainIds);

    /** Seat totals for many runs in one query (batch reload, avoids N+1). */
    @Select("""
            <script>
            SELECT train_run_id AS trainRunId, seat_type_id AS seatTypeId, total_count AS total
            FROM seat_inventory
            WHERE train_run_id IN
            <foreach collection="runIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<Map<String, Object>> seatTotalsByRuns(@Param("runIds") List<Long> runIds);
}
