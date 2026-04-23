<template>
    <div class="app-container">
        <!-- 鎼滅储琛ㄥ崟 -->
        <PageSearch>
            <el-form :model="queryParams" ref="queryFormRef" inline>
                <el-form-item label="鍙嶉绫诲瀷" prop="type">
                    <el-select v-model="queryParams.type" placeholder="璇烽€夋嫨鍙嶉绫诲瀷" clearable @keyup.enter="handleQuery">
                        <el-option v-for="item in feedbackTypes" :key="item.value" :label="item.label"
                            :value="item.value" />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <el-select v-model="queryParams.status" placeholder="请选择状态" clearable @keyup.enter="handleQuery">
                        <el-option v-for="item in feedbackStatus" :key="item.value" :label="item.label"
                            :value="item.value" />
                    </el-select>
                </el-form-item>
                                <el-form-item>
                    <PageSearchActions @search="handleQuery" @reset="resetQuery" />
                </el-form-item>
            </el-form>

        </PageSearch>
        <el-card class="box-card">
            <!-- 鎿嶄綔宸ュ叿鏍?-->
            <template #header>
                <PageToolbar>
                    <PageToolbarGroup kind="danger">
                        <el-button type="danger" v-permission="['sys:feedback:delete']" plain :icon="Delete" :disabled="selectedIds.length === 0"
                            @click="handleBatchDelete">鎵归噺鍒犻櫎
                        </el-button>
                    </PageToolbarGroup>
                </PageToolbar>
            </template>

            <!-- 鏁版嵁琛ㄦ牸 -->
            <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
                <el-table-column type="selection" width="55" align="center" />
                <el-table-column label="头像" align="center" prop="avatar" width="100">
                    <template #default="scope">
                        <el-avatar :src="scope.row.avatar" alt="Avatar" width="50" height="50" />
                    </template>
                </el-table-column>
                <el-table-column label="昵称" align="center" prop="nickname" />
                <el-table-column label="鍙嶉绫诲瀷" align="center" prop="type">
                    <template #default="scope">
                        <span v-for="item in feedbackTypes">
                            <el-tag v-if="item.value === scope.row.type" :key="item.value" :type="item.style">
                                {{ item.label }}
                            </el-tag>
                        </span>
                    </template>
                </el-table-column>
                <el-table-column label="鍙嶉鍐呭" align="center" prop="content" show-overflow-tooltip />
                <el-table-column label="鑱旂郴閭" align="center" prop="email" />
                <el-table-column label="鍥炲鍐呭" align="center" prop="replyContent" show-overflow-tooltip/>
                <el-table-column label="状态" align="center" prop="status">
                    <template #default="scope">
                        <span v-for="item in feedbackStatus">
                            <el-tag v-if="item.value === String(scope.row.status)" :key="item.value" :type="item.style">
                                {{ item.label }}
                            </el-tag>
                        </span>
                    </template>
                </el-table-column>
                <el-table-column label="鍒涘缓鏃堕棿" align="center" prop="createTime" width="170"/>
                <el-table-column label="鎿嶄綔" align="center" class-name="small-padding fixed-width">
                    <template #default="scope">
                        <PageTableActions>
                            <PageTableAction type="primary" :icon="Edit" v-permission="['sys:feedback:update']" @click="handleUpdate(scope.row)">淇敼</PageTableAction>
                            <PageTableAction type="danger" :icon="Delete" v-permission="['sys:feedback:delete']" @click="handleDelete(scope.row)">鍒犻櫎</PageTableAction>
                        </PageTableActions>
                    </template>
                </el-table-column>
            </el-table>

            <!-- 鍒嗛〉宸ュ叿鏍?-->
            <PagePagination
                v-model:current-page="queryParams.pageNum"
                v-model:page-size="queryParams.pageSize"
                :total="total"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
            />

            <!-- 娣诲姞鎴栦慨鏀瑰璇濇 -->
            <el-dialog v-model="open" :title="title" width="500px" append-to-body>
                <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
                    <el-form-item label="回复内容" prop="replyContent">
                        <el-input type="textarea" :rows="5" v-model="form.replyContent" placeholder="请输入回复内容" />
                    </el-form-item>
                    <el-form-item label="状态" prop="status">
                        <el-radio-group v-model="form.status">
                            <el-radio v-for="item in feedbackStatus" :value="Number(item.value)">{{item.label}}</el-radio>
                        </el-radio-group>
                    </el-form-item>
                </el-form>
                <template #footer>
                    <div class="dialog-footer">
                        <el-button type="primary" @click="submitForm">确定</el-button>
                        <el-button @click="cancel">取消</el-button>
                    </div>
                </template>
            </el-dialog>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Refresh, Search } from '@element-plus/icons-vue'
import {
    listSysFeedbackApi,
    detailSysFeedbackApi,
    deleteSysFeedbackApi,
    addSysFeedbackApi,
    updateSysFeedbackApi
} from '@/api/message/feedback'

import {
    getDictDataByDictTypesApi
} from '@/api/system/dict'

// 閬僵灞?
const loading = ref(true)
// 閫変腑鏁扮粍
const selectedIds = ref<any[]>([])
// 鎬绘潯鏁?
const total = ref(0)
// 琛ㄦ牸鏁版嵁
const dataList = ref([])
// 寮瑰嚭灞傛爣棰?
const title = ref('')
// 鏄惁鏄剧ず寮瑰嚭灞?
const open = ref(false)
// 鏌ヨ鍙傛暟
const queryParams = reactive({
    pageNum: 1,
    pageSize: 10,
    type: undefined,
    status: undefined,
    source: 'admin'
})

// 琛ㄥ崟鍙傛暟
const form = reactive<any>({})
// 琛ㄥ崟鏍￠獙
const rules = reactive({
    replyContent: [
        { required: false, message: "鍥炲鍐呭涓嶈兘涓虹┖", trigger: "blur" }
    ],
    status: [
        { required: true, message: "状态不能为空", trigger: "blur" }
    ]
})

const queryFormRef = ref()
const formRef = ref()

const feedbackTypes = ref<any[]>([])
const feedbackStatus = ref<any[]>([])



/** 鏌ヨ鍒楄〃 */
const getList = () => {
    loading.value = true
    listSysFeedbackApi(queryParams).then(response => {
        dataList.value = response.data.records
        total.value = response.data.total
        loading.value = false
    })
}

const getDicts = () => {
    getDictDataByDictTypesApi(['feedback_type', 'feedback_status']).then(res => {
        feedbackTypes.value = res.data.feedback_type.list
        feedbackStatus.value = res.data.feedback_status.list
    })
}

/** 鍙栨秷鎸夐挳 */
const cancel = () => {
    open.value = false
    reset()
}

/** 琛ㄥ崟閲嶇疆 */
const reset = () => {
    form.value = {
        id: undefined,
        userId: undefined,
        type: undefined,
        content: undefined,
        email: undefined,
        replyContent: undefined,
        status: undefined,
        createTime: undefined
    }
    formRef.value?.resetFields()
}

/** 鎼滅储鎸夐挳鎿嶄綔 */
const handleQuery = () => {
    queryParams.pageNum = 1
    getList()
}

/** 閲嶇疆鎸夐挳鎿嶄綔 */
const resetQuery = () => {
    queryFormRef.value?.resetFields()
    handleQuery()
}

/** 澶氶€夋閫変腑鏁版嵁 */
const handleSelectionChange = (selection: { id: any }[]) => {
    selectedIds.value = selection.map(item => item.id)
}

/** 鏂板鎸夐挳鎿嶄綔 */
const handleAdd = () => {
    reset()
    open.value = true
    title.value = "娣诲姞鍙嶉"
}

/** 淇敼鎸夐挳鎿嶄綔 */
const handleUpdate = (row : any) => {
    reset()
    Object.assign(form,row)
    open.value = true
    title.value = "淇敼鍙嶉"
}

/** 鎻愪氦鎸夐挳 */
const submitForm = () => {
    formRef.value?.validate((valid : any) => {
        if (valid) {
            if (form.id !== undefined) {
                updateSysFeedbackApi(form).then(response => {
                    ElMessage.success("淇敼鎴愬姛")
                    open.value = false
                    getList()
                })
            } else {
                addSysFeedbackApi(form).then(response => {
                    ElMessage.success("鏂板鎴愬姛")
                    open.value = false
                    getList()
                })
            }
        }
    })
}

/** 鎵归噺鍒犻櫎鎸夐挳鎿嶄綔 */
const handleBatchDelete = () => {
    if (!selectedIds.value.length) {
        return
    }
    ElMessageBox.confirm(`鏄惁纭鍒犻櫎${selectedIds.value.length}鏉℃暟鎹」?`, "璀﹀憡", {
        confirmButtonText: "纭畾",
        cancelButtonText: "鍙栨秷",
        type: "warning"
    }).then(async () => {
        await deleteSysFeedbackApi(selectedIds.value)
    }).then(() => {
        getList()
        ElMessage.success("鍒犻櫎鎴愬姛")
    })
}

/** 鍒犻櫎鎸夐挳鎿嶄綔 */
const handleDelete = (row : any) =>  {
    ElMessageBox.confirm('鏄惁纭鍒犻櫎鍐呭涓?' + row.content + '"鐨勬暟鎹」?', "璀﹀憡", {
        confirmButtonText: "纭畾",
        cancelButtonText: "鍙栨秷",
        type: "warning"
    }).then(async () => {
        await deleteSysFeedbackApi(row.id)
    }).then(() => {
        getList()
        ElMessage.success("鍒犻櫎鎴愬姛")
    })
}


// 娣诲姞鍒嗛〉鏂规硶
const handleSizeChange = (val : any) => {
  queryParams.pageSize = val
  getList()
}

const handleCurrentChange = (val : any) => {
  queryParams.pageNum = val
  getList()
}

onMounted(() => {
    getList()
    getDicts()
})
</script>
