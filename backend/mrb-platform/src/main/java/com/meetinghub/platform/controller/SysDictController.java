package com.meetinghub.platform.controller;

import com.meetinghub.common.result.Result;
import com.meetinghub.platform.model.entity.SysDict;
import com.meetinghub.platform.model.entity.SysDictItem;
import com.meetinghub.platform.service.SysDictService;
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
public class SysDictController {

    private final SysDictService sysDictService;

    @GetMapping
    public Result<List<SysDict>> list() {
        return Result.ok(sysDictService.list());
    }

    @PostMapping
    public Result<Void> create(@RequestBody SysDict dict) {
        sysDictService.save(dict);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysDict dict) {
        dict.setId(id);
        sysDictService.updateById(dict);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysDictService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/{code}/items")
    public Result<List<SysDictItem>> listItems(@PathVariable String code) {
        return Result.ok(sysDictService.listItemsByDictCode(code));
    }

    @PostMapping("/item")
    public Result<Void> addItem(@RequestBody SysDictItem item) {
        sysDictService.addItem(item);
        return Result.ok();
    }

    @PutMapping("/item/{id}")
    public Result<Void> updateItem(@PathVariable Long id, @RequestBody SysDictItem item) {
        item.setId(id);
        sysDictService.updateItem(item);
        return Result.ok();
    }

    @DeleteMapping("/item/{id}")
    public Result<Void> deleteItem(@PathVariable Long id) {
        sysDictService.removeItem(id);
        return Result.ok();
    }
}
