<template>
  <div class="dict-container">
    <PageSearch>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="字典名称">
          <el-input
            v-model="queryParams.name"
            placeholder="请输入字典名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="启用" value="1" />
            <el-option label="禁用" value="0" />
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
            >
              新增
            </el-button>
          </PageToolbarGroup>
          <PageToolbarGroup kind="danger">
            <el-button
              v-permission="['sys:dict:deleteBatch']"
              type="danger"
              plain
              :icon="Delete"
              :disabled="selectedIds.length === 0"
              @click="handleBatchDelete"
            >
              批量删除
            </el-button>
          </PageToolbarGroup>
        </PageToolbar>
      </template>

      <el-table
        v-loading="loading"
        :data="dictList"
        @selection-change="handleSelectionChange"
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="字典名称" prop="name" align="center" />
        <el-table-column label="字典类型" prop="type" align="center">
          <template #default="{ row }">
            <el-tag type="warning">
              {{ row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
        <el-table-column label="创建时间" align="center" prop="createTime" width="200" />
        <el-table-column label="操作" width="250" align="center">
          <template #default="{ row }">
            <PageTableActions>
              <PageTableAction :icon="List" type="success" @click="handleData(row)">
                字典数据
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

      <PagePagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增字典' : '修改字典'"
      width="500px"
      append-to-body
    >
      <el-form
        ref="dictFormRef"
        :model="dictForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="字典名称" prop="name">
          <el-input v-model="dictForm.name" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="字典类型" prop="type">
          <el-input
            v-model="dictForm.type"
            :disabled="dialogType === 'edit'"
            placeholder="请输入字典类型"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="dictForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="dictForm.remark"
            type="textarea"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dataDialogVisible"
      :title="`字典数据 - ${currentDict?.name}`"
      width="800px"
      append-to-body
      :close-on-click-modal="false"
    >
      <dict-data :dict-id="currentDict?.id" :dict-type="currentDict?.type" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, List, Plus } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import {
  addDictApi,
  deleteDictApi,
  getDictListApi,
  updateDictApi
} from '@/api/system/dict'
import DictData from './components/DictData.vue'

const loading = ref(false)
const total = ref(0)
const dictList = ref<any[]>([])
const dialogVisible = ref(false)
const dialogType = ref<'add' | 'edit'>('add')
const submitLoading = ref(false)
const dictFormRef = ref<FormInstance>()
const selectedIds = ref<number[]>([])
const dataDialogVisible = ref(false)
const currentDict = ref<any>()

const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  name: '',
  type: '',
  status: ''
})

const dictForm = reactive<Partial<any>>({
  name: '',
  type: '',
  status: 1,
  remark: ''
})

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

const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

const resetQuery = () => {
  queryParams.name = ''
  queryParams.status = ''
  handleQuery()
}

const resetForm = () => {
  dictForm.id = undefined
  dictForm.name = ''
  dictForm.type = ''
  dictForm.status = 1
  dictForm.remark = ''
}

const handleAdd = () => {
  resetForm()
  dialogType.value = 'add'
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  resetForm()
  dialogType.value = 'edit'
  dialogVisible.value = true
  Object.assign(dictForm, row)
}

const submitForm = async () => {
  if (!dictFormRef.value) return

  await dictFormRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }

    submitLoading.value = true
    try {
      if (dialogType.value === 'add') {
        await addDictApi(dictForm)
        ElMessage.success('新增成功')
      } else {
        await updateDictApi(dictForm)
        ElMessage.success('修改成功')
      }
      dialogVisible.value = false
      getList()
    } catch (error) {
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要删除字典“${row.name}”吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteDictApi(row.id)
      ElMessage.success('删除成功')
      getList()
    } catch (error) {
    }
  })
}

const handleData = (row: any) => {
  currentDict.value = row
  dataDialogVisible.value = true
}

const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  getList()
}

const handleCurrentChange = (val: number) => {
  queryParams.pageNum = val
  getList()
}

const handleSelectionChange = (selection: any[]) => {
  selectedIds.value = selection.map(item => item.id)
}

const handleBatchDelete = () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请选择要删除的记录')
    return
  }

  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedIds.value.length} 条记录吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteDictApi(selectedIds.value)
      ElMessage.success('删除成功')
      getList()
    } catch (error) {
    }
  })
}

getList()
</script>

<style scoped>
.search-form {
  margin-bottom: 20px;
}
</style>
