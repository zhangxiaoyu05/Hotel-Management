<template>
  <div class="room-image-gallery">
    <div class="gallery-header" v-if="title">
      <h4>{{ title }}</h4>
      <div class="gallery-actions" v-if="!readonly && images.length > 0">
        <el-button
          type="danger"
          size="small"
          text
          @click="clearAllImages"
        >
          清空所有
        </el-button>
      </div>
    </div>

    <!-- 图片上传区域 -->
    <div class="upload-area" v-if="!readonly">
      <el-upload
        ref="uploadRef"
        :action="uploadUrl"
        :headers="uploadHeaders"
        :before-upload="beforeUpload"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :on-remove="handleRemove"
        :file-list="fileList"
        :multiple="true"
        :limit="maxImages"
        :accept="acceptTypes"
        list-type="picture-card"
        :drag="true"
        :show-file-list="false"
      >
        <div class="upload-trigger">
          <el-icon size="24"><Plus /></el-icon>
          <div class="upload-text">
            <div>点击上传或拖拽文件</div>
            <div class="upload-hint">支持 {{ acceptTypes }}，单文件不超过 {{ formatFileSize(maxFileSize) }}</div>
          </div>
        </div>
      </el-upload>
    </div>

    <!-- 图片展示区域 -->
    <div class="gallery-content" v-if="images.length > 0 || fileList.length > 0">
      <draggable
        v-model="displayImages"
        tag="div"
        class="image-grid"
        :item-key="getImageKey"
        handle=".drag-handle"
        @end="handleDragEnd"
        v-if="!readonly"
      >
        <template #item="{ element: item, index }">
          <div class="image-item" :key="getImageKey(item, index)">
            <div class="image-wrapper">
              <img
                :src="getImageUrl(item)"
                :alt="`图片 ${index + 1}`"
                @click="previewImage(item, index)"
              />
              <div class="image-overlay">
                <el-button
                  type="primary"
                  size="small"
                  circle
                  @click="previewImage(item, index)"
                >
                  <el-icon><View /></el-icon>
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  circle
                  @click="removeImage(index)"
                  v-if="!readonly"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <div class="drag-handle" v-if="!readonly">
                <el-icon><Rank /></el-icon>
              </div>
            </div>
          </div>
        </template>
      </draggable>

      <!-- 只读模式 -->
      <div class="image-grid" v-else>
        <div
          class="image-item"
          v-for="(item, index) in displayImages"
          :key="getImageKey(item, index)"
        >
          <div class="image-wrapper">
            <img
              :src="getImageUrl(item)"
              :alt="`图片 ${index + 1}`"
              @click="previewImage(item, index)"
            />
            <div class="image-overlay">
              <el-button
                type="primary"
                size="small"
                circle
                @click="previewImage(item, index)"
              >
                <el-icon><View /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty
      description="暂无图片"
      :image-size="80"
      v-else-if="!loading"
    />

    <!-- 加载状态 -->
    <div class="loading-state" v-if="loading">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- 图片预览 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewUrls"
      :initial-index="previewIndex"
      @close="closePreview"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, View, Delete, Rank } from '@element-plus/icons-vue';
import draggable from 'vuedraggable';
import type { UploadFile, UploadProps } from 'element-plus';

interface Props {
  modelValue: string[];
  readonly?: boolean;
  title?: string;
  maxImages?: number;
  maxFileSize?: number; // MB
  acceptTypes?: string;
}

interface Emits {
  (e: 'update:modelValue', value: string[]): void;
  (e: 'change', value: string[]): void;
}

const props = withDefaults(defineProps<Props>(), {
  readonly: false,
  title: '',
  maxImages: 10,
  maxFileSize: 5,
  acceptTypes: 'image/jpeg,image/jpg,image/png,image/webp'
});

const emit = defineEmits<Emits>();

// 响应式数据
const uploadRef = ref();
const fileList = ref<UploadFile[]>([]);
const loading = ref(false);
const previewVisible = ref(false);
const previewIndex = ref(0);
const uploadUrl = '/api/files/upload';
const uploadHeaders = ref({});

// 计算属性
const images = computed({
  get: () => props.modelValue || [],
  set: (value) => {
    emit('update:modelValue', value);
    emit('change', value);
  }
});

const displayImages = computed(() => {
  // 合并已上传的图片和正在上传的文件
  const result = [...images.value];

  // 添加正在上传的文件
  fileList.value.forEach(file => {
    if (file.url && !result.includes(file.url)) {
      result.push(file.url);
    }
  });

  return result;
});

const previewUrls = computed(() => {
  return displayImages.value.map(item => getImageUrl(item));
});

// 方法
const getImageKey = (item: any, index?: number) => {
  if (typeof item === 'string') {
    return `image-${item}`;
  }
  return `file-${index || item.uid}`;
};

const getImageUrl = (item: any) => {
  if (typeof item === 'string') {
    return item;
  }
  if (item.url) {
    return item.url;
  }
  return '';
};

const formatFileSize = (size: number) => {
  return size < 1024 ? `${size}MB` : `${(size / 1024).toFixed(1)}GB`;
};

const beforeUpload = (file: File) => {
  // 检查文件类型
  const isValidType = props.acceptTypes.split(',').includes(file.type);
  if (!isValidType) {
    ElMessage.error('文件格式不正确');
    return false;
  }

  // 检查文件大小
  const isValidSize = file.size / 1024 / 1024 < props.maxFileSize;
  if (!isValidSize) {
    ElMessage.error(`文件大小不能超过 ${props.maxFileSize}MB`);
    return false;
  }

  // 检查数量限制
  const totalImages = images.value.length + fileList.value.length;
  if (totalImages >= props.maxImages) {
    ElMessage.error(`最多只能上传 ${props.maxImages} 张图片`);
    return false;
  }

  loading.value = true;
  return true;
};

const handleUploadSuccess = (response: any, file: UploadFile) => {
  loading.value = false;

  if (response.success && response.data?.url) {
    const newImages = [...images.value, response.data.url];
    images.value = newImages;

    // 从文件列表中移除已上传的文件
    const index = fileList.value.findIndex(f => f.uid === file.uid);
    if (index > -1) {
      fileList.value.splice(index, 1);
    }

    ElMessage.success('图片上传成功');
  } else {
    ElMessage.error(response.message || '图片上传失败');
  }
};

const handleUploadError = (error: any) => {
  loading.value = false;
  console.error('图片上传失败:', error);
  ElMessage.error('图片上传失败');
};

const handleRemove = (file: UploadFile) => {
  const index = fileList.value.findIndex(f => f.uid === file.uid);
  if (index > -1) {
    fileList.value.splice(index, 1);
  }
};

const removeImage = async (index: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这张图片吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });

    const newImages = [...images.value];
    newImages.splice(index, 1);
    images.value = newImages;

    ElMessage.success('图片删除成功');
  } catch {
    // 用户取消删除
  }
};

const clearAllImages = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要清空所有 ${images.value.length} 张图片吗？`,
      '确认清空',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );

    images.value = [];
    fileList.value = [];
    ElMessage.success('所有图片已清空');
  } catch {
    // 用户取消清空
  }
};

const handleDragEnd = () => {
  // 更新图片顺序
  images.value = displayImages.value.filter(item => typeof item === 'string');
};

const previewImage = (item: any, index: number) => {
  previewIndex.value = index;
  previewVisible.value = true;
};

const closePreview = () => {
  previewVisible.value = false;
};

// 监听器
watch(() => props.modelValue, (newValue) => {
  if (newValue !== images.value) {
    images.value = newValue || [];
  }
}, { immediate: true });
</script>

<style scoped>
.room-image-gallery {
  width: 100%;
}

.gallery-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.gallery-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.upload-area {
  margin-bottom: 16px;
}

.upload-trigger {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
}

.upload-text {
  text-align: center;
}

.upload-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.gallery-content {
  min-height: 120px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}

.image-item {
  position: relative;
  aspect-ratio: 1;
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
}

.image-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.image-wrapper:hover img {
  transform: scale(1.05);
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.image-wrapper:hover .image-overlay {
  opacity: 1;
}

.drag-handle {
  position: absolute;
  top: 4px;
  right: 4px;
  background-color: rgba(0, 0, 0, 0.6);
  color: white;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 2px;
  cursor: move;
  font-size: 12px;
}

.loading-state {
  padding: 16px;
}

:deep(.el-upload--picture-card) {
  width: 100%;
  height: auto;
  min-height: 120px;
}
</style>