<template>
  <div class="home" >
    <div class="content-layout">
      <main class="home-main-content">
        <Carousel
          v-if="carouselSlides?.length > 0"
          :slides="carouselSlides"
          @click="goToPost"
        />
        <MomentsList />

        <div>
          <el-tabs v-model="activeName" @tab-click="handleClick">
            <el-tab-pane
              v-for="category in categories"
              :key="category.id"
              :name="String(category.id)"
            >
              <template slot="label">
                <span class="label-info">
                  <i :class="category.icon"></i>
                  {{ category.name }}
                </span>
              </template>
              <ArticleList
                :articles="articleList"
                :loading="loading"
                :total="total"
                :params="params"
                @article-click="goToPost"
                @page-change="changePage"
                class="article-list"
              />
            </el-tab-pane>
          </el-tabs>
        </div>
      </main>
      <Sidebar :category-count="Math.max(categories.length - 1, 0)" />
    </div>
  </div>
</template>

<script>
import Tabs from 'element-ui/lib/tabs'
import TabPane from 'element-ui/lib/tab-pane'
import 'element-ui/lib/theme-chalk/tabs.css'
import 'element-ui/lib/theme-chalk/tab-pane.css'
import ArticleList from "@/components/ArticleList/index.vue";
import Carousel from "@/views/home/components/carousel.vue";
import Sidebar from "@/components/Sidebar/index.vue";
import MomentsList from "@/views/home/components/moments.vue";
import {
  getArticlesApi,
  getCarouselArticlesApi,
  getAllCategoriesApi,
} from "@/api/article";
import { prewarmImageUrls, resolveImageUrl } from "@/utils/image";

const ARTICLE_LIST_STATE_KEY = "boylu:article-list-state";
const ARTICLE_LIST_STATE_MAX_AGE = 30 * 60 * 1000;

export default {
  name: "Home",
  components: {
    ElTabs: Tabs,
    ElTabPane: TabPane,
    ArticleList,
    Carousel,
    Sidebar,
    MomentsList,
  },
  data() {
    return {
      total: 0,
      params: {
        pageNum: 1,
        pageSize: 10,
      },
      articleList: [],
      carouselSlides: [],
      loading: false,
      articleRequestSeq: 0,
      activeName: "all",
      restoreListState: null,
      categories: [
        {
          id: "all",
          name: "全部",
          icon: "el-icon-menu",
        },
      ],
    };
  },
  methods: {
    prewarmHomeCriticalImages(records = []) {
      const warmupUrls = records
        .slice(0, 2)
        .flatMap((post) => {
          const source = resolveImageUrl(
            (post && post.coverImage && (post.coverImage.fallback || post.coverImage.source)) || post.cover
          );
          if (!source) {
            return [];
          }
          return [source];
        });
      prewarmImageUrls(warmupUrls, 2);
    },
    /**
     * 切换标签
     * @param {string} tab 标签
     * @param {Event} event 事件
     */
    handleClick(tab) {
      this.params.categoryId = tab.name === "all" ? null : tab.name;
      this.params.pageNum = 1;
      this.getArticleList();
    },
    /**
     * 跳转到文章详情
     * @param {number} id 文章id
     */
    goToPost(id) {
      this.saveArticleListState(id);
      this.$router.push(`/post/${id}`);
    },
    saveArticleListState(articleId) {
      const state = {
        articleId,
        activeName: this.activeName,
        params: { ...this.params },
        scrollY: window.pageYOffset || document.documentElement.scrollTop || 0,
        savedAt: Date.now(),
      };
      sessionStorage.setItem(ARTICLE_LIST_STATE_KEY, JSON.stringify(state));
    },
    consumeArticleListState() {
      const raw = sessionStorage.getItem(ARTICLE_LIST_STATE_KEY);
      if (!raw) {
        return null;
      }

      try {
        const state = JSON.parse(raw);
        if (!state.savedAt || Date.now() - state.savedAt > ARTICLE_LIST_STATE_MAX_AGE) {
          sessionStorage.removeItem(ARTICLE_LIST_STATE_KEY);
          return null;
        }
        return state;
      } catch (error) {
        sessionStorage.removeItem(ARTICLE_LIST_STATE_KEY);
        return null;
      }
    },
    restoreArticleListPosition() {
      if (!this.restoreListState) {
        return;
      }

      const scrollY = Number(this.restoreListState.scrollY || 0);
      this.$nextTick(() => {
        setTimeout(() => {
          window.scrollTo({
            top: scrollY,
            behavior: "auto",
          });
          this.restoreListState = null;
          sessionStorage.removeItem(ARTICLE_LIST_STATE_KEY);
        }, 120);
      });
    },
    /**
     * 切换页码
     * @param {number} page 页码
     */
    changePage(page) {
      this.params.pageNum = page;
      this.getArticleList();
      window.scrollTo({
        top: this.$refs.postsSection?.offsetTop - 80,
        behavior: "smooth",
      });
    },
    /**
     * 获取文章列表
     */
    getArticleList() {
      const requestSeq = ++this.articleRequestSeq;
      this.loading = true;
      return getArticlesApi(this.params)
        .then((res) => {
          if (requestSeq !== this.articleRequestSeq) {
            return;
          }
          this.articleList = res.data.records;
          this.total = res.data.total;
          if (Number(this.params.pageNum || 1) === 1) {
            this.prewarmHomeCriticalImages(this.articleList);
          }
        })
        .catch((error) => {
          if (requestSeq !== this.articleRequestSeq) {
            return;
          }
          console.error("Failed to fetch articles:", error);
        })
        .finally(() => {
          if (requestSeq === this.articleRequestSeq) {
            this.loading = false;
          }
        });
    },
    /**
     * 获取轮播和推荐文章
     */
    getCarouselArticles() {
      getCarouselArticlesApi().then((res) => {
        this.carouselSlides = res.data;
      });
    },
    /**
     * 获取所有分类
     */
    getAllCategories() {
      getAllCategoriesApi().then((res) => {
        const icons = [
          "el-icon-document",
          "el-icon-collection",
          "el-icon-reading",
          "el-icon-coffee-cup",
          "el-icon-notebook-2",
          "el-icon-edit",
        ];
        const categoriesWithIcons = res.data.map((category) => ({
          ...category,
          icon: icons[Math.floor(Math.random() * icons.length)],
        }));
        this.categories.push(...categoriesWithIcons);
      });
    },
  },
  created() {
    const savedState = this.consumeArticleListState();
    if (savedState) {
      this.restoreListState = savedState;
      this.activeName = savedState.activeName || this.activeName;
      this.params = {
        ...this.params,
        ...(savedState.params || {}),
      };
    }

    this.getArticleList().then(() => {
      this.restoreArticleListPosition();
    });
    this.getCarouselArticles();
    this.getAllCategories();
  },
};
</script>

<style lang="scss" scoped>
.home {
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  padding: $spacing-lg;

  @include responsive(lg) {
    padding: $spacing-sm;
  }
}

.content-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: $spacing-lg * 2;
  padding: 0 $spacing-xl;
  margin-bottom: $spacing-xl * 2;
  min-height: calc(100vh - 80px);
  align-items: stretch;

  @include responsive(lg) {
    grid-template-columns: 1fr;
    gap: $spacing-lg;
    padding: 0;
  }

  @include responsive(md) {
    gap: $spacing-md;
  }
}

.home-main-content {
  min-width: 0;
  width: 100%;
  height: 100%;

  @include responsive(md) {
    width: 100%;
  }

  .carousel {
    margin-bottom: $spacing-xl;
    width: 100%;
    max-height: 480px;

    @include responsive(md) {
      margin-bottom: $spacing-xl;
      max-height: 280px;
      :deep(h3) {
        font-size: 1.2em;
      }
    }
  }
}

:deep(.el-tabs__nav-scroll) {
  overflow-x: scroll !important;

  &::-webkit-scrollbar {
    display: none !important;
  }
}
:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-tabs__header) {
  margin-bottom: $spacing-md;
}
.label-info {
  display: flex;
  align-items: center;
  gap: $spacing-base;
  color: var(--text-primary);
  // .el-icon{
  //   margin-right: 4px;
  //   vertical-align: middle;
  // }
}
</style>
