package com.meetinghub.platform.service.impl;

import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.platform.api.model.dto.KnowledgeEntryDTO;
import com.meetinghub.platform.model.dto.KnowledgeCreateDTO;
import com.meetinghub.platform.model.entity.KnowledgeEntry;
import com.meetinghub.platform.repository.KnowledgeEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 知识库服务核心逻辑单元测试（重点覆盖轻量检索打分与兜底）。
 */
class KnowledgeServiceImplTest {

    private KnowledgeEntryRepository repository;
    private KnowledgeServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(KnowledgeEntryRepository.class);
        service = new KnowledgeServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", repository);
    }

    @Test
    void should_returnTopMatch_when_searchKnowledgeQuestion() {
        KnowledgeEntry rejected = entry(9L, "EXCEPTION", "异常处理·预约被拒",
                "预约被拒绝了怎么办", "进入「我的预约」查看该预约的拒绝原因，确认后重新预约。", "被拒,拒绝,审批");
        KnowledgeEntry holiday = entry(13L, "ANNOUNCEMENT", "运营公告·节假日安排",
                "节假日期间会议室还能约吗", "节假日期间以管理员公告为准。", "节假日,国庆,公告");
        when(repository.selectList(any())).thenReturn(List.of(rejected, holiday));

        List<KnowledgeEntryDTO> result = service.search("预约被拒绝了怎么办");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("异常处理·预约被拒");
        assertThat(result.get(0).getCategory()).isEqualTo("异常处理");
    }

    @Test
    void should_returnEmpty_when_queryBelowThreshold() {
        KnowledgeEntry advance = entry(1L, "RULES", "预约规则·提前预约天数",
                "我可以提前几天预约", "一般情况下可提前 7 天预约。", "提前,预约,天数");
        when(repository.selectList(any())).thenReturn(List.of(advance));

        assertThat(service.search("附近有什么好吃的")).isEmpty();
    }

    @Test
    void should_toggleStatus_when_enabled() {
        KnowledgeEntry entry = entry(1L, "RULES", "预约规则·提前预约天数",
                "我可以提前几天预约", "一般情况下可提前 7 天预约。", "提前,预约,天数");
        when(repository.selectById(1L)).thenReturn(entry);

        service.toggleStatus(1L);

        verify(repository).updateById(entry);
        assertThat(entry.getStatus()).isEqualTo(EnableStatusEnum.DISABLED.getCode());
    }

    @Test
    void should_throw_when_createWithInvalidCategory() {
        KnowledgeCreateDTO dto = new KnowledgeCreateDTO();
        dto.setCategory("UNKNOWN");
        dto.setTitle("标题");
        dto.setQuestion("问题");
        dto.setAnswer("答案");

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_throw_when_deleteNotFound() {
        when(repository.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(BusinessException.class);
    }

    private KnowledgeEntry entry(Long id, String category, String title,
                                 String question, String answer, String tags) {
        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setId(id);
        entry.setCategory(category);
        entry.setTitle(title);
        entry.setQuestion(question);
        entry.setAnswer(answer);
        entry.setTags(tags);
        entry.setStatus(EnableStatusEnum.ENABLED.getCode());
        return entry;
    }
}
