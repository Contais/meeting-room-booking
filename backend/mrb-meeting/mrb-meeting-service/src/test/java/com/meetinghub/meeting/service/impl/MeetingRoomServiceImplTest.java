package com.meetinghub.meeting.service.impl;

import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.meeting.model.dto.RoomCreateDTO;
import com.meetinghub.meeting.model.dto.RoomUpdateDTO;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.repository.MeetingRoomRepository;
import com.meetinghub.meeting.repository.ReservationRepository;
import com.meetinghub.platform.api.feign.FileFeignClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 会议室服务核心逻辑单元测试。
 */
class MeetingRoomServiceImplTest {

    private MeetingRoomRepository meetingRoomRepository;
    private ReservationRepository reservationRepository;
    private FileFeignClient fileFeignClient;

    private MeetingRoomServiceImpl service;

    @BeforeEach
    void setUp() {
        meetingRoomRepository = mock(MeetingRoomRepository.class);
        reservationRepository = mock(ReservationRepository.class);
        fileFeignClient = mock(FileFeignClient.class);

        service = new MeetingRoomServiceImpl(meetingRoomRepository, reservationRepository, fileFeignClient);
        ReflectionTestUtils.setField(service, "baseMapper", meetingRoomRepository);
    }

    @Test
    void should_applyDefaultRules_when_createRoomWithoutRules() {
        RoomCreateDTO dto = new RoomCreateDTO();
        dto.setName("A101");
        dto.setCapacity(10);
        when(meetingRoomRepository.insert(any(MeetingRoom.class))).thenReturn(1);

        service.createRoom(dto);

        ArgumentCaptor<MeetingRoom> captor = ArgumentCaptor.forClass(MeetingRoom.class);
        verify(meetingRoomRepository).insert(captor.capture());
        assertThat(captor.getValue().getBookableStart()).isEqualTo("08:00");
        assertThat(captor.getValue().getBookableEnd()).isEqualTo("20:00");
        assertThat(captor.getValue().getMinDuration()).isEqualTo(0);
        assertThat(captor.getValue().getMaxDuration()).isEqualTo(480);
        assertThat(captor.getValue().getAdvanceDays()).isEqualTo(7);
        assertThat(captor.getValue().getStatus()).isEqualTo(EnableStatusEnum.ENABLED.getCode());
    }

    @Test
    void should_keepProvidedRules_when_createRoomWithRules() {
        RoomCreateDTO dto = new RoomCreateDTO();
        dto.setName("B201");
        dto.setCapacity(6);
        dto.setBookableStart("09:00");
        dto.setBookableEnd("18:30");
        dto.setMinDuration(30);
        dto.setMaxDuration(120);
        dto.setAdvanceDays(3);
        dto.setNeedApproval(1);
        when(meetingRoomRepository.insert(any(MeetingRoom.class))).thenReturn(1);

        service.createRoom(dto);

        ArgumentCaptor<MeetingRoom> captor = ArgumentCaptor.forClass(MeetingRoom.class);
        verify(meetingRoomRepository).insert(captor.capture());
        assertThat(captor.getValue().getBookableStart()).isEqualTo("09:00");
        assertThat(captor.getValue().getMinDuration()).isEqualTo(30);
        assertThat(captor.getValue().getMaxDuration()).isEqualTo(120);
        assertThat(captor.getValue().getNeedApproval()).isEqualTo(1);
    }

    @Test
    void should_throw_when_createRoomWithInvalidBookableTime() {
        RoomCreateDTO dto = new RoomCreateDTO();
        dto.setName("C301");
        dto.setCapacity(10);
        dto.setBookableStart("25:00");
        dto.setBookableEnd("26:00");

        assertThatThrownBy(() -> service.createRoom(dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
        verify(meetingRoomRepository, never()).insert(any(MeetingRoom.class));
    }

    @Test
    void should_throw_when_createRoomWithNegativeDuration() {
        RoomCreateDTO dto = new RoomCreateDTO();
        dto.setName("C302");
        dto.setCapacity(10);
        dto.setMaxDuration(-1);

        assertThatThrownBy(() -> service.createRoom(dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
        verify(meetingRoomRepository, never()).insert(any(MeetingRoom.class));
    }

    @Test
    void should_throw_when_createRoomWithMinDurationGreaterThanMax() {
        RoomCreateDTO dto = new RoomCreateDTO();
        dto.setName("C303");
        dto.setCapacity(10);
        dto.setMinDuration(120);
        dto.setMaxDuration(60);

        assertThatThrownBy(() -> service.createRoom(dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
        verify(meetingRoomRepository, never()).insert(any(MeetingRoom.class));
    }

    @Test
    void should_toggleStatus_when_enabled() {
        MeetingRoom room = new MeetingRoom();
        room.setId(1L);
        room.setStatus(EnableStatusEnum.ENABLED.getCode());
        when(meetingRoomRepository.selectById(1L)).thenReturn(room);

        service.toggleStatus(1L);

        ArgumentCaptor<MeetingRoom> captor = ArgumentCaptor.forClass(MeetingRoom.class);
        verify(meetingRoomRepository).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EnableStatusEnum.DISABLED.getCode());
    }

    @Test
    void should_throw_when_getRoomDetail_notFound() {
        when(meetingRoomRepository.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.getRoomDetail(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.MEETING_ROOM_NOT_FOUND.getCode()));
    }

    @Test
    void should_toggleStatus_when_disabled() {
        MeetingRoom room = new MeetingRoom();
        room.setId(1L);
        room.setStatus(EnableStatusEnum.DISABLED.getCode());
        when(meetingRoomRepository.selectById(1L)).thenReturn(room);

        service.toggleStatus(1L);

        ArgumentCaptor<MeetingRoom> captor = ArgumentCaptor.forClass(MeetingRoom.class);
        verify(meetingRoomRepository).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EnableStatusEnum.ENABLED.getCode());
    }

    @Test
    void should_throw_when_deleteRoomWithActiveReservation() {
        MeetingRoom room = new MeetingRoom();
        room.setId(1L);
        when(meetingRoomRepository.selectByIdForUpdate(1L)).thenReturn(room);
        when(reservationRepository.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteRoom(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
        verify(meetingRoomRepository, never()).deleteById(any());
        verify(meetingRoomRepository, never()).updateById(any(MeetingRoom.class));
    }

    @Test
    void should_deleteRoom_when_noActiveReservation() {
        MeetingRoom room = new MeetingRoom();
        room.setId(1L);
        when(meetingRoomRepository.selectByIdForUpdate(1L)).thenReturn(room);
        when(reservationRepository.selectCount(any())).thenReturn(0L);
        when(meetingRoomRepository.deleteById(1L)).thenReturn(1);

        service.deleteRoom(1L);

        verify(meetingRoomRepository).deleteById(1L);
    }

    @Test
    void should_updateRoom_preservingNullFields() {
        MeetingRoom room = new MeetingRoom();
        room.setId(1L);
        room.setName("旧名");
        room.setBookableStart("07:00");
        room.setBookableEnd("21:00");
        when(meetingRoomRepository.selectById(1L)).thenReturn(room);

        RoomUpdateDTO dto = new RoomUpdateDTO();
        dto.setId(1L);
        dto.setName("新名");

        service.updateRoom(dto);

        ArgumentCaptor<MeetingRoom> captor = ArgumentCaptor.forClass(MeetingRoom.class);
        verify(meetingRoomRepository).updateById(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("新名");
        assertThat(captor.getValue().getBookableStart()).isEqualTo("07:00");
        assertThat(captor.getValue().getBookableEnd()).isEqualTo("21:00");
    }

}
