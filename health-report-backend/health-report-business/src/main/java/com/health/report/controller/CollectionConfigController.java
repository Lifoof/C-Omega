package com.health.report.controller;

import com.health.report.common.annotation.Log;
import com.health.report.common.core.controller.BaseController;
import com.health.report.common.core.domain.AjaxResult;
import com.health.report.common.core.domain.R;
import com.health.report.common.core.page.TableDataInfo;
import com.health.report.common.enums.BusinessType;
import com.health.report.dto.collection.CategoryConfigVO;
import com.health.report.service.ICollectionConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 采集配置Controller
 *
 * @author ruoyi
 * @date 2026-03-27
 */
@RestController
@RequestMapping("/healthReport/collectionConfig")
public class CollectionConfigController extends BaseController {

    @Autowired
    private ICollectionConfigService collectionConfigService;

    @GetMapping("/categories")
    public R<List<CategoryConfigVO>> listCategories(
            @RequestParam(required = false) Integer gender,
            @RequestParam(required = false) String categoryName) {
        return R.ok(collectionConfigService.listCategoriesWithFields(gender,categoryName));
    }

    @GetMapping("/modelCategories")
    public R<List<CategoryConfigVO>> listModelCategories(
            @RequestParam(required = false) Integer gender,
            @RequestParam(required = false) String categoryName) {
        return R.ok(collectionConfigService.listModelCategoriesWithFields(gender,categoryName));
    }

    @GetMapping("/getCollectionConfigName")
    public R<List<String>> getCollectionConfigName() {
        return R.ok(collectionConfigService.getCollectionConfigName());
    }
}
