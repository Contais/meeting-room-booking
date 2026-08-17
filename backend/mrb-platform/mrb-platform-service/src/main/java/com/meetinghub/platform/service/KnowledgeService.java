package com.meetinghub.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.meetinghub.platform.api.model.dto.KbEntryDTO;
import com.meetinghub.platform.model.dto.KnowledgeCreateDTO;
import com.meetinghub.platform.model.dto.KnowledgePageQuery;
import com.meetinghub.platform.model.dto.KnowledgeUpdateDTO;
import com.meetinghub.platform.model.entity.KnowledgeEntry;
import com.meetinghub.platform.model.vo.KnowledgeEntryVO;

import java.util.List;

/**
 * 知识库服务
 */
public interface KnowledgeService extends IService<KnowledgeEntry> {

    /**
     * 分页查询知识条目。
     */
    IPage<KnowledgeEntryVO> page(KnowledgePageQuery query);

    /**
     * 新增知识条目。
     */
    void create(KnowledgeCreateDTO dto);

    /**
     * 编辑知识条目。
     */
    void update(KnowledgeUpdateDTO dto);

    /**
     * 启用/禁用知识条目。
     */
    void toggleStatus(Long id);

    /**
     * 删除知识条目。
     */
    void delete(Long id);

    /**
     * 按用户问题检索最相关的知识条目（最多 top3），供 AI 生成回答。
     * 未命中返回空列表。
     */
    List<KbEntryDTO> search(String query);

}
