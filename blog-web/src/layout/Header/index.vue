<template>
  <header class="site-header" :class="{ 'header-hidden': !isHeaderVisible }">
    <nav class="navbar">
      <!-- 移动端菜单按钮 -->
      <button class="menu-btn" @click="handleOpenMobileMenu">
        <i class="fas fa-bars"></i>
      </button>

      <div class="nav-left">
        <router-link to="/" class="logo">
          <img :src="siteLogoSrc" :alt="$store.state.webSiteInfo.name">
          <span class="logo-text">{{$store.state.webSiteInfo.name}}</span>
        </router-link>
      </div>

      <div class="nav-center" @mouseleave="resetDockState">
        <div
          v-for="(item, index) in filteredMenuItems"
          :key="item.path"
          class="nav-item"
          @mouseleave="handleMouseLeave"
        >
          <router-link
            :to="item.path"
            class="nav-link"
            :class="{
              'has-dropdown': item.children,
              'active': isActive(item),
              [item.colorClass]: true
            }"
            :style="getDockItemStyle(index)"
            @mouseenter.native="handleMouseEnter(item, index, $event)"
            @mousemove.native="handleDockMove(index, $event)"
          >
            <span class="nav-link__glow"></span>
            <span class="nav-link__content">
              <i :class="item.icon"></i>
              {{ item.name }}
              <i v-if="item.children" class="fas fa-chevron-down dropdown-icon"></i>
            </span>
          </router-link>

          <div v-if="item.children"
               class="dropdown-menu"
               :class="{ active: activeDropdown === item.name }"
          >
            <a
              href="javascript:void(0)"
              v-for="child in item.children"
              :key="child.path"
              class="dropdown-item"
              :class="{ 'active': isChildActive(child) }"
              @click="child.colorClass === 'contact-link' ? triggerContactAction() : handleDropdownItemClick(child)"
            >
              <i :class="child.icon"></i>
              {{ child.name }}
            </a>
          </div>
        </div>
      </div>

      <div class="nav-right">
        <a href="javascript:void(0)" class="search-btn" @click="handleSearch">
          <i class="fas fa-search"></i>
          <span class="search-text">搜索</span>
        </a>

        <!-- 修改消息按钮的跳转路径 -->
        <router-link to="/notifications" class="message-btn">
          <i class="far fa-bell"></i>
          <span class="message-count" v-if="showBage()" />
        </router-link>

        <!-- 移动端搜索按钮 -->
        <button class="mobile-search-btn" @click="handleSearch">
          <i class="fas fa-search"></i>
        </button>

        <div class="user-info">
          <div v-if="$store.state.userInfo" class="user-section" @mouseleave="showDropdown = false">
            <div class="avatar" @mouseenter="showDropdown = true">
              <el-avatar class="avatar-icon" :src="userAvatarSrc" icon="el-icon-user-solid" />
            </div>
            <!-- 用户下拉菜单 -->
            <div class="user-dropdown" v-show="showDropdown">
              <div class="dropdown-header">
                <img :src="userAvatarSrc" :alt="$store.state.userInfo.nickname">
                <div class="user-details">
                  <span class="username">{{ $store.state.userInfo.nickname }}</span>
                  <span class="role">{{ $store.state.userInfo.role === 'admin' ? '管理员' : '普通用户' }}</span>
                </div>
              </div>
              <div class="dropdown-divider"></div>
              <router-link to="/user/profile" class="dropdown-item">
                <i class="fas fa-user"></i>
                个人中心
              </router-link>
              <div class="dropdown-item" @click="handleLogout">
                <i class="fas fa-sign-out-alt"></i>
                退出登录
              </div>
            </div>
          </div>
          <div v-else class="avatar" @click="handleLogin">
            <el-avatar
              class="avatar-icon"
              icon="el-icon-user-solid"
              :src="guestAvatarSrc"
            />
          </div>
        </div>
      </div>
    </nav>
  </header>
</template>

<script>
import { handleContactAction, resolveContactAction } from '@/utils/contact'
import { resolveImageUrl } from '@/utils/image'

export default {
  name: 'TheHeader',
  data() {
    return {
      searchQuery: '',
      showSearchPanel: false,
      showMobileSearch: false,
      lastScrollTop: 0,
      isHeaderVisible: true,
      menuItems: [
        { 
          name: '首页', 
          path: '/', 
          icon: 'fas fa-home',
          colorClass: 'home-link'
        },
        { 
          name: '文章归档', 
          path: '/archives', 
          icon: 'fas fa-archive',
          colorClass: 'archive-link',
          children: [
            { 
              name: '归档', 
              path: '/archive', 
              icon: 'fas fa-clock',
              colorClass: 'clock-link'
            },
            { 
              name: '分类', 
              path: '/categories', 
              icon: 'fas fa-folder',
              colorClass: 'category-link'
            },
            { 
              name: '标签', 
              path: '/tags', 
              icon: 'fas fa-tags',
              colorClass: 'tag-link'
            }
          ]
        },
        { 
          name: '说说', 
          path: '/moments', 
          icon: 'fas fa-comment-dots',
          colorClass: 'talk-link'
        },
        { 
          name: '热搜', 
          path: '/hotSearch', 
          icon: 'fas fa-fire',
          colorClass: 'hot-link'
        },
        {
          name: '资源',
          path: '/resources',
          icon: 'fas fa-cloud-download-alt',
          colorClass: 'resource-link'
        },
        {
          name: 'AI 助手',
          path: '/ai',
          icon: 'fas fa-robot',
          colorClass: 'ai-link'
        },
        {
          name: '相册',
          path: '/photos', 
          icon: 'fas fa-images',
          colorClass: 'photos-link'
        },
        { 
          name: '留言板', 
          path: '/messages', 
          icon: 'fas fa-envelope',
          colorClass: 'message-link'
        },
        { 
          name: '友情链接', 
          path: '/friends', 
          icon: 'fas fa-users',
          colorClass: 'friend-link'
        },
        { 
          name: '关于本站', 
          path: '/about', 
          icon: 'fas fa-info-circle',
          colorClass: 'about-link',
          children: [
            { 
              name: '关于我', 
              path: '/about', 
              icon: 'fas fa-user',
              colorClass: 'about-me-link'
            },
            { 
              name: '联系站长', 
              path: '', 
              icon: 'fas fa-envelope',
              colorClass: 'contact-link',
              external: false 
            },
            { 
              name: '后台管理', 
              path: import.meta.env.VITE_APP_ADMIN_URL || '/boylu1107/login',
              icon: 'fas fa-tv',
              colorClass: 'admin-link',
              external: true 
            }
          ]
        }
      ],
      activeDropdown: null,
      hoveredNavIndex: null,
      hoveredNavOffset: 0,
      hoveredNavTargetOffset: 0,
      dockAnimationFrame: null,
      showDropdown: false,
      showSearch: false,
      unreadCount: 0,
    }
  },
  computed: {
    contactAction() {
      return resolveContactAction(this.$store.state.webSiteInfo)
    },
    siteLogoSrc() {
      return this.resolveSafeAvatar([
        this.$store.state.webSiteInfo?.authorAvatar,
        this.$store.state.webSiteInfo?.touristAvatar,
        this.$store.state.webSiteInfo?.logo
      ])
    },
    userAvatarSrc() {
      return this.resolveSafeAvatar([
        this.$store.state.userInfo?.avatar,
        this.$store.state.webSiteInfo?.authorAvatar,
        this.$store.state.webSiteInfo?.logo
      ])
    },
    guestAvatarSrc() {
      return this.resolveSafeAvatar([
        this.$store.state.webSiteInfo?.touristAvatar,
        this.$store.state.webSiteInfo?.authorAvatar,
        this.$store.state.webSiteInfo?.logo
      ])
    },
    resolvedMenuItems() {
      return this.menuItems.map(item => {
        if (!item.children) {
          return item
        }

        return {
          ...item,
          children: item.children.map(child => {
            if (child.colorClass !== 'contact-link') {
              return child
            }

            return {
              ...child,
              path: this.contactAction.path || child.path || '__contact__',
              external: Boolean(this.contactAction.external),
              icon: this.contactAction.icon || child.icon
            }
          })
        }
      })
    },
    filteredMenuItems() {
      return this.resolvedMenuItems.map(item => ({
        ...item,
        path: item.children ? item.children[0].path : item.path
      }))
    }
  },
  methods: {
    resolveSafeAvatar(candidates = []) {
      const match = candidates
        .map(url => (url || '').toString().trim())
        .find(Boolean)

      return resolveImageUrl(match, this.$store.state.webSiteInfo.authorAvatar || this.$store.state.defaultImage)
    },
    handleOpenMobileMenu() {
      this.$store.commit('SET_MOBILE_MENU_VISIBLE', true)
    },
    handleSearch() {
      this.$store.commit('SET_SEARCH_VISIBLE', true)
    },
    handleLogin() {
      this.$router.push({
        path: '/login',
        query: {
          redirect: this.$route?.fullPath || '/'
        }
      })
    },
    closeAllPanels() {
      this.showMobileSearch = false
    },
    closeMobileSearch() {
      this.showMobileSearch = false
      this.searchQuery = ''
    },
    handleMobileSearch(tag) {
      this.searchQuery = tag
      this.$refs.mobileSearchInput.focus()
    },
    handleMouseEnter(item, index, event) {
      this.hoveredNavIndex = index
      this.updateDockOffset(event)
      if (item.children) {
        this.activeDropdown = item.name
      }
    },
    handleDockMove(index, event) {
      this.hoveredNavIndex = index
      this.updateDockOffset(event)
    },
    updateDockOffset(event) {
      const currentTarget = event && event.currentTarget
      if (!currentTarget || typeof currentTarget.getBoundingClientRect !== 'function') {
        this.hoveredNavTargetOffset = 0
        return
      }

      const rect = currentTarget.getBoundingClientRect()
      if (!rect.width) {
        this.hoveredNavTargetOffset = 0
        return
      }

      const cursorRatio = (event.clientX - rect.left) / rect.width
      const normalized = Math.max(-1, Math.min(1, (cursorRatio - 0.5) * 2))
      this.hoveredNavTargetOffset = Number(normalized.toFixed(3))
      this.startDockAnimation()
    },
    startDockAnimation() {
      if (this.dockAnimationFrame) {
        return
      }

      const step = () => {
        const delta = this.hoveredNavTargetOffset - this.hoveredNavOffset
        if (Math.abs(delta) < 0.006) {
          this.hoveredNavOffset = this.hoveredNavTargetOffset
          this.dockAnimationFrame = null
          return
        }

        this.hoveredNavOffset = Number((this.hoveredNavOffset + delta * 0.14).toFixed(4))
        this.dockAnimationFrame = window.requestAnimationFrame(step)
      }

      this.dockAnimationFrame = window.requestAnimationFrame(step)
    },
    getDockItemStyle(index) {
      if (this.hoveredNavIndex === null) {
        return {
          '--dock-scale': 1,
          '--dock-lift': '0px',
          '--dock-shift': '0px',
          '--dock-glow-opacity': 0,
          '--dock-label-opacity': 0.92
        }
      }

      const distance = Math.abs(this.hoveredNavIndex - index)
      const direction = index > this.hoveredNavIndex ? 1 : -1
      const directionalOffset = this.hoveredNavOffset * 4.2
      const profiles = {
        0: {
          scale: 1.085,
          lift: -4.5,
          shift: directionalOffset,
          glow: 0.68,
          label: 1
        },
        1: {
          scale: 1.028,
          lift: -1.4,
          shift: direction * 1.5 + this.hoveredNavOffset * 1.2,
          glow: 0.24,
          label: 0.975
        }
      }

      const profile = profiles[distance] || {
        scale: 1,
        lift: 0,
        shift: 0,
        glow: 0,
        label: 0.92
      }

      return {
        '--dock-scale': profile.scale,
        '--dock-lift': `${profile.lift}px`,
        '--dock-shift': `${profile.shift}px`,
        '--dock-glow-opacity': profile.glow,
        '--dock-label-opacity': profile.label
      }
    },
    handleMouseLeave() {
      this.activeDropdown = null
    },
    resetDockState() {
      this.activeDropdown = null
      this.hoveredNavIndex = null
      this.hoveredNavTargetOffset = 0
      this.startDockAnimation()
    },
    isActive(item) {
      if (item.children) {
        // 如果子菜单，检查是否有子菜单项被激活
        return item.children.some(child => this.isChildActive(child))
      }
      // 精确匹配路由路径
      return this.$route.path === item.path
    },
    isChildActive(child) {
      return this.$route.path === child.path
    },
    triggerContactAction() {
      this.activeDropdown = null
      handleContactAction(this, this.$store.state.webSiteInfo)
    },
    handleDropdownItemClick(item) {
      this.activeDropdown = null
      if (item.external) {
        window.open(item.path, '_blank')
        return
      }
      this.$router.push(item.path)
    },
    handleLogout() {
      this.$store.dispatch('logout')
      this.$message.success('已退出登录')
      this.showDropdown = false
    },
    showBage() {
      return this.$store.state.isUnread
    },
    handleScroll() {
      const currentScrollTop = window.pageYOffset || document.documentElement.scrollTop
      
      // 如果滚动位置小于 100px，始终显示 header
      if (currentScrollTop < 100) {
        this.isHeaderVisible = true
        this.lastScrollTop = currentScrollTop
        return
      }

      // 判断滚动方向
      if (currentScrollTop > this.lastScrollTop) {
        // 向下滚动
        this.isHeaderVisible = false
      } else {
        // 向上滚动
        this.isHeaderVisible = true
      }
      
      this.lastScrollTop = currentScrollTop
    },
  },
  mounted() {
    // 添加滚动事件监听
    window.addEventListener('scroll', this.handleScroll)
    
    // 保留原有的事件监听
    document.addEventListener('click', (e) => {
      const userSection = this.$el.querySelector('.user-section')
      if (userSection && !userSection.contains(e.target)) {
        this.showDropdown = false
      }
    })
  },
  beforeDestroy() {
    // 移除滚动事件监听
    window.removeEventListener('scroll', this.handleScroll)
    document.removeEventListener('click', this.closeDropdown)
  }
}
</script>

<style lang="scss" scoped>
.site-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: rgba(var(--surface-rgb), 0.65);
  backdrop-filter: blur(10px) saturate(180%);
  -webkit-backdrop-filter: blur(10px) saturate(180%);
  border-bottom: 1px solid rgba(var(--border-color-rgb), 0.08);
  transition: all 0.3s ease;
  transform: translateY(0);

  &.header-hidden {
    transform: translateY(-100%);
    box-shadow: none;
  }

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(
      to bottom,
      rgba(var(--surface-rgb), 0.05),
      rgba(var(--surface-rgb), 0)
    );
    pointer-events: none;
  }
}

.navbar {
  padding: $spacing-sm $spacing-xl;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  height: 64px;
  position: relative;
  gap: 18px;
}

.nav-left {
  margin-right: 20px;
  .logo {
    display: flex;
    align-items: center;
    text-decoration: none;
    gap: $spacing-sm;

    img {
      height: 38px;
      width: 40px;
      border-radius: 5px;
      object-fit: contain;
      background: rgba(255, 255, 255, 0.92);
      padding: 2px;
      box-sizing: border-box;
    }

    .logo-text {
      font-size: 1.2em;
      font-weight: 700;
      background: linear-gradient(135deg, $primary, $secondary);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      max-width: 200px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    @media screen and (max-width: 1400px) {
      .logo-text {
        max-width: 160px;
        font-size: 1.1em;
      }
    }

    @media screen and (max-width: 1200px) {
      .logo-text {
        max-width: 140px;
        font-size: 1em;
      }
    }

    @media screen and (max-width: 1000px) {
      .logo-text {
        max-width: 120px;
        font-size: 0.9em;
      }
    }
  }
}

.nav-center {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1 1 auto;
  min-width: 0;
  padding: 6px 10px;
  margin-right: 8px;
  border: 1px solid rgba($primary, 0.07);
  border-radius: 999px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.8), rgba(255, 255, 255, 0.56)),
    rgba($primary, 0.035);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.72),
    0 14px 28px rgba(15, 23, 42, 0.06);
  flex-wrap: nowrap;
  overflow: visible;

  .nav-item {
    position: relative;
    white-space: nowrap;
    flex: 1 1 0;
    min-width: 0;

    &:hover {
      z-index: 6;

      .dropdown-menu {
        opacity: 1;
        visibility: visible;
        transform: translateX(-50%) translateY(0);
        pointer-events: auto;
      }

      .nav-link .dropdown-icon {
        transform: rotate(180deg);
      }
    }
  }

  .nav-link {
    --dock-scale: 1;
    --dock-lift: 0px;
    --dock-shift: 0px;
    --dock-glow-opacity: 0;
    --dock-label-opacity: 0.9;
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    min-height: 42px;
    padding: 0 16px;
    border-radius: 999px;
    color: var(--text-secondary);
    text-decoration: none;
    font-size: 0.92em;
    font-weight: 600;
    letter-spacing: 0.01em;
    transform: translate3d(var(--dock-shift), var(--dock-lift), 0) scale3d(var(--dock-scale), var(--dock-scale), 1);
    transform-origin: center bottom;
    transition: transform 0.22s cubic-bezier(0.22, 1, 0.36, 1), color 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease, background 0.24s ease;
    border: 1px solid transparent;
    background: rgba(255, 255, 255, 0.24);
    box-shadow: 0 7px 16px rgba(15, 23, 42, 0.04);
    backdrop-filter: blur(8px);
    -webkit-backdrop-filter: blur(8px);
    overflow: visible;
    will-change: transform;
    backface-visibility: hidden;
  }

  .nav-link i {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    line-height: 1;
    transition: transform 0.22s ease;
    font-size: 1.08em;
    flex-shrink: 0;
  }

  .nav-link:hover,
  .nav-link.active {
    color: $primary;
    border-color: rgba($primary, 0.12);
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba($primary, 0.07));
    box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
  }

  .nav-link.has-dropdown {
    .dropdown-icon {
      font-size: 0.72em;
      margin-left: 2px;
      line-height: 1;
      transition: transform 0.26s ease;
    }
  }

  .nav-link.home-link i { color: #4CAF50; }
  .nav-link.archive-link i { color: #9C27B0; }
  .nav-link.clock-link i { color: #00BCD4; }
  .nav-link.category-link i { color: #FF9800; }
  .nav-link.tag-link i { color: #E91E63; }
  .nav-link.talk-link i { color: #2196F3; }
  .nav-link.code-link i { color: #607D8B; }
  .nav-link.hot-link i { color: #F44336; }
  .nav-link.photos-link i { color: #9b36f4; }
  .nav-link.message-link i { color: #009688; }
  .nav-link.friend-link i { color: #3F51B5; }
  .nav-link.about-link i { color: #795548; }
  .nav-link.about-me-link i { color: #8BC34A; }
  .nav-link.github-link i { color: #333333; }
  .nav-link.contact-link i { color: #0f766e; }
  .nav-link.changelog-link i { color: #673AB7; }
  .nav-link.resource-link i { color: #009688; }
  .nav-link.ai-link i { color: #2563eb; }

  .nav-link:hover i,
  .nav-link.active i {
    transform: translateY(-1px) scale(1.06);
  }

  .nav-link__content {
    position: relative;
    z-index: 1;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 9px;
    width: 100%;
    opacity: var(--dock-label-opacity);
  }

  .nav-link__glow {
    position: absolute;
    inset: 4px;
    border-radius: inherit;
    background: radial-gradient(circle at center top, rgba($primary, 0.16), transparent 72%);
    opacity: var(--dock-glow-opacity);
    transition: opacity 0.24s ease;
    pointer-events: none;
  }

  @media screen and (max-width: 1400px) {
    gap: 8px;
    padding: 6px 8px;

    .nav-link {
      padding: 0 13px;
      font-size: 0.84em;
    }
  }

  @media screen and (max-width: 1200px) {
    gap: 6px;
    margin-right: 6px;

    .nav-link {
      padding: 0 11px;
      min-height: 39px;
      font-size: 0.78em;

      i {
        font-size: 0.98em;
      }
    }
  }

  @media screen and (max-width: 1000px) {
    .nav-link {
      padding: 0 10px;
      font-size: 0.74em;

      i {
        display: none;
      }

      .dropdown-icon {
        display: none;
      }
    }

    .nav-link__content {
      gap: 5px;
    }
  }
}

.nav-right {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  margin-left: auto;
  position: relative;
  right: 0px;
  
  .search-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    background: var(--hover-bg);
    border-radius: 20px;
    color: var(--text-secondary);
    text-decoration: none;
    transition: all 0.3s ease;
    border: 1px solid var(--border-color);
    
    i {
      font-size: 0.9em;
      transition: transform 0.3s ease;
    }
    
    .search-text {
      font-size: 0.9em;
      font-weight: 500;
    }
    
    &:hover {
      background: var(--surface);
      color: $primary;
      border-color: $primary;
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba($primary, 0.1);
      
      i {
        transform: scale(1.1);
      }
    }
  }

  .message-btn {
    position: relative;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    padding: 0;
    color: var(--text-secondary);
    text-decoration: none;
    transition: all 0.3s ease;
    border-radius: 50%;
    box-sizing: border-box;

    i {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      line-height: 1;
      font-size: 1.2em;
      transition: transform 0.3s ease;
    }
    
    &:hover {
      color: $primary;
      background: var(--hover-bg);
      
      i {
        transform: scale(1.1);
      }
    }
    
    .message-count {
      position: absolute;
      top: 2px;
      right: 2px;
      background: red;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      transform: none;
      padding: 0;
    }
  }

  .user-info {
    .user-section {
      position: relative;

      &::after {
        content: '';
        position: absolute;
        top: 100%;
        left: 0;
        width: 100%;
        height: 8px;
        background: transparent;
      }
    }

    .avatar {
      width: 40px;
      height: 40px;
      cursor: pointer;
      border-radius: 50%;
      overflow: hidden;
      transition: transform 0.2s;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 2px solid $primary;

      &:hover {
        transform: scale(1.05);
        background: var(--hover-bg);
      }

      .avatar-icon {
        width: 100%;
        height: 100%;

        :deep(img) {
          object-fit: contain;
          background: rgba(255, 255, 255, 0.92);
        }
      }
    }

    .user-dropdown {
      position: absolute;
      top: calc(100% + 8px);
      right: 0;
      width: 240px;
      background: var(--surface);
      border-radius: 12px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
      overflow: hidden;
      z-index: 1000;
      animation: slideDown 0.2s ease;

      .dropdown-header {
        padding: 20px;
        display: flex;
        align-items: center;
        gap: 12px;
        background: linear-gradient(135deg, $primary, $secondary);
        color: #fff;

        img {
          width: 48px;
          height: 48px;
          border-radius: 50%;
          border: 2px solid rgba(255, 255, 255, 0.8);
          object-fit: contain;
          background: rgba(255, 255, 255, 0.92);
        }

        .user-details {
          .username {
            display: block;
            font-weight: 500;
            font-size: 16px;
          }

          .role {
            display: block;
            font-size: 13px;
            opacity: 0.8;
            margin-top: 2px;
          }
        }
      }

      .dropdown-divider {
        height: 1px;
        background: var(--border-color);
        margin: 8px 0;
      }

      .dropdown-item {
        padding: 12px 20px;
        display: flex;
        align-items: center;
        gap: 12px;
        color: var(--text-primary);
        text-decoration: none;
        transition: all 0.2s;
        cursor: pointer;

        i {
          font-size: 16px;
          opacity: 0.8;
        }

        &:hover {
          background: var(--hover-bg);
          color: $primary;

          i {
            opacity: 1;
          }
        }
      }
    }
  }
}

.menu-btn,
.mobile-search-btn {
  display: none;
  background: none;
  border: none;
  padding: $spacing-sm;
  font-size: 1.2em;
  color: $text-secondary;
  cursor: pointer;
  transition: color 0.3s ease;

  &:hover {
    color: $primary;
  }
}

.mobile-search {
  position: fixed;
  top: 0;
  bottom: 0;
  background: white;
  width: 80%;
  max-width: 360px;
  z-index: 1100;
  transform: translateX(-100%);
  transition: transform 0.3s ease;
  overflow-y: auto;

  &.active {
    transform: translateX(0);
  }
}


@include responsive(lg) {
  .nav-center {
    display: none;
  }

  .nav-right .search-btn {
    width: 160px;
  }
}

@include responsive(md) {
  .navbar {
    padding: $spacing-sm $spacing-md;
  }

  .menu-btn,
  .mobile-search-btn {
    display: block;
  }

  .nav-center,
  .nav-right .search-btn {
    display: none;
  }

  .nav-left {
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
    margin-right: 0;
    
    .logo {
      img {
        height: 40px;
        width: 42px;
      }
      
      .logo-text {
        max-width: 160px;
        font-size: 1em;
        display: block;
      }
    }
  }

  .nav-right .user-info {
    display: none;
  }

  .nav-right .message-btn {
    display: none;
  }
}

@media screen and (max-width: 480px) {
  .nav-left .logo .logo-text {
    max-width: 120px;
    font-size: 1.3em;
  }
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%) translateY(15px);
  background: var(--surface);
  border-radius: $border-radius-md;
  box-shadow: $shadow-lg;
  width: max-content;
  min-width: 120px;
  opacity: 0;
  visibility: hidden;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  pointer-events: none;
  padding: $spacing-xs 0;

  &::before {
    content: '';
    position: absolute;
    top: -8px;
    left: 0;
    width: 100%;
    height: 8px;
    background: transparent;
  }
  
  .dropdown-item {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    padding: 8px $spacing-md;
    height: 36px;
    color: var(--text-secondary);
    text-decoration: none;
    transition: all 0.3s ease;
    white-space: nowrap;
    font-size: 0.9em;
    i {
      width: 16px;
      text-align: center;
      font-size: 0.9em;
      color: var(--text-secondary);
      margin-right: 8px;
    }

    &:hover {
      color: $primary;
      background: var(--hover-bg);
      
      i {
        color: $primary;
      }
    }
  }

  &.active {
    opacity: 1;
    visibility: visible;
    transform: translateX(-50%) translateY(0);
    pointer-events: auto;
  }

  @keyframes dropdownEnter {
    from {
      opacity: 0;
      transform: translateX(-50%) translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateX(-50%) translateY(0);
    }
  }

  &.active {
    animation: dropdownEnter 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 色模式适配 */
:root[data-theme='dark'] {
  .site-header {
    background: rgba(var(--surface-rgb), 0.75);
    backdrop-filter: blur(10px) saturate(160%);
    -webkit-backdrop-filter: blur(10px) saturate(160%);
    border-bottom-color: rgba(var(--border-color-rgb), 0.15);
  }

  .user-dropdown {
    background: var(--surface);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);

    .dropdown-item {
      color: var(--text-primary);

      &:hover {
        background: var(--hover-bg);
      }
    }
  }
}

/* 深色模式下的图标颜色整 */
:root[data-theme='dark'] {
  .nav-link {
    &.home-link i { color: #81C784; }
    &.archive-link i { color: #CE93D8; }
    &.clock-link i { color: #4DD0E1; }
    &.category-link i { color: #FFB74D; }
    &.tag-link i { color: #F06292; }
    &.talk-link i { color: #64B5F6; }
    &.code-link i { color: #90A4AE; }
    &.hot-link i { color: #EF5350; }
    &.message-link i { color: #4DB6AC; }
    &.friend-link i { color: #7986CB; }
    &.about-link i { color: #A1887F; }
    &.about-me-link i { color: #AED581; }
    &.github-link i { color: #FFFFFF; }
    &.contact-link i { color: #5eead4; }
    &.changelog-link i { color: #9575CD; }
    &.ai-link i { color: #60a5fa; }
  }
}

/* 添加图标悬浮动画 */
@keyframes iconFloat {
  0% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-2px);
  }
  100% {
    transform: translateY(0);
  }
}

.nav-link:hover i,
.nav-link.active i {
  animation: iconFloat 0.5s ease-in-out;
}

.nav-link {
  font-family: 'Poppins', sans-serif;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.logo-text {
  font-family: 'Poppins', sans-serif;
  font-weight: 600;
  letter-spacing: -0.5px;
}

</style>
