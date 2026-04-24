<template>
  <div class="dict-data">
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
          v-permission="['sys:dict:delete']"
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

    <el-table
      :data="dictDataList"
      style="width: 100%; margin-top: 20px;"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="字典标签" prop="label" />
      <el-table-column label="字典键值" prop="value">
        <template #default="{ row }">
          <el-tag :type="row.style">{{ row.value }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sort" width="80" align="center" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center">
        <template #default="{ row }">
          <PageTableActions>
            <PageTableAction
              type="primary"
              :icon="Edit"
              v-permission="['sys:dict:update']"
              @click="handleEdit(row)"
            >
              修改
            </PageTableAction>
            <PageTableAction
              type="danger"
              :icon="Delete"
              v-permission="['sys:dict:delete']"
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增字典数据' : '修改字典数据'"
      width="500px"
      append-to-body
    >
      <el-form
        ref="dictDataFormRef"
        :model="dictDataForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="dictDataForm.dictType" disabled />
        </el-form-item>
        <el-form-item label="数据标签" prop="label">
          <el-input v-model="dictDataForm.label" placeholder="请输入数据标签" />
        </el-form-item>
        <el-form-item label="数据键值" prop="value">
          <el-input v-model="dictDataForm.value" placeholder="请输入数据键值" />
        </el-form-item>
        <el-form-item label="显示排序" prop="sort">
          <el-input-number v-model="dictDataForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="回显样式" prop="style">
          <el-select v-model="dictDataForm.style" placeholder="请选择显示样式">
            <el-option
              v-for="(item, index) in styleOptions"
              :key="index"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="dictDataForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="dictDataForm.remark"
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
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import {
  addDictDataApi,
  deleteDictDataApi,
  getDictDataListApi,
  updateDictDataApi
} from '@/api/system/dict'

const props = defineProps<{
  dictId: number
  dictType: string
}>()

const dialogVisible = ref(false)
const dialogType = ref<'add' | 'edit'>('add')
const submitLoading = ref(false)
const dictDataFormRef = ref<FormInstance>()
const dictDataList = ref<any[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  dictId: props.dictId
})

const dictDataForm = reactive<Partial<any>>({
  dictType: props.dictType,
  dictId: props.dictId,
  label: '',
  value: '',
  sort: 0,
  status: 1,
  style: 'primary',
  remark: ''
})

const styleOptions = [
  { label: 'primary', value: 'primary' },
  { label: 'success', value: 'success' },
  { label: 'info', value: 'info' },
  { label: 'warning', value: 'warning' },
  { label: 'danger', value: 'danger' }
]

const rules = {
  label: [
    { required: true, message: '请输入数据标签', trigger: 'blur' }
  ],
  value: [
    { required: true, message: '请输入数据键值', trigger: 'blur' }
  ],
  sort: [
    { required: true, message: '请输入显示排序', trigger: 'blur' }
  ],
  style: [
    { required: true, message: '请选择显示样式', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const handleSelectionChange = (selection: any[]) => {
  selectedIds.value = selection.map(item => item.id)
}

const getList = async () => {
  try {
    const { data } = await getDictDataListApi(queryParams)
    dictDataList.value = data.records
    total.value = data.total
  } catch (error) {
  }
}

const resetForm = () => {
  dictDataForm.id = undefined
  dictDataForm.dictType = props.dictType
  dictDataForm.dictId = props.dictId
  dictDataForm.label = ''
  dictDataForm.value = ''
  dictDataForm.sort = 0
  dictDataForm.status = 1
  dictDataForm.style = 'primary'
  dictDataForm.remark = ''
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
  Object.assign(dictDataForm, row)
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
      await deleteDictDataApi(selectedIds.value)
      ElMessage.success('删除成功')
      getList()
    } catch (error) {
    }
  })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要删除“${row.label}”吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteDictDataApi(row.id)
      ElMessage.success('删除成功')
      getList()
    } catch (error) {
    }
  })
}

const submitForm = async () => {
  if (!dictDataFormRef.value) return

  await dictDataFormRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }

    submitLoading.value = true
    try {
      if (dialogType.value === 'add') {
        await addDictDataApi(dictDataForm)
        ElMessage.success('新增成功')
      } else {
        await updateDictDataApi(dictDataForm)
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

const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  getList()
}

const handleCurrentChange = (val: number) => {
  queryParams.pageNum = val
  getList()
}

watch(() => props.dictId, (newVal) => {
  if (newVal) {
    queryParams.dictId = newVal
    dictDataForm.dictId = newVal
    getList()
  }
})

watch(() => props.dictType, (newVal) => {
  dictDataForm.dictType = newVal
})

if (props.dictId) {
  getList()
}
</script>
