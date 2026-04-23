<template>
  <div class="app-container">
    <!-- 鎼滅储琛ㄥ崟 -->
    <PageSearch>
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="瑙掕壊鍚嶇О" prop="name">
          <el-input
            v-model="queryParams.name"
            placeholder="请输入角色名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
                <el-form-item>
          <PageSearchActions @search="handleQuery" @reset="resetQuery" />
        </el-form-item>
      </el-form>
    </PageSearch>

    <!-- 鎿嶄綔鎸夐挳鍖哄煙 -->
    <el-card class="box-card">
      <template #header>
        <PageToolbar>
          <PageToolbarGroup>
            <el-button
              v-permission="['sys:role:add']"
              type="primary"
              :icon="Plus"
              @click="handleAdd"
            >鏂板</el-button>
          </PageToolbarGroup>
          <PageToolbarGroup kind="danger">
            <el-button
              v-permission="['sys:role:delete']"
              type="danger"
              plain
              :icon="Delete"
              :disabled="selectedIds.length === 0"
              @click="handleBatchDelete"
            >鎵归噺鍒犻櫎</el-button>
          </PageToolbarGroup>
        </PageToolbar>
      </template>

      <!-- 鏁版嵁琛ㄦ牸 -->
      <el-table
        v-loading="loading"
        :data="roleList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="瑙掕壊鍚嶇О" align="center" prop="name" show-overflow-tooltip />
        <el-table-column label="瑙掕壊缂栫爜" align="center" prop="code" show-overflow-tooltip />
        <el-table-column label="澶囨敞" prop="remarks"  align="center" width="400" show-overflow-tooltip />
        <el-table-column label="鍒涘缓鏃堕棿" align="center" prop="createTime" width="180" show-overflow-tooltip />
        <el-table-column label="??" align="center" width="250" fixed="right">
          <template #default="scope">
            <PageTableActions>
              <PageTableAction
                v-permission="['sys:role:update']"
                type="primary"
                :icon="Edit"
                @click="handleUpdate(scope.row)"
              >??</PageTableAction>
              <PageTableAction
                v-permission="['sys:role:menus']"
                type="primary"
                :icon="Setting"
                @click="handlePermission(scope.row)"
              >??</PageTableAction>
              <PageTableAction
                v-permission="['sys:role:delete']"
                type="danger"
                :icon="Delete"
                @click="handleDelete(scope.row)"
              >??</PageTableAction>
            </PageTableActions>
          </template>
        </el-table-column>
      </el-table>

      <!-- 鍒嗛〉缁勪欢 -->
      <PagePagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <!-- 娣诲姞鎴栦慨鏀硅鑹插璇濇 -->
    <el-dialog
      :title="dialog.title"
      v-model="dialog.visible"
      width="580px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="roleFormRef"
        :model="roleForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="瑙掕壊鍚嶇О" prop="name">
          <el-input v-model="roleForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="瑙掕壊缂栫爜" prop="code">
          <el-input v-model="roleForm.code" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="澶囨敞">
          <el-input v-model="roleForm.remarks" type="textarea" :rows="3" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>

        </div>
      </template>
    </el-dialog>

    <!-- 鍒嗛厤鏉冮檺瀵硅瘽妗?-->
    <el-dialog
      title="鍒嗛厤鏉冮檺"
      v-model="permissionDialog.visible"
      width="600px"
      append-to-body
      destroy-on-close
      top="5vh"
    >
      <el-form label-width="80px">
        <el-form-item label="瑙掕壊鍚嶇О">
          <el-input v-model="permissionDialog.roleInfo.name" disabled />
        </el-form-item>
          <el-form-item label="鏉冮檺璁剧疆">
            <el-scrollbar height="400px">
              <el-tree
                ref="menuTreeRef"
                node-key="id"
                show-checkbox
                :props="{ label: 'title', children: 'children' }"
                :data="menuOptions"
                :default-expand-all="true"
              ></el-tree>
            </el-scrollbar>
          </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="submitLoading" @click="submitPermission">确定</el-button>
          <el-button @click="permissionDialog.visible = false">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search, Setting } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getRoleListApi,
  createRoleApi,
  updateRoleApi,
  deleteRoleApi,
  getRoleMenusApi,
  updateRoleMenusApi
} from '@/api/system/role'
import {
  getMenuListApi
} from '@/api/system/menu'

// 鏌ヨ鍙傛暟
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: ''
})

const loading = ref(false)
const total = ref(0)
const roleList = ref([])
const queryFormRef = ref<FormInstance>()
const roleFormRef = ref<FormInstance>()
const menuTreeRef = ref<any>()

// 寮圭獥鎺у埗
const dialog = reactive({
  title: '',
  visible: false
})

// 琛ㄥ崟鏁版嵁
const roleForm = reactive({
  id: undefined,
  name: '',
  code: '',
  sort: 0,
  status: 1,
  remarks: ''
})

// 琛ㄥ崟鏍￠獙瑙勫垯
const rules = reactive<FormRules>({
  name: [
    { required: true, message: '瑙掕壊鍚嶇О涓嶈兘涓虹┖', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '瑙掕壊缂栫爜涓嶈兘涓虹┖', trigger: 'blur' }
  ]
})

// 鏉冮檺璁剧疆寮圭獥
const permissionDialog = reactive<any>({
  visible: false,
  roleInfo: {
    id: undefined,
    name: ''
  }
})

const menuOptions = ref<any>([])

// 娣诲姞閫変腑椤规暟缁?
const selectedIds = ref<number[]>([])

// 琛ㄦ牸閫夋嫨椤瑰彉鍖栧鐞?
const handleSelectionChange = (selection: any[]) => {
  selectedIds.value = selection.map(item => item.id)
}

// 鎵归噺鍒犻櫎澶勭悊
const handleBatchDelete = () => {
  if (selectedIds.value.length === 0) {
    return
  }

  ElMessageBox.confirm('鏄惁纭鎵归噺鍒犻櫎閫変腑鐨勮鑹?', '璀﹀憡', {
    confirmButtonText: '纭畾',
    cancelButtonText: '鍙栨秷',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteRoleApi(selectedIds.value)
      ElMessage.success('鎵归噺鍒犻櫎鎴愬姛')
      // 閲嶆柊鍔犺浇鍒楄〃
      getList()
      // 娓呯┖閫変腑
      selectedIds.value = []
    } catch (error) {
    }
  })
}

// 鑾峰彇瑙掕壊鍒楄〃
const getList = async () => {
  loading.value = true
  try {
    const { data } = await getRoleListApi(queryParams)
    roleList.value = data.records
    total.value = data.total
  } catch (error) {
    console.error(error)
  }
  loading.value = false
}

// 鎼滅储
const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

// 閲嶇疆
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

// 鏂板瑙掕壊
const handleAdd = () => {
  dialog.title = '娣诲姞瑙掕壊'
  dialog.visible = true
  Object.assign(roleForm, {
    id: undefined,
    name: '',
    code: '',
    sort: 0,
    status: 1,
    remarks: ''
  })
}

// 淇敼瑙掕壊
const handleUpdate = (row: any) => {
  dialog.title = '淇敼瑙掕壊'
  dialog.visible = true
  Object.assign(roleForm, row)
}

// 鎻愪氦琛ㄥ崟
const submitForm = async () => {
  if (!roleFormRef.value) return

  await roleFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (roleForm.id) {
          await updateRoleApi(roleForm)
          ElMessage.success('淇敼鎴愬姛')
        } else {
          await createRoleApi(roleForm)
          ElMessage.success('鏂板鎴愬姛')
        }
        dialog.visible = false
        getList()
      } catch (error) {
      }
    }
  })
}

// 鍒犻櫎瑙掕壊
const handleDelete = (row: any) => {
  ElMessageBox.confirm(`鏄惁纭鍒犻櫎瑙掕壊鍚嶇О涓?${row.name}"鐨勬暟鎹」?`, '璀﹀憡', {
    confirmButtonText: '纭畾',
    cancelButtonText: '鍙栨秷',
    type: 'warning'
  }).then(async () => {
    await deleteRoleApi(row.id)
    ElMessage.success('鍒犻櫎鎴愬姛')
    getList()
  })
}


// 鎵撳紑鏉冮檺璁剧疆
const handlePermission = async (row: any) => {
  permissionDialog.roleInfo = { ...row }
  permissionDialog.visible = true

  // 鑾峰彇瑙掕壊鐨勮彍鍗曟潈闄?
  const { data } = await getRoleMenusApi(row.id)
  const checkedMenuIds = data;
  checkedMenuIds.forEach((menuId: number) =>
    menuTreeRef.value.setChecked(menuId, true, false)
  );
}

// 鎻愪氦鏉冮檺璁剧疆
const submitPermission = async () => {
  const checkedMenuIds: number[] = menuTreeRef.value
      .getCheckedNodes(false, true)
      .map((node: any) => node.id);
  await updateRoleMenusApi(permissionDialog.roleInfo.id, checkedMenuIds)
  ElMessage.success('璁剧疆鎴愬姛')
  permissionDialog.visible = false
}

// 鍙栨秷鎸夐挳
const cancel = () => {
  dialog.visible = false
  roleFormRef.value?.resetFields()
}

// 娣诲姞鍒嗛〉澶勭悊鍑芥暟
const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  getList()
}

const handleCurrentChange = (val: number) => {
  queryParams.pageNum = val
  getList()
}
const getMenuList = async () => {
  const { data } = await getMenuListApi()
  menuOptions.value = data
}

const submitLoading = ref(false)

onMounted(() => {
  getList()
  getMenuList()
})
</script>

<style lang="scss" scoped>
:deep(.el-form-item__content) {
    display: block !important;
  }

</style>
