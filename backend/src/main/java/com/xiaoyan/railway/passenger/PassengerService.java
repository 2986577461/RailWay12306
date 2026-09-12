package com.xiaoyan.railway.passenger;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaoyan.railway.common.BizException;
import com.xiaoyan.railway.passenger.dto.PassengerCommand;
import com.xiaoyan.railway.passenger.dto.PassengerVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PassengerService {
    private static final int MAX_PASSENGERS = 10;

    private final PassengerMapper passengerMapper;

    public PassengerService(PassengerMapper passengerMapper) {
        this.passengerMapper = passengerMapper;
    }

    public void add(Long userId, PassengerCommand command) {
        Long count = passengerMapper.selectCount(Wrappers.<Passenger>lambdaQuery().eq(Passenger::getUserId, userId));
        if (count >= MAX_PASSENGERS) {
            throw new BizException("乘车人数量已达上限（" + MAX_PASSENGERS + "人）");
        }
        LocalDateTime now = LocalDateTime.now();
        Passenger passenger = Passenger.builder()
                .userId(userId)
                .name(command.name())
                .idCardNo(command.idCardNo())
                .passengerType(command.passengerType() == null ? 1 : command.passengerType())
                .phone(command.phone())
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            passengerMapper.insert(passenger);
        } catch (DuplicateKeyException e) {
            throw new BizException("该乘车人已存在");
        }
    }

    public List<PassengerVO> list(Long userId) {
        return passengerMapper.selectList(Wrappers.<Passenger>lambdaQuery()
                        .eq(Passenger::getUserId, userId)
                        .orderByDesc(Passenger::getCreatedAt))
                .stream().map(this::toVO).toList();
    }

    public void update(Long userId, Long id, PassengerCommand command) {
        Passenger passenger = getOwned(userId, id);
        passenger.setName(command.name());
        passenger.setIdCardNo(command.idCardNo());
        passenger.setPassengerType(command.passengerType() == null ? 1 : command.passengerType());
        passenger.setPhone(command.phone());
        passenger.setUpdatedAt(LocalDateTime.now());
        try {
            passengerMapper.updateById(passenger);
        } catch (DuplicateKeyException e) {
            throw new BizException("该乘车人已存在");
        }
    }

    public void delete(Long userId, Long id) {
        Passenger passenger = getOwned(userId, id);
        passengerMapper.deleteById(passenger.getId());
    }

    /** Fetches a passenger that must belong to {@code userId}; throws otherwise (ownership check). */
    private Passenger getOwned(Long userId, Long id) {
        Passenger passenger = passengerMapper.selectOne(Wrappers.<Passenger>lambdaQuery()
                .eq(Passenger::getId, id)
                .eq(Passenger::getUserId, userId));
        if (passenger == null) {
            throw new BizException("乘车人不存在");
        }
        return passenger;
    }

    private PassengerVO toVO(Passenger p) {
        return new PassengerVO(p.getId(), p.getName(), p.getIdCardNo(), p.getPassengerType(),
                p.getPhone(), p.getCreatedAt());
    }
}
