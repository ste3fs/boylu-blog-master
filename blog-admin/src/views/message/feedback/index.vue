<template>
  <div class="app-container">
    <PageSearch>
      <el-form ref="queryFormRef" :model="queryParams" inline>
        <el-form-item label="反馈类型" prop="type">
          <el-select
            v-model="queryParams.type"
            placeholder="请选择反馈类型"
            clearable
            @keyup.enter="handleQuery"
          >
            <el-option
              v-for="item in feedbackTypes"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select
            v-model="queryParams.status"
            placeholder="请选择状态"
            clearable
            @keyup.enter="handleQuery"
          >
            <el-option
              v-for="item in feedbackStatus"
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
              type="danger"
              plain
              :icon="Delete"
              :disabled="selectedIds.length === 0"
              v-permission="['sys:feedback:delete']"
              @click="handleBatchDelete"
            >
              批量删除
            </el-button>
          </PageToolbarGroup>
        </PageToolbar>
      </template>

      <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="头像" align="center" prop="avatar" width="100">
          <template #default="scope">
            <el-avatar :src="scope.row.avatar" alt="Avatar" :size="50" />
          </template>
        </el-table-column>
        <el-table-column label="昵称" align="center" prop="nickname" />
        <el-table-column label="反馈类型" align="center" prop="type">
          <template #default="scope">
            <span v-for="item in feedbackTypes" :key="item.value">
              <el-tag v-if="item.value === scope.row.type" :type="item.style">
                {{ item.label }}
              </el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="反馈内容" align="center" prop="content" show-overflow-tooltip />
        <el-table-column label="联系邮箱" align="center" prop="email" />
        <el-table-column label="回复内容" align="center" prop="replyContent" show-overflow-tooltip />
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <span v-for="item in feedbackStatus" :key="item.value">
              <el-tag v-if="item.value === String(scope.row.status)" :type="item.style">
                {{ item.label }}
              </el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="170" />
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <PageTableActions>
              <PageTableAction
                type="primary"
                :icon="Edit"
                v-permission="['sys:feedback:update']"
                @click="handleUpdate(scope.row)"
              >
                修改
              </PageTableAction>
              <PageTableAction
                type="danger"
                :icon="Delete"
                v-permission="['sys:feedback:delete']"
                @click="handleDelete(scope.row)"
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

      <el-dialog v-model="open" :title="title" width="500px" append-to-body>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
          <el-form-item label="回复内容" prop="replyContent">
            <el-input
              v-model="form.replyContent"
              type="textarea"
              :rows="5"
              placeholder="请输入回复内容"
            />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio
                v-for="item in feedbackStatus"
                :key="item.value"
                :value="Number(item.value)"
              >
                {{ item.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button type="primary" @click="submitForm">确定</el-button>
            <el-button @click="cancel">取消</el-button>
          </div>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit } from '@element-plus/icons-vue'
import {
  addSysFeedbackApi,
  deleteSysFeedbackApi,
  listSysFeedbackApi,
  updateSysFeedbackApi
} from '@/api/message/feedback'
import { getDictDataByDictTypesApi } from '@/api/system/dict'

const loading = ref(true)
const selectedIds = ref<number[]>([])
const total = ref(0)
const dataList = ref<any[]>([])
const title = ref('')
const open = ref(false)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  type: undefined as string | undefined,
  status: undefined as string | undefined,
  source: 'admin'
})

const form = reactive<any>({
  id: undefined,
  userId: undefined,
  type: undefined,
  content: undefined,
  email: undefined,
  replyContent: '',
  status: undefined,
  createTime: undefined
})

const rules = reactive({
  replyContent: [
    { required: false, message: '回复内容不能为空', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '状态不能为空', trigger: 'blur' }
  ]
})

const queryFormRef = ref()
const formRef = ref()
const feedbackTypes = ref<any[]>([])
const feedbackStatus = ref<any[]>([])

const getList = () => {
  loading.value = true
  listSysFeedbackApi(queryParams).then((response) => {
    dataList.value = response.data.records
    total.value = response.data.total
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

const getDicts = () => {
  getDictDataByDictTypesApi(['feedback_type', 'feedback_status']).then((res) => {
    feedbackTypes.value = res.data.feedback_type.list
    feedbackStatus.value = res.data.feedback_status.list
  })
}

const resetForm = () => {
  Object.assign(form, {
    id: undefined,
    userId: undefined,
    type: undefined,
    content: undefined,
    email: undefined,
    replyContent: '',
    status: undefined,
    createTime: undefined
  })
  formRef.value?.resetFields()
}

const cancel = () => {
  open.value = false
  resetForm()
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const handleSelectionChange = (selection: { id: number }[]) => {
  selectedIds.value = selection.map(item => item.id)
}

const handleUpdate = (row: any) => {
  resetForm()
  Object.assign(form, row)
  open.value = true
  title.value = '修改反馈'
}

const submitForm = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) {
      return
    }

    if (form.id !== undefined) {
      updateSysFeedbackApi(form).then(() => {
        ElMessage.success('修改成功')
        open.value = false
        getList()
      })
      return
    }

    addSysFeedbackApi(form).then(() => {
      ElMessage.success('新增成功')
      open.value = false
      getList()
    })
  })
}

const handleBatchDelete = () => {
  if (!selectedIds.value.length) {
    return
  }

  ElMessageBox.confirm(`是否确认删除 ${selectedIds.value.length} 条反馈记录？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteSysFeedbackApi(selectedIds.value)
    getList()
    ElMessage.success('删除成功')
  })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`是否确认删除这条反馈：“${row.content}”？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteSysFeedbackApi(row.id)
    getList()
    ElMessage.success('删除成功')
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

onMounted(() => {
  getList()
  getDicts()
})
</script>
