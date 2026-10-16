package com.xiaoyan.railway.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderPassengerMapper extends BaseMapper<OrderPassenger> {

    /**
     * Id-card numbers, among {@code idCardNos}, held by an order on the same train run &amp; segment that
     * is not cancelled, refunded or lock-failed — used to block a passenger from double-booking the same ride.
     */
    @Select("""
            <script>
            SELECT DISTINCT p.id_card_no
            FROM order_passenger op
            JOIN orders o ON o.id = op.order_id
            JOIN passenger p ON p.id = op.passenger_id
            WHERE o.train_run_id = #{trainRunId}
              AND o.from_seq = #{fromSeq}
              AND o.to_seq = #{toSeq}
              AND o.order_status NOT IN (3, 5)
              AND o.lock_status &lt;&gt; 2
              AND p.id_card_no IN
              <foreach collection="idCardNos" item="idCardNo" open="(" separator="," close=")">#{idCardNo}</foreach>
            </script>
            """)
    List<String> findDuplicateIdCardNos(@Param("trainRunId") Long trainRunId, @Param("fromSeq") Integer fromSeq,
                                         @Param("toSeq") Integer toSeq, @Param("idCardNos") List<String> idCardNos);
}
