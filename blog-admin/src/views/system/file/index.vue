<template>
  <div class="app-container file-page">
    <PageSearch>
      <el-form ref="queryFormRef" :model="queryParams" :inline="!isMobile">
        <el-form-item label="文件名" prop="filename">
          <el-input
            v-model="queryParams.filename"
            placeholder="请输入文件名"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="文件类型" prop="ext">
          <el-select v-model="queryParams.ext" placeholder="请选择文件类型" clearable>
            <el-option
              v-for="item in fileTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <PageSearchActions @search="handleQuery" @reset="resetQuery" />
        </el-form-item>
      </el-form>
    </PageSearch>

    <el-card class="box-card">
      <template #header>
        <PageToolbar>
          <PageToolbarGroup>
            <el-button type="success" :icon="Setting" @click="handleOpenOssConfig">
              云存储配置
            </el-button>
          </PageToolbarGroup>
        </PageToolbar>
      </template>

      <el-table v-loading="loading" :data="fileList">
        <el-table-column label="预览" width="92" align="center">
          <template #default="{ row }">
            <el-image
              :preview-src-list="[row.url]"
              :initial-index="0"
              :src="row.url"
              fit="cover"
              class="file-preview"
            />
          </template>
        </el-table-column>
        <el-table-column label="文件名" prop="filename" min-width="180" show-overflow-tooltip />
        <el-table-column label="文件类型" prop="ext" align="center" width="110" />
        <el-table-column label="大小" prop="size" align="center" width="110">
          <template #default="{ row }">
            {{ (Number(row.size || 0) / 1024).toFixed(1) }} KB
          </template>
        </el-table-column>
        <el-table-column label="访问地址" prop="url" min-width="220" show-overflow-tooltip />
        <el-table-column label="存储路径" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ `${row.basePath || ''}${row.path || ''}${row.filename || ''}` }}
          </template>
        </el-table-column>
        <el-table-column label="存储平台" prop="platform" align="center" width="120">
          <template #default="{ row }">
            <el-tag :type="resolveOssMeta(row.platform).style || 'info'">
              {{ resolveOssMeta(row.platform).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上传时间" prop="createTime" align="center" width="170" />
        <el-table-column label="操作" align="center" width="160" fixed="right">
          <template #default="{ row }">
            <PageTableActions>
              <PageTableAction
                v-permission="['sys:file:delete']"
                type="danger"
                :icon="Delete"
                @click="handleDelete(row)"
              >
                删除
              </PageTableAction>
              <PageTableAction type="primary" :icon="Download" @click="handleDownload(row)">
                下载
              </PageTableAction>
            </PageTableActions>
          </template>
        </el-table-column>
      </el-table>

      <PagePagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <el-drawer
      v-model="drawerVisible"
      title="云存储配置"
      direction="rtl"
      :size="isMobile ? '100%' : '40%'"
      class="oss-drawer"
    >
      <el-form
        ref="ossConfigFormRef"
        :model="ossConfigForm"
        :rules="rules"
        label-position="left"
        label-width="110px"
        class="oss-form"
      >
        <el-form-item label="平台" prop="platform">
          <el-radio-group v-model="ossConfigForm.platform" @change="handleChangePlatform">
            <el-radio
              v-for="item in ossOptions"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="ossConfigForm.platform !== 'local'">
          <el-form-item label="Access Key" prop="accessKey">
            <el-input v-model="ossConfigForm.accessKey" placeholder="请输入 Access Key" />
          </el-form-item>
          <el-form-item label="Secret Key" prop="secretKey">
            <el-input v-model="ossConfigForm.secretKey" placeholder="请输入 Secret Key" />
          </el-form-item>
          <el-form-item label="存储空间" prop="bucket">
            <el-input v-model="ossConfigForm.bucket" placeholder="请输入 Bucket 名称" />
          </el-form-item>
          <el-form-item label="区域" prop="region">
            <el-input v-model="ossConfigForm.region" placeholder="请输入存储区域" />
          </el-form-item>
        </template>

        <el-form-item label="域名" prop="domain">
          <el-input v-model="ossConfigForm.domain" placeholder="请输入访问域名，以 / 结尾更稳妥" />
        </el-form-item>
        <el-form-item label="基础路径" prop="basePath">
          <el-input v-model="ossConfigForm.basePath" placeholder="请输入文件基础路径，可留空" />
        </el-form-item>

        <template v-if="ossConfigForm.platform === 'local'">
          <el-form-item label="本地存储路径" prop="storagePath">
            <el-input
              v-model="ossConfigForm.storagePath"
              placeholder="例如 D:/Temp/ 或 /data/upload/"
            />
          </el-form-item>
          <el-form-item v-if="ossConfigForm.enableAccess === 1" label="访问路径" prop="pathPatterns">
            <el-input
              v-model="ossConfigForm.pathPatterns"
              placeholder="例如 /upload/**，需与域名后的路径一致"
            />
          </el-form-item>
          <el-form-item label="允许访问" prop="enableAccess">
            <el-switch v-model="ossConfigForm.enableAccess" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </template>

        <el-form-item label="启用存储" prop="isEnable">
          <el-switch v-model="ossConfigForm.isEnable" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button
            type="primary"
            :icon="CircleCheck"
            :loading="ossConfigLoading"
            v-permission="['sys:oss:submit']"
            @click="handleSaveOssConfig"
          >
            保存
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Delete, Download, Setting } from '@element-plus/icons-vue'
import type { FormRules } from 'element-plus'
import {
  addOssApi,
  deleteFileApi,
  getFileListApi,
  getOssConfigApi,
  updateOssApi
} from '@/api/file'
import { getDictDataByDictTypesApi } from '@/api/system/dict'

const isMobile = ref(false)
const loading = ref(false)
const total = ref(0)
const fileList = ref<any[]>([])
const fileTypeOptions = ref<any[]>([])
const ossOptions = ref<any[]>([])
const ossConfigList = ref<any[]>([])
const drawerVisible = ref(false)
const ossConfigLoading = ref(false)

const queryFormRef = ref()
const ossConfigFormRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  filename: undefined as string | undefined,
  ext: undefined as string | undefined
})

const createOssForm = () => ({
  id: undefined,
  platform: '',
  accessKey: '',
  secretKey: '',
  bucket: '',
  domain: '',
  basePath: '',
  storagePath: '',
  region: '',
  isEnable: 0,
  enableAccess: 0,
  pathPatterns: ''
})

const ossConfigForm = reactive<any>(createOssForm())

const rules = reactive<FormRules>({
  platform: [
    { required: true, message: '请选择存储平台', trigger: 'change' }
  ],
  accessKey: [
    { required: true, message: '请输入 Access Key', trigger: 'blur' }
  ],
  secretKey: [
    { required: true, message: '请输入 Secret Key', trigger: 'blur' }
  ],
  bucket: [
    { required: true, message: '请输入存储空间', trigger: 'blur' }
  ],
  domain: [
    { required: true, message: '请输入访问域名', trigger: 'blur' }
  ],
  storagePath: [
    { required: true, message: '请输入本地存储路径', trigger: 'blur' }
  ],
  pathPatterns: [
    { required: true, message: '请输入访问路径', trigger: 'blur' }
  ]
})

const syncMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

const assignOssForm = (payload?: any) => {
  Object.assign(ossConfigForm, createOssForm(), payload || {})
}

const resolveOssMeta = (value: string) => {
  return ossOptions.value.find(item => item.value === value) || {
    label: value || '未设置',
    style: 'info'
  }
}

const getList = async () => {
  loading.value = true
  try {
    const { data } = await getFileListApi(queryParams)
    fileList.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const getDictList = async () => {
  const { data } = await getDictDataByDictTypesApi(['sys_file_type', 'sys_file_oss'])
  fileTypeOptions.value = data.sys_file_type?.list || []
  ossOptions.value = data.sys_file_oss?.list || []
}

const getOssConfig = async () => {
  const { data } = await getOssConfigApi()
  ossConfigList.value = data || []
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm(
    `确定要删除文件“${row.filename}”吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await deleteFileApi(row.url)
  ElMessage.success('文件删除成功')
  getList()
}

const handleDownload = (row: any) => {
  window.open(row.url)
}

const handleOpenOssConfig = () => {
  if (!ossOptions.value.length) {
    ElMessage.warning('请先在字典中配置云存储平台')
    return
  }

  const enabledConfig = ossConfigList.value.find(item => item.isEnable === 1)
  assignOssForm(enabledConfig || { platform: ossOptions.value[0]?.value || '' })
  drawerVisible.value = true
}

const handleChangePlatform = () => {
  const config = ossConfigList.value.find(item => item.platform === ossConfigForm.platform)
  assignOssForm(config || { platform: ossConfigForm.platform })
}

const handleSaveOssConfig = async () => {
  await ossConfigFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return

    ossConfigLoading.value = true
    try {
      if (ossConfigForm.id) {
        await updateOssApi(ossConfigForm)
        ElMessage.success('云存储配置修改成功')
      } else {
        await addOssApi(ossConfigForm)
        ElMessage.success('云存储配置保存成功')
      }
      drawerVisible.value = false
      await getOssConfig()
    } finally {
      ossConfigLoading.value = false
    }
  })
}

const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  getList()
}

const handleCurrentChange = (val: number) => {
  queryParams.pageNum = val
  getList()
}

onMounted(async () => {
  syncMobile()
  window.addEventListener('resize', syncMobile)
  await Promise.all([getDictList(), getOssConfig()])
  getList()
})

onUnmounted(() => {
  window.removeEventListener('resize', syncMobile)
})
</script>

<style scoped lang="scss">
.file-preview {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: var(--el-fill-color);
}

@media (max-width: 768px) {
  .file-page {
    :deep(.oss-form .el-form-item) {
      display: block;
      margin-bottom: 16px !important;
    }

    :deep(.oss-form .el-form-item__label) {
      width: 100% !important;
      padding: 0 0 8px !important;
      font-weight: 600;
      text-align: left !important;
    }

    :deep(.oss-form .el-form-item__content) {
      margin-left: 0 !important;
      width: 100%;
    }
  }
}
</style>
