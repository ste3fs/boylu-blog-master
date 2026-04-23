<template>
  <div class="app-container">

    <!-- 鎿嶄綔鎸夐挳鍖哄煙 -->
    <el-card class="box-card">
      <template #header>
        <PageToolbar>
          <PageToolbarGroup>
            <el-button v-permission="['sys:moment:add']" type="primary" :icon="Plus" @click="handleAdd">鏂板</el-button>
          </PageToolbarGroup>
          <PageToolbarGroup kind="danger">
            <el-button v-permission="['sys:moment:delete']" type="danger" :icon="Delete"
              :disabled="selectedIds.length === 0" @click="handleBatchDelete">鎵归噺鍒犻櫎</el-button>
          </PageToolbarGroup>
        </PageToolbar>
      </template>

      <!-- 鏁版嵁琛ㄦ牸 -->
      <el-table v-loading="loading" :data="momentList" style="width: 100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="鍐呭" align="center" prop="content" show-overflow-tooltip />
        <el-table-column label="鍥剧墖" align="center" prop="content">
          <template #default="scope">
            <el-image v-for="item in parseImage(scope.row.images)" :src="item" style="width: 50px; height: 50px" />
          </template>
        </el-table-column>
        <el-table-column label="鍒涘缓鏃堕棿" align="center" prop="createTime" width="180" />
        <el-table-column label="鎿嶄綔" align="center" width="280" fixed="right">
          <template #default="scope">
            <PageTableActions>
              <PageTableAction v-permission="['sys:moment:update']" type="primary" :icon="Edit"
                @click="handleUpdate(scope.row)">淇敼</PageTableAction>
              <PageTableAction v-permission="['sys:moment:delete']" type="danger" :icon="Delete"
                @click="handleDelete(scope.row)">鍒犻櫎</PageTableAction>
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

    <!-- 娣诲姞鎴栦慨鏀瑰璇濇 -->
    <el-dialog :title="dialog.title" v-model="dialog.visible" width="600px" append-to-body destroy-on-close
      class="custom-dialog">
      <el-form ref="momentFormRef" :model="momentForm" :rules="rules" label-width="80px" class="custom-form">
        <el-form-item label="鍐呭" prop="content">
            <div style="border: 1px solid #ccc;">
                <Toolbar style="border-bottom: 1px solid #ccc;" :editor="editorRef" :defaultConfig="toolbarConfig" :mode="mode" />
                <Editor style=" overflow-y: hidden;min-height: 300px;" v-model="momentForm.content" :defaultConfig="editorConfig" :mode="mode"
                @onCreated="handleCreated"/>
            </div>
        </el-form-item>
        <el-form-item label="鍥剧墖" prop="images">
          <UploadImage v-model="momentForm.images" :source="'moment'" :limit="9" :multiple="true" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">鍙?娑?</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">纭?瀹?</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getSysMomentListApi,
  addSysMomentApi,
  updateSysMomentApi,
  deleteSysMomentApi
} from '@/api/article/moment'
import UploadImage from '@/components/Upload/Image.vue'


import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
const editorRef = shallowRef()
const mode = 'default'
const toolbarConfig = {}
const editorConfig = {
  placeholder: "璇疯緭鍏ュ唴瀹?..",
  MENU_CONF: {
    codeSelectLang: {
      // 浠ｇ爜璇█
      codeLangs: [
        { text: "CSS", value: "css" },
        { text: "HTML", value: "html" },
        { text: "XML", value: "xml" },
        { text: "Java", value: "java" },
      ],
    },
  },
}

// 鏌ヨ鍙傛暟
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
})

const loading = ref(false)
const total = ref(0)
const momentList = ref([])
const momentFormRef = ref<FormInstance>()
const submitLoading = ref(false)

// 閫変腑椤规暟缁?
const selectedIds = ref<number[]>([])

// 寮圭獥鎺у埗
const dialog = reactive({
  title: '',
  visible: false,
  type: 'add'
})

// 琛ㄥ崟鏁版嵁
const momentForm = reactive<any>({
  id: undefined,
  content: '',
  images: '',
})

// 琛ㄥ崟鏍￠獙瑙勫垯
const rules = reactive<FormRules>({
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' }
  ],
})

const parseImage = (images: string) => {
  return images.split(',')
}

// 鑾峰彇鏍囩鍒楄〃
const getList = async () => {
  loading.value = true
  try {
    const { data } = await getSysMomentListApi(queryParams)
    momentList.value = data.records
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

  ElMessageBox.confirm(`鏄惁纭鍒犻櫎 ${selectedIds.value.length} 涓璇?`, '璀﹀憡', {
    confirmButtonText: '纭畾',
    cancelButtonText: '鍙栨秷',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteSysMomentApi(selectedIds.value)
      ElMessage.success('鎵归噺鍒犻櫎鎴愬姛')
      getList()
      selectedIds.value = []
    } catch (error) {
    }
  })
}

// 鍒犻櫎
const handleDelete = (row: any) => {
  ElMessageBox.confirm(`鏄惁纭鍒犻櫎 ${row.content} 杩欎釜璇磋?`, '璀﹀憡', {
    confirmButtonText: '纭畾',
    cancelButtonText: '鍙栨秷',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteSysMomentApi(row.id)
      ElMessage.success('鍒犻櫎鎴愬姛')
      getList()
    } catch (error) {
    }
  })
}


// 鏂板璇磋
const handleAdd = () => {
  dialog.type = 'add'
  dialog.title = '鏂板璇磋'
  dialog.visible = true
  momentForm.id = undefined
  momentForm.content = ''
  momentForm.images = ''
}

// 淇敼璇磋
const handleUpdate = (row: any) => {
  dialog.type = 'edit'
  dialog.title = '淇敼璇磋'
  dialog.visible = true
  Object.assign(momentForm, row)
  momentForm.images = momentForm.images.split(',')
}

// 瀵屾枃鏈紪杈戝櫒鍒涘缓瀹屾垚
const handleCreated = (editor:any) => {
  editorRef.value = editor // 璁板綍 editor 瀹炰緥锛岄噸瑕侊紒
}

// 鎻愪氦琛ㄥ崟
const submitForm = async () => {
  if (!momentFormRef.value) return

  await momentFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (momentForm.images && momentForm.images.length > 0) {
          momentForm.images = momentForm.images.join(',')
        }
        if (dialog.type === 'add') {
          await addSysMomentApi(momentForm)
          ElMessage.success('鏂板鎴愬姛')
        } else {
          await updateSysMomentApi(momentForm)
          ElMessage.success('淇敼鎴愬姛')
        }
        getList()
        dialog.visible = false
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
  momentFormRef.value?.resetFields()
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

// 鍒濆鍖?
onMounted(() => {
  getList()
})
</script>
