package com.xiaoyan.railway.query;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * Read-model queries for train search. Custom joins live here as @Select
 * (MyBatis-Plus), keeping complex SQL out of the entity-based layer.
 */
@Mapper
public interface QueryMapper {

    /** Finds trains running on {@code date} whose stops span from → to (direct). */
    @Select("""
            SELECT tr.id AS trainRunId,
                   t.id AS trainId,
                   t.train_no AS trainNo,
                   t.train_type AS trainType,
                   fs.station_id AS fromStationId,
                   s1.station_name AS fromStationName,
                   ts.station_id AS toStationId,
                   s2.station_name AS toStationName,
                   fs.stop_seq AS fromSeq,
                   ts.stop_seq AS toSeq,
                   DATE_FORMAT(fs.depart_time, '%H:%i') AS departTime,
                   DATE_FORMAT(ts.arrive_time, '%H:%i') AS arriveTime
            FROM train t
            JOIN train_stop fs ON fs.train_id = t.id
            JOIN station s1 ON s1.id = fs.station_id
            JOIN train_stop ts ON ts.train_id = t.id AND ts.stop_seq > fs.stop_seq
            JOIN station s2 ON s2.id = ts.station_id
            JOIN train_run tr ON tr.train_id = t.id AND tr.run_date = #{date} AND tr.status = 1
            WHERE t.status = 1
              AND (s1.station_name = #{from} OR s1.city_name = #{from} OR s1.station_code = #{from})
              AND (s2.station_name = #{to} OR s2.city_name = #{to} OR s2.station_code = #{to})
            ORDER BY fs.depart_time
            """)
    List<Map<String, Object>> searchTrains(@Param("from") String from, @Param("to") String to, @Param("date") String date);

    /** Seat types offered on a run, with their planned total. */
    @Select("""
            SELECT si.seat_type_id AS seatTypeId,
                   st.code AS seatTypeCode,
                   st.name AS seatTypeName,
                   si.total_count AS total
            FROM seat_inventory si
            JOIN seat_type st ON st.id = si.seat_type_id
            WHERE si.train_run_id = #{trainRunId}
            ORDER BY st.id
            """)
    List<Map<String, Object>> listSeatTypesForRun(@Param("trainRunId") Long trainRunId);
}
