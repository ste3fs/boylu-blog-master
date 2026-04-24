<template>
  <div class="app-container job-log-page">
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
        <el-form-item label="执行状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择执行状态" clearable>
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
          <PageToolbarGroup kind="danger">
            <el-button
              v-permission="['sys:jobLog:delete']"
              type="danger"
              :icon="Delete"
              :disabled="selectedIds.length === 0"
              @click="handleBatchDelete"
            >
              批量删除
            </el-button>
            <el-button
              v-permission="['sys:jobLog:clean']"
              type="danger"
              plain
              :icon="Delete"
              @click="handleClean"
            >
              清空日志
            </el-button>
          </PageToolbarGroup>
        </PageToolbar>
      </template>

      <el-table v-loading="loading" :data="logList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="任务名称" prop="jobName" min-width="140" show-overflow-tooltip />
        <el-table-column label="任务分组" prop="jobGroup" align="center" width="110">
          <template #default="{ row }">
            {{ jobGroupFormat(row) }}
          </template>
        </el-table-column>
        <el-table-column label="调用目标" prop="invokeTarget" min-width="220" show-overflow-tooltip />
        <el-table-column label="日志信息" prop="jobMessage" min-width="180" show-overflow-tooltip />
        <el-table-column label="执行状态" align="center" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ statusFormat(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="执行时间" prop="createTime" align="center" width="170" />
        <el-table-column label="操作" align="center" width="160" fixed="right">
          <template #default="{ row }">
            <PageTableActions>
              <PageTableAction type="primary" :icon="Document" @click="handleDetail(row)">
                详情
              </PageTableAction>
              <PageTableAction
                v-permission="['sys:jobLog:delete']"
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

    <el-drawer
      v-model="drawer"
      title="日志详情"
      direction="rtl"
      :size="isMobile ? '100%' : '40%'"
      class="job-log-drawer"
    >
      <el-descriptions :column="1" border>
        <el-descriptions-item label="日志编号">
          {{ form.id }}
        </el-descriptions-item>
        <el-descriptions-item label="任务名称">
          {{ form.jobName }}
        </el-descriptions-item>
        <el-descriptions-item label="任务分组">
          {{ jobGroupFormat(form) }}
        </el-descriptions-item>
        <el-descriptions-item label="调用目标">
          {{ form.invokeTarget }}
        </el-descriptions-item>
        <el-descriptions-item label="日志信息">
          {{ form.jobMessage }}
        </el-descriptions-item>
        <el-descriptions-item label="执行状态">
          <el-tag :type="form.status === 0 ? 'success' : 'danger'">
            {{ statusFormat(form) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">
          {{ form.startTime }}
        </el-descriptions-item>
        <el-descriptions-item label="结束时间">
          {{ form.stopTime }}
        </el-descriptions-item>
        <el-descriptions-item label="异常信息">
          <pre class="job-log-error">{{ form.exceptionInfo || '无' }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Document } from '@element-plus/icons-vue'
import { cleanJobLogApi, delleteJobLogApi, listJobLogApi } from '@/api/monitor/jobLog'

const isMobile = ref(false)
const queryFormRef = ref()
const loading = ref(false)
const total = ref(0)
const logList = ref<any[]>([])
const selectedIds = ref<Array<string | number>>([])
const drawer = ref(false)
const form = ref<any>({})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  jobName: undefined as string | undefined,
  jobGroup: undefined as string | undefined,
  status: undefined as number | undefined
})

const jobGroupOptions = [
  { value: 'DEFAULT', label: '默认' },
  { value: 'SYSTEM', label: '系统' }
]

const statusOptions = [
  { value: 0, label: '成功' },
  { value: 1, label: '失败' }
]

const syncMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

const jobGroupFormat = (row: any) => {
  return jobGroupOptions.find(item => item.value === row.jobGroup)?.label || row.jobGroup || '-'
}

const statusFormat = (row: any) => {
  return statusOptions.find(item => item.value === row.status)?.label || '未知'
}

const getList = async () => {
  loading.value = true
  try {
    const { data } = await listJobLogApi(queryParams)
    logList.value = data.records || []
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
  selectedIds.value = selection.map(item => item.id)
}

const handleDetail = (row: any) => {
  form.value = row
  drawer.value = true
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm(
    `确定要删除任务“${row.jobName}”的这条日志吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await delleteJobLogApi(row.id)
  ElMessage.success('日志删除成功')
  getList()
}

const handleBatchDelete = async () => {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择要删除的日志')
    return
  }

  await ElMessageBox.confirm(
    `确定要删除选中的 ${selectedIds.value.length} 条调度日志吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await delleteJobLogApi(selectedIds.value)
  ElMessage.success('批量删除成功')
  getList()
}

const handleClean = async () => {
  await ElMessageBox.confirm('确定要清空所有调度日志吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await cleanJobLogApi()
  ElMessage.success('日志已清空')
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
.job-log-error {
  margin: 0;
  max-height: 320px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: Consolas, 'Courier New', monospace;
}
</style>
