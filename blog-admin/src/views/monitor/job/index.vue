<template>
  <div class="app-container job-page">
    <PageSearch>
      <el-form ref="queryFormRef" :model="queryParams" :inline="!isMobile">
        <el-form-item label="任务名称" prop="jobName">
          <el-input
            v-model="queryParams.jobName"
            placeholder="请输入任务名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="任务分组" prop="jobGroup">
          <el-select v-model="queryParams.jobGroup" placeholder="请选择任务分组" clearable>
            <el-option
              v-for="item in jobGroupOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="任务状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择任务状态" clearable>
            <el-option
              v-for="item in statusOptions"
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
              v-permission="['sys:job:add']"
              type="primary"
              :icon="Plus"
              @click="handleAdd"
            >
              新增任务
            </el-button>
          </PageToolbarGroup>
          <PageToolbarGroup kind="danger">
            <el-button
              v-permission="['sys:job:deleteBatch']"
              type="danger"
              plain
              :icon="Delete"
              :disabled="selectedIds.length === 0"
              @click="handleBatchDelete"
            >
              批量删除
            </el-button>
          </PageToolbarGroup>
          <template #right>
            <PageToolbarGroup kind="utility">
              <el-button type="info" plain :icon="Document" @click="handleLog">
                日志
              </el-button>
            </PageToolbarGroup>
          </template>
        </PageToolbar>
      </template>

      <el-table v-loading="loading" :data="jobList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="任务编号" prop="jobId" align="center" width="90" />
        <el-table-column label="任务名称" prop="jobName" min-width="140" show-overflow-tooltip />
        <el-table-column label="任务分组" prop="jobGroup" align="center" width="110">
          <template #default="{ row }">
            {{ jobGroupFormat(row) }}
          </template>
        </el-table-column>
        <el-table-column label="调用目标" prop="invokeTarget" min-width="220" show-overflow-tooltip />
        <el-table-column label="Cron 表达式" prop="cronExpression" min-width="170" show-overflow-tooltip />
        <el-table-column label="状态" align="center" width="90" v-permission="['sys:job:update']">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="0"
              inactive-value="1"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="220" fixed="right">
          <template #default="{ row }">
            <PageTableActions>
              <PageTableAction
                v-permission="['sys:job:update']"
                type="info"
                :icon="VideoPlay"
                @click="handleRun(row)"
              >
                执行一次
              </PageTableAction>
              <PageTableAction
                v-permission="['sys:job:update']"
                type="primary"
                :icon="Edit"
                @click="handleUpdate(row)"
              >
                编辑
              </PageTableAction>
              <PageTableAction
                v-permission="['sys:job:delete']"
                type="danger"
                :icon="Delete"
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
      :title="title"
      v-model="open"
      :width="isMobile ? '100vw' : '760px'"
      :top="isMobile ? '0' : '5vh'"
      :fullscreen="isMobile"
      append-to-body
      destroy-on-close
      class="job-dialog"
    >
      <el-form
        ref="jobFormRef"
        :model="form"
        :rules="rules"
        :label-position="isMobile ? 'top' : 'right'"
        label-width="120px"
        class="job-form"
      >
        <el-row :gutter="18">
          <el-col :xs="24" :sm="24" :md="12">
            <el-form-item label="任务名称" prop="jobName">
              <el-input v-model="form.jobName" placeholder="请输入任务名称" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12">
            <el-form-item label="任务分组" prop="jobGroup">
              <el-select v-model="form.jobGroup" placeholder="请选择任务分组">
                <el-option
                  v-for="item in jobGroupOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="调用目标" prop="invokeTarget">
          <el-input v-model="form.invokeTarget" placeholder="请输入调用目标，例如 task.demo('hello')">
            <template #append>
              <el-tooltip placement="top">
                <template #content>
                  Bean 调用示例：demoTask.handle('hello')
                  <br />
                  类调用示例：com.demo.task.DemoTask.handle('hello')
                </template>
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="Cron 表达式" prop="cronExpression">
          <el-input v-model="form.cronExpression" placeholder="请输入 Cron 表达式">
            <template #append>
              <el-tooltip content="打开 Cron 表达式生成器" placement="top">
                <el-button @click="handleShowCron">
                  <el-icon><Timer /></el-icon>
                </el-button>
              </el-tooltip>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="执行策略" prop="misfirePolicy">
          <el-radio-group v-model="form.misfirePolicy">
            <el-radio value="1">立即执行</el-radio>
            <el-radio value="2">执行一次</el-radio>
            <el-radio value="3">放弃执行</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="是否并发" prop="concurrent">
          <el-radio-group v-model="form.concurrent">
            <el-radio value="0">允许</el-radio>
            <el-radio value="1">禁止</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="cronVisible"
      title="Cron 表达式生成器"
      :width="isMobile ? '100vw' : '760px'"
      :top="isMobile ? '0' : '6vh'"
      :fullscreen="isMobile"
      append-to-body
      destroy-on-close
      class="cron-dialog"
    >
      <CronTab
        v-model="form.cronExpression"
        :visible="cronVisible"
        @update:visible="cronVisible = $event"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete,
  Document,
  Edit,
  Plus,
  QuestionFilled,
  Timer,
  VideoPlay
} from '@element-plus/icons-vue'
import {
  addJobApi,
  changeJobStatusApi,
  delJobApi,
  getJobApi,
  listJobApi,
  runJobApi,
  updateJobApi
} from '@/api/monitor/job'
import CronTab from './components/CronTab.vue'

const router = useRouter()
const isMobile = ref(false)
const queryFormRef = ref()
const jobFormRef = ref()
const loading = ref(false)
const total = ref(0)
const open = ref(false)
const cronVisible = ref(false)
const title = ref('')
const selectedIds = ref<Array<string | number>>([])
const jobList = ref<any[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  jobName: undefined as string | undefined,
  jobGroup: undefined as string | undefined,
  status: undefined as string | undefined
})

const form = reactive<any>({
  jobId: undefined,
  jobName: undefined,
  jobGroup: undefined,
  invokeTarget: undefined,
  cronExpression: undefined,
  misfirePolicy: '1',
  concurrent: '1',
  status: '1'
})

const rules = reactive({
  jobName: [
    { required: true, message: '请输入任务名称', trigger: 'blur' }
  ],
  jobGroup: [
    { required: true, message: '请选择任务分组', trigger: 'change' }
  ],
  invokeTarget: [
    { required: true, message: '请输入调用目标', trigger: 'blur' }
  ],
  cronExpression: [
    { required: true, message: '请输入 Cron 表达式', trigger: 'blur' }
  ]
})

const jobGroupOptions = [
  { value: 'DEFAULT', label: '默认' },
  { value: 'SYSTEM', label: '系统' }
]

const statusOptions = [
  { value: '0', label: '正常' },
  { value: '1', label: '暂停' }
]

const syncMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

const jobGroupFormat = (row: any) => {
  return jobGroupOptions.find(item => item.value === row.jobGroup)?.label || row.jobGroup || '-'
}

const reset = () => {
  form.jobId = undefined
  form.jobName = undefined
  form.jobGroup = undefined
  form.invokeTarget = undefined
  form.cronExpression = undefined
  form.misfirePolicy = '1'
  form.concurrent = '1'
  form.status = '1'
}

const getList = async () => {
  loading.value = true
  try {
    const { data } = await listJobApi(queryParams)
    jobList.value = data.records || []
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

const handleSelectionChange = (selection: any[]) => {
  selectedIds.value = selection.map(item => item.jobId)
}

const handleStatusChange = async (row: any) => {
  const previous = row.status === '0' ? '1' : '0'
  try {
    await changeJobStatusApi(row.jobId, row.status)
    ElMessage.success(row.status === '0' ? '任务已启用' : '任务已暂停')
  } catch (error) {
    row.status = previous
  }
}

const handleRun = async (row: any) => {
  await runJobApi(row)
  ElMessage.success('任务已立即执行一次')
}

const handleShowCron = () => {
  cronVisible.value = true
}

const handleAdd = () => {
  reset()
  title.value = '新增定时任务'
  open.value = true
}

const handleUpdate = async (row: any) => {
  reset()
  const { data } = await getJobApi(row.jobId)
  Object.assign(form, data)
  title.value = '编辑定时任务'
  open.value = true
}

const cancel = () => {
  open.value = false
  jobFormRef.value?.resetFields()
  reset()
}

const submitForm = async () => {
  await jobFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return

    if (form.jobId) {
      await updateJobApi(form)
      ElMessage.success('任务修改成功')
    } else {
      await addJobApi(form)
      ElMessage.success('任务新增成功')
    }
    open.value = false
    getList()
  })
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm(
    `确定要删除任务“${row.jobName}”吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await delJobApi(row.jobId)
  ElMessage.success('任务删除成功')
  getList()
}

const handleBatchDelete = async () => {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择要删除的任务')
    return
  }

  await ElMessageBox.confirm(
    `确定要删除选中的 ${selectedIds.value.length} 个定时任务吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await delJobApi(selectedIds.value)
  ElMessage.success('批量删除成功')
  getList()
}

const handleLog = () => {
  router.push('/monitor/job-log')
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
  .job-page {
    :deep(.job-dialog),
    :deep(.cron-dialog) {
      border-radius: 0 !important;
    }

    :deep(.job-form .el-form-item) {
      display: block;
      margin-bottom: 16px !important;
    }

    :deep(.job-form .el-form-item__label) {
      display: block;
      width: 100% !important;
      padding: 0 0 8px !important;
      text-align: left !important;
      font-weight: 600;
    }

    :deep(.job-form .el-form-item__content) {
      margin-left: 0 !important;
      width: 100%;
    }
  }
}
</style>
