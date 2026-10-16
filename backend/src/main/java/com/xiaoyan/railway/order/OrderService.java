package com.xiaoyan.railway.order;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.railway.basic.Station;
import com.xiaoyan.railway.basic.StationMapper;
import com.xiaoyan.railway.basic.Train;
import com.xiaoyan.railway.basic.TrainMapper;
import com.xiaoyan.railway.basic.TrainRun;
import com.xiaoyan.railway.basic.TrainRunMapper;
import com.xiaoyan.railway.common.BizException;
import com.xiaoyan.railway.order.dto.OrderDetailVO;
import com.xiaoyan.railway.order.dto.OrderListItemVO;
import com.xiaoyan.railway.passenger.Passenger;
import com.xiaoyan.railway.passenger.PassengerMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderPassengerMapper orderPassengerMapper;
    private final PassengerMapper passengerMapper;
    private final TrainRunMapper trainRunMapper;
    private final TrainMapper trainMapper;
    private final StationMapper stationMapper;

    public OrderService(OrderRepository orderRepository, OrderPassengerMapper orderPassengerMapper,
                        PassengerMapper passengerMapper, TrainRunMapper trainRunMapper,
                        TrainMapper trainMapper, StationMapper stationMapper) {
        this.orderRepository = orderRepository;
        this.orderPassengerMapper = orderPassengerMapper;
        this.passengerMapper = passengerMapper;
        this.trainRunMapper = trainRunMapper;
        this.trainMapper = trainMapper;
        this.stationMapper = stationMapper;
    }

    /**
     * Locks the passenger rows first, so two orders for the same passenger serialize here. Only then are the
     * idempotency key and the same-ride check read, so the second request sees the first one's committed rows.
     */
    @Transactional
    public OrderRepository.OrderSummary placeOrder(long orderId, Long userId, TicketRequestCommand command,
                                                   String idempotencyKey) {
        List<Passenger> passengers = passengerMapper.selectList(Wrappers.<Passenger>lambdaQuery()
                .in(Passenger::getId, command.passengerIds())
                .orderByAsc(Passenger::getId)
                .last("FOR UPDATE"));
        return orderRepository.findByIdempotencyKey(userId, idempotencyKey).orElseGet(() -> {
            List<String> idCardNos = passengers.stream().map(Passenger::getIdCardNo).toList();
            if (!idCardNos.isEmpty() && !orderPassengerMapper.findDuplicateIdCardNos(
                    command.trainRunId(), command.fromSeq(), command.toSeq(), idCardNos).isEmpty()) {
                throw new BizException("存在乘车人已购买该车次该区间的车票，请核实后再提交");
            }
            return orderRepository.create(orderId, userId, command, idempotencyKey);
        });
    }

    public OrderDetailVO detail(Long userId, String orderNo) {
        Order order = orderRepository.findOrder(userId, orderNo);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        return toDetailVO(order, stationMapper.selectBatchIds(
                Set.of(order.getFromStationId(), order.getToStationId())),
                trainRunMapper.selectBatchIds(List.of(order.getTrainRunId())));
    }

    public List<OrderListItemVO> list(Long userId) {
        List<Order> orders = orderRepository.listByUser(userId, 50);
        if (orders.isEmpty()) {
            return List.of();
        }

        Map<Long, TrainRun> runs = index(trainRunMapper.selectBatchIds(
                orders.stream().map(Order::getTrainRunId).collect(Collectors.toSet())), TrainRun::getId);
        Set<Long> trainIds = runs.values().stream().map(TrainRun::getTrainId).collect(Collectors.toSet());
        Map<Long, Train> trains = trainIds.isEmpty() ? Map.of()
                : index(trainMapper.selectBatchIds(trainIds), Train::getId);

        Set<Long> stationIds = new HashSet<>();
        orders.forEach(o -> { stationIds.add(o.getFromStationId()); stationIds.add(o.getToStationId()); });
        Map<Long, Station> stations = index(stationMapper.selectBatchIds(stationIds), Station::getId);

        return orders.stream().map(order -> {
            TrainRun run = runs.get(order.getTrainRunId());
            Train train = run == null ? null : trains.get(run.getTrainId());
            int lockStatus = order.getLockStatus() == null ? LockStatus.PROCESSING.getCode() : order.getLockStatus();
            return new OrderListItemVO(
                    order.getOrderNo(),
                    order.getOrderStatus(),
                    OrderStatus.fromCode(order.getOrderStatus()).getLabel(),
                    lockStatus,
                    LockStatus.fromCode(lockStatus).getLabel(),
                    order.getTotalAmount(),
                    order.getCreatedAt(),
                    name(stations, order.getFromStationId()),
                    name(stations, order.getToStationId()),
                    train == null ? null : train.getTrainNo());
        }).toList();
    }

    private OrderDetailVO toDetailVO(Order order, List<Station> stations, List<TrainRun> runs) {
        Map<Long, Station> stationIndex = index(stations, Station::getId);
        TrainRun run = runs.isEmpty() ? null : runs.get(0);
        Train train = run == null ? null : trainMapper.selectById(run.getTrainId());
        int lockStatus = order.getLockStatus() == null ? LockStatus.PROCESSING.getCode() : order.getLockStatus();
        return new OrderDetailVO(
                order.getOrderNo(),
                order.getOrderStatus(),
                OrderStatus.fromCode(order.getOrderStatus()).getLabel(),
                lockStatus,
                LockStatus.fromCode(lockStatus).getLabel(),
                order.getLockFailReason(),
                order.getTotalAmount(),
                order.getExpireAt(),
                order.getCreatedAt(),
                name(stationIndex, order.getFromStationId()),
                name(stationIndex, order.getToStationId()),
                train == null ? null : train.getTrainNo());
    }

    private static String name(Map<Long, Station> stations, Long id) {
        Station station = stations.get(id);
        return station == null ? null : station.getStationName();
    }

    private static <T> Map<Long, T> index(List<T> items, Function<T, Long> idFn) {
        return items.stream().collect(Collectors.toMap(idFn, Function.identity()));
    }
}
