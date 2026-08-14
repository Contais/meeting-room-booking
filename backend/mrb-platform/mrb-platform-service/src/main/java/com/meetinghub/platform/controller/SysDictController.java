package com.meetinghub.platform.controller;

import com.meetinghub.common.result.Result;
import com.meetinghub.platform.model.entity.SysDict;
import com.meetinghub.platform.model.entity.SysDictItem;
import com.meetinghub.platform.service.SysDictService;
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
 * 字典控制器
 * <p>
 * 字典与字典项的 CRUD，供后台运营维护枚举型展示数据。预留扩展，暂未接入业务。
 * </p>
 */
@RestController
@RequestMapping("/platform/dict")
@RequiredArgsConstructor
@Tag(name = "字典", description = "字典与字典项维护")
public class SysDictController {

    private final SysDictService sysDictService;

    @Operation(summary = "查询字典列表")
    @GetMapping
    public Result<List<SysDict>> list() {
        return Result.ok(sysDictService.list());
    }

    @Operation(summary = "新增字典")
    @PostMapping
    public Result<Void> create(@RequestBody SysDict dict) {
        sysDictService.save(dict);
        return Result.ok();
    }

    @Operation(summary = "编辑字典")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysDict dict) {
        dict.setId(id);
        sysDictService.updateById(dict);
        return Result.ok();
    }

    @Operation(summary = "删除字典")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysDictService.removeById(id);
        return Result.ok();
    }

    @Operation(summary = "按字典编码查询字典项")
    @GetMapping("/{code}/items")
    public Result<List<SysDictItem>> listItems(@PathVariable String code) {
        return Result.ok(sysDictService.listItemsByDictCode(code));
    }

    @Operation(summary = "新增字典项")
    @PostMapping("/item")
    public Result<Void> addItem(@RequestBody SysDictItem item) {
        sysDictService.addItem(item);
        return Result.ok();
    }

    @Operation(summary = "编辑字典项")
    @PutMapping("/item/{id}")
    public Result<Void> updateItem(@PathVariable Long id, @RequestBody SysDictItem item) {
        item.setId(id);
        sysDictService.updateItem(item);
        return Result.ok();
    }

    @Operation(summary = "删除字典项")
    @DeleteMapping("/item/{id}")
    public Result<Void> deleteItem(@PathVariable Long id) {
        sysDictService.removeItem(id);
        return Result.ok();
    }
}
