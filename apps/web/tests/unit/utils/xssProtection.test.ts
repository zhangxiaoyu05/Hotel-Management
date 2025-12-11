import { describe, it, expect } from 'vitest'
import {
  escapeHtml,
  sanitizeHtml,
  isSafeUrl,
  cleanForAttribute,
  cleanUsername,
  containsSuspiciousContent,
  truncateText,
  cleanAndTruncateComment,
  cleanImageUrl,
  createSafeHtml
} from '@/utils/xssProtection'

describe('XSS Protection Utils', () => {
  describe('escapeHtml', () => {
    it('escapes HTML characters', () => {
      expect(escapeHtml('<script>alert("xss")</script>'))
        .toBe('&lt;script&gt;alert(&quot;xss&quot;)&lt;/script&gt;')
    })

    it('handles null and empty strings', () => {
      expect(escapeHtml(null)).toBe(null)
      expect(escapeHtml('')).toBe('')
      expect(escapeHtml(undefined)).toBe(undefined)
    })
  })

  describe('sanitizeHtml', () => {
    it('removes script tags', () => {
      expect(sanitizeHtml('Hello <script>alert("xss")</script> World'))
        .toBe('Hello  World')
    })

    it('allows safe tags', () => {
      expect(sanitizeHtml('Hello <b>bold</b> and <i>italic</i>'))
        .toBe('Hello <b>bold</b> and <i>italic</i>')
    })

    it('removes dangerous attributes', () => {
      expect(sanitizeHtml('<span onclick="alert(1)">Click</span>'))
        .toBe('<span>Click</span>')
    })

    it('removes dangerous protocols', () => {
      expect(sanitizeHtml('<a href="javascript:alert(1)">Link</a>'))
        .toBe('<a>Link</a>')
    })
  })

  describe('isSafeUrl', () => {
    it('allows safe protocols', () => {
      expect(isSafeUrl('https://example.com')).toBe(true)
      expect(isSafeUrl('http://example.com')).toBe(true)
      expect(isSafeUrl('mailto:test@example.com')).toBe(true)
    })

    it('blocks dangerous protocols', () => {
      expect(isSafeUrl('javascript:alert(1)')).toBe(false)
      expect(isSafeUrl('vbscript:msgbox(1)')).toBe(false)
      expect(isSafeUrl('data:text/html,<script>alert(1)</script>')).toBe(false)
      expect(isSafeUrl('file:///etc/passwd')).toBe(false)
    })

    it('handles empty strings', () => {
      expect(isSafeUrl('')).toBe(false)
      expect(isSafeUrl(null as any)).toBe(false)
    })
  })

  describe('cleanForAttribute', () => {
    it('escapes attribute characters', () => {
      expect(cleanForAttribute('test"value'and&other<char'))
        .toBe('test&quot;value&quot;and&#39;&amp;other&lt;char')
    })
  })

  describe('cleanUsername', () => {
    it('keeps safe characters', () => {
      expect(cleanUsername('user_123')).toBe('user_123')
      expect(cleanUsername('test@example.com')).toBe('test@example.com')
      expect(cleanUsername('用户名')).toBe('用户名')
    })

    it('removes special characters', () => {
      expect(cleanUsername('user<script>')).toBe('userscript')
      expect(cleanUsername('user$name')).toBe('username')
    })
  })

  describe('containsSuspiciousContent', () => {
    it('detects dangerous patterns', () => {
      expect(containsSuspiciousContent('<script>alert(1)</script>')).toBe(true)
      expect(containsSuspiciousContent('javascript:alert(1)')).toBe(true)
      expect(containsSuspiciousContent('onclick="alert(1)"')).toBe(true)
      expect(containsSuspiciousContent('expression(alert(1))')).toBe(true)
    })

    it('allows safe content', () => {
      expect(containsSuspiciousContent('This is a safe comment')).toBe(false)
      expect(containsSuspiciousContent('Nice hotel!')).toBe(false)
    })
  })

  describe('truncateText', () => {
    it('truncates long text', () => {
      expect(truncateText('This is a very long text', 10))
        .toBe('This is a ...')
    })

    it('keeps short text unchanged', () => {
      expect(truncateText('Short', 10)).toBe('Short')
    })
  })

  describe('cleanAndTruncateComment', () => {
    it('cleans and truncates comment', () => {
      const comment = '<script>alert(1)</script>This is a very long comment that should be truncated'
      const result = cleanAndTruncateComment(comment, 20)

      expect(result).not.toContain('<script>')
      expect(result).toContain('...')
      expect(result.length).toBeLessThanOrEqual(23) // 20 chars + ...
    })
  })

  describe('cleanImageUrl', () => {
    it('allows safe image URLs', () => {
      expect(cleanImageUrl('https://example.com/image.jpg'))
        .toBe('https://example.com/image.jpg')
    })

    it('blocks dangerous URLs', () => {
      expect(cleanImageUrl('javascript:alert(1)')).toBe(null)
      expect(cleanImageUrl('data:image/svg,<script>alert(1)</script>')).toBe(null)
    })

    it('handles empty URLs', () => {
      expect(cleanImageUrl('')).toBe(null)
      expect(cleanImageUrl(null as any)).toBe(null)
    })
  })

  describe('createSafeHtml', () => {
    it('creates safe HTML object', () => {
      const result = createSafeHtml('<b>Bold</b> <script>alert(1)</script>')
      expect(result).toHaveProperty('__html')
      expect(result.__html).toContain('<b>Bold</b>')
      expect(result.__html).not.toContain('<script>')
    })
  })
})