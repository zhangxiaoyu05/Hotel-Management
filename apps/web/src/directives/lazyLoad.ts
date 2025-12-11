import type { Directive } from 'vue'

interface LazyLoadElement extends HTMLElement {
  _lazyLoadObserver?: IntersectionObserver
  _lazyLoadSrc?: string
}

const lazyLoad: Directive<LazyLoadElement, string> = {
  mounted(el, binding) {
    el._lazyLoadSrc = binding.value

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const img = entry.target as HTMLImageElement
            const src = img._lazyLoadSrc

            if (src) {
              // 创建临时图片对象预加载
              const tempImg = new Image()
              tempImg.onload = () => {
                img.src = src
                img.classList.add('lazy-loaded')
                observer.unobserve(img)
              }
              tempImg.onerror = () => {
                img.classList.add('lazy-error')
                observer.unobserve(img)
              }
              tempImg.src = src
            }
          }
        })
      },
      {
        rootMargin: '50px', // 提前50px开始加载
        threshold: 0.1
      }
    )

    el._lazyLoadObserver = observer
    observer.observe(el)

    // 添加加载状态样式
    el.classList.add('lazy-loading')
  },

  updated(el, binding) {
    // 如果图片源发生变化，更新并重新观察
    if (el._lazyLoadSrc !== binding.value) {
      el._lazyLoadSrc = binding.value
      el.classList.remove('lazy-loaded', 'lazy-error')
      el.classList.add('lazy-loading')

      // 重新开始观察
      if (el._lazyLoadObserver) {
        el._lazyLoadObserver.unobserve(el)
        el._lazyLoadObserver.observe(el)
      }
    }
  },

  unmounted(el) {
    // 清理观察器
    if (el._lazyLoadObserver) {
      el._lazyLoadObserver.disconnect()
    }
  }
}

export default lazyLoad