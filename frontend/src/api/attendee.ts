import request from '@/utils/request'
import type { Result } from '@/types/api'
import type { Attendee } from '@/types/reservation'

/** 邀请参会人（按用户ID列表），返回实际新增数量 */
export function inviteAttendees(reservationId: number, userIds: number[]): Promise<Result<number>> {
  return request.post(`/api/meeting/reservation/attendee/${reservationId}/invite`, { userIds })
}

/** 按部门邀请参会人，返回实际新增数量 */
export function inviteDepartment(reservationId: number, departmentId: number): Promise<Result<number>> {
  return request.post(`/api/meeting/reservation/attendee/${reservationId}/invite-department`, { departmentId })
}

/** 查询预约的参会人列表 */
export function listAttendees(reservationId: number): Promise<Result<Attendee[]>> {
  return request.get(`/api/meeting/reservation/attendee/${reservationId}/list`)
}

/** 移除参会人 */
export function removeAttendee(reservationId: number, userId: number): Promise<Result<void>> {
  return request.delete(`/api/meeting/reservation/attendee/${reservationId}/${userId}`)
}

/** 参会人响应邀请（更新自己的参会状态: 1-已接受, 2-已拒绝） */
export function respondInvitation(reservationId: number, status: number): Promise<Result<void>> {
  return request.put(`/api/meeting/reservation/attendee/${reservationId}/respond`, { status })
}
