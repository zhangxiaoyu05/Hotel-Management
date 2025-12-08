/**
 * XSS防护工具
 *
 * 提供前端XSS防护功能，确保用户生成内容安全展示
 */

// 基础白名单 - 允许的HTML标签和属性
const ALLOWED_TAGS = [
  'b', 'i', 'em', 'strong', 'u', 'span', 'br', 'p'
]

const ALLOWED_ATTRIBUTES = {
  'span': ['class'],
  'p': ['class'],
  'br': ['class']
}

// 危险的模式
const DANGEROUS_PATTERNS = [
  /<script[^>]*>.*?<\/script>/gi,
  /javascript:/gi,
  /vbscript:/gi,
  /data:/gi,
  /on\w+\s*=/gi,
  /expression\s*\(/gi,
  /url\s*\(/gi,
  /@import/gi
]

/**
 * 转义HTML字符
 */
export function escapeHtml(text: string): string {
  if (!text) return text

  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

/**
 * 清理HTML，移除危险内容
 */
export function sanitizeHtml(html: string): string {
  if (!html) return html

  // 先移除危险模式
  let cleaned = html
  DANGEROUS_PATTERNS.forEach(pattern => {
    cleaned = cleaned.replace(pattern, '')
  })

  // 使用DOMParser清理
  try {
    const parser = new DOMParser()
    const doc = parser.parseFromString(cleaned, 'text/html')

    // 递归清理节点
    const cleanNode = (node: Node): Node => {
      if (node.nodeType === Node.TEXT_NODE) {
        return node
      }

      if (node.nodeType === Node.ELEMENT_NODE) {
        const element = node as Element

        // 检查是否是允许的标签
        if (!ALLOWED_TAGS.includes(element.tagName.toLowerCase())) {
          // 不允许的标签，替换为纯文本
            const textNode = document.createTextNode(element.textContent || '')
          return textNode
        }

        // 清理属性
        const allowedAttrs = ALLOWED_ATTRIBUTES[element.tagName.toLowerCase()] || []
        for (let i = element.attributes.length - 1; i >= 0; i--) {
          const attr = element.attributes[i]
          if (!allowedAttrs.includes(attr.name.toLowerCase())) {
            element.removeAttribute(attr.name)
          }
        }

        // 递归清理子节点
        const children = Array.from(element.childNodes)
        children.forEach(child => {
          const cleanedChild = cleanNode(child)
          if (cleanedChild !== child) {
            element.replaceChild(cleanedChild, child)
          }
        })

        return element
      }

      return node
    }

    // 清理body内容
    const body = doc.body
    Array.from(body.childNodes).forEach(node => {
      const cleanedNode = cleanNode(node)
      if (cleanedNode !== node) {
        body.replaceChild(cleanedNode, node)
      }
    })

    return body.innerHTML
  } catch (error) {
    console.warn('HTML清理失败，使用转义方式:', error)
    return escapeHtml(html)
  }
}

/**
 * 验证URL是否安全
 */
export function isSafeUrl(url: string): boolean {
  if (!url) return false

  const lowerUrl = url.toLowerCase().trim()

  // 检查危险协议
  const dangerousProtocols = ['javascript:', 'vbscript:', 'data:', 'file:']
  return !dangerousProtocols.some(protocol => lowerUrl.startsWith(protocol))
}

/**
 * 清理用于属性值的文本
 */
export function cleanForAttribute(text: string): string {
  if (!text) return text

  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/**
 * 清理用户名
 */
export function cleanUsername(username: string): string {
  if (!username) return username

  // 保留字母、数字、中文和基本符号
  return username.replace(/[^\w\u4e00-\u9fa5@._-]/g, '')
}

/**
 * 检查文本是否包含可疑内容
 */
export function containsSuspiciousContent(text: string): boolean {
  if (!text) return false

  const lowerText = text.toLowerCase()

  // 检查危险模式
  return DANGEROUS_PATTERNS.some(pattern => pattern.test(lowerText))
}

/**
 * 截断文本
 */
export function truncateText(text: string, maxLength: number): string {
  if (!text || text.length <= maxLength) return text

  return text.substring(0, maxLength) + '...'
}

/**
 * 清理并截断评价内容
 */
export function cleanAndTruncateComment(comment: string, maxLength: number = 500): string {
  if (!comment) return comment

  // 先转义HTML
  const escaped = escapeHtml(comment)

  // 再截断长度
  return truncateText(escaped, maxLength)
}

/**
 * 清理图片URL
 */
export function cleanImageUrl(url: string): string | null {
  if (!url) return null

  // 验证URL安全
  if (!isSafeUrl(url)) {
    return null
  }

  return url
}

/**
 * 创建安全的HTML内容
 */
export function createSafeHtml(content: string): { __html: string } {
  return {
    __html: sanitizeHtml(content)
  }
}

/**
 * Vue指令：v-safe-html
 */
export const SafeHtmlDirective = {
  mounted(el: HTMLElement, binding: any) {
    el.innerHTML = sanitizeHtml(binding.value || '')
  },
  updated(el: HTMLElement, binding: any) {
    el.innerHTML = sanitizeHtml(binding.value || '')
  }
}