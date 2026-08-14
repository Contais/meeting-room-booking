package com.meetinghub.platform.controller;

import com.meetinghub.common.result.Result;
import com.meetinghub.platform.model.entity.SysConfig;
import com.meetinghub.platform.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统配置控制器
 * <p>
 * 键值对配置的 CRUD，供后台运营维护。预留扩展，暂未接入业务读取。
 * </p>
 */
@RestController
@RequestMapping("/platform/config")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "键值对配置维护")
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @Operation(summary = "查询配置列表")
    @GetMapping
    public Result<List<SysConfig>> list() {
        return Result.ok(sysConfigService.list());
    }

    @Operation(summary = "按 key 查询配置")
    @GetMapping("/{key}")
    public Result<SysConfig> getByKey(@PathVariable String key) {
        return Result.ok(sysConfigService.getByKey(key));
    }

    @Operation(summary = "新增配置")
    @PostMapping
    public Result<Void> create(@RequestBody SysConfig config) {
        sysConfigService.save(config);
        return Result.ok();
    }

    @Operation(summary = "编辑配置")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysConfig config) {
        config.setId(id);
        sysConfigService.updateById(config);
        return Result.ok();
    }

    @Operation(summary = "删除配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysConfigService.removeById(id);
        return Result.ok();
    }
}
