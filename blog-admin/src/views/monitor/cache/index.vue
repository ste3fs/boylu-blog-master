<template>
  <div class="app-container cache-page">
    <el-row :gutter="16" class="cache-overview">
      <el-col :xs="24" :sm="24" :md="12">
        <el-card class="cache-card">
          <template #header>
            <div class="card-header">
              <span>基础信息</span>
              <el-button v-permission="['monitor:cache']" type="danger" @click="handleClear">
                <el-icon><Delete /></el-icon>
                清空缓存
              </el-button>
            </div>
          </template>
          <el-descriptions :column="isMobile ? 1 : 2" border>
            <el-descriptions-item label="Redis 版本">
              {{ cacheInfo.version || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="运行模式">
              {{ cacheInfo.mode || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="端口">
              {{ cacheInfo.port || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="运行时长">
              {{ cacheInfo.uptime || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="客户端连接数">
              {{ cacheInfo.clients || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="内存上限">
              {{ cacheInfo.maxmemory || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="AOF 是否开启">
              {{ cacheInfo.aofEnabled || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="RDB 保存状态">
              {{ cacheInfo.rdbLastSaveStatus || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="Key 数量">
              {{ cacheInfo.keys || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="网络输入 / 输出">
              {{ cacheInfo.instantaneousInputKbps || 0 }} / {{ cacheInfo.instantaneousOutputKbps || 0 }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="12">
        <el-card class="cache-card cache-card--memory">
          <template #header>
            <div class="card-header">
              <span>内存信息</span>
              <el-button v-permission="['monitor:cache']" type="primary" @click="getMemoryInfo">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>
          <div class="chart-box">
            <el-progress type="dashboard" :percentage="memoryUsage" :color="memoryColor">
              <template #default="{ percentage }">
                <div class="progress-content">
                  <h3>{{ percentage }}%</h3>
                  <p>内存使用率</p>
                </div>
              </template>
            </el-progress>
            <div class="memory-info">
              <p>已用内存：{{ formatMemory(memoryInfo.used) }}</p>
              <p>总内存：{{ formatMemory(memoryInfo.total) }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="cache-table-card">
      <template #header>
        <div class="card-header card-header--wrap">
          <span>缓存列表</span>
          <div class="cache-search">
            <el-input
              v-model="queryParams.key"
              placeholder="请输入缓存键名"
              clearable
              @keyup.enter="handleQuery"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="handleQuery">搜索</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="keyList">
        <el-table-column label="序号" type="index" width="70" align="center" />
        <el-table-column label="键名" prop="key" min-width="220" show-overflow-tooltip />
        <el-table-column label="类型" prop="type" width="110" align="center" />
        <el-table-column label="大小" prop="size" width="120" align="center">
          <template #default="{ row }">
            {{ formatSize(row.size) }}
          </template>
        </el-table-column>
        <el-table-column label="过期时间" prop="ttl" min-width="160" align="center">
          <template #default="{ row }">
            {{ formatTTL(row.ttl) }}
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
import { Delete, Refresh, Search } from '@element-plus/icons-vue'
import { getCacheInfoApi, getCacheKeyListApi, getCacheMemoryApi, clearCacheApi } from '@/api/monitor/cache'

const isMobile = ref(false)
const loading = ref(false)
const total = ref(0)
const keyList = ref<any[]>([])
const cacheInfo = ref<any>({})
const memoryInfo = ref<any>({
  used: 0,
  total: 0
})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  key: ''
})

const memoryUsage = computed(() => {
  if (!memoryInfo.value.total) return 0
  return Math.round((memoryInfo.value.used / memoryInfo.value.total) * 100)
})

const memoryColor = computed(() => {
  if (memoryUsage.value < 70) return '#67C23A'
  if (memoryUsage.value < 90) return '#E6A23C'
  return '#F56C6C'
})

const syncMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

const getInfo = async () => {
  const { data } = await getCacheInfoApi()
  cacheInfo.value = data || {}
}

const getMemoryInfo = async () => {
  const { data } = await getCacheMemoryApi()
  memoryInfo.value = data || { used: 0, total: 0 }
}

const getList = async () => {
  loading.value = true
  try {
    const { data } = await getCacheKeyListApi(queryParams)
    keyList.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const handleClear = async () => {
  await ElMessageBox.confirm('确定要清空所有缓存吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await clearCacheApi()
  ElMessage.success('缓存已清空')
  getList()
  getMemoryInfo()
}

const handleQuery = () => {
  queryParams.pageNum = 1
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

const formatMemory = (bytes: number) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, index)).toFixed(2))} ${sizes[index]}`
}

const formatSize = (size: number) => formatMemory(size)

const formatTTL = (ttl: number) => {
  if (ttl === -1) return '永不过期'
  if (ttl === -2) return '已过期'
  return `${ttl} 秒`
}

onMounted(() => {
  syncMobile()
  getInfo()
  getMemoryInfo()
  getList()
  window.addEventListener('resize', syncMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', syncMobile)
})
</script>

<style scoped lang="scss">
.cache-overview,
.cache-table-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.card-header--wrap {
  flex-wrap: wrap;
}

.chart-box {
  display: flex;
  justify-content: space-around;
  align-items: center;
  gap: 18px;
  padding: 12px 4px;
}

.progress-content {
  text-align: center;

  h3 {
    margin: 0;
    font-size: 24px;
    color: #303133;
  }

  p {
    margin: 6px 0 0;
    font-size: 13px;
    color: #909399;
  }
}

.memory-info {
  color: var(--el-text-color-regular);
  line-height: 1.9;
}

.cache-search {
  display: flex;
  align-items: center;
  gap: 10px;

  .el-input {
    width: 240px;
  }
}

@media (max-width: 768px) {
  .chart-box {
    flex-direction: column;
    align-items: center;
  }

  .cache-search {
    width: 100%;
    flex-direction: column;
    align-items: stretch;

    .el-input {
      width: 100%;
    }
  }
}
</style>
