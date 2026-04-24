<template>
  <div class="app-container system-config-page">
    <PageSearch>
      <el-form ref="queryFormRef" :model="queryParams" :inline="!isMobile">
        <el-form-item label="参数名称" prop="configName">
          <el-input
            v-model="queryParams.configName"
            placeholder="请输入参数名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="参数键名" prop="configKey">
          <el-input
            v-model="queryParams.configKey"
            placeholder="请输入参数键名"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="系统内置" prop="configType">
          <el-select v-model="queryParams.configType" placeholder="请选择是否内置" clearable>
            <el-option
              v-for="item in typeOptions"
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
            <el-button
              type="primary"
              :icon="Plus"
              v-permission="['sys:config:add']"
              @click="handleAdd"
            >
              新增参数
            </el-button>
          </PageToolbarGroup>
          <PageToolbarGroup kind="danger">
            <el-button
              type="danger"
              plain
              :icon="Delete"
              :disabled="selectedIds.length === 0"
              v-permission="['sys:config:delete']"
              @click="handleBatchDelete"
            >
              批量删除
            </el-button>
          </PageToolbarGroup>
        </PageToolbar>
      </template>

      <el-table
        v-loading="loading"
        :data="dataList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="参数名称" prop="configName" min-width="140" show-overflow-tooltip />
        <el-table-column label="参数键名" prop="configKey" min-width="150" show-overflow-tooltip />
        <el-table-column label="参数键值" prop="configValue" min-width="180" show-overflow-tooltip />
        <el-table-column label="系统内置" prop="configType" align="center" width="100">
          <template #default="{ row }">
            <el-tag :type="row.configType === 'Y' ? 'success' : 'info'">
              {{ row.configType === 'Y' ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" align="center" width="170" />
        <el-table-column label="更新时间" prop="updateTime" align="center" width="170" />
        <el-table-column label="备注" prop="remark" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" align="center" width="160" fixed="right">
          <template #default="{ row }">
            <PageTableActions>
              <PageTableAction
                type="primary"
                :icon="Edit"
                v-permission="['sys:config:update']"
                @click="handleUpdate(row)"
              >
                编辑
              </PageTableAction>
              <PageTableAction
                type="danger"
                :icon="Delete"
                v-permission="['sys:config:delete']"
                @click="handleDelete(row)"
              >
                删除
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

    <el-dialog
      v-model="open"
      :title="title"
      :width="isMobile ? '100vw' : '560px'"
      :top="isMobile ? '0' : '8vh'"
      :fullscreen="isMobile"
      append-to-body
      destroy-on-close
      class="config-dialog"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        :label-position="isMobile ? 'top' : 'right'"
        label-width="88px"
        class="config-form"
      >
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="form.configName" placeholder="请输入参数名称" />
        </el-form-item>
        <el-form-item label="参数键名" prop="configKey">
          <el-input v-model="form.configKey" placeholder="请输入参数键名" />
        </el-form-item>
        <el-form-item label="参数键值" prop="configValue">
          <el-input
            v-model="form.configValue"
            type="textarea"
            :rows="4"
            placeholder="请输入参数键值"
          />
        </el-form-item>
        <el-form-item label="系统内置" prop="configType">
          <el-radio-group v-model="form.configType">
            <el-radio
              v-for="item in typeOptions"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="cancel">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import {
  addSysConfigApi,
  deleteSysConfigApi,
  detailSysConfigApi,
  listSysConfigApi,
  updateSysConfigApi
} from '@/api/system/config'

const isMobile = ref(false)
const loading = ref(false)
const total = ref(0)
const open = ref(false)
const title = ref('')
const dataList = ref<any[]>([])
const selectedIds = ref<number[]>([])

const queryFormRef = ref()
const formRef = ref()

const typeOptions = [
  { label: '是', value: 'Y' },
  { label: '否', value: 'N' }
]

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  configName: undefined as string | undefined,
  configKey: undefined as string | undefined,
  configType: undefined as string | undefined
})

const form = reactive<any>({
  id: undefined,
  configName: '',
  configKey: '',
  configValue: '',
  configType: 'N',
  remark: ''
})

const rules = reactive({
  configName: [
    { required: true, message: '请输入参数名称', trigger: 'blur' }
  ],
  configKey: [
    { required: true, message: '请输入参数键名', trigger: 'blur' }
  ],
  configValue: [
    { required: true, message: '请输入参数键值', trigger: 'blur' }
  ],
  configType: [
    { required: true, message: '请选择系统内置状态', trigger: 'change' }
  ]
})

const syncMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

const reset = () => {
  form.id = undefined
  form.configName = ''
  form.configKey = ''
  form.configValue = ''
  form.configType = 'N'
  form.remark = ''
}

const getList = async () => {
  loading.value = true
  try {
    const { data } = await listSysConfigApi(queryParams)
    dataList.value = data.records || []
    total.value = data.total || 0
    selectedIds.value = []
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const handleSelectionChange = (selection: Array<{ id: number }>) => {
  selectedIds.value = selection.map(item => item.id)
}

const handleAdd = () => {
  reset()
  title.value = '新增参数配置'
  open.value = true
}

const handleUpdate = async (row: any) => {
  reset()
  const { data } = await detailSysConfigApi(row.id)
  Object.assign(form, data)
  title.value = '编辑参数配置'
  open.value = true
}

const submitForm = () => {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return

    if (form.id !== undefined) {
      await updateSysConfigApi(form)
      ElMessage.success('参数配置修改成功')
    } else {
      await addSysConfigApi(form)
      ElMessage.success('参数配置新增成功')
    }
    open.value = false
    getList()
  })
}

const cancel = () => {
  open.value = false
  formRef.value?.resetFields()
  reset()
}

const handleBatchDelete = async () => {
  if (!selectedIds.value.length) return

  await ElMessageBox.confirm(
    `确定要删除选中的 ${selectedIds.value.length} 条参数配置吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await deleteSysConfigApi(selectedIds.value)
  ElMessage.success('删除成功')
  getList()
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm(
    `确定要删除参数“${row.configName}”吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await deleteSysConfigApi(row.id)
  ElMessage.success('删除成功')
  getList()
}

const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  getList()
}

const handleCurrentChange = (val: number) => {
  queryParams.pageNum = val
  getList()
}

onMounted(() => {
  syncMobile()
  getList()
  window.addEventListener('resize', syncMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', syncMobile)
})
</script>

<style scoped lang="scss">
@media (max-width: 768px) {
  .system-config-page {
    :deep(.config-dialog) {
      border-radius: 0 !important;
    }

    :deep(.config-form .el-form-item) {
      display: block;
      margin-bottom: 16px !important;
    }

    :deep(.config-form .el-form-item__label) {
      display: block;
      width: 100% !important;
      padding: 0 0 8px !important;
      text-align: left !important;
      font-weight: 600;
    }

    :deep(.config-form .el-form-item__content) {
      margin-left: 0 !important;
      width: 100%;
    }
  }
}
</style>
