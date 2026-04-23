<template>
  <div class="dict-container">
    <!-- 鎼滅储琛ㄥ崟 -->
    <PageSearch>
      <!-- 鎼滅储鍖哄煙 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="瀛楀吀鍚嶇О">
          <el-input
            v-model="queryParams.name"
            placeholder="请输入字典名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="鍚敤" value="1" />
            <el-option label="绂佺敤" value="0" />
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
              v-permission="['sys:dict:add']"
              type="primary"
              :icon="Plus"
              @click="handleAdd"
            >鏂板</el-button>
          </PageToolbarGroup>
          <PageToolbarGroup kind="danger">
            <el-button
              v-permission="['sys:dict:deleteBatch']"
              type="danger"
              plain
              :icon="Delete"
              :disabled="selectedIds.length === 0"
              @click="handleBatchDelete"
            >鎵归噺鍒犻櫎</el-button>
          </PageToolbarGroup>
        </PageToolbar>
      </template>

      <!-- 琛ㄦ牸鍖哄煙 -->
      <el-table
        v-loading="loading"
        :data="dictList"
        @selection-change="handleSelectionChange"
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="瀛楀吀鍚嶇О" prop="name" align="center"/>
        <el-table-column label="瀛楀吀绫诲瀷" prop="type" align="center">
          <template #default="{ row }">
            <el-tag type="warning">
              {{ row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '鍚敤' : '绂佺敤' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="澶囨敞" align="center" prop="remark" show-overflow-tooltip />
        <el-table-column label="鍒涘缓鏃堕棿" align="center" prop="createTime" width="200" />
        <el-table-column label="操作" width="250" align="center">
          <template #default="{ row }">
            <PageTableActions>
              <PageTableAction :icon="List" type="success" @click="handleData(row)">
                瀛楀吀鏁版嵁
              </PageTableAction>
              <PageTableAction
                v-permission="['sys:dict:update']"
                type="primary"
                :icon="Edit"
                @click="handleEdit(row)"
              >
                修改
              </PageTableAction>
              <PageTableAction
                v-permission="['sys:dict:delete']"
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

      <!-- 鍒嗛〉鍖哄煙 -->
      <PagePagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <!-- 瀛楀吀绫诲瀷瀵硅瘽妗?-->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '鏂板瀛楀吀' : '淇敼瀛楀吀'"
      width="500px"
      append-to-body
    >
      <el-form
        ref="dictFormRef"
        :model="dictForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="瀛楀吀鍚嶇О" prop="name">
          <el-input v-model="dictForm.name" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="瀛楀吀绫诲瀷" prop="type">
          <el-input :disabled="dialogType === 'edit'" v-model="dictForm.type" placeholder="请输入字典类型" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="dictForm.status">
            <el-radio :value="1">鍚敤</el-radio>
            <el-radio :value="0">绂佺敤</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="澶囨敞" prop="remark">
          <el-input
            v-model="dictForm.remark"
            type="textarea"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">鍙栨秷</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">纭畾</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 瀛楀吀鏁版嵁瀵硅瘽妗?-->
    <el-dialog
      v-model="dataDialogVisible"
      :title="`瀛楀吀鏁版嵁 - ${currentDict?.name}`"
      width="800px"
      append-to-body
      :close-on-click-modal="false"
    >
      <dict-data
        :dict-id="currentDict?.id"
        :dict-type="currentDict?.type"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, List, Plus } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import {
  getDictListApi,
  addDictApi,
  updateDictApi,
  deleteDictApi
} from '@/api/system/dict'
import DictData from './components/DictData.vue'

const loading = ref(false)
const total = ref(0)
const dictList = ref<any[]>([])
const dialogVisible = ref(false)
const dialogType = ref<'add' | 'edit'>('add')
const submitLoading = ref(false)
const dictFormRef = ref<FormInstance>()

// 鏌ヨ鍙傛暟
const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  name: '',
  type: '',
  status: ''
})

// 瀛楀吀琛ㄥ崟瀵硅薄
const dictForm = reactive<Partial<any>>({
  name: '',
  type: '',
  status: '1',
  remark: ''
})

// 琛ㄥ崟鏍￠獙瑙勫垯
const rules = {
  name: [
    { required: true, message: '请输入字典名称', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请输入字典类型', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// 鑾峰彇瀛楀吀鍒楄〃
const getList = async () => {
  loading.value = true
  try {
    const { data } = await getDictListApi(queryParams)
    dictList.value = data.records
    total.value = data.total
  } catch (error) {
  }
  loading.value = false
}

// 鎼滅储
const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

// 閲嶇疆鏌ヨ
const resetQuery = () => {
  queryParams.name = ''
  queryParams.status = ''
  handleQuery()
}

// 閲嶇疆琛ㄥ崟
const resetForm = () => {
  dictForm.id = undefined
  dictForm.name = ''
  dictForm.type = ''
  dictForm.status = 1
  dictForm.remark = ''
}

// 鏂板瀛楀吀
const handleAdd = () => {
  resetForm()
  dialogType.value = 'add'
  dialogVisible.value = true
}

// 淇敼瀛楀吀
const handleEdit = (row: any) => {
  resetForm()
  dialogType.value = 'edit'
  dialogVisible.value = true
  Object.assign(dictForm, row)
}

// 鎻愪氦琛ㄥ崟
const submitForm = async () => {
  if (!dictFormRef.value) return

  await dictFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (dialogType.value === 'add') {
          await addDictApi(dictForm)
          ElMessage.success('鏂板鎴愬姛')
        } else {
          await updateDictApi(dictForm)
          ElMessage.success('淇敼鎴愬姛')
        }
        dialogVisible.value = false
        getList()
      } catch (error) {
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 鍒犻櫎瀛楀吀
const handleDelete = (row: any) => {
  ElMessageBox.confirm(
    `确定要删除字典“${row.name}”吗？`,
    '璀﹀憡',
    {
      confirmButtonText: '纭畾',
      cancelButtonText: '鍙栨秷',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteDictApi(row.id)
      ElMessage.success('鍒犻櫎鎴愬姛')
      getList()
    } catch (error) {
    }
  })
}

// 瀛楀吀鏁版嵁鐩稿叧
const dataDialogVisible = ref(false)
const currentDict = ref<any>()

// 娣诲姞 handleData 鏂规硶
const handleData = (row: any) => {
  currentDict.value = row
  dataDialogVisible.value = true
}

// 鍒嗛〉鏂规硶
const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  getList()
}

const handleCurrentChange = (val: number) => {
  queryParams.pageNum = val
  getList()
}

// 娣诲姞閫変腑椤规暟缁?
const selectedIds = ref<number[]>([])

// 閫夋嫨鍙樺寲
const handleSelectionChange = (selection: any[]) => {
  selectedIds.value = selection.map(item => item.id)
}

// 鎵归噺鍒犻櫎
const handleBatchDelete = () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('璇烽€夋嫨瑕佸垹闄ょ殑璁板綍')
    return
  }

  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedIds.value.length} 条记录吗？`,
    '璀﹀憡',
    {
      confirmButtonText: '纭畾',
      cancelButtonText: '鍙栨秷',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteDictApi(selectedIds.value)
      ElMessage.success('鍒犻櫎鎴愬姛')
      getList()
    } catch (error) {
    }
  })
}

// 鍒濆鍖?
getList()
</script>

<style scoped>

.search-form {
  margin-bottom: 20px;
}

</style>
