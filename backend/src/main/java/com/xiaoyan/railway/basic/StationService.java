package com.xiaoyan.railway.basic;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoyan.railway.basic.dto.StationCommand;
import com.xiaoyan.railway.basic.dto.StationVO;
import com.xiaoyan.railway.common.BizException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StationService {
    private final StationMapper stationMapper;

    public StationService(StationMapper stationMapper) {
        this.stationMapper = stationMapper;
    }

    public List<StationVO> list(String keyword) {
        LambdaQueryWrapper<Station> wrapper = Wrappers.<Station>lambdaQuery().eq(Station::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim() + "%";
            wrapper.and(w -> w.like(Station::getStationName, like)
                    .or().like(Station::getCityName, like)
                    .or().like(Station::getPinyin, like)
                    .or().like(Station::getStationCode, like));
        }
        wrapper.orderByAsc(Station::getId);
        return stationMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    public void create(StationCommand command) {
        try {
            stationMapper.insert(Station.builder()
                    .stationCode(command.stationCode())
                    .stationName(command.stationName())
                    .cityName(command.cityName())
                    .pinyin(command.pinyin())
                    .status(1)
                    .build());
        } catch (DuplicateKeyException e) {
            throw new BizException("站码已存在");
        }
    }

    public void update(Long id, StationCommand command) {
        Station station = stationMapper.selectById(id);
        if (station == null) {
            throw new BizException("车站不存在");
        }
        station.setStationCode(command.stationCode());
        station.setStationName(command.stationName());
        station.setCityName(command.cityName());
        station.setPinyin(command.pinyin());
        stationMapper.updateById(station);
    }

    public void delete(Long id) {
        stationMapper.deleteById(id);
    }

    private StationVO toVO(Station s) {
        return new StationVO(s.getId(), s.getStationCode(), s.getStationName(), s.getCityName(), s.getPinyin());
    }
}
