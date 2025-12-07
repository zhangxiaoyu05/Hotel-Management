<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  images: string[]
}

const props = defineProps<Props>()

const currentImageIndex = ref(0)

function nextImage() {
  currentImageIndex.value = (currentImageIndex.value + 1) % props.images.length
}

function prevImage() {
  currentImageIndex.value = currentImageIndex.value === 0
    ? props.images.length - 1
    : currentImageIndex.value - 1
}

function selectImage(index: number) {
  currentImageIndex.value = index
}
</script>

<template>
  <div class="image-gallery">
    <div class="main-image-container">
      <img
        :src="images[currentImageIndex]"
        :alt="`酒店图片 ${currentImageIndex + 1}`"
        class="main-image"
      />
      <div v-if="images.length > 1" class="image-controls">
        <button @click="prevImage" class="control-btn prev-btn">
          <i class="el-icon-arrow-left"></i>
        </button>
        <button @click="nextImage" class="control-btn next-btn">
          <i class="el-icon-arrow-right"></i>
        </button>
      </div>
      <div class="image-counter">
        {{ currentImageIndex + 1 }} / {{ images.length }}
      </div>
    </div>

    <!-- Thumbnails -->
    <div v-if="images.length > 1" class="thumbnails">
      <div
        v-for="(image, index) in images"
        :key="index"
        class="thumbnail"
        :class="{ active: currentImageIndex === index }"
        @click="selectImage(index)"
      >
        <img
          :src="image"
          :alt="`缩略图 ${index + 1}`"
          class="thumbnail-image"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.image-gallery {
  width: 100%;
}

.main-image-container {
  position: relative;
  width: 100%;
  aspect-ratio: 16/9;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
}

.main-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: opacity 0.3s;
}

.image-controls {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  opacity: 0;
  transition: opacity 0.3s;
}

.main-image-container:hover .image-controls {
  opacity: 1;
}

.control-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: background-color 0.3s;
}

.control-btn:hover {
  background: rgba(0, 0, 0, 0.8);
}

.prev-btn {
  transform: translateX(-10px);
}

.next-btn {
  transform: translateX(10px);
}

.main-image-container:hover .prev-btn,
.main-image-container:hover .next-btn {
  transform: translateX(0);
}

.image-counter {
  position: absolute;
  bottom: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.thumbnails {
  display: flex;
  gap: 10px;
  margin-top: 15px;
  overflow-x: auto;
  padding: 5px 0;
}

.thumbnails::-webkit-scrollbar {
  height: 6px;
}

.thumbnails::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.thumbnails::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.thumbnails::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.thumbnail {
  flex-shrink: 0;
  width: 80px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.3s;
  opacity: 0.7;
}

.thumbnail:hover {
  opacity: 1;
  transform: scale(1.05);
}

.thumbnail.active {
  border-color: #409eff;
  opacity: 1;
}

.thumbnail-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

@media (max-width: 768px) {
  .main-image-container {
    aspect-ratio: 4/3;
  }

  .control-btn {
    width: 35px;
    height: 35px;
    font-size: 14px;
  }

  .thumbnails {
    gap: 8px;
  }

  .thumbnail {
    width: 60px;
    height: 45px;
  }
}
</style>