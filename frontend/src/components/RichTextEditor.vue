<template>
  <div class="rich-text-editor">
    <Toolbar
      :editor="editorRef"
      :defaultConfig="toolbarConfig"
      mode="default"
      class="editor-toolbar"
    />
    <Editor
      v-model="valueHtml"
      :defaultConfig="editorConfig"
      mode="default"
      class="editor-content"
      @onCreated="handleCreated"
      @onChange="handleChange"
    />
  </div>
</template>

<script setup>
import { ref, shallowRef, watch, onBeforeUnmount } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/file'

const props = defineProps({
  modelValue: { type: String, default: '' },
  // 上传图片时关联的业务 id（文章 id），新增时传 '0'
  businessId: { type: String, default: '0' },
  placeholder: { type: String, default: '请输入正文内容' },
})

const emit = defineEmits(['update:modelValue'])

const editorRef = shallowRef()
const valueHtml = ref(props.modelValue || '')

const toolbarConfig = {
  excludeKeys: ['group-video', 'fullScreen'],
}

const editorConfig = {
  placeholder: props.placeholder,
  MENU_CONF: {
    uploadImage: {
      async customUpload(file, insertFn) {
        try {
          const res = await uploadFile({
            file,
            businessType: 'ARTICLE',
            businessId: props.businessId,
            businessField: 'content'
          })
          const url = res?.data?.filePath
          if (url) insertFn(url, file.name, url)
        } catch (err) {
          ElMessage.error('图片上传失败')
          console.error(err)
        }
      }
    }
  }
}

const handleCreated = (editor) => {
  editorRef.value = editor
}

const handleChange = (editor) => {
  emit('update:modelValue', editor.getHtml())
}

// 外部值变化时同步到编辑器（如编辑模式加载数据）
watch(() => props.modelValue, (val) => {
  if (val !== valueHtml.value) {
    valueHtml.value = val || ''
  }
})

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor) editor.destroy()
})
</script>

<style lang="scss" scoped>
.rich-text-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.editor-toolbar {
  border-bottom: 1px solid #dcdfe6;
}

.editor-content {
  height: 400px;
  overflow-y: auto;

  :deep(.w-e-text-container) {
    height: 400px !important;
  }
}
</style>
