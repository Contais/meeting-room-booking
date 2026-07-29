/** 文件业务类型 */
export type FileBizType = 'AVATAR' | 'ROOM_IMAGE'

/** 文件上传响应 */
export interface FileUploadVO {
  /** 可直接用于 <img src> 的访问 URL */
  url: string
  /** 对象键（删除时回传） */
  objectKey: string
  /** 业务类型 */
  bizType: string
  /** 原始文件名 */
  originalName: string
  /** 文件大小（字节） */
  size: number
}
