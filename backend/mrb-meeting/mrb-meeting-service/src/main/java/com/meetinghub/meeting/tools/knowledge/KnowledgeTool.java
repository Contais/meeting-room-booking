package com.meetinghub.meeting.tools.knowledge;

import com.meetinghub.common.result.Result;
import com.meetinghub.meeting.model.vo.tool.ToolResult;
import com.meetinghub.platform.api.feign.KnowledgeFeignClient;
import com.meetinghub.platform.api.model.dto.KbEntryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 聊天助手工具类 - 知识库域。
 * <p>
 * 将知识检索封装为 {@code @Tool} 方法，与会议室/预约工具统一注册到 ChatClient，
 * 由模型自行判断「知识类问题」并调用；检索结果由模型组织为自然语言回答。
 * 知识库数据在 mrb-platform，本工具通过 Feign 内部接口检索，不直连其数据库。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeTool {

    private final KnowledgeFeignClient knowledgeFeignClient;

    @Tool(description = "检索会议室预约系统知识库（预约规则、操作流程、异常处理、公告等非结构化知识），返回最相关的知识条目。规则/流程/FAQ/公告类问题应优先调用；结构化查询（哪些会议室需要审批、哪间有视频会议、可约时段/时长）请改用会议室工具")
    public KbSearchResult searchKnowledge(
            @ToolParam(description = "用户问题") String query) {
        try {
            Result<List<KbEntryDTO>> result = knowledgeFeignClient.search(query);
            if (result == null || result.getData() == null || result.getData().isEmpty()) {
                return KbSearchResult.miss();
            }
            List<KbEntry> entries = result.getData().stream()
                    .map(dto -> new KbEntry(dto.getTitle(), dto.getCategory(), dto.getAnswer(), dto.getTags()))
                    .toList();
            return new KbSearchResult(entries, false);
        } catch (Exception e) {
            log.error("searchKnowledge 调用失败, query={}", query, e);
            return new KbSearchResult(List.of(), true, "知识库检索暂时不可用，请稍后重试");
        }
    }

    /**
     * 知识条目简要结构（屏蔽内部字段，仅暴露回答所需内容）。
     */
    public record KbEntry(String title, String category, String answer, String tags) {
    }

    /**
     * 知识检索结果。
     *
     * @param entries 命中的知识条目（空表示未命中）
     * @param empty   是否未命中，用于触发「知识库暂未收录」兜底
     * @param hint    错误/降级提示（正常命中时为 null）
     */
    public record KbSearchResult(List<KbEntry> entries, boolean empty, String hint) implements ToolResult {

        public KbSearchResult(List<KbEntry> entries, boolean empty) {
            this(entries, empty, null);
        }

        public static KbSearchResult miss() {
            return new KbSearchResult(List.of(), true);
        }
    }

}
