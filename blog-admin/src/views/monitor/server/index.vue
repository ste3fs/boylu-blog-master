<template>
  <div v-loading="loading" class="app-container server-page">
    <el-row :gutter="16" class="server-overview">
      <el-col :xs="24" :sm="24" :md="12">
        <el-card class="server-metric-card">
          <template #header>
            <div class="card-header">
              <span>CPU 使用率</span>
            </div>
          </template>
          <div class="chart-wrapper">
            <el-progress type="dashboard" :percentage="serverInfo.cpu?.used || 0" :color="customColors" />
          </div>
          <div class="detail-list">
            <div class="detail-item">
              <span>核心数</span>
              <strong>{{ serverInfo.cpu?.cpuNum || 0 }}</strong>
            </div>
            <div class="detail-item">
              <span>用户占用</span>
              <strong>{{ serverInfo.cpu?.used || 0 }}%</strong>
            </div>
            <div class="detail-item">
              <span>系统占用</span>
              <strong>{{ serverInfo.cpu?.sys || 0 }}%</strong>
            </div>
            <div class="detail-item">
              <span>空闲率</span>
              <strong>{{ serverInfo.cpu?.free || 0 }}%</strong>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="12">
        <el-card class="server-metric-card">
          <template #header>
            <div class="card-header">
              <span>内存使用率</span>
            </div>
          </template>
          <div class="chart-wrapper">
            <el-progress type="dashboard" :percentage="serverInfo.mem?.usage || 0" :color="customColors" />
          </div>
          <div class="detail-list">
            <div class="detail-item">
              <span>总内存</span>
              <strong>{{ formatBytes(serverInfo.mem?.total) }}</strong>
            </div>
            <div class="detail-item">
              <span>已用内存</span>
              <strong>{{ formatBytes(serverInfo.mem?.used) }}</strong>
            </div>
            <div class="detail-item">
              <span>剩余内存</span>
              <strong>{{ formatBytes(serverInfo.mem?.free) }}</strong>
            </div>
            <div class="detail-item">
              <span>使用率</span>
              <strong>{{ serverInfo.mem?.usage || 0 }}%</strong>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="server-detail-card">
      <template #header>
        <div class="card-header">
          <span>服务器信息</span>
        </div>
      </template>
      <div class="detail-list detail-list--wide">
        <div class="detail-item">
          <span>服务器名称</span>
          <strong>{{ serverInfo.sys?.computerName || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span>操作系统</span>
          <strong>{{ serverInfo.sys?.osName || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span>服务器 IP</span>
          <strong>{{ serverInfo.sys?.computerIp || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span>系统架构</span>
          <strong>{{ serverInfo.sys?.osArch || '-' }}</strong>
        </div>
      </div>
    </el-card>

    <el-card class="server-detail-card">
      <template #header>
        <div class="card-header">
          <span>JVM 信息</span>
        </div>
      </template>
      <div class="detail-list detail-list--wide">
        <div class="detail-item">
          <span>JVM 名称</span>
          <strong>{{ serverInfo.jvm?.name || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span>JVM 版本</span>
          <strong>{{ serverInfo.jvm?.version || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span>启动时间</span>
          <strong>{{ serverInfo.jvm?.startTime || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span>运行时长</span>
          <strong>{{ serverInfo.jvm?.runTime || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span>安装路径</span>
          <strong>{{ serverInfo.jvm?.home || '-' }}</strong>
        </div>
        <div class="detail-item">
          <span>项目路径</span>
          <strong>{{ serverInfo.sys?.userDir || '-' }}</strong>
        </div>
      </div>
    </el-card>

    <el-card class="server-detail-card">
      <template #header>
        <div class="card-header">
          <span>磁盘信息</span>
        </div>
      </template>
      <el-table :data="serverInfo.sysFiles || []" border>
        <el-table-column prop="dirName" label="盘符路径" min-width="120" />
        <el-table-column prop="typeName" label="文件系统" min-width="120" />
        <el-table-column label="总大小" min-width="110">
          <template #default="{ row }">
            {{ formatToGB(row.total) }}
          </template>
        </el-table-column>
        <el-table-column label="可用大小" min-width="110">
          <template #default="{ row }">
            {{ formatToGB(row.free) }}
          </template>
        </el-table-column>
        <el-table-column label="已用大小" min-width="110">
          <template #default="{ row }">
            {{ formatToGB(row.used) }}
          </template>
        </el-table-column>
        <el-table-column prop="usage" label="使用率" min-width="140">
          <template #default="{ row }">
            <el-progress :percentage="row.usage || 0" :color="customColors" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { getServerInfoApi } from '@/api/monitor/server'

const loading = ref(false)
const serverInfo = ref<any>({})
let timer: number | undefined

const customColors = [
  { color: '#67C23A', percentage: 60 },
  { color: '#E6A23C', percentage: 80 },
  { color: '#F56C6C', percentage: 100 }
]

const formatBytes = (bytes?: number) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, index)).toFixed(2)} ${sizes[index]}`
}

const formatToGB = (bytes?: number) => {
  if (!bytes) return '0 GB'
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
}

const getList = async () => {
  loading.value = true
  try {
    const { data } = await getServerInfoApi()
    serverInfo.value = data || {}
  } finally {
    loading.value = false
  }
}

const startTimer = () => {
  timer = window.setInterval(() => {
    getList()
  }, 30000)
}

onMounted(() => {
  getList()
  startTimer()
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped lang="scss">
.server-overview,
.server-detail-card {
  margin-bottom: 16px;
}

.chart-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.detail-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-list--wide {
  gap: 16px 20px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--el-fill-color-extra-light);
}

.detail-item span {
  color: var(--el-text-color-secondary);
}

.detail-item strong {
  color: var(--el-text-color-primary);
  font-weight: 600;
  text-align: right;
  word-break: break-word;
}

@media (max-width: 768px) {
  .detail-list {
    grid-template-columns: 1fr;
  }
}
</style>
