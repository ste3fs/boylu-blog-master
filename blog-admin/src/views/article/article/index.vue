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
              <el-button type="info" plain :icon="Setting" v-permission="['sys:article:list']"
              @click="openNotionLogs()">Notion 日志</el-button>
              <el-button type="warning" plain :icon="Refresh" v-permission="['sys:article:list']"
              @click="openNotionImageQueue">图片队列</el-button>
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
        <el-table-column label="Notion 状态" align="center" width="220">
          <template #default="scope">
            <div v-if="isNotionArticle(scope.row)" class="notion-status-cell">
              <el-tag :type="notionStatusType(scope.row.notionStatus)" size="small">
                {{ notionStatusLabel(scope.row.notionStatus) }}
              </el-tag>
              <el-tag :type="notionImageStatusType(scope.row.notionImageStatus)" size="small" effect="plain">
                图片：{{ notionImageStatusLabel(scope.row) }}
              </el-tag>
              <span class="notion-status-message">{{ scope.row.notionMessage || '暂无同步日志' }}</span>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="440" fixed="right">
          <template #default="scope">
            <PageTableActions>
              <PageTableAction type="success" :icon="Position" @click="handlePushBaidu(scope.row)"
                v-permission="['sys:article:update']">推送百度</PageTableAction>
              <PageTableAction v-if="isNotionArticle(scope.row)" type="warning" :icon="Position" @click="handleSyncNotion(scope.row)"
                v-permission="['sys:article:update']">同步 Notion</PageTableAction>
              <PageTableAction v-if="isNotionArticle(scope.row)" type="info" :icon="Setting" @click="openNotionLogs(scope.row)"
                v-permission="['sys:article:list']">日志</PageTableAction>
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

            <div v-if="isNotionArticle(row)" class="mobile-article-card__notion">
              <el-tag :type="notionStatusType(row.notionStatus)" size="small">
                {{ notionStatusLabel(row.notionStatus) }}
              </el-tag>
              <el-tag :type="notionImageStatusType(row.notionImageStatus)" size="small" effect="plain">
                图片：{{ notionImageStatusLabel(row) }}
              </el-tag>
              <span>{{ row.notionMessage || '暂无同步日志' }}</span>
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
              <el-button v-if="isNotionArticle(row)" type="warning" plain :icon="Position" @click="handleSyncNotion(row)">同步 Notion</el-button>
              <el-button v-if="isNotionArticle(row)" type="info" plain :icon="Setting" @click="openNotionLogs(row)">日志</el-button>
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
          <div v-if="form.cover" class="cover-upload-receipt">
            <div class="cover-upload-receipt__head">
              <strong>封面上传回执</strong>
              <el-tag size="small" effect="plain" :type="isManagedImageUrl(form.cover) ? 'success' : 'info'">
                {{ isManagedImageUrl(form.cover) ? '已返回托管地址' : '旧地址/外链' }}
              </el-tag>
            </div>
            <div class="cover-upload-receipt__url">{{ form.cover }}</div>
            <div class="cover-upload-receipt__meta">
              <span>尺寸：{{ coverReceiptSize }}</span>
              <span>回退：{{ coverReceiptFallback }}</span>
            </div>
          </div>
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
          <el-tag type="info">先保留原链接，导入后后台下载到本站</el-tag>
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
        title="需要先在服务端配置 NOTION_API_TOKEN，并把 Notion 页面共享给该 Integration。默认保留图片原链接以加快导入，需要长期保存图片时再开启下载到本站。"
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

    <el-dialog
      v-model="notionLogDialog.visible"
      :title="notionLogDialog.title || 'Notion 同步日志'"
      :width="isMobile ? '100vw' : '1120px'"
      :fullscreen="isMobile"
      class="notion-log-dialog"
    >
      <div class="notion-log-toolbar">
        <div class="notion-log-toolbar__summary">
          共 {{ filteredNotionLogs.length }} 条
          <span v-if="selectedNotionLogIds.length">，已选 {{ selectedNotionLogIds.length }} 条</span>
          <span v-if="notionLogShouldAutoRefresh">，队列自动刷新中</span>
        </div>
        <div class="notion-log-toolbar__filters">
          <el-select
            v-model="notionLogFilter.imageStatus"
            clearable
            placeholder="图片状态"
            style="width: 150px"
          >
            <el-option
              v-for="item in notionImageStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-input
            v-model="notionLogFilter.keyword"
            clearable
            placeholder="筛选标题 / 链接 / 提示"
            style="width: min(320px, 100%)"
          />
        </div>
        <div class="notion-log-toolbar__actions">
          <el-button :icon="Refresh" :loading="notionLogLoading" @click="refreshNotionLogs">刷新</el-button>
          <el-button
            type="danger"
            plain
            :icon="Delete"
            :disabled="selectedNotionLogIds.length === 0"
            @click="handleDeleteSelectedNotionLogs"
          >
            删除选中
          </el-button>
          <el-button
            type="danger"
            plain
            :disabled="notionLogs.length === 0"
            @click="handleDeleteVisibleNotionLogs"
          >
            清空当前列表
          </el-button>
        </div>
      </div>

      <div v-if="notionLogDialog.mode === 'queue'" class="notion-queue-summary">
        <div class="notion-queue-summary__card">
          <span>处理中</span>
          <strong>{{ notionQueueSummary.running }}</strong>
        </div>
        <div class="notion-queue-summary__card">
          <span>待处理</span>
          <strong>{{ notionQueueSummary.pending }}</strong>
        </div>
        <div class="notion-queue-summary__card is-danger">
          <span>失败</span>
          <strong>{{ notionQueueSummary.failed }}</strong>
        </div>
        <div class="notion-queue-summary__card is-warning">
          <span>部分失败</span>
          <strong>{{ notionQueueSummary.partialFailed }}</strong>
        </div>
      </div>

      <div v-loading="notionLogLoading" class="notion-log-list">
        <el-empty
          v-if="!filteredNotionLogs.length"
          :description="notionLogDialog.mode === 'queue' ? '当前没有待处理或异常的图片本地化任务' : '暂无 Notion 同步日志'"
        />
        <el-scrollbar v-else :max-height="isMobile ? 'calc(100vh - 150px)' : '62vh'">
          <article
            v-for="row in filteredNotionLogs"
            :key="row.id"
            class="notion-log-card"
            :class="`is-${row.status || 'empty'}`"
          >
            <div class="notion-log-card__head">
              <el-checkbox
                :model-value="selectedNotionLogIds.includes(row.id)"
                @change="(checked) => toggleNotionLogSelection(row.id, Boolean(checked))"
              />
              <div class="notion-log-card__title">
                <strong>{{ row.articleTitle || '未创建文章' }}</strong>
                <span>{{ row.updateTime || row.createTime || '-' }}</span>
              </div>
              <div class="notion-log-card__badges">
                <el-tag size="small" effect="plain">{{ notionActionLabel(row.action) }}</el-tag>
                <el-tag :type="notionStatusType(row.status)" size="small">
                  {{ notionStatusLabel(row.status) }}
                </el-tag>
                <el-tag :type="notionImageStatusType(row.imageStatus)" size="small" effect="plain">
                  图片：{{ notionImageStatusLabel(row) }}
                </el-tag>
              </div>
              <div class="notion-log-card__actions">
                <el-button
                  v-if="canRetryNotionImage(row)"
                  link
                  type="primary"
                  :icon="Refresh"
                  @click="handleRetryNotionImage(row)"
                >
                  重试图片
                </el-button>
                <el-button link type="danger" :icon="Delete" @click="handleDeleteNotionLog(row)">删除</el-button>
              </div>
            </div>

            <div class="notion-log-card__meta">
              <div>
                <span>块数量</span>
                <strong>{{ row.importedBlocks ?? 0 }}</strong>
              </div>
              <div>
                <span>改动字段</span>
                <strong>{{ row.changedFields ?? 0 }}</strong>
              </div>
              <div>
                <span>图片进度</span>
                <strong>{{ row.localizedImages || 0 }}/{{ row.totalImages || 0 }}</strong>
              </div>
              <div>
                <span>失败图片</span>
                <strong>{{ row.failedImages || 0 }}</strong>
              </div>
            </div>

            <div v-if="row.sourceUrl" class="notion-log-source">
              <span>来源</span>
              <el-link :href="row.sourceUrl" target="_blank" type="primary">
                {{ row.sourceUrl }}
              </el-link>
            </div>

            <div class="notion-log-result">
              <div class="notion-log-message">{{ cleanNotionLogText(row.message) || '-' }}</div>
              <div v-if="row.warnings" class="notion-log-warning">{{ cleanNotionLogText(row.warnings) }}</div>
              <el-collapse v-if="row.errorDetail" class="notion-log-error-collapse">
                <el-collapse-item title="查看技术错误详情" :name="String(row.id)">
                  <pre class="notion-log-error">{{ row.errorDetail }}</pre>
                </el-collapse-item>
              </el-collapse>
            </div>
          </article>
        </el-scrollbar>
      </div>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Setting, Position, Refresh } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import UploadImage from '@/components/Upload/Image.vue'
import { isManagedImageUrl } from '@/utils/image'
import { getCategoryListApi } from '@/api/article/category'
import { getTagListApi } from '@/api/article/tag'
import {
  getArticleListApi, getDetailApi, deleteArticleApi,
  addArticleApi, updateArticleApi, updateStatusApi, reptileArticleApi, pushBaiduApi, pushBaiduRecentApi, importNotionArticleApi, syncNotionArticleApi, getNotionSyncLogsApi, getNotionImageQueueApi, retryNotionImageLocalizationApi, deleteNotionSyncLogsApi
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

const notionLogDialog = reactive({
  visible: false,
  title: '',
  articleId: undefined as any,
  mode: 'recent' as 'recent' | 'article' | 'queue'
})
const notionLogs = ref<any[]>([])
const notionLogLoading = ref(false)
const selectedNotionLogIds = ref<any[]>([])
const notionLogFilter = reactive({
  imageStatus: '',
  keyword: ''
})
const notionLogAutoRefresh = ref(true)
let notionLogRefreshTimer: ReturnType<typeof setInterval> | null = null

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
  importImages: false
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

const coverReceiptSize = computed(() => {
  const width = Number(form.coverImage?.width || 0)
  const height = Number(form.coverImage?.height || 0)
  if (width > 0 && height > 0) {
    return `${width} x ${height}`
  }
  return '待服务端返回尺寸'
})

const coverReceiptFallback = computed(() => {
  const fallback = String(form.coverImage?.fallback || form.cover || '').trim()
  return fallback || '-'
})

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
  notionForm.importImages = false
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
    const actionText = result.updated ? '同步成功' : '导入成功'
    ElMessage.success(`${actionText}：${result.title || 'Notion 笔记'}，图片会继续在后台下载到本站`)
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
  pushBaiduApi(row.id).then((res: any) => {
    if (res?.data) {
      ElMessage.success('百度推送成功')
    } else {
      ElMessage.error('百度推送失败，请检查推送配置或稍后重试')
    }
  }).catch(() => {
    ElMessage.error('推送失败，请检查百度收录配置')
  })
}

const handlePushBaiduRecent = () => {
  pushBaiduRecentApi().then((res: any) => {
    const count = Number(res.data || 0)
    if (count > 0) {
      ElMessage.success(`成功推送 ${count} 篇文章`)
    } else {
      ElMessage.warning('本次没有成功推送的文章，请检查百度推送配置')
    }
  }).catch(() => {
    ElMessage.error('推送失败，请检查百度收录配置')
  })
}

const isNotionArticle = (row: any) => {
  return String(row?.originalUrl || '').includes('notion.so')
}

const notionActionLabel = (action?: string) => {
  const map: Record<string, string> = {
    import: '导入',
    sync: '同步'
  }
  return map[String(action || '')] || '同步'
}

const notionStatusLabel = (status?: string) => {
  const map: Record<string, string> = {
    running: '同步中',
    success: '同步成功',
    failed: '同步失败',
    skipped: '已跳过'
  }
  return map[String(status || '')] || '暂无日志'
}

const notionStatusType = (status?: string) => {
  const map: Record<string, any> = {
    running: 'warning',
    success: 'success',
    failed: 'danger',
    skipped: 'info'
  }
  return map[String(status || '')] || 'info'
}

const notionImageStatusLabel = (value: any) => {
  const status = typeof value === 'string' ? value : value?.notionImageStatus || value?.imageStatus
  const total = typeof value === 'object' ? (value?.notionImageTotal ?? value?.totalImages) : undefined
  const localized = typeof value === 'object' ? (value?.notionImageLocalized ?? value?.localizedImages) : undefined
  const failed = typeof value === 'object' ? (value?.notionImageFailed ?? value?.failedImages) : undefined
  const map: Record<string, string> = {
    pending: '待处理',
    running: '处理中',
    success: '完成',
    partial_failed: '部分失败',
    failed: '失败',
    none: '无远程图',
    skipped: '已跳过'
  }
  const label = map[String(status || '')] || '暂无日志'
  if (typeof total === 'number' && total > 0) {
    return failed ? `${label} ${localized || 0}/${total}，失败 ${failed}` : `${label} ${localized || 0}/${total}`
  }
  return label
}

const notionImageStatusType = (status?: string) => {
  const map: Record<string, any> = {
    pending: 'info',
    running: 'warning',
    success: 'success',
    partial_failed: 'warning',
    failed: 'danger',
    none: 'info',
    skipped: 'info'
  }
  return map[String(status || '')] || 'info'
}

const canRetryNotionImage = (row: any) => {
  const imageStatus = String(row?.imageStatus || row?.notionImageStatus || '')
  return Boolean(row?.id) && ['failed', 'partial_failed'].includes(imageStatus)
}

const notionImageStatusOptions = [
  { label: '待处理', value: 'pending' },
  { label: '处理中', value: 'running' },
  { label: '完成', value: 'success' },
  { label: '部分失败', value: 'partial_failed' },
  { label: '失败', value: 'failed' },
  { label: '无远程图', value: 'none' },
  { label: '已跳过', value: 'skipped' }
]

const cleanNotionLogText = (text?: string) => {
  return String(text || '')
    .replace(/com\.boylu\.[\s\S]*$/g, '')
    .replace(/\s+at\s+[\s\S]*$/g, '')
    .trim()
}

const filteredNotionLogs = computed(() => {
  const keyword = String(notionLogFilter.keyword || '').trim().toLowerCase()
  const imageStatus = String(notionLogFilter.imageStatus || '').trim()
  return notionLogs.value.filter((row) => {
    if (imageStatus && String(row?.imageStatus || row?.notionImageStatus || '') !== imageStatus) {
      return false
    }
    if (!keyword) {
      return true
    }
    return [
      row?.articleTitle,
      row?.sourceUrl,
      row?.message,
      row?.warnings,
      row?.errorDetail
    ].some((field) => String(field || '').toLowerCase().includes(keyword))
  })
})

const notionQueueSummary = computed(() => {
  return notionLogs.value.reduce((summary, row) => {
    const status = String(row?.imageStatus || '')
    if (status === 'running') {
      summary.running += 1
    } else if (status === 'pending') {
      summary.pending += 1
    } else if (status === 'failed') {
      summary.failed += 1
    } else if (status === 'partial_failed') {
      summary.partialFailed += 1
    }
    return summary
  }, {
    running: 0,
    pending: 0,
    failed: 0,
    partialFailed: 0
  })
})

const notionLogHasActiveQueue = computed(() => {
  return notionLogs.value.some((row) => {
    const imageStatus = String(row?.imageStatus || row?.notionImageStatus || '')
    const status = String(row?.status || row?.notionStatus || '')
    return imageStatus === 'pending' || imageStatus === 'running' || status === 'running'
  })
})

const notionLogShouldAutoRefresh = computed(() => {
  return notionLogDialog.visible && notionLogAutoRefresh.value && notionLogHasActiveQueue.value
})

const stopNotionLogAutoRefresh = () => {
  if (notionLogRefreshTimer) {
    clearInterval(notionLogRefreshTimer)
    notionLogRefreshTimer = null
  }
}

const startNotionLogAutoRefresh = () => {
  stopNotionLogAutoRefresh()
  notionLogRefreshTimer = setInterval(() => {
    if (!notionLogLoading.value) {
      refreshNotionLogs()
    }
  }, 8000)
}

const loadNotionLogs = async (articleId?: any) => {
  notionLogLoading.value = true
  try {
    const res: any = notionLogDialog.mode === 'queue'
      ? await getNotionImageQueueApi({ limit: 200 })
      : await getNotionSyncLogsApi(articleId)
    notionLogs.value = res.data || []
    selectedNotionLogIds.value = selectedNotionLogIds.value.filter((id) =>
      filteredNotionLogs.value.some((row) => row.id === id)
    )
  } finally {
    notionLogLoading.value = false
  }
}

const openNotionLogs = async (row?: any) => {
  notionLogFilter.imageStatus = ''
  notionLogFilter.keyword = ''
  notionLogDialog.mode = row?.id ? 'article' : 'recent'
  notionLogDialog.title = row?.id ? `${row.title || '文章'} - Notion 同步日志` : '最近 Notion 同步日志'
  notionLogDialog.articleId = row?.id
  notionLogDialog.visible = true
  selectedNotionLogIds.value = []
  await loadNotionLogs(row?.id)
}

const openNotionImageQueue = async () => {
  notionLogFilter.imageStatus = ''
  notionLogFilter.keyword = ''
  notionLogDialog.mode = 'queue'
  notionLogDialog.title = 'Notion 图片本地化队列'
  notionLogDialog.articleId = undefined
  notionLogDialog.visible = true
  selectedNotionLogIds.value = []
  await loadNotionLogs()
}

const refreshNotionLogs = () => {
  return loadNotionLogs(notionLogDialog.articleId)
}

const toggleNotionLogSelection = (id: any, checked: boolean) => {
  if (!id) return
  if (checked) {
    if (!selectedNotionLogIds.value.includes(id)) {
      selectedNotionLogIds.value.push(id)
    }
    return
  }
  selectedNotionLogIds.value = selectedNotionLogIds.value.filter((item) => item !== id)
}

const deleteNotionLogs = async (ids: any[]) => {
  const validIds = ids.filter(Boolean)
  if (!validIds.length) return
  await deleteNotionSyncLogsApi(validIds.join(','))
  ElMessage.success('Notion 日志已删除')
  selectedNotionLogIds.value = selectedNotionLogIds.value.filter((id) => !validIds.includes(id))
  await refreshNotionLogs()
  getList()
}

const handleDeleteNotionLog = async (row: any) => {
  if (!row?.id) return
  await ElMessageBox.confirm('确定删除这条 Notion 同步日志吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteNotionLogs([row.id])
}

const handleDeleteSelectedNotionLogs = async () => {
  if (!selectedNotionLogIds.value.length) {
    ElMessage.warning('请先选择要删除的日志')
    return
  }
  await ElMessageBox.confirm(`确定删除选中的 ${selectedNotionLogIds.value.length} 条 Notion 同步日志吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteNotionLogs(selectedNotionLogIds.value)
}

const handleDeleteVisibleNotionLogs = async () => {
  const ids = filteredNotionLogs.value.map((row) => row.id).filter(Boolean)
  if (!ids.length) return
  await ElMessageBox.confirm(`确定删除当前列表中的 ${ids.length} 条 Notion 同步日志吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteNotionLogs(ids)
}

const handleRetryNotionImage = async (row: any) => {
  if (!row?.id) return
  await retryNotionImageLocalizationApi(row.id)
  ElMessage.success('图片本地化已重新加入队列，系统会先刷新 Notion 临时图链再重试')
  await refreshNotionLogs()
  getList()
}

const handleSyncNotion = async (row: any) => {
  if (!row?.id) return
  const res: any = await syncNotionArticleApi(row.id)
  const result = res?.data || {}
  if (result.updated === false) {
    ElMessage.info('Notion 页面无变化，已跳过导入')
  } else {
    ElMessage.success('Notion 同步完成，图片会继续在后台下载到本站')
  }
  getList()
  await openNotionLogs({
    id: row.id,
    title: result.title || row.title
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
  notionLogDialog.visible = false
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

watch(
  notionLogShouldAutoRefresh,
  (enabled) => {
    if (enabled) {
      startNotionLogAutoRefresh()
      return
    }
    stopNotionLogAutoRefresh()
  },
  { immediate: true }
)

watch(
  () => notionLogDialog.visible,
  (visible) => {
    if (!visible) {
      stopNotionLogAutoRefresh()
    }
  }
)

onUnmounted(() => {
  stopNotionLogAutoRefresh()
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

.notion-log-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.notion-log-toolbar__summary {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.notion-log-toolbar__filters {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 12px;
}

.notion-log-toolbar__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.notion-queue-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.notion-queue-summary__card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
}

.notion-queue-summary__card span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.notion-queue-summary__card strong {
  color: var(--el-text-color-primary);
  font-size: 24px;
  line-height: 1;
}

.notion-queue-summary__card.is-danger {
  background: rgba(245, 108, 108, 0.08);
}

.notion-queue-summary__card.is-warning {
  background: rgba(230, 162, 60, 0.1);
}

.notion-log-list {
  min-height: 220px;
}

.notion-log-card {
  margin-bottom: 12px;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-left: 4px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.notion-log-card.is-success {
  border-left-color: var(--el-color-success);
}

.notion-log-card.is-failed {
  border-left-color: var(--el-color-danger);
}

.notion-log-card.is-running {
  border-left-color: var(--el-color-warning);
}

.notion-log-card.is-skipped {
  border-left-color: var(--el-color-info);
}

.notion-log-card__head {
  display: grid;
  grid-template-columns: auto minmax(180px, 1fr) auto auto;
  align-items: center;
  gap: 12px;
}

.notion-log-card__title {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.notion-log-card__title strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 14px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notion-log-card__title span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.notion-log-card__badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.notion-log-card__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}

.notion-log-card__meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.notion-log-card__meta > div {
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
}

.notion-log-card__meta span,
.notion-log-source span {
  display: block;
  margin-bottom: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.notion-log-card__meta strong {
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.notion-log-source {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
}

.notion-log-source :deep(.el-link__inner) {
  max-width: 100%;
  overflow-wrap: anywhere;
  line-height: 1.5;
  text-align: left;
}

.notion-log-result {
  margin-top: 12px;
}

.notion-log-message,
.notion-log-warning {
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.notion-log-message {
  background: var(--el-fill-color-extra-light);
  color: var(--el-text-color-primary);
}

.notion-log-warning {
  margin-top: 8px;
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning-dark-2);
}

.notion-log-error-collapse {
  margin-top: 8px;
}

.notion-log-error {
  max-height: 220px;
  margin: 0;
  padding: 10px 12px;
  overflow: auto;
  border-radius: 8px;
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.cover-upload-receipt {
  width: 100%;
  margin-top: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--el-border-color-lighter);
  background: linear-gradient(180deg, rgba(250, 250, 252, 0.96), rgba(245, 247, 250, 0.96));
}

.cover-upload-receipt__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.cover-upload-receipt__url {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-primary);
  word-break: break-all;
}

.cover-upload-receipt__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 10px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
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

  .notion-status-cell {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
  }

  .notion-status-message {
    max-width: 190px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .notion-log-count,
  .notion-log-message,
  .notion-log-warning,
  .notion-log-error {
    font-size: 12px;
    line-height: 1.5;
  }

  .notion-log-count {
    margin-top: 4px;
    color: var(--el-text-color-secondary);
  }

  .notion-log-warning {
    margin-top: 4px;
    color: var(--el-color-warning);
    white-space: pre-wrap;
  }

  .notion-log-error {
    max-height: 120px;
    margin-top: 4px;
    color: var(--el-color-danger);
    overflow: auto;
    white-space: pre-wrap;
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
  .notion-log-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .notion-log-toolbar__filters,
  .notion-log-toolbar__actions {
    width: 100%;
  }

  .notion-log-toolbar__actions {
    justify-content: flex-start;
  }

  .notion-queue-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .notion-log-card {
    padding: 12px;
  }

  .notion-log-card__head {
    grid-template-columns: auto 1fr;
    align-items: flex-start;
  }

  .notion-log-card__badges,
  .notion-log-card__actions {
    grid-column: 2;
    justify-content: flex-start;
  }

  .notion-log-card__meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

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

    .mobile-article-card__notion {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 6px;
      margin-top: 12px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
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
