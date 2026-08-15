import { pinyin } from 'pinyin-pro'
import type { UserInfo } from '@/types/user'

/**
 * 用户拼音检索工具
 * <p>
 * 支持三种输入命中同一目标：中文姓名「张三」、全拼「zhangsan」、首字母「zs」，
 * 兼容用户名 / 手机号 / 邮箱子串匹配。姓名拼音按字符串缓存，避免重复转换。
 * </p>
 */

interface NamePinyin {
  full: string
  initials: string
}

const pinyinCache = new Map<string, NamePinyin>()

function getNamePinyin(name: string): NamePinyin {
  let cached = pinyinCache.get(name)
  if (!cached) {
    cached = {
      full: pinyin(name, { toneType: 'none', type: 'array', nonZh: 'consecutive' }).join(''),
      initials: pinyin(name, { pattern: 'first', toneType: 'none', type: 'array', nonZh: 'consecutive' }).join('')
    }
    pinyinCache.set(name, cached)
  }
  return cached
}

/** 用户是否匹配关键字（姓名/拼音全拼/拼音首字母/用户名/手机号/邮箱） */
export function matchUserByKeyword(user: UserInfo, keyword: string): boolean {
  const kw = keyword.trim().toLowerCase()
  if (!kw) return true
  const name = user.realName || ''
  const { full, initials } = name ? getNamePinyin(name) : { full: '', initials: '' }
  return (
    name.toLowerCase().includes(kw) ||
    full.includes(kw) ||
    initials.includes(kw) ||
    (user.username || '').toLowerCase().includes(kw) ||
    (user.phone || '').includes(keyword.trim()) ||
    (user.email || '').toLowerCase().includes(kw)
  )
}
