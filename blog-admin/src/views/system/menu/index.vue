<template>
  <div class="app-container menu-page">
    <el-card class="box-card">
      <template #header>
        <PageToolbar>
          <PageToolbarGroup>
            <el-button
              v-permission="['sys:menu:add']"
              type="primary"
              :icon="Plus"
              @click="handleAdd()"
            >
              新增菜单
            </el-button>
          </PageToolbarGroup>
        </PageToolbar>
      </template>

      <el-table
        v-loading="loading"
        :data="menuList"
        row-key="id"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      >
        <el-table-column label="菜单名称" prop="title" min-width="180" show-overflow-tooltip />
        <el-table-column label="图标" align="center" width="80">
          <template #default="{ row }">
            <el-icon v-if="row.icon">
              <DynamicIcon :name="formatIconName(row.icon)" />
            </el-icon>
            <span v-else class="menu-empty-text">-</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" align="center" width="92">
          <template #default="{ row }">
            <el-tag :type="resolveTypeMeta(row.type).type">
              {{ resolveTypeMeta(row.type).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="路由地址" prop="path" min-width="150" show-overflow-tooltip />
        <el-table-column label="组件路径" prop="component" min-width="180" show-overflow-tooltip />
        <el-table-column label="权限标识" prop="perm" min-width="150" show-overflow-tooltip />
        <el-table-column label="排序" prop="sort" width="80" align="center" />
        <el-table-column label="状态" align="center" width="92">
          <template #default="{ row }">
            <el-tag :type="row.hidden === 0 ? 'success' : 'info'">
              {{ row.hidden === 0 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <PageTableActions>
              <PageTableAction
                type="success"
                :icon="Plus"
                v-permission="['sys:menu:add']"
                @click="handleAdd(row)"
              >
                新增下级
              </PageTableAction>
              <PageTableAction
                type="primary"
                :icon="Edit"
                v-permission="['sys:menu:update']"
                @click="handleEdit(row)"
              >
                编辑
              </PageTableAction>
              <PageTableAction
                type="danger"
                :icon="Delete"
                v-permission="['sys:menu:delete']"
                @click="handleDelete(row)"
              >
                删除
              </PageTableAction>
            </PageTableActions>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增菜单' : '编辑菜单'"
      :width="isMobile ? '100vw' : '760px'"
      :top="isMobile ? '0' : '5vh'"
      :fullscreen="isMobile"
      append-to-body
      destroy-on-close
      class="menu-dialog"
    >
      <el-form
        ref="menuFormRef"
        :model="menuForm"
        :rules="rules"
        :label-position="isMobile ? 'top' : 'right'"
        label-width="104px"
        class="menu-form"
      >
        <el-row :gutter="18">
          <el-col :xs="24" :sm="24" :md="24">
            <el-form-item label="上级菜单" prop="parentId">
              <el-tree-select
                v-model="menuForm.parentId"
                :data="menuOptions"
                :props="{ label: 'title', value: 'id' }"
                value-key="id"
                placeholder="请选择上级菜单"
                check-strictly
                :render-after-expand="false"
                class="menu-form__full"
              />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="24" :md="24">
            <el-form-item label="菜单类型" prop="type">
              <el-radio-group v-model="menuForm.type" class="menu-type-group">
                <el-radio-button :value="MenuTypeEnum.CATALOG">目录</el-radio-button>
                <el-radio-button :value="MenuTypeEnum.MENU">菜单</el-radio-button>
                <el-radio-button :value="MenuTypeEnum.BUTTON">按钮</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="24" :md="12">
            <el-form-item label="菜单名称" prop="title">
              <el-input v-model="menuForm.title" placeholder="请输入菜单名称" />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="24" :md="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="menuForm.sort" :min="1" class="menu-form__full" />
            </el-form-item>
          </el-col>

          <el-col v-if="menuForm.type !== MenuTypeEnum.BUTTON" :xs="24" :sm="24" :md="24">
            <el-form-item label="图标">
              <el-input v-model="menuForm.icon" placeholder="点击右侧按钮选择图标" readonly>
                <template #prefix>
                  <el-icon v-if="menuForm.icon">
                    <DynamicIcon :name="formatIconName(menuForm.icon)" />
                  </el-icon>
                </template>
                <template #append>
                  <el-button @click="showIconSelect = true">
                    <el-icon><View /></el-icon>
                    选择图标
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>

          <el-col v-if="menuForm.type !== MenuTypeEnum.BUTTON" :xs="24" :sm="24" :md="24">
            <el-form-item label="路由地址" prop="path">
              <el-input v-model="menuForm.path" placeholder="请输入路由地址，例如 /system/user" />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="24" :md="24">
            <el-form-item label="重定向地址" prop="redirect">
              <el-input v-model="menuForm.redirect" placeholder="可选，例如 /dashboard" />
            </el-form-item>
          </el-col>

          <el-col v-if="menuForm.type === MenuTypeEnum.MENU" :xs="24" :sm="24" :md="24">
            <el-form-item label="组件路径" prop="component">
              <el-input v-model="menuForm.component" placeholder="/system/user/index">
                <template #prepend>src/views</template>
                <template #append>.vue</template>
              </el-input>
            </el-form-item>
          </el-col>

          <el-col v-if="menuForm.type === MenuTypeEnum.BUTTON" :xs="24" :sm="24" :md="24">
            <el-form-item label="权限标识" prop="perm">
              <el-input v-model="menuForm.perm" placeholder="请输入权限标识，例如 sys:user:add" />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="24" :md="12">
            <el-form-item label="显示状态" prop="hidden">
              <el-radio-group v-model="menuForm.hidden">
                <el-radio :value="0">显示</el-radio>
                <el-radio :value="1">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="24" :md="12">
            <el-form-item label="是否外链" prop="isExternal">
              <el-radio-group v-model="menuForm.isExternal">
                <el-radio :value="0">否</el-radio>
                <el-radio :value="1">是</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <icon-select v-model="menuForm.icon" v-model:visible="showIconSelect" />
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Delete, Edit, Plus, View } from '@element-plus/icons-vue'
import DynamicIcon from '@/components/DynamicIcon/index.vue'
import IconSelect from '@/components/IconSelect/index.vue'
import { MenuTypeEnum } from '@/enum/MenuTypeEnum'
import {
  createMenuApi,
  deleteMenuApi,
  getMenuDetailApi,
  getMenuListApi,
  updateMenuApi
} from '@/api/system/menu'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogType = ref<'add' | 'edit'>('add')
const showIconSelect = ref(false)
const submitLoading = ref(false)
const isMobile = ref(false)

const menuFormRef = ref<FormInstance>()
const menuList = ref<any[]>([])
const menuOptions = ref<any[]>([])

const menuForm = reactive<any>({
  id: undefined,
  parentId: 0,
  title: '',
  sort: 1,
  path: '',
  component: '',
  redirect: '',
  type: MenuTypeEnum.CATALOG,
  perm: '',
  icon: '',
  hidden: 0,
  isExternal: 0
})

const validatePath = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (menuForm.type !== MenuTypeEnum.BUTTON && !value) {
    callback(new Error('请输入路由地址'))
    return
  }
  callback()
}

const validateComponent = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (menuForm.type === MenuTypeEnum.MENU && !value) {
    callback(new Error('菜单类型必须填写组件路径'))
    return
  }
  callback()
}

const validatePerm = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (menuForm.type === MenuTypeEnum.BUTTON && !value) {
    callback(new Error('按钮类型必须填写权限标识'))
    return
  }
  callback()
}

const rules = reactive({
  parentId: [
    { required: true, message: '请选择上级菜单', trigger: 'change' }
  ],
  title: [
    { required: true, message: '请输入菜单名称', trigger: 'blur' }
  ],
  sort: [
    { required: true, message: '请输入排序值', trigger: 'blur' }
  ],
  path: [
    { validator: validatePath, trigger: 'blur' }
  ],
  component: [
    { validator: validateComponent, trigger: 'blur' }
  ],
  perm: [
    { validator: validatePerm, trigger: 'blur' }
  ],
  hidden: [
    { required: true, message: '请选择显示状态', trigger: 'change' }
  ]
})

const syncMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

const resolveTypeMeta = (type: string) => {
  if (type === MenuTypeEnum.CATALOG) return { label: '目录', type: 'primary' }
  if (type === MenuTypeEnum.MENU) return { label: '菜单', type: 'success' }
  return { label: '按钮', type: 'info' }
}

const formatIconName = (icon: string) => {
  if (!icon) return ''
  if (icon.startsWith('el-icon-')) {
    return icon.replace('el-icon-', '')
  }
  return icon.charAt(0).toUpperCase() + icon.slice(1)
}

const buildMenuOptions = (list: any[]) => ([
  {
    id: 0,
    title: '顶级菜单',
    children: list || []
  }
])

const resetForm = () => {
  menuForm.id = undefined
  menuForm.parentId = 0
  menuForm.title = ''
  menuForm.sort = 1
  menuForm.path = ''
  menuForm.redirect = ''
  menuForm.component = ''
  menuForm.type = MenuTypeEnum.CATALOG
  menuForm.perm = ''
  menuForm.icon = ''
  menuForm.hidden = 0
  menuForm.isExternal = 0
}

const getList = async () => {
  loading.value = true
  try {
    const { data } = await getMenuListApi()
    menuList.value = data || []
    menuOptions.value = buildMenuOptions(menuList.value)
  } finally {
    loading.value = false
  }
}

const handleAdd = (row?: any) => {
  resetForm()
  if (row?.id) {
    menuForm.parentId = row.id
  }
  dialogType.value = 'add'
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  resetForm()
  dialogType.value = 'edit'
  dialogVisible.value = true
  const { data } = await getMenuDetailApi(String(row.id))
  Object.assign(menuForm, data)
  menuForm.icon = formatIconName(menuForm.icon)
}

const submitForm = async () => {
  if (!menuFormRef.value) return

  await menuFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      const payload = {
        ...menuForm,
        icon: formatIconName(menuForm.icon)
      }
      if (dialogType.value === 'add') {
        await createMenuApi(payload)
        ElMessage.success('菜单新增成功')
      } else {
        await updateMenuApi(payload)
        ElMessage.success('菜单修改成功')
      }
      dialogVisible.value = false
      getList()
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm(
    `确定要删除菜单“${row.title}”吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await deleteMenuApi(String(row.id))
  ElMessage.success('菜单删除成功')
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

<style scoped lang="scss">
.menu-empty-text {
  color: var(--el-text-color-disabled);
}

.menu-form__full {
  width: 100%;
}

.menu-type-group {
  display: flex;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .menu-page {
    :deep(.menu-dialog) {
      border-radius: 0 !important;
    }

    :deep(.menu-form .el-form-item) {
      display: block;
      margin-bottom: 16px !important;
    }

    :deep(.menu-form .el-form-item__label) {
      display: block;
      width: 100% !important;
      padding: 0 0 8px !important;
      text-align: left !important;
      font-weight: 600;
    }

    :deep(.menu-form .el-form-item__content) {
      margin-left: 0 !important;
      width: 100%;
    }

    .menu-type-group {
      width: 100%;

      :deep(.el-radio-button) {
        flex: 1;
      }

      :deep(.el-radio-button__inner) {
        width: 100%;
      }
    }
  }
}
</style>
