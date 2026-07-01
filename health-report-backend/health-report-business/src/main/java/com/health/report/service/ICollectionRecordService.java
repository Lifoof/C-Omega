package com.health.report.service;

import com.health.report.domain.CollectionRecord;
import com.health.report.dto.collection.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 采集记录Service接口
 *
 * @author ruoyi
 * @date 2026-03-27
 */
public interface ICollectionRecordService {
    /**
     * 查询采集记录
     *
     * @param id 采集记录主键
     * @return 采集记录
     */
    CollectionRecord getById(Long id);

    /**
     * 查询采集记录列表
     *
     * @param collectionRecord 查询条件
     * @return 采集记录集合
     */
    List<CollectionRecord> page(CollectionRecord collectionRecord);

    /**
     * 新增采集记录
     *
     * @param collectionRecord 采集记录
     * @return 结果
     */
    CollectionRecord create(CollectionRecord collectionRecord);

    /**
     * 保存分类条目数据
     *
     * @param collectionId 采集记录ID
     * @param dto 分类数据DTO
     */
    void saveCategoryDataItem(Long collectionId, CategorySaveDTO dto);

    void saveBatchCategoryData(BatchCategorySaveDTO dto);

    /**
     * 获取采集记录详情
     *
     * @param collectionId 采集记录ID
     * @return 详情VO
     */
    CollectionDetailVO getDetail(Long collectionId);

    /**
     * 完成采集记录
     *
     * @param id 采集记录ID
     */
    void complete(Long id);

    /**
     * 删除采集记录
     *
     * @param id 采集记录ID
     */
    void deleteById(Long id);

    /**
     * 更新完整率
     *
     * @param id 采集记录ID
     */
    void updateCompleteness(Long id);
}
