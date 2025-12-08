<template>
  <el-dialog
    v-model="dialogVisible"
    title="评价回复"
    width="60%"
    :before-close="handleClose"
    destroy-on-close
  >
    <div v-if="review" class="reply-content">
      <!-- 原评价信息 -->
      <div class="original-review mb-6">
        <div class="bg-gray-50 rounded-lg p-4">
          <div class="flex items-center justify-between mb-3">
            <div class="flex items-center space-x-3">
              <el-rate
                v-model="review.overallRating"
                disabled
                show-score
                text-color="#ff9900"
                score-template="{value}"
              />
              <span class="text-sm text-gray-600">
                {{ formatDate(review.createdAt) }}
              </span>
            </div>
          </div>
          <p class="text-gray-800 leading-relaxed">
            {{ review.comment }}
          </p>
        </div>
      </div>

      <!-- 回复表单 -->
      <div class="reply-form">
        <el-form
          ref="formRef"
          :model="replyForm"
          :rules="formRules"
          label-width="80px"
        >
          <el-form-item label="回复内容" prop="content">
            <div class="reply-editor">
              <!-- 富文本编辑器工具栏 -->
              <div class="editor-toolbar mb-2">
                <el-button-group size="small">
                  <el-button @click="insertText('[用户您好]')">
                    用户您好
                  </el-button>
                  <el-button @click="insertText('[感谢您的评价]')">
                    感谢评价
                  </el-button>
                  <el-button @click="insertText('[我们会改进]')">
                    改进服务
                  </el-button>
                  <el-button @click="insertText('[期待再次光临]')">
                    再次光临
                  </el-button>
                </el-button-group>
              </div>

              <el-input
                v-model="replyForm.content"
                type="textarea"
                :rows="6"
                placeholder="请输入回复内容..."
                maxlength="1000"
                show-word-limit
                resize="vertical"
              />

              <!-- 快捷模板 -->
              <div class="reply-templates mt-3">
                <el-collapse>
                  <el-collapse-item title="快捷回复模板" name="templates">
                    <div class="grid grid-cols-1 gap-2">
                      <div
                        v-for="(template, index) in replyTemplates"
                        :key="index"
                        class="template-item p-3 bg-gray-50 rounded cursor-pointer hover:bg-gray-100"
                        @click="useTemplate(template)"
                      >
                        <div class="font-medium text-sm mb-1">{{ template.title }}</div>
                        <div class="text-xs text-gray-600">{{ template.content.substring(0, 50) }}...</div>
                      </div>
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="发布状态" prop="status">
            <el-radio-group v-model="replyForm.status">
              <el-radio label="DRAFT">
                <div class="flex items-center">
                  <el-icon class="mr-1"><Edit /></el-icon>
                  保存草稿
                </div>
              </el-radio>
              <el-radio label="PUBLISHED">
                <div class="flex items-center">
                  <el-icon class="mr-1"><View /></el-icon>
                  立即发布
                </div>
              </el-radio>
            </el-radio-group>

            <div class="mt-2 text-sm text-gray-600">
              <p v-if="replyForm.status === 'DRAFT'">
                草稿状态：回复内容保存但用户不可见，可以稍后编辑发布
              </p>
              <p v-else>
                发布状态：回复内容将立即显示给用户
              </p>
            </div>
          </el-form-item>

          <!-- 已有回复列表 -->
          <div v-if="existingReplies.length > 0" class="existing-replies">
            <el-divider content-position="left">已有回复</el-divider>
            <div class="space-y-3">
              <div
                v-for="reply in existingReplies"
                :key="reply.id"
                class="bg-white border rounded-lg p-4"
              >
                <div class="flex items-center justify-between mb-2">
                  <div class="flex items-center space-x-2">
                    <el-tag :type="reply.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
                      {{ reply.status === 'PUBLISHED' ? '已发布' : '草稿' }}
                    </el-tag>
                    <span class="text-sm text-gray-600">
                      管理员 {{ reply.adminName }}
                    </span>
                  </div>
                  <div class="flex items-center space-x-2">
                    <span class="text-sm text-gray-500">
                      {{ formatDate(reply.createdAt) }}
                    </span>
                    <el-button
                      size="small"
                      text
                      @click="editReply(reply)"
                    >
                      编辑
                    </el-button>
                    <el-button
                      size="small"
                      text
                      type="danger"
                      @click="deleteReply(reply.id)"
                    >
                      删除
                    </el-button>
                  </div>
                </div>
                <p class="text-gray-800 whitespace-pre-wrap">{{ reply.content }}</p>
              </div>
            </div>
          </div>
        </el-form>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button
          type="primary"
          @click="handleSubmit"
          :loading="submitting"
          :disabled="!replyForm.content.trim()"
        >
          {{ replyForm.status === 'DRAFT' ? '保存草稿' : '发布回复' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  Edit,
  View
} from '@element-plus/icons-vue'
import type { ReviewResponse, ReviewReplyRequest, ReviewReplyResponse } from '@/services/reviewService'
import reviewService from '@/services/reviewService'

interface Props {
  visible: boolean
  review: ReviewResponse | null
  reply: ReviewReplyResponse | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  confirm: [request: ReviewReplyRequest]
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const existingReplies = ref<ReviewReplyResponse[]>([])

// 表单数据
const replyForm = reactive({
  content: '',
  status: 'PUBLISHED'
})

// 表单验证规则
const formRules: FormRules = {
  content: [
    { required: true, message: '请输入回复内容', trigger: 'blur' },
    { min: 5, message: '回复内容至少需要5个字符', trigger: 'blur' },
    { max: 1000, message: '回复内容不能超过1000字符', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择发布状态', trigger: 'change' }
  ]
}

// 回复模板
const replyTemplates = ref([
  {
    title: '感谢评价模板',
    content: '尊敬的用户，非常感谢您抽出宝贵时间对我们的服务进行评价。您的认可是我们前进的动力，我们将继续努力为您提供更优质的服务体验。'
  },
  {
    title: '问题改进模板',
    content: '感谢您的反馈，我们已经注意到您提到的问题，并将立即进行改进。如果您还有其他建议或需要协助的地方，请随时联系我们的客服团队。'
  },
  {
    title: '期待光临模板',
    content: '感谢您的选择和信任，期待您的再次光临。我们将一如既往地为您提供舒适贴心的服务，祝您生活愉快！'
  },
  {
    title: '节日祝福模板',
    content: '值此佳节之际，祝您节日快乐！感谢您选择我们的酒店，我们将为您提供一个温馨舒适的住宿体验。'
  }
])

// 计算属性
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

// 监听review变化
watch(() => props.review, async (newReview) => {
  if (newReview && props.visible) {
    await loadExistingReplies(newReview.id)
    replyForm.content = ''
    replyForm.status = 'PUBLISHED'
  }
}, { immediate: true })

// 方法定义
const loadExistingReplies = async (reviewId: number) => {
  try {
    const response = await reviewService.getReviewReplies(reviewId)
    if (response.success) {
      existingReplies.value = response.data
    }
  } catch (error) {
    console.error('加载已有回复失败:', error)
  }
}

const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const insertText = (text: string) => {
  const textarea = document.querySelector('textarea') as HTMLTextAreaElement
  if (textarea) {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const beforeText = replyForm.content.substring(0, start)
    const afterText = replyForm.content.substring(end)
    replyForm.content = beforeText + text + afterText

    // 重新聚焦并设置光标位置
    setTimeout(() => {
      textarea.focus()
      textarea.setSelectionRange(start + text.length, start + text.length)
    }, 0)
  }
}

const useTemplate = (template: any) => {
  replyForm.content = template.content
}

const editReply = async (reply: ReviewReplyResponse) => {
  replyForm.content = reply.content
  replyForm.status = reply.status
}

const deleteReply = async (replyId: number) => {
  if (!props.review) return

  try {
    await ElMessageBox.confirm(
      '确定要删除这条回复吗？',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await reviewService.deleteReviewReply(props.review.id, replyId)
    if (response.success) {
      ElMessage.success('删除成功')
      await loadExistingReplies(props.review.id)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除回复失败:', error)
      ElMessage.error('删除回复失败')
    }
  }
}

const handleClose = () => {
  dialogVisible.value = false
}

const handleSubmit = async () => {
  if (!props.review) return

  try {
    await formRef.value?.validate()

    submitting.value = true

    const request: ReviewReplyRequest = {
      content: replyForm.content.trim(),
      status: replyForm.status as any
    }

    // 如果是立即发布，需要确认
    if (request.status === 'PUBLISHED') {
      try {
        await ElMessageBox.confirm(
          '确定要立即发布这条回复吗？发布后将显示给所有用户。',
          '确认发布',
          {
            confirmButtonText: '确定发布',
            cancelButtonText: '取消',
            type: 'info'
          }
        )
      } catch (error) {
        if (error === 'cancel') {
          return
        }
      }
    }

    emit('confirm', request)
    dialogVisible.value = false

    ElMessage.success(
      request.status === 'PUBLISHED' ? '回复发布成功' : '草稿保存成功'
    )
  } catch (error) {
    console.error('表单验证失败:', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.reply-content {
  max-height: 60vh;
  overflow-y: auto;
}

.reply-editor {
  width: 100%;
}

.editor-toolbar {
  border: 1px solid #dcdfe6;
  border-bottom: none;
  padding: 8px;
  background-color: #fafafa;
  border-radius: 4px 4px 0 0;
}

.template-item {
  transition: all 0.2s;
}

.template-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.existing-replies {
  margin-top: 24px;
}

:deep(.el-textarea__inner) {
  border-top-left-radius: 0;
  border-top-right-radius: 0;
}

:deep(.el-radio) {
  margin-right: 24px;
  margin-bottom: 8px;
}

:deep(.el-radio:last-child) {
  margin-right: 0;
}
</style>