package com.meetinghub.meeting.service.impl;

import com.meetinghub.common.enums.ApprovalModeEnum;
import com.meetinghub.common.enums.EnableStatusEnum;
import com.meetinghub.common.exception.BusinessException;
import com.meetinghub.common.exception.ErrorCode;
import com.meetinghub.meeting.api.enums.ReservationStatusEnum;
import com.meetinghub.meeting.model.dto.ReservationCreateDTO;
import com.meetinghub.meeting.model.entity.MeetingRoom;
import com.meetinghub.meeting.model.entity.MeetingRoomReservation;
import com.meetinghub.meeting.repository.MeetingRoomRepository;
import com.meetinghub.meeting.repository.ReservationRepository;
import com.meetinghub.meeting.service.ReservationAttendeeService;
import com.meetinghub.platform.api.mq.producer.NotificationSender;
import com.meetinghub.user.api.feign.UserFeignClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 预约服务核心逻辑单元测试。
 * <p>
 * 由于 {@code ReservationServiceImpl} 继承 MyBatis-Plus 的 {@code ServiceImpl}，
 * 测试中通过反射把 {@code baseMapper} 注入为 {@code ReservationRepository} mock，
 * 使 getById/save/updateById/count/list 等继承方法落到 mock 上，无需真实数据库。
 * </p>
 */
class ReservationServiceImplTest {

    private ReservationRepository reservationRepository;
    private MeetingRoomRepository meetingRoomRepository;
    private UserFeignClient userFeignClient;
    private ReservationAttendeeService attendeeService;
    private NotificationSender notificationSender;
    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;

    private ReservationServiceImpl service;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        meetingRoomRepository = mock(MeetingRoomRepository.class);
        userFeignClient = mock(UserFeignClient.class);
        attendeeService = mock(ReservationAttendeeService.class);
        notificationSender = mock(NotificationSender.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);

        service = new ReservationServiceImpl(
                reservationRepository, meetingRoomRepository, userFeignClient,
                attendeeService, notificationSender, stringRedisTemplate);
        ReflectionTestUtils.setField(service, "baseMapper", reservationRepository);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
    }

    private MeetingRoom enabledRoom() {
        MeetingRoom room = new MeetingRoom();
        room.setId(1L);
        room.setStatus(EnableStatusEnum.ENABLED.getCode());
        room.setBookableStart("08:00");
        room.setBookableEnd("20:00");
        room.setMaxDuration(480);
        room.setAdvanceDays(7);
        room.setNeedApproval(ApprovalModeEnum.FREE_APPROVAL.getCode());
        return room;
    }

    private ReservationCreateDTO validDto() {
        ReservationCreateDTO dto = new ReservationCreateDTO();
        dto.setRoomId(1L);
        dto.setSubject("周会");
        dto.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0));
        dto.setEndTime(dto.getStartTime().plusHours(1));
        return dto;
    }

    @Test
    void should_createConfirmedReservation_when_freeApproval() {
        when(meetingRoomRepository.selectById(1L)).thenReturn(enabledRoom());
        when(reservationRepository.selectCount(any())).thenReturn(0L);
        when(reservationRepository.insert(any(MeetingRoomReservation.class))).thenReturn(1);

        String code = service.createReservation(100L, validDto());

        assertThat(code).startsWith("B").hasSize(15);
        ArgumentCaptor<MeetingRoomReservation> captor = ArgumentCaptor.forClass(MeetingRoomReservation.class);
        verify(reservationRepository).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatusEnum.CONFIRMED.getCode());
        assertThat(captor.getValue().getUserId()).isEqualTo(100L);
    }

    @Test
    void should_createPendingReservation_when_needApproval() {
        MeetingRoom room = enabledRoom();
        room.setNeedApproval(ApprovalModeEnum.NEED_APPROVAL.getCode());
        when(meetingRoomRepository.selectById(1L)).thenReturn(room);
        when(reservationRepository.selectCount(any())).thenReturn(0L);
        when(reservationRepository.insert(any(MeetingRoomReservation.class))).thenReturn(1);

        service.createReservation(100L, validDto());

        ArgumentCaptor<MeetingRoomReservation> captor = ArgumentCaptor.forClass(MeetingRoomReservation.class);
        verify(reservationRepository).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatusEnum.PENDING.getCode());
    }

    @Test
    void should_throw_when_roomNotFound() {
        when(meetingRoomRepository.selectById(99L)).thenReturn(null);
        ReservationCreateDTO dto = validDto();
        dto.setRoomId(99L);

        assertThatThrownBy(() -> service.createReservation(100L, dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.MEETING_ROOM_NOT_FOUND.getCode()));
    }

    @Test
    void should_throw_when_roomDisabled() {
        MeetingRoom room = enabledRoom();
        room.setStatus(EnableStatusEnum.DISABLED.getCode());
        when(meetingRoomRepository.selectById(1L)).thenReturn(room);

        assertThatThrownBy(() -> service.createReservation(100L, validDto()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.MEETING_ROOM_DISABLED.getCode()));
    }

    @Test
    void should_throw_when_endNotAfterStart() {
        when(meetingRoomRepository.selectById(1L)).thenReturn(enabledRoom());
        ReservationCreateDTO dto = validDto();
        dto.setEndTime(dto.getStartTime());

        assertThatThrownBy(() -> service.createReservation(100L, dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void should_throw_when_timeConflict() {
        when(meetingRoomRepository.selectById(1L)).thenReturn(enabledRoom());
        when(reservationRepository.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.createReservation(100L, validDto()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.RESERVATION_CONFLICT.getCode()));
    }

    @Test
    void should_throw_when_exceedsMaxDuration() {
        MeetingRoom room = enabledRoom();
        room.setMaxDuration(30);
        when(meetingRoomRepository.selectById(1L)).thenReturn(room);
        ReservationCreateDTO dto = validDto();
        dto.setEndTime(dto.getStartTime().plusHours(2));

        assertThatThrownBy(() -> service.createReservation(100L, dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void should_throw_when_cancelByNonOwner() {
        MeetingRoomReservation reservation = new MeetingRoomReservation();
        reservation.setId(10L);
        reservation.setUserId(999L);
        when(reservationRepository.selectById(10L)).thenReturn(reservation);

        assertThatThrownBy(() -> service.cancelReservation(100L, 10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.FORBIDDEN.getCode()));
    }

    @Test
    void should_cancelReservation_when_ownerAndNotStarted() {
        MeetingRoomReservation reservation = new MeetingRoomReservation();
        reservation.setId(10L);
        reservation.setUserId(100L);
        reservation.setSubject("周会");
        reservation.setReservationCode("B20260814000001");
        reservation.setStatus(ReservationStatusEnum.CONFIRMED.getCode());
        reservation.setStartTime(LocalDateTime.now().plusDays(1));
        when(reservationRepository.selectById(10L)).thenReturn(reservation);
        when(attendeeService.listAttendees(10L)).thenReturn(Collections.emptyList());

        service.cancelReservation(100L, 10L);

        ArgumentCaptor<MeetingRoomReservation> captor = ArgumentCaptor.forClass(MeetingRoomReservation.class);
        verify(reservationRepository).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatusEnum.CANCELLED.getCode());
    }

    @Test
    void should_approveReservation_when_pending() {
        MeetingRoomReservation reservation = new MeetingRoomReservation();
        reservation.setId(10L);
        reservation.setUserId(100L);
        reservation.setSubject("周会");
        reservation.setReservationCode("B20260814000001");
        reservation.setStatus(ReservationStatusEnum.PENDING.getCode());
        reservation.setStartTime(LocalDateTime.now().plusDays(1));
        reservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        when(reservationRepository.selectById(10L)).thenReturn(reservation);
        when(reservationRepository.update(any(MeetingRoomReservation.class), any())).thenReturn(1);
        when(attendeeService.listAttendees(10L)).thenReturn(Collections.emptyList());

        service.approveReservation(10L);

        verify(reservationRepository).update(any(MeetingRoomReservation.class), any());
    }

    @Test
    void should_allow_when_durationEqualsMaxDuration() {
        MeetingRoom room = enabledRoom();
        room.setMaxDuration(60);
        when(meetingRoomRepository.selectById(1L)).thenReturn(room);
        when(reservationRepository.selectCount(any())).thenReturn(0L);
        when(reservationRepository.insert(any(MeetingRoomReservation.class))).thenReturn(1);
        ReservationCreateDTO dto = validDto();
        dto.setEndTime(dto.getStartTime().plusMinutes(60));

        String code = service.createReservation(100L, dto);

        assertThat(code).startsWith("B");
    }

    @Test
    void should_throw_when_startBeforeBookableStart() {
        when(meetingRoomRepository.selectById(1L)).thenReturn(enabledRoom());
        ReservationCreateDTO dto = validDto();
        dto.setStartTime(dto.getStartTime().withHour(7).withMinute(30));
        dto.setEndTime(dto.getStartTime().plusHours(1));

        assertThatThrownBy(() -> service.createReservation(100L, dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void should_throw_when_endAfterBookableEnd() {
        when(meetingRoomRepository.selectById(1L)).thenReturn(enabledRoom());
        ReservationCreateDTO dto = validDto();
        dto.setStartTime(dto.getStartTime().withHour(19).withMinute(30));
        dto.setEndTime(dto.getStartTime().plusHours(1));

        assertThatThrownBy(() -> service.createReservation(100L, dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void should_allow_when_exactlyOnBookableWindow() {
        MeetingRoom room = enabledRoom();
        room.setBookableStart("08:00");
        room.setBookableEnd("09:00");
        room.setMaxDuration(60);
        when(meetingRoomRepository.selectById(1L)).thenReturn(room);
        when(reservationRepository.selectCount(any())).thenReturn(0L);
        when(reservationRepository.insert(any(MeetingRoomReservation.class))).thenReturn(1);
        ReservationCreateDTO dto = validDto();
        dto.setStartTime(dto.getStartTime().withHour(8).withMinute(0));
        dto.setEndTime(dto.getStartTime().plusHours(1));

        String code = service.createReservation(100L, dto);

        assertThat(code).startsWith("B");
    }

    @Test
    void should_throw_when_bookingDateExceedsAdvanceDays() {
        MeetingRoom room = enabledRoom();
        room.setAdvanceDays(1);
        when(meetingRoomRepository.selectById(1L)).thenReturn(room);
        ReservationCreateDTO dto = validDto();
        dto.setStartTime(dto.getStartTime().plusDays(1));
        dto.setEndTime(dto.getStartTime().plusHours(1));

        assertThatThrownBy(() -> service.createReservation(100L, dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void should_allow_when_bookingDateEqualsAdvanceDays() {
        MeetingRoom room = enabledRoom();
        room.setAdvanceDays(1);
        when(meetingRoomRepository.selectById(1L)).thenReturn(room);
        when(reservationRepository.selectCount(any())).thenReturn(0L);
        when(reservationRepository.insert(any(MeetingRoomReservation.class))).thenReturn(1);
        ReservationCreateDTO dto = validDto();
        dto.setEndTime(dto.getStartTime().plusHours(1));

        String code = service.createReservation(100L, dto);

        assertThat(code).startsWith("B");
    }

    @Test
    void should_rejectReservation_when_pending() {
        MeetingRoomReservation reservation = new MeetingRoomReservation();
        reservation.setId(10L);
        reservation.setUserId(100L);
        reservation.setSubject("周会");
        reservation.setReservationCode("B20260814000001");
        reservation.setStatus(ReservationStatusEnum.PENDING.getCode());
        when(reservationRepository.selectById(10L)).thenReturn(reservation);
        when(reservationRepository.update(any(MeetingRoomReservation.class), any())).thenReturn(1);

        service.rejectReservation(10L, "时间冲突");

        ArgumentCaptor<MeetingRoomReservation> captor = ArgumentCaptor.forClass(MeetingRoomReservation.class);
        verify(reservationRepository).update(captor.capture(), any());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatusEnum.REJECTED.getCode());
        assertThat(captor.getValue().getRejectReason()).isEqualTo("时间冲突");
    }

    @Test
    void should_throw_when_rejectNonPending() {
        MeetingRoomReservation reservation = new MeetingRoomReservation();
        reservation.setId(10L);
        reservation.setStatus(ReservationStatusEnum.CONFIRMED.getCode());
        when(reservationRepository.selectById(10L)).thenReturn(reservation);

        assertThatThrownBy(() -> service.rejectReservation(10L, "重复"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void should_throw_when_deleteNonDeletable() {
        MeetingRoomReservation reservation = new MeetingRoomReservation();
        reservation.setId(10L);
        reservation.setUserId(100L);
        reservation.setStatus(ReservationStatusEnum.CONFIRMED.getCode());
        when(reservationRepository.selectById(10L)).thenReturn(reservation);

        assertThatThrownBy(() -> service.deleteReservation(100L, 10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }

    @Test
    void should_throw_when_cancelAlreadyStarted() {
        MeetingRoomReservation reservation = new MeetingRoomReservation();
        reservation.setId(10L);
        reservation.setUserId(100L);
        reservation.setStatus(ReservationStatusEnum.CONFIRMED.getCode());
        reservation.setStartTime(LocalDateTime.now().minusHours(1));
        when(reservationRepository.selectById(10L)).thenReturn(reservation);

        assertThatThrownBy(() -> service.cancelReservation(100L, 10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_ERROR.getCode()));
    }
}
