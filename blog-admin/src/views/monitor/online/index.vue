<template>
  <div class="app-container">
    <PageSearch>
      <el-form ref="queryFormRef" :model="queryParams" :inline="!isMobile">
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="queryParams.username"
            placeholder="请输入用户名"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <PageSearchActions @search="handleQuery" @reset="resetQuery" />
        </el-form-item>
      </el-form>
    </PageSearch>

    <el-card class="box-card">
      <el-table v-loading="loading" :data="tableData">
        <el-table-column label="会话凭证" prop="tokenValue" min-width="220" show-overflow-tooltip />
        <el-table-column label="账号" prop="username" min-width="120" show-overflow-tooltip />
        <el-table-column label="登录 IP" prop="ip" min-width="140" />
        <el-table-column label="登录地点" prop="ipLocation" min-width="140" show-overflow-tooltip />
        <el-table-column label="浏览器" prop="browser" min-width="130" show-overflow-tooltip />
        <el-table-column label="操作系统" prop="os" min-width="130" show-overflow-tooltip />
        <el-table-column label="登录时间" prop="lastLoginTime" min-width="170" align="center" />
        <el-table-column label="操作" align="center" width="120" fixed="right">
          <template #default="{ row }">
            <PageTableActions>
              <PageTableAction
                v-permission="['monitor:online:forceLogout']"
                type="danger"
                :icon="Delete"
                @click="handleDelete(row)"
              >
                强退
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
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import { forceLogoutApi, getOnlineUserApi } from '@/api/system/user'

const isMobile = ref(false)
const loading = ref(false)
const total = ref(0)
const tableData = ref<any[]>([])
const queryFormRef = ref<FormInstance>()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  username: null as string | null
})

const syncMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

const getList = async () => {
  loading.value = true
  try {
    const { data } = await getOnlineUserApi(queryParams)
    tableData.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm(
    `确定要强制下线账号“${row.username}”吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await forceLogoutApi(row.tokenValue)
  ElMessage.success('用户已强制下线')
  getList()
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
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
