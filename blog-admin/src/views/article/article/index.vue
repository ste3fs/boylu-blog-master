<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <PageSearch>
      <el-form ref="queryFormRef" :model="queryParams" :inline="!isMobile">
        <el-form-item label="标题" prop="title">
          <el-input v-model="queryParams.title" placeholder="请输入文章标题" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable>
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" prop="tagId">
          <el-select v-model="queryParams.tagId" placeholder="请选择标签" clearable>
            <el-option v-for="item in tagOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option v-for="item in statusOptions" :key="item.id" :value="item.value" :label="item.label" />
          </el-select>
        </el-form-item>
                <el-form-item>
          <PageSearchActions @search="handleQuery" @reset="resetQuery" />
        </el-form-item>
      </el-form>
    </PageSearch>

    <!-- 操作按钮区域 -->
    <el-card class="box-card">
      <template #header>
        <PageToolbar>
          <PageToolbarGroup>
              <el-button type="primary" :icon="Plus" @click="handleAdd" v-permission="['sys:article:add']">新增文章</el-button>
              <el-button type="success" plain :icon="Position" v-permission="['sys:article:update']"
              @click="handlePushBaiduRecent">推送近期到百度</el-button>
              <el-button type="warning" plain :icon="Setting" v-permission="['sys:article:reptile']"
              @click="reptileDialog.visible = true">爬取文章</el-button>
              <el-button type="primary" plain :icon="Plus" v-permission="['sys:article:add']"
              @click="openNotionImport">导入 Notion</el-button>
          </PageToolbarGroup>
          <PageToolbarGroup kind="danger">
              <el-button type="danger" plain :icon="Delete" :disabled="selectedIds.length === 0"
              v-permission="['sys:article:delete']" @click="handleBatchDelete">批量删除</el-button>
          </PageToolbarGroup>
        </PageToolbar>
      </template>

      <!-- 数据表格 -->
      <el-table v-if="!isMobile" v-loading="loading" :data="tableData" style="width: 100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="封面" align="center" width="133">
          <template #default="scope">
            <el-image style="width: 120px; height: 80px;border-radius: 10px" :src="scope.row.cover" />
          </template>
        </el-table-column>
        <el-table-column label="标题" align="center" prop="title" width="200" show-overflow-tooltip>
          <template #default="scope">
            <span style="color: var(--el-color-primary);">{{ scope.row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="作者" align="center" prop="nickname" show-overflow-tooltip />
        <el-table-column label="分类" align="center" prop="categoryName" />
        <el-table-column label="标签" align="center" width="200">
          <template #default="scope">
            <el-tag v-for="tag in scope.row.tags" :key="tag.id" class="mx-1" size="small">
              {{ tag.name }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布状态" align="center" prop="status">
          <template #default="scope">
            <el-switch @change="handleChangeStatus(scope.row)"
              style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949" v-model="scope.row.status"
              :active-value="1" :inactive-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="是否推荐" align="center">
          <template #default="{ row }">
            <span v-for="item in yesNoOptions">
              <el-tag :type="item.style" v-if="row.isRecommend === Number(item.value)">
                {{ item.label }}
              </el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="是否置顶" align="center">
          <template #default="{ row }">
            <span v-for="item in yesNoOptions">
              <el-tag :type="item.style" v-if="row.isStick === Number(item.value)">
                {{ item.label }}
              </el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="阅读量" align="center" prop="quantity" />
        <el-table-column label="发布时间" align="center" prop="createTime" width="180" />
        <el-table-column label="操作" align="center" width="280" fixed="right">
          <template #default="scope">
            <PageTableActions>
              <PageTableAction type="success" :icon="Position" @click="handlePushBaidu(scope.row)"
                v-permission="['sys:article:update']">推送百度</PageTableAction>
              <PageTableAction type="primary" :icon="Edit" @click="handleUpdate(scope.row)"
                v-permission="['sys:article:update']">编辑</PageTableAction>
              <PageTableAction type="danger" :icon="Delete" @click="handleDelete(scope.row)"
                v-permission="['sys:article:delete']">删除</PageTableAction>
            </PageTableActions>
          </template>
        </el-table-column>
      </el-table>

      <div v-else v-loading="loading" class="mobile-article-list">
        <div v-if="selectedIds.length" class="mobile-selection-bar">
          <span>已选 {{ selectedIds.length }} 篇文章</span>
          <el-button link type="primary" @click="selectedIds = []">清空</el-button>
        </div>

        <template v-if="tableData.length">
          <article v-for="row in tableData" :key="row.id" class="mobile-article-card">
            <div class="mobile-article-card__top">
              <el-checkbox
                class="mobile-article-card__check"
                :model-value="isArticleSelected(row.id)"
                @change="(checked) => toggleArticleSelection(row.id, Boolean(checked))"
              />
              <el-image class="mobile-article-card__cover" :src="row.cover" fit="cover">
                <template #error>
                  <div class="mobile-article-card__cover-fallback">无封面</div>
                </template>
              </el-image>
              <div class="mobile-article-card__main">
                <div class="mobile-article-card__title">{{ row.title || '未命名文章' }}</div>
                <div class="mobile-article-card__summary">{{ row.summary || '暂无摘要' }}</div>
              </div>
            </div>

            <div class="mobile-article-card__meta">
              <div class="mobile-article-card__meta-item">
                <span class="mobile-article-card__meta-label">作者</span>
                <span>{{ row.nickname || '未知' }}</span>
              </div>
              <div class="mobile-article-card__meta-item">
                <span class="mobile-article-card__meta-label">分类</span>
                <span>{{ row.categoryName || '未分类' }}</span>
              </div>
              <div class="mobile-article-card__meta-item">
                <span class="mobile-article-card__meta-label">阅读</span>
                <span>{{ row.quantity || 0 }}</span>
              </div>
              <div class="mobile-article-card__meta-item">
                <span class="mobile-article-card__meta-label">发布时间</span>
                <span>{{ row.createTime || '未发布' }}</span>
              </div>
            </div>

            <div class="mobile-article-card__tags" v-if="row.tags && row.tags.length">
              <el-tag
                v-for="tag in row.tags.slice(0, 3)"
                :key="tag.id || tag.name"
                size="small"
                effect="plain"
              >
                {{ tag.name }}
              </el-tag>
            </div>

            <div class="mobile-article-card__status">
              <div class="mobile-article-card__switch">
                <span>发布状态</span>
                <el-switch
                  v-model="row.status"
                  :active-value="1"
                  :inactive-value="0"
                  style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
                  @change="handleChangeStatus(row)"
                />
              </div>
              <div class="mobile-article-card__flags">
                <el-tag
                  size="small"
                  effect="light"
                  :type="resolveOptionMeta(yesNoOptions, row.isRecommend).style || 'info'"
                >
                  推荐 {{ resolveOptionMeta(yesNoOptions, row.isRecommend).label }}
                </el-tag>
                <el-tag
                  size="small"
                  effect="light"
                  :type="resolveOptionMeta(yesNoOptions, row.isStick).style || 'info'"
                >
                  置顶 {{ resolveOptionMeta(yesNoOptions, row.isStick).label }}
                </el-tag>
              </div>
            </div>

            <div class="mobile-article-card__actions">
              <el-button type="primary" plain :icon="Edit" @click="handleUpdate(row)">编辑</el-button>
              <el-button type="danger" plain :icon="Delete" @click="handleDelete(row)">删除</el-button>
            </div>
          </article>
        </template>

        <el-empty v-else description="暂无文章数据" />
      </div>

      <!-- 分页组件 -->
      <PagePagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <!-- 添加或修改对话框 -->
    <el-dialog
      :title="dialog.title"
      v-model="dialog.visible"
      :width="isMobile ? '100vw' : '1400px'"
      :top="isMobile ? '0' : '3vh'"
      :fullscreen="isMobile"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        :label-width="isMobile ? '100%' : '100px'"
        :label-position="isMobile ? 'top' : 'right'"
      >
        <el-form-item label="文章标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入文章标题" />
        </el-form-item>

        <el-form-item label="文章封面" prop="cover">
          <UploadImage
            v-model="form.cover"
            :limit="1"
            :source="'article-cover'"
            @metadata-change="handleCoverImageMetadataChange"
          />
        </el-form-item>

        <el-form-item label="文章简介" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="请输入文章简介" />
        </el-form-item>

        <el-row :gutter="20" class="mb-20">
          <el-col :xs="24" :sm="24" :md="12">
            <el-form-item label="分类" prop="categoryName">
              <el-tag
                type="success"
                v-show="form.categoryName"
                style="margin: 0 1rem 0 0"
                :closable="true"
                @close="removeCategory()"
              >
                {{ form.categoryName }}
              </el-tag>
              <!-- 分类选项 -->
              <el-popover
                placement="bottom-start"
                width="460"
                trigger="click"
                v-if="!form.categoryName"
              >
                <div class="popover-title">分类</div>
                <!-- 输入框 -->
                <el-input
                  style="width: 100%"
                  v-model="categoryName"
                  placeholder="请输入分类名,enter添加自定义分类"
                  @keyup.enter="saveCategory"
                />
                <!-- 分类 -->
                <div class="popover-container">
                  <div>添加分类</div>
                  <el-tag
                    v-for="(item, index) of categoryOptions"
                    :key="index"
                    style="margin-left: 3px; margin-top: 2px"
                    class="category-item"
                    @click="addCategory(item.name)"
                  >
                    {{ item.name }}
                  </el-tag>
                </div>
                <template #reference>
                  <el-button type="success" plain> 添加分类 </el-button>
                </template>
              </el-popover>
            </el-form-item>

          </el-col>
          <el-col :xs="24" :sm="24" :md="12">
            <el-form-item label="标签" prop="tags">
              <el-tag
                v-for="(item, index) of form.tags"
                :key="index"
                style="margin: 0 1rem 0 0"
                :closable="true"
                @close="removeTag(item)"
              >
                {{ item }}
              </el-tag>
              <!-- 标签选项 -->
              <el-popover
                placement="bottom-start"
                width="460"
                trigger="click"
                v-if="form.tags && form.tags.length < 3"
              >
                <div class="popover-title">标签</div>
                <!-- 搜索框 -->
                <el-input
                  style="width: 100%"
                  v-model="tagName"
                  placeholder="请输入标签名,enter添加自定义标签"
                  @keyup.enter="saveTag"
                />
                <!-- 标签 -->
                <div class="popover-container">
                  <div>添加标签</div>
                  <el-tag
                    v-for="(item, index) of tagOptions"
                    :key="index"
                    style="margin-left: 3px; margin-top: 2px"
                    @click="addTag(item.name)"
                  >
                    {{ item.name }}
                  </el-tag>
                </div>
                <template #reference>
                  <el-button type="primary" plain> 添加标签 </el-button>
                </template>
              </el-popover>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20" class="mb-20">
          <el-col :xs="24" :sm="24" :md="8">
            <el-form-item label="阅读方式" prop="readType">
              <el-select v-model="form.readType" placeholder="请选择阅读方式">
                <el-option label="免费" :value="1" />
                <el-option label="会员" :value="2" />
                <el-option label="付费" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="8">
            <el-form-item label="文章类型" prop="isOriginal">
              <el-select v-model="form.isOriginal" placeholder="请选择文章类型">
                <el-option label="原创" :value="1" />
                <el-option label="转载" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="8">
            <el-form-item label="关键词" prop="keywords">
              <el-input v-model="form.keywords" placeholder="请输入关键词" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="转载地址" prop="originalUrl" v-if="form.isOriginal === 0" class="mb-20">
          <el-input v-model="form.originalUrl" placeholder="请输入转载地址" />
        </el-form-item>

        <el-row :gutter="20" class="mb-20">
          <el-col :xs="12" :sm="12" :md="6">
            <el-form-item label="是否置顶" prop="isStick">
              <el-switch style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949" v-model="form.isStick"
                :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="12" :md="6">
            <el-form-item label="是否发布" prop="status">
              <el-select v-model="form.status" placeholder="请选择文章状态">
                <el-option v-for="item in statusOptions" :key="item.id" :value="Number(item.value)"
                  :label="item.label" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="12" :md="6">
            <el-form-item label="首页轮播" prop="isCarousel">
              <el-switch style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949" v-model="form.isCarousel"
                :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="12" :md="6">
            <el-form-item label="是否推荐" prop="isRecommend">
              <el-switch style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
                v-model="form.isRecommend" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="文章内容" prop="content" class="mb-20">
          <div v-if="isMobile" class="article-editor__toolbar-actions">
            <div class="article-editor__meta">
              已输入 {{ articleContentStats.characters }} 字
              <span v-if="articleContentStats.lines"> · {{ articleContentStats.lines }} 行</span>
            </div>
            <el-button type="primary" size="large" @click="contentEditorDialogVisible = true">
              展开大编辑器
            </el-button>
          </div>
          <div v-else class="article-editor">
            <Toolbar
              class="article-editor__toolbar"
              :editor="articleEditorRef"
              :defaultConfig="articleToolbarConfig"
              :mode="articleEditorMode"
            />
            <Editor
              class="article-editor__body"
              :style="{ height: `${articleEditorHeight}px` }"
              v-model="form.content"
              :defaultConfig="articleEditorConfig"
              :mode="articleEditorMode"
              @onCreated="handleArticleEditorCreated"
              @onChange="handleEditorChange"
            />
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="contentEditorDialogVisible"
      title="编辑文章内容"
      width="100vw"
      top="0"
      fullscreen
      append-to-body
      :close-on-click-modal="false"
      class="article-content-dialog"
    >
      <div class="article-content-dialog__header">
        <div class="article-content-dialog__meta">
          已输入 {{ articleContentStats.characters }} 字
          <span v-if="articleContentStats.lines"> · {{ articleContentStats.lines }} 行</span>
        </div>
      </div>
      <div v-if="contentEditorDialogVisible" class="article-editor article-editor--fullscreen">
        <Toolbar
          class="article-editor__toolbar"
          :editor="fullscreenArticleEditorRef"
          :defaultConfig="articleToolbarConfig"
          :mode="articleEditorMode"
        />
        <Editor
          class="article-editor__body"
          :style="{ height: `${fullscreenArticleEditorHeight}px` }"
          v-model="form.content"
          :defaultConfig="articleEditorConfig"
          :mode="articleEditorMode"
          @onCreated="handleFullscreenArticleEditorCreated"
          @onChange="handleEditorChange"
        />
      </div>
      <template #footer>
        <div class="dialog-footer article-content-dialog__footer">
          <el-button @click="contentEditorDialogVisible = false">完成编辑</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 爬取文章对话框 -->
    <el-dialog title="爬取文章" v-model="reptileDialog.visible" :width="isMobile ? '100vw' : '800px'" :fullscreen="isMobile">
      <el-form ref="reptileFormRef" :model="reptileForm" :rules="rules" :label-width="isMobile ? '88px' : '100px'">
        <el-form-item label="爬取地址" prop="url">
          <el-input v-model="reptileForm.url" placeholder="请输入爬取地址" />
        </el-form-item>
      </el-form>
      <div style="margin-top: 20px;">
        <el-alert title="暂时只支持Csdn的文章爬取" type="success" :closable="false" />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitReptile">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      title="导入 Notion 笔记"
      v-model="notionDialog.visible"
      :width="isMobile ? '100vw' : '720px'"
      :fullscreen="isMobile"
      @close="resetNotionForm"
    >
      <el-form :model="notionForm" :label-width="isMobile ? '88px' : '110px'">
        <el-form-item label="页面地址" required>
          <el-input v-model="notionForm.pageUrl" clearable placeholder="粘贴 Notion 页面 URL 或 Page ID" />
        </el-form-item>
        <el-form-item label="文章分类" required>
          <el-select
            v-model="notionForm.categoryName"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入分类"
            style="width: 100%"
          >
            <el-option
              v-for="item in categoryOptions"
              :key="item.id || item.name"
              :label="item.name"
              :value="item.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="文章标签">
          <el-select
            v-model="notionForm.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入标签"
            style="width: 100%"
          >
            <el-option
              v-for="item in tagOptions"
              :key="item.id || item.name"
              :label="item.name"
              :value="item.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="保存状态">
          <el-radio-group v-model="notionForm.status">
            <el-radio-button :label="0">草稿</el-radio-button>
            <el-radio-button :label="1">发布</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="图片处理">
          <el-switch v-model="notionForm.importImages" active-text="下载到本站" inactive-text="保留原链接" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input
            v-model="notionForm.summary"
            type="textarea"
            :rows="3"
            maxlength="220"
            show-word-limit
            placeholder="可留空，系统会从 Notion 正文自动生成"
          />
        </el-form-item>
      </el-form>
      <el-alert
        title="需要先在服务端配置 NOTION_API_TOKEN，并把 Notion 页面共享给该 Integration。导入后会生成文章草稿，可继续编辑封面、摘要和正文。"
        type="info"
        :closable="false"
      />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="notionDialog.visible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitNotionImport">导入</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Setting, Position } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import UploadImage from '@/components/Upload/Image.vue'
import { isManagedImageUrl } from '@/utils/image'
import { getCategoryListApi } from '@/api/article/category'
import { getTagListApi } from '@/api/article/tag'
import {
  getArticleListApi, getDetailApi, deleteArticleApi,
  addArticleApi, updateArticleApi, updateStatusApi, reptileArticleApi, pushBaiduApi, pushBaiduRecentApi, importNotionArticleApi
} from '@/api/article'
import { uploadApi, deleteFileApi } from '@/api/file'
import { getDictDataByDictTypesApi } from '@/api/system/dict'

// 模拟数据
const categoryOptions = ref<any>([])

const tagOptions = ref<any>([])

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  categoryId: undefined,
  tagId: undefined,
  status: undefined
})

const loading = ref(false)
const total = ref(0)
const tableData = ref<any[]>([])
const queryFormRef = ref<FormInstance>()
const formRef = ref<FormInstance>()
const articleEditorRef = shallowRef()
const fullscreenArticleEditorRef = shallowRef()
const articleEditorMode = 'default'
const submitLoading = ref(false)
const isMobile = ref(false)
const contentEditorDialogVisible = ref(false)
const viewportHeight = ref(0)

// 选中项数组
const selectedIds = ref<Array<string | number>>([])

// 弹窗控制
const dialog = reactive({
  title: '',
  visible: false,
  type: 'add'
})

const reptileDialog = reactive({
  visible: false,
})

const notionDialog = reactive({
  visible: false
})

// 表单数据
const form = reactive<any>({
  id: undefined,
  title: '',
  cover: '',
  coverImage: undefined,
  summary: '',
  categoryName: '',
  tags: [],
  content: '',
  contentMd: '',
  readType: 1,
  isOriginal: 1,
  originalUrl: '',
  isStick: 0,
  status: 1,
  isCarousel: 0,
  isRecommend: 0,
  keywords: ''
})

const reptileForm = reactive({
  url: ''
})

const notionForm = reactive<any>({
  pageUrl: '',
  categoryName: 'Notion',
  tags: ['Notion'],
  summary: '',
  status: 0,
  readType: 1,
  isOriginal: 1,
  isStick: 0,
  isCarousel: 0,
  isRecommend: 0,
  importImages: true
})

const statusOptions = ref<any>([])
const yesNoOptions = ref<any>([])

const tagName = ref('')
const categoryName = ref('')

const syncMobile = () => {
  isMobile.value = window.innerWidth <= 768
  viewportHeight.value = window.innerHeight || document.documentElement.clientHeight || 0
}

const MOBILE_ARTICLE_TOOLBAR_KEYS = [
  'headerSelect',
  'bold',
  'italic',
  'underline',
  'color',
  'bgColor',
  '|',
  'bulletedList',
  'numberedList',
  'blockquote',
  '|',
  'insertLink',
  'insertImage',
  'insertVideo',
  '|',
  'undo',
  'redo'
]

const articleToolbarConfig = computed(() => (
  isMobile.value
    ? { toolbarKeys: MOBILE_ARTICLE_TOOLBAR_KEYS }
    : {}
))

const articleEditorHeight = computed(() => {
  if (!isMobile.value) {
    return 500
  }
  const base = viewportHeight.value || 820
  return Math.max(430, base - 250)
})

const fullscreenArticleEditorHeight = computed(() => {
  const base = viewportHeight.value || 820
  return Math.max(520, base - 150)
})

const articleContentStats = computed(() => {
  const content = String(form.content || '')
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/\r/g, '')
  if (!content) {
    return { characters: 0, lines: 0 }
  }
  return {
    characters: content.length,
    lines: content.split('\n').length
  }
})

// 用于追踪编辑器内图片变化，实现“删除图片即删服务器文件”
const lastImages = ref<string[]>([])

const extractImagesFromHtml = (html: string) => {
  if (!html) return []
  const urls: string[] = []
  const imgReg = /<img [^>]*src=['"]([^'"]+)[^>]*>/gi
  let match
  while ((match = imgReg.exec(html)) !== null) {
    urls.push(match[1])
  }
  return urls
}

const handleEditorChange = (editor: any) => {
  const currentHtml = editor.getHtml()
  const currentImages = extractImagesFromHtml(currentHtml)

  // 找出被删除的图片
  const deletedImages = lastImages.value.filter(url => !currentImages.includes(url))

  if (deletedImages.length > 0) {
    deletedImages.forEach(url => {
      // 只有托管在自己服务器上的图片才调用删除接口
      if (isManagedImageUrl(url)) {
        console.log('检测到编辑器内图片删除:', url)
        deleteFileApi(url).catch(err => {
          console.error('服务器文件删除失败:', url, err)
        })
      }
    })
  }

  lastImages.value = currentImages
}

const buildUploadUrl = async (file: File) => {
  const formdata = new FormData()
  formdata.append('file', file)
  const res = await uploadApi(formdata, 'article-content')
  const uploadedUrl = extractUploadUrl(res)
  if (!uploadedUrl) {
    throw new Error('上传返回地址为空')
  }
  return uploadedUrl
}

const articleEditorConfig = computed(() => ({
  placeholder: '输入文章内容...',
  MENU_CONF: {
    uploadImage: {
      customUpload: async (file: File, insertFn: (url: string) => void) => {
        const url = await buildUploadUrl(file)
        insertFn(url)
      }
    },
    uploadVideo: {
      customUpload: async (file: File, insertFn: (url: string) => void) => {
        const url = await buildUploadUrl(file)
        insertFn(url)
      }
    }
  }
}))

const handleArticleEditorCreated = (editor: any) => {
  articleEditorRef.value = editor
}

const handleFullscreenArticleEditorCreated = (editor: any) => {
  fullscreenArticleEditorRef.value = editor
}

const isArticleSelected = (id: string | number) => selectedIds.value.includes(id)

const toggleArticleSelection = (id: string | number, checked: boolean) => {
  if (checked) {
    if (!selectedIds.value.includes(id)) {
      selectedIds.value.push(id)
    }
    return
  }
  selectedIds.value = selectedIds.value.filter(item => item !== id)
}

const resolveOptionMeta = (options: any[], value: string | number) => {
  return options.find(item => Number(item.value) === Number(value)) || {}
}

const handleCoverImageMetadataChange = (metadata: any) => {
  form.coverImage = metadata || undefined
}

const extractUploadUrl = (payload: any) => {
  if (!payload) {
    return ''
  }
  if (typeof payload === 'string') {
    return payload
  }
  const data = payload.data
  if (typeof data === 'string') {
    return data
  }
  if (data && typeof data === 'object') {
    return String(data.fallback || data.url || data.src || '')
  }
  return String(payload.url || '')
}





// 表单校验规则
const rules = reactive<FormRules>({
  title: [
    { required: true, message: '请输入文章标题', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  categoryName: [
    { required: true, message: '请选择文章分类', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入文章内容', trigger: 'blur' }
  ],
  summary: [
    { required: true, message: '请输入文章简介', trigger: 'blur' },
    { max: 500, message: '简介最多500个字符', trigger: 'blur' }
  ],
  readType: [
    { required: true, message: '请选择阅读方式', trigger: 'change' }
  ],
  isOriginal: [
    { required: true, message: '请选择文章类型', trigger: 'change' }
  ],
  tags: [
    { required: true, message: '请选择文章标签', trigger: 'change' }
  ],
  originalUrl: [
    {
      required: true,
      message: '请输入转载地址',
      trigger: 'blur',
      validator: (rule: any, value: string, callback: any) => {
        if (form.isOriginal === 0 && !value) {
          callback(new Error('转载文章必须填写转载地址'))
        } else {
          callback()
        }
      }
    }
  ]
})

const removeTag = (tag: string) => {
  form.tags = form.tags.filter((item: string) => item !== tag)
}

const addTag = (tag: any) => {
  if (form.tags.includes(tag)) {
    ElMessage.warning('标签已存在')
    return
  }
  form.tags.push(tag)
}

const saveTag = () => {
  if (tagName.value.trim() !== "") {
    addTag(tagName.value);
    tagName.value = "";
  }
}

const removeCategory = () => {
  form.categoryName = ''
}

const addCategory = (category: any) => {
  if (form.categoryName.includes(category)) {
    ElMessage.warning('分类已存在')
    return
  }
  form.categoryName = category
}

const saveCategory = () => {
  if (categoryName.value.trim() !== "") {
    addCategory(categoryName.value);
    categoryName.value = "";
  }
}



// 获取分类列表
const getList = async () => {
  loading.value = true
  try {
    const { data } = await getArticleListApi(queryParams)
    tableData.value = data.records
    total.value = data.total
    selectedIds.value = []
  } catch (error) {
  }
  loading.value = false
}

// 获取状态列表
const getStatusList = async () => {
  const { data } = await getDictDataByDictTypesApi(['article_status', 'sys_yes_no'])
  statusOptions.value = data.article_status.list
  yesNoOptions.value = data.sys_yes_no.list
}

// 表格选择项变化
const handleSelectionChange = (selection: any[]) => {
  selectedIds.value = selection.map(item => item.id)
}

// 爬取文章
const submitReptile = () => {
  if (!reptileForm.url) return
  reptileArticleApi(reptileForm.url).then((res) => {
    ElMessage.success('爬取成功')
    getList()
    reptileDialog.visible = false
    reptileForm.url = ''
  })
}

const resetNotionForm = () => {
  notionForm.pageUrl = ''
  notionForm.categoryName = 'Notion'
  notionForm.tags = ['Notion']
  notionForm.summary = ''
  notionForm.status = 0
  notionForm.readType = 1
  notionForm.isOriginal = 1
  notionForm.isStick = 0
  notionForm.isCarousel = 0
  notionForm.isRecommend = 0
  notionForm.importImages = true
}

const openNotionImport = () => {
  resetNotionForm()
  notionDialog.visible = true
}

const submitNotionImport = async () => {
  const pageUrl = String(notionForm.pageUrl || '').trim()
  const categoryName = String(notionForm.categoryName || '').trim()
  const tags = Array.isArray(notionForm.tags)
    ? notionForm.tags.map((item: string) => String(item || '').trim()).filter(Boolean)
    : []

  if (!pageUrl) {
    ElMessage.warning('请填写 Notion 页面地址')
    return
  }
  if (!categoryName) {
    ElMessage.warning('请选择或输入文章分类')
    return
  }

  submitLoading.value = true
  try {
    const res = await importNotionArticleApi({
      ...notionForm,
      pageUrl,
      categoryName,
      tags: tags.length ? tags : ['Notion']
    })
    const result = res.data || {}
    ElMessage.success(`导入成功：${result.title || 'Notion 笔记'}`)
    notionDialog.visible = false
    getList()
    if (result.articleId) {
      handleUpdate({ id: result.articleId })
    }
  } finally {
    submitLoading.value = false
  }
}

// 批量删除
const handleBatchDelete = () => {
  if (selectedIds.value.length === 0) return

  ElMessageBox.confirm(`是否确认删除 ${selectedIds.value.length} 篇文章?`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteArticleApi(selectedIds.value)
      ElMessage.success('删除成功')
      selectedIds.value = []
      getList()
    } catch (error) {
    }
  })
}

// 删除
const handleDelete = (row: any) => {
  ElMessageBox.confirm(`是否确认删除 ${row.title} 这篇文章?`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteArticleApi(row.id)
      ElMessage.success('删除成功')
      getList()
    } catch (error) {
    }
  })
}

// 发布文章
const handleChangeStatus = (row: any) => {
  updateStatusApi({ id: row.id, status: row.status }).then((res) => {
    ElMessage.success('修改成功')
    getList()
  })
}

// 推送百度
const handlePushBaidu = (row: any) => {
  pushBaiduApi(row.id).then((res) => {
    ElMessage.success('推送任务已提交')
  }).catch(() => {
    ElMessage.error('推送失败，请检查百度收录配置')
  })
}

const handlePushBaiduRecent = () => {
  pushBaiduRecentApi().then((res: any) => {
    ElMessage.success(`成功提交 ${res.data} 篇文章的推送任务`)
  }).catch(() => {
    ElMessage.error('推送失败，请检查百度收录配置')
  })
}

// 搜索
const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

// 重置查询
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

//清空表单
const clearForm = () => {
  form.id = undefined
  form.title = ''
  form.cover = undefined
  form.coverImage = undefined
  form.summary = ''
  form.categoryName = ''
  form.tags = []
  form.content = ''
  form.contentMd = ''
  form.originalUrl = ''
  form.isStick = 0
  form.status = 1
  form.isCarousel = 0
  form.isRecommend = 0
  form.keywords = ''
  lastImages.value = []
  contentEditorDialogVisible.value = false
}

// 新增用户
const handleAdd = () => {
  dialog.type = 'add'
  dialog.title = '新增文章'
  dialog.visible = true
  clearForm()
}

// 修改分类
const handleUpdate = (row: any) => {
  clearForm()
  dialog.type = 'edit'
  dialog.title = '修改文章'
  dialog.visible = true
  getDetailApi(row.id).then((res) => {
    Object.assign(form, res.data)
    if (!form.content && form.contentMd) {
      form.content = String(form.contentMd)
    }
    // 初始化编辑器图片列表，用于后续删除追踪
    lastImages.value = extractImagesFromHtml(form.content)
  })
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      form.contentMd = String(form.contentMd || form.content || '')
      try {
        if (dialog.type === 'add') {
          await addArticleApi(form)
          ElMessage.success('新增成功')
        } else {
          await updateArticleApi(form)
          ElMessage.success('修改成功')
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

// 取消按钮
const cancel = () => {
  dialog.visible = false
  reptileDialog.visible = false
  notionDialog.visible = false
  contentEditorDialogVisible.value = false
  formRef.value?.resetFields()
  reptileForm.url = ''
}

// 分页大小改变
const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  getList()
}

// 页码改变
const handleCurrentChange = (val: number) => {
  queryParams.pageNum = val
  getList()
}

// 初始化
onMounted(() => {
  syncMobile()
  getList()
  getCategoryListApi({ pageNum: 1, pageSize: 1000 }).then((res) => {
    categoryOptions.value = res.data.records
  })
  getTagListApi({ pageNum: 1, pageSize: 1000 }).then((res) => {
    tagOptions.value = res.data.records
  })

  getStatusList()
  window.addEventListener('resize', syncMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', syncMobile)
  articleEditorRef.value?.destroy?.()
  fullscreenArticleEditorRef.value?.destroy?.()
})

</script>

<style lang="scss" scoped>
.avatar-uploader {
  :deep(.el-upload) {
    border: 2px dashed var(--el-border-color-lighter);
    border-radius: 8px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: var(--el-transition-duration-fast);

    &:hover {
      border-color: var(--el-color-primary);
      background-color: var(--el-color-primary-light-9);
    }
  }

  .avatar-uploader-icon {
    font-size: 28px;
    color: var(--el-text-color-secondary);
    width: 120px;
    height: 120px;
    text-align: center;
    line-height: 120px;
  }

  .avatar {
    width: 120px !important;
    height: 120px !important;
  }
}

.app-container {

  .pagination-container {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }

  :deep(.el-dialog) {
    .el-dialog__body {
      padding: 20px 40px;
    }

    .el-form {
      .el-form-item {
        margin-bottom: 22px;

        &:last-child {
          margin-bottom: 0;
        }

        .el-form-item__label {
          font-weight: 500;
          padding-right: 20px;
        }
      }

      .el-select {
        width: 100%;
      }
    }

  }

  .mb-20 {
    margin-bottom: 20px;
  }

  .article-editor__toolbar-actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
  }

  .article-editor {
    width: 100%;
    overflow: hidden;
    border: 1px solid var(--el-border-color);
    border-radius: 12px;
    background: var(--el-bg-color);
  }

  .article-editor__toolbar {
    border-bottom: 1px solid var(--el-border-color);
  }

  .article-editor__body {
    overflow-y: hidden;
  }

  .article-editor__meta {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.5;
  }

  .article-content-dialog__header {
    margin-bottom: 12px;
  }

  .article-content-dialog__footer {
    text-align: right;
  }

  :deep(.article-content-dialog .el-dialog__body) {
    padding: 14px !important;
  }

  :deep(.article-content-dialog .article-editor) {
    margin-bottom: 0;
  }



  .dialog-footer {
    text-align: right;
    padding-top: 20px;
    border-top: 1px solid var(--el-border-color-lighter);

    .el-button {
      padding: 12px 25px;

      &+.el-button {
        margin-left: 12px;
      }
    }
  }

  .el-tag {
    margin-right: 8px;
    margin-bottom: 8px;
  }
}

@media (max-width: 768px) {
  .app-container {
    .mobile-article-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .mobile-selection-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 4px 4px 0;
      color: var(--el-text-color-secondary);
      font-size: 13px;
    }

    .mobile-article-card {
      padding: 14px;
      border-radius: 16px;
      background: var(--el-fill-color-extra-light);
      border: 1px solid var(--el-border-color-lighter);
      box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
    }

    .mobile-article-card__top {
      display: flex;
      align-items: flex-start;
      gap: 10px;
    }

    .mobile-article-card__check {
      padding-top: 4px;
      flex: 0 0 auto;
    }

    .mobile-article-card__cover {
      width: 96px;
      height: 72px;
      border-radius: 12px;
      overflow: hidden;
      background: var(--el-fill-color);
      flex: 0 0 auto;
    }

    .mobile-article-card__cover-fallback {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--el-text-color-secondary);
      font-size: 12px;
    }

    .mobile-article-card__main {
      min-width: 0;
      flex: 1;
    }

    .mobile-article-card__title {
      font-size: 15px;
      font-weight: 700;
      line-height: 1.45;
      color: var(--el-text-color-primary);
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .mobile-article-card__summary {
      margin-top: 6px;
      font-size: 13px;
      line-height: 1.6;
      color: var(--el-text-color-secondary);
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .mobile-article-card__meta {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 10px 12px;
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid var(--el-border-color-lighter);
    }

    .mobile-article-card__meta-item {
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 4px;
      font-size: 13px;
      color: var(--el-text-color-primary);

      span:last-child {
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }

    .mobile-article-card__meta-label {
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }

    .mobile-article-card__tags {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-top: 12px;
    }

    .mobile-article-card__status {
      margin-top: 12px;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .mobile-article-card__switch {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 13px;
      color: var(--el-text-color-primary);
    }

    .mobile-article-card__flags {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .mobile-article-card__actions {
      display: flex;
      gap: 10px;
      margin-top: 14px;

      .el-button {
        flex: 1;
        min-height: 38px;
        margin: 0;
      }
    }

    :deep(.el-dialog) {
      border-radius: 0 !important;

      .el-dialog__header {
        padding: 14px 16px;
      }

      .el-dialog__body {
        padding: 14px !important;
      }

      .el-dialog__footer {
        position: sticky;
        bottom: 0;
        z-index: 2;
      }

      .el-form-item {
        display: block;
        margin-bottom: 16px !important;

        .el-form-item__label {
          display: block;
          width: 100% !important;
          padding: 0 0 8px !important;
          text-align: left !important;
        }

        .el-form-item__content {
          margin-left: 0 !important;
          width: 100%;
        }
      }

      .el-row {
        margin-left: 0 !important;
        margin-right: 0 !important;
      }

      .el-col {
        padding-left: 0 !important;
        padding-right: 0 !important;
      }

      .article-editor {
        width: 100% !important;
        min-width: 0 !important;
        margin-bottom: 0;
      }

      .article-editor__toolbar {
        display: flex !important;
        flex-wrap: nowrap !important;
        height: auto !important;
        min-height: 48px;
        overflow-x: auto;
        overflow-y: hidden;
        scrollbar-width: none;

        &::-webkit-scrollbar {
          display: none;
        }

        :deep(.w-e-bar-item),
        :deep(.w-e-bar-divider) {
          flex: 0 0 auto;
        }
      }

      .article-editor__body {
        min-height: 340px !important;
      }
    }

    .article-editor__toolbar-actions {
      align-items: flex-start;
      flex-direction: column;

      :deep(.el-button) {
        width: 100%;
      }
    }

    .dialog-footer {
      padding-top: 0;
      border-top: none;
    }
  }
}

:root[data-theme='dark'] {
  .app-container {
    :deep(.el-dialog) {
      .el-form {
        .el-input__wrapper {
          box-shadow: 0 0 0 1px var(--el-border-color) inset;
        }
      }
    }
  }
}
</style>
