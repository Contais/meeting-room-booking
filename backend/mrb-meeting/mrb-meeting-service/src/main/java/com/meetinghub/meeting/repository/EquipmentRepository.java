package com.meetinghub.meeting.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.meeting.model.dto.EquipmentPageQuery;
import com.meetinghub.meeting.model.entity.Equipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 设备数据访问层
 */
@Mapper
public interface EquipmentRepository extends BaseMapper<Equipment> {

    /**
     * 设备列表分页查询（关键字 OR + 多条件动态过滤，下沉 XML 提升可读性）
     */
    IPage<Equipment> selectEquipmentPage(IPage<Equipment> page, @Param("query") EquipmentPageQuery query);
}
