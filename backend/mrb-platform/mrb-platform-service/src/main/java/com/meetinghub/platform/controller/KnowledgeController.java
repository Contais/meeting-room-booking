package com.meetinghub.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meetinghub.common.annotation.RequiresRole;
import com.meetinghub.common.result.Result;
import com.meetinghub.platform.model.dto.KnowledgeCreateDTO;
import com.meetinghub.platform.model.dto.KnowledgePageQuery;
import com.meetinghub.platform.model.dto.KnowledgeUpdateDTO;
import com.meetinghub.platform.model.vo.KnowledgeEntryVO;
import com.meetinghub.platform.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 知识库控制器
 * <p>
 * 供管理员维护 AI 助手的非结构化知识条目（预约规则条款、流程、异常处理、公告）。
 * 检索能力见 {@link KnowledgeInternalController}。
 * </p>
 */
@RestController
@RequestMapping("/platform/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库", description = "AI 助手知识库维护")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "分页查询知识条目")
    @GetMapping("/admin/list")
    public Result<IPage<KnowledgeEntryVO>> page(KnowledgePageQuery query) {
        return Result.ok(knowledgeService.page(query));
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "新增知识条目")
    @PostMapping("/admin/create")
    public Result<Void> create(@Valid @RequestBody KnowledgeCreateDTO dto) {
        knowledgeService.create(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "编辑知识条目")
    @PutMapping("/admin/update")
    public Result<Void> update(@Valid @RequestBody KnowledgeUpdateDTO dto) {
        knowledgeService.update(dto);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "启用/禁用知识条目")
    @PutMapping("/admin/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        knowledgeService.toggleStatus(id);
        return Result.ok();
    }

    @RequiresRole("ROLE_ADMIN")
    @Operation(summary = "删除知识条目")
    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.ok();
    }

}
