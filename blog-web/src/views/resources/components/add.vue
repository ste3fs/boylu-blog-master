<template>
  <div>
    <!-- 上传资源对话框 -->
    <el-dialog
      title="上传资源"
      :visible.sync="dialogVisible"
      width="500px"
      custom-class="upload-dialog"
      @close="handleClose"
    >
      <el-form
        ref="uploadForm"
        :model="uploadForm"
        :rules="uploadRules"
        label-width="80px"
      >
        <el-form-item label="资源名称" prop="name">
          <el-input v-model="uploadForm.name" placeholder="请输入资源名称" />
        </el-form-item>

        <el-form-item label="资源分类" prop="category">
          <el-select v-model="uploadForm.category" placeholder="请选择分类">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.label"
              :value="category.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="封面图片" prop="cover">
          <el-upload
            action="#"
            :show-file-list="false"
            :http-request="uploadCover"
            :before-upload="beforeCoverUpload"
          >
            <div class="resource-cover-uploader">
              <img
                v-if="uploadForm.cover"
                :src="resolveCover(uploadForm.cover)"
                class="resource-cover-preview"
                alt="资源封面"
              >
              <div v-else class="resource-cover-placeholder">
                <i class="el-icon-picture-outline"></i>
                <span>上传封面</span>
              </div>
            </div>
          </el-upload>
        </el-form-item>

        <el-form-item label="资源描述" prop="description">
          <el-input
            v-model="uploadForm.description"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="简单说明这个资源的用途、版本或使用场景"
          />
        </el-form-item>

        <el-form-item label="资源类型" prop="isFree">
          <el-radio-group v-model="uploadForm.isFree">
            <el-radio :label="1">免费</el-radio>
            <el-radio :label="0">付费</el-radio>
          </el-radio-group>
        </el-form-item>

        
        <el-form-item label="网盘地址" prop="panPath">
          <el-input v-model="uploadForm.panPath" placeholder="请输入网盘地址" />
        </el-form-item>

        <el-form-item label="提取码" prop="panCode">
          <el-input v-model="uploadForm.panCode" placeholder="没有提取码可以不填" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="handleClose">取 消</el-button>
        <el-button type="primary" @click="submitUpload" :loading="uploading">
          确 定
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Option from 'element-ui/lib/option'
import Radio from 'element-ui/lib/radio'
import RadioGroup from 'element-ui/lib/radio-group'
import Select from 'element-ui/lib/select'
import Upload from 'element-ui/lib/upload'
import 'element-ui/lib/theme-chalk/option.css'
import 'element-ui/lib/theme-chalk/radio.css'
import 'element-ui/lib/theme-chalk/radio-group.css'
import 'element-ui/lib/theme-chalk/select.css'
import 'element-ui/lib/theme-chalk/select-dropdown.css'
import 'element-ui/lib/theme-chalk/upload.css'
import { addResourceApi } from '@/api/resources';
import { uploadFileApi } from '@/api/file'
import { resolveImageUrl } from '@/utils/image'
export default {
  name: "AddResource",
  components: {
    ElOption: Option,
    ElRadio: Radio,
    ElRadioGroup: RadioGroup,
    ElSelect: Select,
    ElUpload: Upload
  },
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    categories: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      uploading: false,
      uploadForm: {
        name: '',
        category: '',
        cover: '',
        description: '',
        isFree: 1,
        panPath: '',
        panCode: ''
      },
      uploadRules: {
        name: [
          { required: true, message: '请输入资源名称', trigger: 'blur' },
          { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
        ],
        category: [
          { required: true, message: '请选择资源分类', trigger: 'change' }
        ],
        description: [
          { max: 200, message: '资源描述不能超过 200 个字符', trigger: 'blur' }
        ],
        isFree: [
          { required: true, message: '请选择资源类型', trigger: 'change' }
        ],
        panPath: [
          { required: true, message: '请输入网盘地址', trigger: 'blur' }
        ],
        panCode: []
      }
    };
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible;
      },
      set(value) {
        this.$emit('update:visible', value);
      }
    }
  },
  methods: {
 
    /**
     * 关闭对话框
     */
    handleClose() {
      this.dialogVisible = false;
      this.$refs.uploadForm.resetFields();
      this.uploadForm.cover = ''
      this.uploadForm.description = ''
    },
    resolveCover(url) {
      return resolveImageUrl(url, '')
    },
    beforeCoverUpload(file) {
      const isImage = /^image\/(jpeg|png|gif|webp)$/.test(file.type)
      const isLt5M = file.size / 1024 / 1024 <= 5
      if (!isImage) {
        this.$message.error('只能上传 jpg/png/gif/webp 图片')
        return false
      }
      if (!isLt5M) {
        this.$message.error('封面图片不能超过 5MB')
        return false
      }
      return true
    },
    async uploadCover(options) {
      const formData = new FormData()
      formData.append('file', options.file)
      try {
        const res = await uploadFileApi(formData, 'resource-cover')
        this.uploadForm.cover = typeof res.data === 'string'
          ? res.data
          : (res.data?.fallback || '')
        this.$message.success('封面上传成功')
        options.onSuccess(res)
      } catch (error) {
        this.$message.error(error.message || '封面上传失败')
        options.onError(error)
      }
    },
    /**
     * 提交上传
     */
    submitUpload() {
      this.$refs.uploadForm.validate((valid) => {
        if (valid) {
          this.uploading = true
          addResourceApi(this.uploadForm).then(() => {
            this.$message.success("资源上传成功，等待博主审核！");
            this.$emit('success')
            // 重置表单
            this.$refs.uploadForm.resetFields();
            this.handleClose();
          }).catch(error => {
            this.$message.error(error.message || '资源上传失败')
          }).finally(() => {
            this.uploading = false
          })
        }
      });
    },
  },
};
</script>

<style scoped lang="scss">
.resource-cover-uploader {
  width: 112px;
  height: 82px;
  border: 1px dashed #d9e2ef;
  border-radius: 10px;
  overflow: hidden;
  background: #f8fafc;
  cursor: pointer;
}

.resource-cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.resource-cover-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #94a3b8;
  font-size: 12px;
}
</style>
