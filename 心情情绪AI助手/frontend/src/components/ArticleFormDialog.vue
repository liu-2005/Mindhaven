<template>
  <el-dialog
    :model-value="modelValue"
    :title="type === 'add' ? '新增文章' : '编辑文章'"
    width="820px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:modelValue', $event)"
    @closed="resetForm"
  >
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入标题" maxlength="200" show-word-limit />
      </el-form-item>
      <el-form-item label="所属分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%;">
          <el-option
            v-for="item in categoryList"
            :key="item.id"
            :label="item.name || item.categoryName || item.title || item.label || item.text || `分类${item.id}`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="封面图片">
        <el-upload
          class="cover-uploader"
          :show-file-list="false"
          :before-upload="handleCoverBeforeUpload"
          accept="image/*"
        >
          <img v-if="form.coverImage" :src="form.coverImage" class="cover-preview" alt="封面预览" />
          <div v-else class="cover-uploader-placeholder">
            <el-icon><Plus /></el-icon>
            <span>点击上传图片</span>
          </div>
        </el-upload>
        <el-button v-if="form.coverImage" link type="danger" size="small" @click="clearCover" style="margin-top: 8px;">移除</el-button>
      </el-form-item>
      <el-form-item label="文章摘要">
        <el-input v-model="form.summary" type="textarea" :rows="4" placeholder="选填,请输入摘要" maxlength="1000" show-word-limit />
      </el-form-item>
      <el-form-item label="标签">
        <el-select v-model="form.tags" multiple placeholder="选填,请选择标签" style="width: 100%;">
          <el-option v-for="tag in commonTags" :key="tag" :label="tag" :value="tag" />
        </el-select>
      </el-form-item>
      <el-form-item label="文章内容" prop="content">
        <RichTextEditor
          v-model="form.content"
          :business-id="form.id || '0'"
          style="width: 100%;"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import RichTextEditor from '@/components/RichTextEditor.vue'
import { addArticle, updateArticle } from '@/api/knowledge'
import { uploadFile } from '@/api/file'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  type: { type: String, default: 'add' },
  article: { type: Object, default: () => ({}) },
  categoryList: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:modelValue', 'submit-success'])

const commonTags = [
  '情绪管理', '焦虑', '抑郁', '压力', '睡眠',
  '冥想', '正念', '放松', '心理健康', '自我成长',
  '人际关系', '工作压力', '学习方法', '生活技巧',
]

const formRef = ref(null)
const submitLoading = ref(false)
const coverFile = ref(null)

const form = reactive({
  id: '',
  title: '',
  content: '',
  coverImage: '',
  categoryId: '',
  summary: '',
  tags: [],
})

const formRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [
    {
      required: true,
      validator: (rule, value, callback) => {
        // 富文本编辑器为空时会返回 <p><br></p> 之类占位 HTML，需剥掉标签判断真实内容
        const text = (value || '').replace(/<[^>]+>/g, '').replace(/&nbsp;/ig, '').trim()
        if (!text) {
          callback(new Error('请输入正文'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

const resetForm = () => {
  form.id = ''
  form.title = ''
  form.content = ''
  form.coverImage = ''
  form.categoryId = ''
  form.summary = ''
  form.tags = []
  coverFile.value = null
  formRef.value?.clearValidate()
}

// Watch for article data changes (edit mode)
watch(() => props.article, (val) => {
  if (props.type === 'edit' && val && val.id) {
    Object.assign(form, {
      id: val.id,
      title: val.title || '',
      content: val.content || '',
      coverImage: val.coverImage || val.cover || val.coverUrl || val.cover_image || '',
      categoryId: val.categoryId || val.category?.id || '',
      summary: val.summary || '',
      tags: val.tags ? String(val.tags).split(',') : [],
    })
  }
}, { immediate: true })

// 打开弹窗时，如果是新增则清空表单
watch(() => props.modelValue, (visible) => {
  if (visible && props.type === 'add') {
    resetForm()
  }
})

const clearCover = () => {
  form.coverImage = ''
  coverFile.value = null
}

const handleCoverBeforeUpload = (file) => {
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 5MB')
    return false
  }
  const reader = new FileReader()
  reader.onload = (e) => {
    form.coverImage = e.target.result
  }
  reader.readAsDataURL(file)
  coverFile.value = file
  return false
}

const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const payload = { ...form }
      delete payload.id
      if (Array.isArray(payload.tags)) {
        payload.tags = payload.tags.join(',')
      }
      // 仅当要上传新封面时，才从主 payload 移除 coverImage（待上传后单独回写 filePath）
      // 否则保留：空字符串表示用户清空封面，原值表示未改动
      if (coverFile.value) {
        delete payload.coverImage
      }

      if (props.type === 'add') {
        const res = await addArticle(payload)
        const articleId = res?.data?.id || res?.data?.articleId || res?.id || res?.articleId
        if (coverFile.value && articleId) {
          try {
            const uploadRes = await uploadFile({
              file: coverFile.value,
              businessType: 'ARTICLE',
              businessId: articleId,
              businessField: 'cover'
            })
            // 上传成功后，把封面路径写回文章表，否则查询文章详情时拿不到封面
            const filePath = uploadRes?.data?.filePath || uploadRes?.filePath
            if (filePath) {
              await updateArticle(articleId, { coverImage: filePath })
            }
          } catch (uploadErr) {
            console.error('封面上传失败', uploadErr)
            ElMessage.error('文章已创建，但封面图片上传失败，请编辑后重新上传')
            emit('update:modelValue', false)
            emit('submit-success')
            coverFile.value = null
            submitLoading.value = false
            return
          }
        }
        ElMessage.success('新增成功')
      } else {
        await updateArticle(form.id, payload)
        if (coverFile.value) {
          try {
            const uploadRes = await uploadFile({
              file: coverFile.value,
              businessType: 'ARTICLE',
              businessId: String(form.id),
              businessField: 'cover'
            })
            // 上传成功后，把封面路径写回文章表
            const filePath = uploadRes?.data?.filePath || uploadRes?.filePath
            if (filePath) {
              await updateArticle(form.id, { coverImage: filePath })
            }
          } catch (uploadErr) {
            console.error('封面上传失败', uploadErr)
            ElMessage.error('文章已保存，但封面图片上传失败，请编辑后重新上传')
            emit('update:modelValue', false)
            emit('submit-success')
            coverFile.value = null
            submitLoading.value = false
            return
          }
        }
        ElMessage.success('修改成功')
      }
      emit('update:modelValue', false)
      emit('submit-success')
      coverFile.value = null
    } catch (e) {
      console.error(e)
    } finally {
      submitLoading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.cover-uploader {
  :deep(.el-upload) {
    width: 180px;
    height: 120px;
    border: 1px dashed #d9d9d9;
    border-radius: 8px;
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: border-color 0.2s;

    &:hover {
      border-color: #409eff;
    }
  }
}

.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-uploader-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 13px;

  .el-icon {
    font-size: 28px;
  }
}
</style>
