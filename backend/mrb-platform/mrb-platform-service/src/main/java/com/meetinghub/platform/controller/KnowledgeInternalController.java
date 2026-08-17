package com.meetinghub.platform.controller;

import com.meetinghub.common.result.Result;
import com.meetinghub.platform.api.model.dto.KnowledgeEntryDTO;
import com.meetinghub.platform.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识库内部接口控制器（服务间 Feign）
 * <p>
 * 路径前缀 {@code /platform/internal/knowledge/**}，仅供服务间 Feign 调用，不经过网关。
 * </p>
 */
@RestController
@RequestMapping("/platform/internal/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库内部接口", description = "服务间 Feign 调用，不经过网关")
public class KnowledgeInternalController {

    private final KnowledgeService knowledgeService;

    @Operation(summary = "检索知识库")
    @GetMapping("/search")
    public Result<List<KnowledgeEntryDTO>> search(@RequestParam("query") String query) {
        return Result.ok(knowledgeService.search(query));
    }

}
