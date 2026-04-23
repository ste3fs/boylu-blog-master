<template>
  <div class="app-container">
    <PageSearch>
      <!-- 鎼滅储宸ュ叿鏍?-->
      <el-form :model="queryParams" ref="queryFormRef" :inline="true">
        <el-form-item label="浠诲姟鍚嶇О" prop="jobName">
          <el-input
            v-model="queryParams.jobName"
            placeholder="请输入任务名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="浠诲姟缁勫悕" prop="jobGroup">
          <el-select v-model="queryParams.jobGroup" placeholder="璇烽€夋嫨浠诲姟缁勫悕" clearable>
            <el-option
              v-for="dict in jobGroupOptions"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="任务状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择任务状态" clearable>
            <el-option
              v-for="dict in statusOptions"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
                <el-form-item>
          <PageSearchActions @search="handleQuery" @reset="resetQuery" />
        </el-form-item>
      </el-form>
    </PageSearch>
    <el-card>
      <!-- 鎿嶄綔宸ュ叿鏍?-->
      <template #header>
        <PageToolbar>
          <PageToolbarGroup>
            <el-button
              v-permission="['sys:job:add']"
              type="primary"
              :icon="Plus"
              @click="handleAdd"
              >鏂板</el-button>
          </PageToolbarGroup>
          <PageToolbarGroup kind="danger">
            <el-button
              v-permission="['sys:job:deleteBatch']"
              type="danger"
              plain
              :icon="Delete"
              :disabled="!selectedIds.length"
              @click="handleBatcheDelete"
            >鎵归噺鍒犻櫎</el-button>
          </PageToolbarGroup>
          <template #right>
            <PageToolbarGroup kind="utility">
              <el-button
                type="info"
                plain
                :icon="Document"
                @click="handleLog"
              >鏃ュ織</el-button>
            </PageToolbarGroup>
          </template>
        </PageToolbar>
      </template>

      <!-- 鏁版嵁琛ㄦ牸 -->
      <el-table
        v-loading="loading"
        :data="jobList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="浠诲姟缂栧彿" align="center" prop="jobId" />
        <el-table-column label="浠诲姟鍚嶇О" align="center" prop="jobName" :show-overflow-tooltip="true" />
        <el-table-column label="浠诲姟缁勫悕" align="center" prop="jobGroup">
          <template #default="{ row }">
            {{ jobGroupFormat(row) }}
          </template>
        </el-table-column>
        <el-table-column label="调用目标字符串" align="center" prop="invokeTarget" :show-overflow-tooltip="true" />
        <el-table-column label="cron 表达式" align="center" prop="cronExpression" :show-overflow-tooltip="true" />
        <el-table-column label="状态" align="center" v-permission="['sys:job:update']">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="0"
              inactive-value="1"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="鎿嶄綔" align="center" width="250">
          <template #default="{ row }">
            <PageTableActions>
              <PageTableAction
                v-permission="['sys:job:update']"
                type="info"
                :icon="VideoPlay"
                @click="handleRun(row)"
              >鎵ц涓€娆?</PageTableAction>
              <PageTableAction
                v-permission="['sys:job:update']"
                type="primary"
                :icon="Edit"
                @click="handleUpdate(row)"
              >淇敼</PageTableAction>
              <PageTableAction
                v-permission="['sys:job:delete']"
                type="danger"
                :icon="Delete"
                @click="handleDelete(row)"
              >鍒犻櫎</PageTableAction>
            </PageTableActions>
          </template>
        </el-table-column>
      </el-table>

      <!-- 鍒嗛〉 -->
      <PagePagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />

      <!-- 娣诲姞鎴栦慨鏀瑰畾鏃朵换鍔″璇濇 -->
      <el-dialog
        :title="title"
        v-model="open"
        width="700px"
        append-to-body
      >
        <el-form ref="jobFormRef" :model="form" :rules="rules" label-width="120px">
          <el-row>
            <el-col :span="12">
              <el-form-item label="浠诲姟鍚嶇О" prop="jobName">
                <el-input v-model="form.jobName" placeholder="请输入任务名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="浠诲姟缁勫悕" prop="jobGroup">
                <el-select v-model="form.jobGroup" placeholder="璇烽€夋嫨浠诲姟缁勫悕">
                  <el-option
                    v-for="dict in jobGroupOptions"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <el-form-item label="璋冪敤鏂规硶" prop="invokeTarget" >
                <el-input v-model="form.invokeTarget" placeholder="璇疯緭鍏ヨ皟鐢ㄧ洰鏍囧瓧绗︿覆">
                  <template #append>
                    <el-tooltip  placement="top">
                        <template #content>
                            Bean璋冪敤绀轰緥:neatTask.neatParams('neat')
                            <br />Class绫昏皟鐢ㄧず渚?com.neat.quartz.taskQuartz.neatParams('neat')
                            <br />鍙傛暟璇存槑锛氭敮鎸佸瓧绗︿覆锛屽竷灏旂被鍨嬶紝闀挎暣鍨嬶紝娴偣鍨嬶紝鏁村瀷
                        </template>
                      <el-icon><QuestionFilled /></el-icon>
                    </el-tooltip>
                  </template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <el-form-item label="cron 表达式" prop="cronExpression">
                <el-input v-model="form.cronExpression" placeholder="请输入 cron 表达式">
                  <template #append>
                    <el-tooltip content="Cron琛ㄨ揪寮忕敓鎴愬櫒" placement="top">
                      <el-button @click="handleShowCron">
                        <el-icon><Timer /></el-icon>
                      </el-button>
                    </el-tooltip>
                  </template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <el-form-item label="鎵ц绛栫暐" prop="misfirePolicy">
                <el-radio-group v-model="form.misfirePolicy">
                  <el-radio value="1">绔嬪嵆鎵ц</el-radio>
                  <el-radio value="2">执行一次</el-radio>
                  <el-radio value="3">鏀惧純鎵ц</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <el-form-item label="鏄惁骞跺彂" prop="concurrent">
                <el-radio-group v-model="form.concurrent">
                  <el-radio value="0">鍏佽</el-radio>
                  <el-radio value="1">绂佹</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="cancel">取消</el-button>
            <el-button type="primary" @click="submitForm">确定</el-button>

          </div>
        </template>
      </el-dialog>

      <!-- Cron琛ㄨ揪寮忕敓鎴愬櫒 -->
      <el-dialog top="5vh" title="Cron琛ㄨ揪寮忕敓鎴愬櫒" v-model="cronVisible" width="700px" append-to-body>
        <CronTab
          v-model="form.cronExpression"
          :visible="cronVisible"
          @update:visible="cronVisible = $event"
        />
      </el-dialog>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Document, Edit, Plus, VideoPlay } from '@element-plus/icons-vue'
import { listJobApi, getJobApi, addJobApi, updateJobApi, delJobApi,exportJobApi, changeJobStatusApi, runJobApi } from '@/api/monitor/job'
import CronTab from './components/CronTab.vue'

// 鍦ㄨ繖閲屽垵濮嬪寲 router
const router = useRouter()

// 閬嶅巻鍣?
const queryFormRef = ref()
const jobFormRef = ref()

// 閫変腑鏁扮粍
const selectedIds = ref<Array<string | number>>([])

const dialogVisible = ref(false)
// 闈炲崟涓鐢?
const single = ref(true)
// 闈炲涓鐢?
const multiple = ref(true)
// 鏄剧ず鎼滅储鏉′欢
const showSearch = ref(true)
// 鎬绘潯鏁?
const total = ref(0)
// 瀹氭椂浠诲姟琛ㄦ牸鏁版嵁
const jobList = ref([])
// 寮瑰嚭灞傛爣棰?
const title = ref('')
// 鏄惁鏄剧ず寮瑰嚭灞?
const open = ref(false)
// cron琛ㄨ揪寮忓脊鍑哄眰
const cronVisible = ref(false)
const loading = ref(false)
// 鏄惁鏄剧ず璇︾粏淇℃伅
const detailOpen = ref(false)
// 鏌ヨ鍙傛暟
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  jobName: undefined,
  jobGroup: undefined,
  status: undefined
})

// 琛ㄥ崟鍙傛暟
const form = reactive({
  jobId: undefined,
  jobName: undefined,
  jobGroup: undefined,
  invokeTarget: undefined,
  cronExpression: undefined,
  misfirePolicy: '1',
  concurrent: '1',
  status: '1'
})

// 琛ㄥ崟鏍￠獙
const rules = {
  jobName: [
    { required: true, message: '浠诲姟鍚嶇О涓嶈兘涓虹┖', trigger: 'blur' }
  ],
  jobGroup: [
    { required: true, message: '浠诲姟缁勫悕涓嶈兘涓虹┖', trigger: 'change' }
  ],
  invokeTarget: [
    { required: true, message: '调用目标字符串不能为空', trigger: 'blur' }
  ],
  cronExpression: [
    { required: true, message: 'cron 表达式不能为空', trigger: 'blur' }
  ]
}

// 浠诲姟缁勫悕瀛楀吀
const jobGroupOptions = [
  { value: 'DEFAULT', label: '榛樿' },
  { value: 'SYSTEM', label: '绯荤粺' }
]

// 鐘舵€佸瓧鍏?
const statusOptions = [
  { value: '0', label: '姝ｅ父' },
  { value: '1', label: '鏆傚仠' }
]

// 浠诲姟缁勫悕鏍煎紡鍖?
const jobGroupFormat = (row: any) => {
  return jobGroupOptions.find(item => item.value === row.jobGroup)?.label
}

/** 鏌ヨ瀹氭椂浠诲姟鍒楄〃 */
const getList = async () => {
  loading.value = true
  try {
    const { data } = await listJobApi(queryParams)
    jobList.value = data.records
    total.value = data.total
  } catch (error) {
  } finally {
    loading.value = false
  }
}

/** 鍙栨秷鎸夐挳 */
const cancel = () => {
  open.value = false
  reset()
}

/** 琛ㄥ崟閲嶇疆 */
const reset = () => {
  form.jobId = undefined
  form.jobName = undefined
  form.jobGroup = undefined
  form.invokeTarget = undefined
  form.cronExpression = undefined
  form.misfirePolicy = '1'
  form.concurrent = '1'
  form.status = '1'
  jobFormRef.value?.resetFields()
}

/** 鎼滅储鎸夐挳鎿嶄綔 */
const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

/** 閲嶇疆鎸夐挳鎿嶄綔 */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

/** 澶氶€夋閫変腑鏁版嵁 */
const handleSelectionChange = (selection: any[]) => {
  selectedIds.value = selection.map(item => item.jobId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 浠诲姟鐘舵€佷慨鏀?*/
const handleStatusChange = async (row: any) => {
  const text = row.status === '0' ? '鍚敤' : '鍋滅敤'
  try {
    await changeJobStatusApi(row.jobId, row.status)
    ElMessage.success(text + '鎴愬姛')
  } catch (error) {
  }
}

/** 绔嬪嵆鎵ц涓€娆?*/
const handleRun = async (row: any) => {
  try {
    await runJobApi(row)
    ElMessage.success('鎵ц鎴愬姛')
  } catch (error) {
  }
}

/** 鏄剧ずCron琛ㄨ揪寮忕敓鎴愬櫒 */
const handleShowCron = () => {
  cronVisible.value = true
}

/** 鏂板鎸夐挳鎿嶄綔 */
const handleAdd = () => {
  reset()
  open.value = true
  title.value = '娣诲姞瀹氭椂浠诲姟'
}

/** 淇敼鎸夐挳鎿嶄綔 */
const handleUpdate = async (row: any) => {
  try {
    reset()
    const { data } = await getJobApi(row.jobId)
    Object.assign(form, data)
    open.value = true
    title.value = '淇敼瀹氭椂浠诲姟'
  } catch (error) {
  }
}

/** 鎻愪氦鎸夐挳 */
const submitForm = async () => {
  try {
    await jobFormRef.value.validate()
    if (form.jobId) {
      await updateJobApi(form)
      ElMessage.success('淇敼鎴愬姛')
    } else {
      await addJobApi(form)
      ElMessage.success('鏂板鎴愬姛')
    }
    open.value = false
    getList()
  } catch (error) {
  }
}


/** 鍒犻櫎鎸夐挳鎿嶄綔 */
const handleDelete = async (row?: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除任务“${row.jobName}”吗？`)
    await delJobApi(row.jobId)
    getList()
    ElMessage.success('鍒犻櫎鎴愬姛')
  } catch (error) {
  }
}

/** 鎵归噺鍒犻櫎鎸夐挳鎿嶄綔 */
const handleBatcheDelete = async () => {
  if (!selectedIds.value?.length) {
    return ElMessage.warning('璇烽€夋嫨瑕佸垹闄ょ殑鏁版嵁')
  }
  try {
    await ElMessageBox.confirm(`确定要删除 ${selectedIds.value.length} 个定时任务吗？`)
    await delJobApi(selectedIds.value)
    getList()
    ElMessage.success('鍒犻櫎鎴愬姛')
  } catch (error) {
  }
}

/** 瀵煎嚭鎸夐挳鎿嶄綔 */
const handleExport = async () => {
  try {
    await exportJobApi(queryParams)
    ElMessage.success('瀵煎嚭鎴愬姛')
  } catch (error) {
  }
}

/** 璺宠浆浠诲姟鏃ュ織椤甸潰 */
const handleLog = () => {
  router.push('/monitor/job-log')
}

/** 鍒嗛〉澶у皬鏀瑰彉 */
const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  getList()
}

/** 椤电爜鏀瑰彉 */
const handleCurrentChange = (val: number) => {
  queryParams.pageNum = val
  getList()
}

onMounted(() => {
  getList()
})
</script>

<style lang="scss" scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
