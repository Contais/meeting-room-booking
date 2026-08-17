package com.meetinghub.platform.api.feign;

import com.meetinghub.common.result.Result;
import com.meetinghub.platform.api.model.dto.KnowledgeEntryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 知识库检索远程调用客户端（服务间 Feign）
 * <p>
 * 知识库数据归属 mrb-platform，AI 工具链在 mrb-meeting，
 * 检索能力通过 {@code /platform/internal/knowledge/search} 内部接口暴露。
 * </p>
 */
@FeignClient(name = "mrb-platform", contextId = "knowledgeFeignClient")
public interface KnowledgeFeignClient {

    @GetMapping("/platform/internal/knowledge/search")
    Result<List<KnowledgeEntryDTO>> search(@RequestParam("query") String query);

}
