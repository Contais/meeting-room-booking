import request from '@/utils/request'
import type { Result } from '@/types/api'
import type { FileBizType, FileUploadVO } from '@/types/file'

/**
 * 上传文件（统一接口，需登录鉴权）
 * @param file 文件
 * @param bizType 业务类型 AVATAR / ROOM_IMAGE
 */
export function uploadFile(file: File | Blob, bizType: FileBizType): Promise<Result<FileUploadVO>> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('bizType', bizType)
  return request.post('/api/platform/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000,
  })
}

/**
 * 删除文件
 * @param objectKey 对象键
 */
export function deleteFile(objectKey: string): Promise<Result<void>> {
  return request.delete('/api/platform/file/delete', { params: { objectKey } })
}
