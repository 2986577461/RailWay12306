package com.xiaoyan.railway.order;

import com.xiaoyan.railway.basic.Station;
import com.xiaoyan.railway.basic.StationMapper;
import com.xiaoyan.railway.basic.Train;
import com.xiaoyan.railway.basic.TrainMapper;
import com.xiaoyan.railway.basic.TrainRun;
import com.xiaoyan.railway.basic.TrainRunMapper;
import com.xiaoyan.railway.common.BizException;
import com.xiaoyan.railway.order.dto.OrderDetailVO;
import com.xiaoyan.railway.order.dto.OrderListItemVO;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final TrainRunMapper trainRunMapper;
    private final TrainMapper trainMapper;
    private final StationMapper stationMapper;

    public OrderService(OrderRepository orderRepository, TrainRunMapper trainRunMapper,
                        TrainMapper trainMapper, StationMapper stationMapper) {
        this.orderRepository = orderRepository;
        this.trainRunMapper = trainRunMapper;
        this.trainMapper = trainMapper;
        this.stationMapper = stationMapper;
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
            return new OrderListItemVO(
                    order.getOrderNo(),
                    order.getOrderStatus(),
                    OrderStatus.fromCode(order.getOrderStatus()).getLabel(),
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
        return new OrderDetailVO(
                order.getOrderNo(),
                order.getOrderStatus(),
                OrderStatus.fromCode(order.getOrderStatus()).getLabel(),
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
