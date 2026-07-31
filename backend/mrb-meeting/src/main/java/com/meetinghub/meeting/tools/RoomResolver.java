package com.meetinghub.meeting.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.RoomListResult;
import com.meetinghub.meeting.model.vo.tool.RoomToolResults.RoomSummary;
import com.meetinghub.meeting.model.vo.tool.ToolResult;
import com.meetinghub.meeting.repository.MeetingRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 会议室解析器：按名称模糊匹配并唯一确定一间启用状态的会议室。
 * <p>
 * 抽取自各工具中重复的「按名称查询 → 处理 0/1/多 匹配」逻辑，
 * 供 {@link MeetingRoomTool}、{@link ReservationTool} 共享。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class RoomResolver {

    private final MeetingRoomRepository meetingRoomRepository;

    /**
     * 按名称匹配会议室的密封结果。
     * <ul>
     *   <li>{@link Single}：唯一匹配，携带会议室实体；</li>
     *   <li>{@link Ambiguous}：匹配到多间，需用户明确指定；</li>
     *   <li>{@link NotFound}：未匹配到任何启用状态的会议室。</li>
     * </ul>
     */
    public sealed interface RoomMatch permits RoomMatch.Single, RoomMatch.Ambiguous, RoomMatch.NotFound {
        /** 唯一匹配 */
        record Single(MeetingRoom room) implements RoomMatch {
        }

        /** 多间匹配 */
        record Ambiguous(List<MeetingRoom> rooms) implements RoomMatch {
        }

        /** 未匹配 */
        record NotFound(String keyword) implements RoomMatch {
        }
    }

    /**
     * 按名称模糊匹配启用状态的会议室。
     *
     * @param roomName 会议室名称关键词
     * @return 匹配结果
     */
    public RoomMatch resolveByName(String roomName) {
        List<MeetingRoom> rooms = meetingRoomRepository.selectList(
                new LambdaQueryWrapper<MeetingRoom>()
                        .like(MeetingRoom::getName, roomName)
                        .eq(MeetingRoom::getStatus, EnableStatusEnum.ENABLED.getCode())
        );
        if (rooms.isEmpty()) {
            return new RoomMatch.NotFound(roomName);
        }
        if (rooms.size() > 1) {
            return new RoomMatch.Ambiguous(rooms);
        }
        return new RoomMatch.Single(rooms.get(0));
    }

    /**
     * 将会议室实体转换为简要 VO（屏蔽内部字段）。
     *
     * @param room 会议室实体
     * @return 简要 VO
     */
    public static RoomSummary toSummary(MeetingRoom room) {
        return new RoomSummary(room.getName(), room.getLocation(),
                room.getCapacity() != null ? room.getCapacity() : 0,
                room.getEquipment());
    }

    /**
     * 批量查询会议室名称映射。
     *
     * @param roomIds 会议室ID列表
     * @return 会议室ID -> 名称映射，空入参返回空 Map
     */
    public Map<Long, String> batchNames(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return Map.of();
        }
        List<MeetingRoom> rooms = meetingRoomRepository.selectBatchIds(roomIds);
        return rooms.stream().collect(Collectors.toMap(MeetingRoom::getId, MeetingRoom::getName, (a, b) -> a));
    }

    /**
     * 将「非唯一匹配」结果转换为可直供格式化器渲染的 {@link ToolResult}：
     * 未匹配 → 纯文本提示；多间匹配 → 会议室列表（提示用户明确指定）。
     * <p>
     * 仅在匹配结果为 {@link RoomMatch.Ambiguous} 或 {@link RoomMatch.NotFound} 时调用，
     * 传入 {@link RoomMatch.Single} 将抛出 {@link IllegalArgumentException}。
     * </p>
     *
     * @param match 匹配结果（非唯一）
     * @return 错误/歧义对应的工具结果
     */
    public static ToolResult toErrorResult(RoomMatch match) {
        if (match instanceof RoomMatch.NotFound n) {
            return new ToolResult.TextResult("未找到名为「" + n.keyword() + "」的可用会议室");
        }
        if (match instanceof RoomMatch.Ambiguous a) {
            List<RoomSummary> summaries = a.rooms().stream().map(RoomResolver::toSummary).toList();
            return new RoomListResult("匹配到多个会议室，请明确指定：", summaries);
        }
        throw new IllegalArgumentException("唯一匹配无需错误结果: " + match);
    }
}
