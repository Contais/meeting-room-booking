package com.meetinghub.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.platform.api.model.dto.KnowledgeEntryDTO;
import com.meetinghub.platform.enums.KnowledgeCategoryEnum;
import com.meetinghub.platform.model.dto.KnowledgeCreateDTO;
import com.meetinghub.platform.model.dto.KnowledgePageQuery;
import com.meetinghub.platform.model.dto.KnowledgeUpdateDTO;
import com.meetinghub.platform.model.entity.KnowledgeEntry;
import com.meetinghub.platform.model.vo.KnowledgeEntryVO;
import com.meetinghub.platform.repository.KnowledgeEntryRepository;
import com.meetinghub.platform.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识库服务实现
 * <p>
 * 检索采用轻量关键词匹配：对中文按「相邻两字二元组」、对英文/数字按整词提取特征，
 * 按「标题/问题命中 > 标签命中 > 答案正文命中」加权打分，召回 top3。
 * 无需引入 embedding 与向量库，后续可平滑升级为向量检索。
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeEntryRepository, KnowledgeEntry>
        implements KnowledgeService {

    /** 检索召回条目上限 */
    private static final int SEARCH_TOP_N = 3;

    /** 检索最小相关度得分，低于该值视为未命中 */
    private static final int SEARCH_MIN_SCORE = 5;

    /** 中文连续串（长度 >= 2 的汉字片段） */
    private static final Pattern CHINESE_RUN = Pattern.compile("[\\u4e00-\\u9fa5]{2,}");

    /** 英文/数字连续串 */
    private static final Pattern ASCII_TOKEN = Pattern.compile("[a-z0-9]{2,}");

    @Override
    public IPage<KnowledgeEntryVO> page(KnowledgePageQuery query) {
        Page<KnowledgeEntry> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<KnowledgeEntry> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(KnowledgeEntry::getTitle, query.getKeyword())
                    .or().like(KnowledgeEntry::getQuestion, query.getKeyword())
                    .or().like(KnowledgeEntry::getAnswer, query.getKeyword())
                    .or().like(KnowledgeEntry::getTags, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getCategory())) {
            wrapper.eq(KnowledgeEntry::getCategory, query.getCategory());
        }
        if (query.getStatus() != null) {
            wrapper.eq(KnowledgeEntry::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(KnowledgeEntry::getCategory)
                .orderByAsc(KnowledgeEntry::getSort)
                .orderByDesc(KnowledgeEntry::getId);
        return page(page, wrapper).convert(this::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(KnowledgeCreateDTO dto) {
        validateCategory(dto.getCategory());
        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setCategory(dto.getCategory());
        entry.setTitle(dto.getTitle());
        entry.setQuestion(dto.getQuestion());
        entry.setAnswer(dto.getAnswer());
        entry.setTags(dto.getTags());
        entry.setSort(dto.getSort() != null ? dto.getSort() : 0);
        entry.setStatus(dto.getStatus() != null ? dto.getStatus() : EnableStatusEnum.ENABLED.getCode());
        save(entry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(KnowledgeUpdateDTO dto) {
        validateCategory(dto.getCategory());
        KnowledgeEntry entry = getById(dto.getId());
        if (entry == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "知识条目不存在");
        }
        entry.setCategory(dto.getCategory());
        entry.setTitle(dto.getTitle());
        entry.setQuestion(dto.getQuestion());
        entry.setAnswer(dto.getAnswer());
        entry.setTags(dto.getTags());
        if (dto.getSort() != null) {
            entry.setSort(dto.getSort());
        }
        if (dto.getStatus() != null) {
            entry.setStatus(dto.getStatus());
        }
        updateById(entry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        KnowledgeEntry entry = getById(id);
        if (entry == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "知识条目不存在");
        }
        Integer newStatus = EnableStatusEnum.ENABLED.getCode().equals(entry.getStatus())
                ? EnableStatusEnum.DISABLED.getCode()
                : EnableStatusEnum.ENABLED.getCode();
        entry.setStatus(newStatus);
        updateById(entry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        KnowledgeEntry entry = getById(id);
        if (entry == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "知识条目不存在");
        }
        removeById(id);
    }

    @Override
    public List<KnowledgeEntryDTO> search(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        Set<String> queryFeatures = extractFeatures(query);
        if (queryFeatures.isEmpty()) {
            return List.of();
        }

        List<KnowledgeEntry> enabledEntries = list(new LambdaQueryWrapper<KnowledgeEntry>()
                .eq(KnowledgeEntry::getStatus, EnableStatusEnum.ENABLED.getCode()));

        List<KnowledgeEntryDTO> hits = enabledEntries.stream()
                .map(entry -> new ScoredEntry(entry, score(entry, queryFeatures)))
                .filter(scored -> scored.score() >= SEARCH_MIN_SCORE)
                .sorted(Comparator.comparingInt(ScoredEntry::score).reversed())
                .limit(SEARCH_TOP_N)
                .map(scored -> toDTO(scored.entry()))
                .toList();
        log.info("知识库检索 query={}, 启用条目数={}, 命中数={}",
                query, enabledEntries.size(), hits.size());
        return hits;
    }

    /**
     * 计算条目与查询特征的相关度得分。
     */
    private int score(KnowledgeEntry entry, Set<String> queryFeatures) {
        Set<String> titleFeatures = extractFeatures(entry.getTitle());
        Set<String> questionFeatures = extractFeatures(entry.getQuestion());
        Set<String> tagFeatures = extractFeatures(entry.getTags());
        Set<String> answerFeatures = extractFeatures(entry.getAnswer());

        int score = 0;
        for (String feature : queryFeatures) {
            if (titleFeatures.contains(feature)) {
                score += 3;
            }
            if (questionFeatures.contains(feature)) {
                score += 3;
            }
            if (tagFeatures.contains(feature)) {
                score += 2;
            }
            if (answerFeatures.contains(feature)) {
                score += 1;
            }
        }
        return score;
    }

    /**
     * 提取文本特征：中文取相邻两字二元组，英文/数字取整词。
     */
    private Set<String> extractFeatures(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        Set<String> features = new HashSet<>();
        String lower = text.toLowerCase();

        Matcher chinese = CHINESE_RUN.matcher(lower);
        while (chinese.find()) {
            String run = chinese.group();
            for (int i = 0; i + 1 < run.length(); i++) {
                features.add(run.substring(i, i + 2));
            }
        }

        Matcher ascii = ASCII_TOKEN.matcher(lower);
        while (ascii.find()) {
            features.add(ascii.group());
        }
        return features;
    }

    private KnowledgeEntryDTO toDTO(KnowledgeEntry entry) {
        KnowledgeEntryDTO dto = new KnowledgeEntryDTO();
        dto.setTitle(entry.getTitle());
        dto.setCategory(categoryLabel(entry.getCategory()));
        dto.setAnswer(entry.getAnswer());
        dto.setTags(entry.getTags());
        return dto;
    }

    private KnowledgeEntryVO toVO(KnowledgeEntry entry) {
        KnowledgeEntryVO vo = new KnowledgeEntryVO();
        vo.setId(entry.getId());
        vo.setCategory(entry.getCategory());
        vo.setCategoryName(categoryLabel(entry.getCategory()));
        vo.setTitle(entry.getTitle());
        vo.setQuestion(entry.getQuestion());
        vo.setAnswer(entry.getAnswer());
        vo.setTags(entry.getTags());
        vo.setSort(entry.getSort());
        vo.setStatus(entry.getStatus());
        vo.setCreateTime(entry.getCreateTime());
        vo.setUpdateTime(entry.getUpdateTime());
        return vo;
    }

    private String categoryLabel(String code) {
        KnowledgeCategoryEnum category = KnowledgeCategoryEnum.fromCode(code);
        return category != null ? category.getLabel() : code;
    }

    private void validateCategory(String code) {
        if (KnowledgeCategoryEnum.fromCode(code) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "知识分类不合法: " + code);
        }
    }

    private record ScoredEntry(KnowledgeEntry entry, int score) {
    }

}
