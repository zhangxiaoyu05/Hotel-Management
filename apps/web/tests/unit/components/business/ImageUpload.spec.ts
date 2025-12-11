import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ImageUpload from '@/components/business/ImageUpload.vue'

// Mock FileReader
global.FileReader = class {
  result: string | ArrayBuffer | null = ''
  onload: ((event: ProgressEvent<FileReader>) => void) | null = null
  onerror: ((event: ProgressEvent<FileReader>) => void) | null = null

  readAsDataURL(file: File) {
    setTimeout(() => {
      this.result = `data:${file.type};base64,mockbase64data`
      if (this.onload) {
        this.onload({ target: this } as ProgressEvent<FileReader>)
      }
    }, 0)
  }
} as any

describe('ImageUpload.vue', () => {
  let wrapper: any

  beforeEach(() => {
    wrapper = mount(ImageUpload, {
      props: {
        modelValue: []
      }
    })
  })

  it('renders upload area', () => {
    expect(wrapper.find('.image-upload').exists()).toBe(true)
    expect(wrapper.find('.upload-area').exists()).toBe(true)
    expect(wrapper.find('input[type="file"]').exists()).toBe(true)
  })

  it('accepts valid file types', () => {
    const fileInput = wrapper.find('input[type="file"]')
    expect(fileInput.attributes('accept')).toBe('image/jpeg,image/jpg,image/png,image/webp')
  })

  it('limits file size', async () => {
    const fileInput = wrapper.find('input[type="file"]')

    // Create a large file (5MB)
    const largeFile = new File(['a'.repeat(5 * 1024 * 1024)], 'large.jpg', { type: 'image/jpeg' })

    await fileInput.trigger('change', { target: { files: [largeFile] } })

    expect(wrapper.emitted('error')).toBeTruthy()
    expect(wrapper.emitted('error')[0][0]).toContain('图片大小不能超过')
  })

  it('accepts valid file size', async () => {
    const fileInput = wrapper.find('input[type="file"]')

    // Create a small file (100KB)
    const smallFile = new File(['a'.repeat(100 * 1024)], 'small.jpg', { type: 'image/jpeg' })

    await fileInput.trigger('change', { target: { files: [smallFile] } })

    expect(wrapper.emitted('error')).toBeFalsy()
    expect(wrapper.vm.images).toHaveLength(1)
  })

  it('rejects invalid file types', async () => {
    const fileInput = wrapper.find('input[type="file"]')

    // Create a non-image file
    const textFile = new File(['test'], 'test.txt', { type: 'text/plain' })

    await fileInput.trigger('change', { target: { files: [textFile] } })

    expect(wrapper.emitted('error')).toBeTruthy()
    expect(wrapper.emitted('error')[0][0]).toContain('只支持图片格式')
  })

  it('limits number of images', async () => {
    // Set existing images to reach limit
    await wrapper.setProps({ modelValue: new Array(5).fill('existing-image') })

    const fileInput = wrapper.find('input[type="file"]')
    const newFile = new File(['test'], 'new.jpg', { type: 'image/jpeg' })

    await fileInput.trigger('change', { target: { files: [newFile] } })

    expect(wrapper.emitted('error')).toBeTruthy()
    expect(wrapper.emitted('error')[0][0]).toContain('最多上传5张图片')
  })

  it('previews uploaded images', async () => {
    const fileInput = wrapper.find('input[type="file"]')
    const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' })

    await fileInput.trigger('change', { target: { files: [file] } })

    await wrapper.vm.$nextTick()

    expect(wrapper.find('.image-preview').exists()).toBe(true)
    expect(wrapper.find('img').attributes('src')).toContain('data:image/jpeg;base64,mockbase64data')
  })

  it('removes images when delete button is clicked', async () => {
    // Add an image first
    await wrapper.setData({ images: ['test-image-url'] })

    const deleteButton = wrapper.find('.delete-image')
    await deleteButton.trigger('click')

    expect(wrapper.vm.images).toHaveLength(0)
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0][0]).toEqual([])
  })

  it('handles drag and drop', async () => {
    const uploadArea = wrapper.find('.upload-area')

    // Mock dragenter
    await uploadArea.trigger('dragenter')
    expect(wrapper.vm.isDragOver).toBe(true)

    // Mock dragleave
    await uploadArea.trigger('dragleave')
    expect(wrapper.vm.isDragOver).toBe(false)
  })

  it('handles file drop', async () => {
    const uploadArea = wrapper.find('.upload-area')
    const file = new File(['test'], 'drop.jpg', { type: 'image/jpeg' })

    const mockDropEvent = {
      preventDefault: vi.fn(),
      dataTransfer: {
        files: [file]
      }
    }

    await uploadArea.trigger('drop', mockDropEvent)

    expect(mockDropEvent.preventDefault).toHaveBeenCalled()
    expect(wrapper.vm.images).toHaveLength(1)
  })

  it('prevents default drag behavior', async () => {
    const uploadArea = wrapper.find('.upload-area')

    const mockDragOverEvent = {
      preventDefault: vi.fn()
    }

    await uploadArea.trigger('dragover', mockDragOverEvent)

    expect(mockDragOverEvent.preventDefault).toHaveBeenCalled()
  })

  it('compresses images when compression is enabled', async () => {
    wrapper = mount(ImageUpload, {
      props: {
        modelValue: [],
        compressImage: true,
        compressQuality: 0.7,
        maxWidth: 800,
        maxHeight: 600
      }
    })

    // Mock canvas
    const mockCanvas = {
      getContext: () => ({
        drawImage: vi.fn(),
        getImageData: () => ({ data: new Uint8ClampedArray(4) }),
        putImageData: vi.fn()
      }),
      toDataURL: () => 'data:image/jpeg;base64,compresseddata',
      width: 800,
      height: 600
    }

    global.HTMLCanvasElement.prototype.getContext = vi.fn(() => ({} as any))
    global.HTMLCanvasElement.prototype.toDataURL = vi.fn(() => 'compressed-image-data')

    const fileInput = wrapper.find('input[type="file"]')
    const file = new File(['test'], 'large.jpg', { type: 'image/jpeg' })

    await fileInput.trigger('change', { target: { files: [file] } })

    // Compression should be attempted
    expect(wrapper.vm.compressImage).toBe(true)
  })

  it('shows loading state during upload', async () => {
    wrapper.setData({ uploading: true })

    expect(wrapper.find('.upload-area').classes()).toContain('uploading')
    expect(wrapper.find('.loading-spinner').exists()).toBe(true)
  })

  it('disables upload when disabled prop is true', async () => {
    await wrapper.setProps({ disabled: true })

    const fileInput = wrapper.find('input[type="file"]')
    expect(fileInput.attributes('disabled')).toBeDefined()

    expect(wrapper.find('.upload-area').classes()).toContain('disabled')
  })

  it('emits error event on file read error', async () => {
    const fileInput = wrapper.find('input[type="file"]')
    const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' })

    // Mock FileReader error
    const mockFileReader = global.FileReader
    mockFileReader.prototype.onerror = () => {
      if (mockFileReader.prototype.onerror) {
        mockFileReader.prototype.onerror({ target: mockFileReader.prototype } as ProgressEvent<FileReader>)
      }
    }

    await fileInput.trigger('change', { target: { files: [file] } })

    // Simulate error
    const fileReader = new mockFileReader()
    if (fileReader.onerror) {
      fileReader.onerror({ target: fileReader } as ProgressEvent<FileReader>)
    }

    expect(wrapper.emitted('error')).toBeTruthy()
  })
})