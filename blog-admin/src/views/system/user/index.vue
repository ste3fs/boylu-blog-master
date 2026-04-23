<template>
  <div class="app-container">
    <!-- 鎼滅储琛ㄥ崟 -->
    <PageSearch>
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="用户名" prop="nickname">
          <el-input
            v-model="queryParams.nickname"
            placeholder="璇疯緭鍏ョ敤鎴峰悕"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="鐧诲綍鏂瑰紡" prop="loginType">
          <el-select v-model="queryParams.loginType" placeholder="璇烽€夋嫨鐧诲綍鏂瑰紡" clearable>
            <el-option v-for="item in loginTypes" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
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

    <!-- 鎿嶄綔鎸夐挳鍖哄煙 -->
    <el-card class="box-card">
      <template #header>
        <PageToolbar>
          <PageToolbarGroup>
            <el-button
              v-permission="['sys:user:add']"
              type="primary"
              :icon="Plus"
              @click="handleAdd"
            >鏂板</el-button>
          </PageToolbarGroup>
          <PageToolbarGroup kind="danger">
            <el-button
             v-permission="['sys:user:delete']"
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
        :data="userList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection"  width="55" align="center" />
        <el-table-column label="澶村儚"  prop="avatar" align="center">
          <template #default="{ row }">
            <el-image :src="row.avatar" style="width: 40px; height: 40px; border-radius: 5px;" />
          </template>
        </el-table-column>
        <el-table-column label="鏄电О" align="center" prop="nickname" show-overflow-tooltip />
        <el-table-column label="鐧诲綍鏂瑰紡" align="center" prop="ipLocation" >
          <template #default="{ row }">
            <span v-for="item in loginTypes">
                <el-tag :type="item.style" v-if="row.loginType === item.value">
                  {{ item.label}}
              </el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="鐧诲綍IP" align="center" prop="ip" show-overflow-tooltip />
        <el-table-column label="鐧诲綍鍦板潃" align="center" prop="ipLocation" show-overflow-tooltip />
        <el-table-column label="状态" align="center" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '鍚敤' : '绂佺敤' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录时间" align="center" prop="lastLoginTime" width="160" />
        <el-table-column label="鍒涘缓鏃堕棿" align="center" prop="createTime" width="160" />
        <el-table-column label="鎿嶄綔" align="center" width="280" fixed="right">
          <template #default="scope">
            <PageTableActions>
              <PageTableAction
                v-permission="['sys:user:update']"
                type="primary"
                :icon="Edit"
                @click="handleUpdate(scope.row)"
              >淇敼</PageTableAction>
              <PageTableAction
                v-permission="['sys:user:reset']"
                type="info"
                :icon="Key"
                @click="handleResetPwd(scope.row)"
              >閲嶇疆瀵嗙爜</PageTableAction>
              <PageTableAction
                v-permission="['sys:user:delete']"
                type="danger"
                :icon="Delete"
                @click="handleDelete(scope.row)"
              >鍒犻櫎</PageTableAction>
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

    <!-- 娣诲姞鎴栦慨鏀圭敤鎴峰璇濇 -->
    <el-dialog
      :title="dialog.title"
      v-model="dialog.visible"
      width="600px"
      append-to-body
      destroy-on-close
      class="custom-dialog"
    >
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="rules"
        label-width="80px"
        class="custom-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="userForm.username"
                placeholder="璇疯緭鍏ョ敤鎴峰悕"
                :disabled="dialog.type === 'edit'"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="鏄电О" prop="nickname">
              <el-input
                v-model="userForm.nickname"
                placeholder="请输入昵称"
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号" prop="mobile">
              <el-input
                v-model="userForm.mobile"
                placeholder="璇疯緭鍏ユ墜鏈哄彿"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="閭" prop="email">
              <el-input
                v-model="userForm.email"
                placeholder="请输入邮箱"
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="鎬у埆" prop="sex">
              <el-radio-group v-model="userForm.sex">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
                <el-radio :value="0">淇濆瘑</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="瀵嗙爜" prop="password" v-if="dialog.type === 'add'">
              <el-input
                v-model="userForm.password"
                type="password"
                placeholder="请输入密码"
                show-password
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="瑙掕壊" prop="roleIds">
          <el-select
            v-model="userForm.roleIds"
            multiple
            placeholder="璇烽€夋嫨瑙掕壊"
            style="width: 100%"
            :disabled="userForm.username === 'admin'"
            clearable

          >
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="userForm.status">
            <el-radio :value="1">鍚敤</el-radio>
            <el-radio :value="0">绂佺敤</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 娣诲姞閲嶇疆瀵嗙爜寮圭獥 -->
    <el-dialog
      title="閲嶇疆瀵嗙爜"
      v-model="resetPwdDialog.visible"
      width="500px"
      append-to-body
      destroy-on-close
      class="custom-dialog"
    >
      <el-form
        ref="resetPwdFormRef"
        :model="resetPwdForm"
        :rules="resetPwdRules"
        label-width="100px"
      >
        <el-form-item label="新密码" prop="password">
          <el-input
            v-model="resetPwdForm.password"
            type="password"
            placeholder="璇疯緭鍏ユ柊瀵嗙爜"
            show-password
            clearable
          />
        </el-form-item>
        <el-form-item label="纭瀵嗙爜" prop="confirmPassword">
          <el-input
            v-model="resetPwdForm.confirmPassword"
            type="password"
            placeholder="璇峰啀娆¤緭鍏ユ柊瀵嗙爜"
            show-password
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="resetPwdDialog.visible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitResetPwd">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Key, Plus, Refresh, Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getUserListApi,
  createUserApi,
  updateUserApi,
  deleteUserApi,
  resetPasswordApi
} from '@/api/system/user'
import { getAllRoleList } from '@/api/system/role'
import { getDictDataByDictTypesApi } from '@/api/system/dict'

// 鏌ヨ鍙傛暟
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  nickname: '',
  status: '',
  loginType: ''
})

const loading = ref(false)
const total = ref(0)
const userList = ref([])
const queryFormRef = ref<FormInstance>()
const userFormRef = ref<FormInstance>()
const submitLoading = ref(false)

// 閫変腑椤规暟缁?
const selectedIds = ref<string[]>([])

// 寮圭獥鎺у埗
const dialog = reactive({
  title: '',
  visible: false,
  type: 'add'
})

// 瑙掕壊閫夐」
const roleOptions = ref<any[]>([])

// 琛ㄥ崟鏁版嵁
const userForm = reactive({
  id: undefined,
  username: '',
  nickname: '',
  password: null,
  mobile: '',
  email: '',
  sex: 0,
  status: 1,
  ip: undefined,
  ipLocation: undefined,
  lastLoginTime: undefined,
  createTime: undefined,
  roleIds: [] as number[]
})

// 琛ㄥ崟鏍￠獙瑙勫垯
const rules = reactive<FormRules>({
  username: [
    { required: true, message: '璇疯緭鍏ョ敤鎴峰悕', trigger: 'blur' },
    { min: 3, max: 20, message: '长度应在 3 到 20 个字符之间', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度应在 6 到 20 个字符之间', trigger: 'blur' }
  ],
  mobile: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '璇疯緭鍏ユ纭殑閭鍦板潃', trigger: 'blur' }
  ],
  roleIds: [
    { required: true, message: '璇烽€夋嫨瑙掕壊', trigger: 'change' }
  ],
  sex: [
    { required: true, message: '璇烽€夋嫨鎬у埆', trigger: 'change' }
  ]
})

// 閲嶇疆瀵嗙爜寮圭獥鎺у埗
const resetPwdDialog = reactive({
  id: undefined,
  visible: false,
  userId: undefined
})

// 閲嶇疆瀵嗙爜琛ㄥ崟
const resetPwdForm = reactive({
  password: '',
  confirmPassword: ''
})

// 閲嶇疆瀵嗙爜琛ㄥ崟鏍￠獙瑙勫垯
const resetPwdRules = reactive<FormRules>({
  password: [
    { required: true, message: '璇疯緭鍏ユ柊瀵嗙爜', trigger: 'blur' },
    { min: 6, max: 20, message: '长度应在 6 到 20 个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '璇峰啀娆¤緭鍏ユ柊瀵嗙爜', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== resetPwdForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

const resetPwdFormRef = ref<FormInstance>()

const loginTypes = ref<any>([])

// 鑾峰彇鐢ㄦ埛鍒楄〃
const getList = async () => {
  loading.value = true
  try {
    const { data } = await getUserListApi(queryParams)
    userList.value = data.records
    total.value = data.total
  } catch (error) {
  }
  loading.value = false
}

// 琛ㄦ牸閫夋嫨椤瑰彉鍖?
const handleSelectionChange = (selection: any[]) => {
  selectedIds.value = selection.map(item => item.id)
}

// 鎵归噺鍒犻櫎
const handleBatchDelete = () => {
  if (selectedIds.value.length === 0) return

  ElMessageBox.confirm('鏄惁纭鎵归噺鍒犻櫎閫変腑鐨勭敤鎴?', '璀﹀憡', {
    confirmButtonText: '纭畾',
    cancelButtonText: '鍙栨秷',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteUserApi(selectedIds.value)
      ElMessage.success('鎵归噺鍒犻櫎鎴愬姛')
      getList()
      selectedIds.value = []
    } catch (error) {
    }
  })
}

// 鎼滅储
const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

// 閲嶇疆鏌ヨ
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

// 鏂板鐢ㄦ埛
const handleAdd = () => {
  dialog.type = 'add'
  dialog.title = '鏂板鐢ㄦ埛'
  dialog.visible = true
  userForm.id = undefined
  userForm.username = ''
  userForm.nickname = ''
  userForm.password = null
  userForm.mobile = ''
  userForm.email = ''
  userForm.sex = 0
  userForm.status = 1
  userForm.ip = undefined
  userForm.ipLocation = undefined
  userForm.lastLoginTime = undefined
  userForm.createTime = undefined
  userForm.roleIds = []
}

// 淇敼鐢ㄦ埛
const handleUpdate = (row: any) => {
  dialog.type = 'edit'
  dialog.title = '淇敼鐢ㄦ埛'
  dialog.visible = true
  Object.assign(userForm, row)
  userForm.password = null
}

// 鎻愪氦琛ㄥ崟
const submitForm = async () => {
  if (!userFormRef.value) return

  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const data = {user: userForm, roleIds: userForm.roleIds}
        if (dialog.type === 'add') {
          await createUserApi(data)
          ElMessage.success('鏂板鎴愬姛')
        } else {
          await updateUserApi(data)
          ElMessage.success('淇敼鎴愬姛')
        }
        dialog.visible = false
        getList()
      } catch (error) {
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 鍒犻櫎鐢ㄦ埛
const handleDelete = (row: any) => {
  ElMessageBox.confirm(`鏄惁纭鍒犻櫎鐢ㄦ埛"${row.username}"?`, '璀﹀憡', {
    confirmButtonText: '纭畾',
    cancelButtonText: '鍙栨秷',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteUserApi(row.id)
      ElMessage.success('鍒犻櫎鎴愬姛')
      getList()
    } catch (error) {
    }
  })
}

// 淇敼閲嶇疆瀵嗙爜鏂规硶
const handleResetPwd = (row: any) => {
  resetPwdDialog.id = row.id
  resetPwdDialog.visible = true
  resetPwdForm.password = ''
  resetPwdForm.confirmPassword = ''
}

// 鎻愪氦閲嶇疆瀵嗙爜
const submitResetPwd = async () => {
  if (!resetPwdFormRef.value) return

  await resetPwdFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        await resetPasswordApi({
          id: resetPwdDialog.id,
          password: resetPwdForm.password
        })
        ElMessage.success('閲嶇疆瀵嗙爜鎴愬姛')
        resetPwdDialog.visible = false
      } catch (error) {
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 鍙栨秷鎸夐挳
const cancel = () => {
  dialog.visible = false
  userFormRef.value?.resetFields()
}

// 鍒嗛〉澶у皬鏀瑰彉
const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  getList()
}

// 椤电爜鏀瑰彉
const handleCurrentChange = (val: number) => {
  queryParams.pageNum = val
  getList()
}

// 鑾峰彇瑙掕壊鍒楄〃
const getRoleOptions = async () => {
  try {
    const { data } = await getAllRoleList()

    roleOptions.value = data
  } catch (error) {
  }
}
const getDicts = async () => {
  try {
    const { data } = await getDictDataByDictTypesApi(['login_type'])
    loginTypes.value = data.login_type.list
  } catch (error) {
  }
}


// 鍒濆鍖?
onMounted(() => {
  getList()
  getRoleOptions()
  getDicts()
})
</script>
