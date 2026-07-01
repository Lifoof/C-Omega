package com.health.report.mapper;


import com.health.report.domain.CollectionDataItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CollectionDataItemMapper {
    // 删除旧指标
    void deleteByCollectionIdAndCategoryCode(@Param("collectionId") Long collectionId, @Param("categoryCode") String categoryCode);

    void deleteByCollectionId(@Param("collectionId") Long collectionId);

    // 批量插入
    void insertBatch(@Param("list") List<CollectionDataItem> list);

    /**
     * 根据采集ID查询所有行式指标数据
     */
    List<CollectionDataItem> selectByCollectionId(@Param("collectionId") Long collectionId);

    /**
     * 查询某个采集单 已填写的指标数量
     */
    Integer selectCountByCollectionId(@Param("collectionId") Long collectionId);

    /**
     * 根据采集ID查询所有已填写（非空）的字段编码
     */
    List<String> selectFilledFieldCodes(@Param("collectionId") Long collectionId);
    List<Map<String, Object>> selectValuesByCollectionId(@Param("collectionId") Long collectionId);

}
