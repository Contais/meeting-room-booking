/**
 * 判断头像字段是否为图片 URL（而非旧的图标 JSON 配置）。
 * 头像字段兼容两种格式：
 *  1. 图片 URL：以 http(s):// 或 /api/ 开头（文件上传后的地址）
 *  2. 图标 JSON：{ icon, gradient } 旧格式
 */
export function isAvatarUrl(avatar: string | undefined | null): boolean {
  if (!avatar) return false
  return /^(https?:\/\/|\/api\/)/i.test(avatar)
}
