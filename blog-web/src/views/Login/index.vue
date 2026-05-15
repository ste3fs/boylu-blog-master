<template>
  <div class="login-page" data-build="20260411-2">
    <div class="login-shell">
      <section class="brand-panel">
        <div class="brand-glow glow-a"></div>
        <div class="brand-glow glow-b"></div>
        <div class="brand-grid"></div>

        <div class="brand-top">
          <div class="brand-badge">
            <span class="brand-mark">B</span>
            <div>
              <strong>{{ brandName }}</strong>
              <p>{{ brandSummary }}</p>
            </div>
          </div>
        </div>

        <div class="brand-main">
          <div class="character-stage">
            <div class="scene-orb orb-a"></div>
            <div class="scene-orb orb-b"></div>
            <div class="scene-wave wave-a"></div>
            <div class="scene-wave wave-b"></div>
            <div
              ref="purpleRef"
              class="character purple-character"
              :style="purpleCharacterStyle"
            >
              <div class="eye-group purple-eyes" :style="purpleEyesStyle">
                <span class="eye-ball" :class="{ blinking: isPurpleBlinking }">
                  <span class="eye-pupil" :style="purplePupilStyle"></span>
                </span>
                <span class="eye-ball" :class="{ blinking: isPurpleBlinking }">
                  <span class="eye-pupil" :style="purplePupilStyle"></span>
                </span>
              </div>
            </div>

            <div
              ref="blackRef"
              class="character black-character"
              :style="blackCharacterStyle"
            >
              <div class="eye-group black-eyes" :style="blackEyesStyle">
                <span class="eye-ball eye-ball--small" :class="{ blinking: isBlackBlinking }">
                  <span class="eye-pupil eye-pupil--small" :style="blackPupilStyle"></span>
                </span>
                <span class="eye-ball eye-ball--small" :class="{ blinking: isBlackBlinking }">
                  <span class="eye-pupil eye-pupil--small" :style="blackPupilStyle"></span>
                </span>
              </div>
            </div>

            <div
              ref="orangeRef"
              class="character orange-character"
              :style="orangeCharacterStyle"
            >
              <div class="eye-group orange-eyes" :style="orangeEyesStyle">
                <span class="eye-ball eye-ball--dot-shell">
                  <span class="eye-pupil eye-pupil--dot" :style="orangePupilStyle"></span>
                </span>
                <span class="eye-ball eye-ball--dot-shell">
                  <span class="eye-pupil eye-pupil--dot" :style="orangePupilStyle"></span>
                </span>
              </div>
            </div>

            <div
              ref="yellowRef"
              class="character yellow-character"
              :style="yellowCharacterStyle"
            >
              <div class="eye-group yellow-eyes" :style="yellowEyesStyle">
                <span class="eye-ball eye-ball--dot-shell">
                  <span class="eye-pupil eye-pupil--dot" :style="yellowPupilStyle"></span>
                </span>
                <span class="eye-ball eye-ball--dot-shell">
                  <span class="eye-pupil eye-pupil--dot" :style="yellowPupilStyle"></span>
                </span>
              </div>
              <span class="yellow-mouth" :style="yellowMouthStyle"></span>
            </div>
          </div>
        </div>

        <div class="brand-footer">
          <a href="javascript:void(0)" @click="backToHome">返回首页</a>
          <a href="javascript:void(0)" @click="$router.push('/resources')">资源库</a>
          <a href="javascript:void(0)" @click="$router.push('/about')">关于我</a>
        </div>
      </section>

      <section class="form-panel">
        <div class="panel-frame">
          <div class="mobile-brand">
            <span class="brand-mark brand-mark--light">B</span>
            <strong>{{ brandName }}</strong>
          </div>

          <div class="panel-toolbar">
            <button class="icon-btn" type="button" @click="handleClose">
              <i class="el-icon-back"></i>
            </button>
          </div>

          <div class="panel-header">
            <span class="panel-kicker">{{ panelKicker }}</span>
            <h2>{{ panelTitle }}</h2>
            <p>{{ panelSubtitle }}</p>
          </div>

          <div class="entry-switch">
            <button
              type="button"
              class="entry-chip"
              :class="{ active: currentForm === 'account' }"
              @click="switchForm('account')"
            >
              账号登录
            </button>
            <button
              type="button"
              class="entry-chip"
              :class="{ active: currentForm === 'wechat' }"
              @click="switchForm('wechat')"
            >
              微信扫码
            </button>
          </div>

          <div v-show="currentForm === 'account'" class="form-surface">
            <el-form :model="loginForm" :rules="rules" ref="ruleFrom">
              <el-form-item class="form-item" prop="username">
                <label class="field-label">账号</label>
                <el-input
                  prefix-icon="el-icon-user-solid"
                  v-model="loginForm.username"
                  placeholder="请输入用户名"
                  autocomplete="off"
                  @focus="setTypingState(true)"
                  @blur="setTypingState(false)"
                  @keyup.enter.native="handleLogin"
                />
              </el-form-item>

              <el-form-item class="form-item" prop="password">
                <label class="field-label">密码</label>
                <el-input
                  prefix-icon="el-icon-lock"
                  :type="showPassword ? 'text' : 'password'"
                  v-model="loginForm.password"
                  placeholder="请输入密码"
                  autocomplete="off"
                  @focus="setTypingState(true)"
                  @blur="setTypingState(false)"
                  @keyup.enter.native="handleLogin"
                >
                  <i
                    slot="suffix"
                    :class="showPassword ? 'fas fa-eye-slash' : 'fas fa-eye'"
                    class="password-toggle"
                    @click.stop="togglePassword"
                  ></i>
                </el-input>
              </el-form-item>

              <div class="option-row">
                <el-checkbox v-model="rememberMe">30 天内记住我</el-checkbox>
                <a href="javascript:void(0)" class="inline-link" @click="switchForm('forgot')">
                  忘记密码？
                </a>
              </div>

              <el-form-item class="form-item form-item--submit">
                <el-button
                  class="submit-btn"
                  :loading="loading"
                  @click="handleLogin"
                  type="primary"
                >
                  {{ loading ? '登录中...' : '登录' }}
                </el-button>
              </el-form-item>
            </el-form>

            <div class="panel-divider">
              <span>其他登录方式</span>
            </div>

            <div class="third-party-grid">
              <button
                v-for="(item, type) in loginTypes"
                :key="type"
                v-if="type !== 'wechat'"
                type="button"
                class="social-login-btn"
                @click="handleThirdPartyLogin(type)"
              >
                <i :class="item.icon"></i>
                <span>{{ item.title }}</span>
              </button>
            </div>

            <div class="surface-footer">
              <span>还没有账号？</span>
              <a href="javascript:void(0)" @click="switchForm('register')">立即注册</a>
            </div>
          </div>

          <div v-show="currentForm === 'register'" class="form-surface">
            <el-form :model="registerForm" :rules="rules" ref="registerForm">
              <el-form-item class="form-item" prop="nickname">
                <label class="field-label">昵称</label>
                <el-input
                  prefix-icon="el-icon-user-solid"
                  v-model="registerForm.nickname"
                  placeholder="请输入昵称"
                />
              </el-form-item>

              <el-form-item class="form-item" prop="email">
                <label class="field-label">邮箱</label>
                <el-input
                  prefix-icon="el-icon-message"
                  v-model="registerForm.email"
                  placeholder="请输入邮箱"
                />
              </el-form-item>

              <el-form-item class="form-item" prop="code">
                <label class="field-label">验证码</label>
                <el-input
                  prefix-icon="el-icon-key"
                  v-model="registerForm.code"
                  placeholder="请输入验证码"
                >
                  <template slot="append">
                    <el-button @click="sendRegisterCode" :disabled="codeSending">
                      {{ codeButtonText }}
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item class="form-item" prop="password">
                <label class="field-label">密码</label>
                <el-input
                  prefix-icon="el-icon-lock"
                  v-model="registerForm.password"
                  placeholder="请输入密码"
                  show-password
                />
              </el-form-item>

              <el-form-item class="form-item form-item--submit">
                <el-button class="submit-btn" :loading="loading" @click="handleRegister">
                  {{ loading ? '注册中...' : '注册' }}
                </el-button>
              </el-form-item>
            </el-form>

            <div class="surface-footer">
              <span>已有账号？</span>
              <a href="javascript:void(0)" @click="switchForm('account')">立即登录</a>
            </div>
          </div>

          <div v-show="currentForm === 'forgot'" class="form-surface">
            <el-form :model="forgotForm" :rules="rules" ref="forgotForm">
              <el-form-item class="form-item" prop="email">
                <label class="field-label">邮箱</label>
                <el-input
                  prefix-icon="el-icon-message"
                  v-model="forgotForm.email"
                  placeholder="请输入注册邮箱"
                />
              </el-form-item>

              <el-form-item class="form-item" prop="code">
                <label class="field-label">验证码</label>
                <el-input
                  prefix-icon="el-icon-key"
                  v-model="forgotForm.code"
                  placeholder="请输入验证码"
                >
                  <template slot="append">
                    <el-button @click="sendVerificationCode" :disabled="codeSending">
                      {{ codeButtonText }}
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item class="form-item" prop="password">
                <label class="field-label">新密码</label>
                <el-input
                  prefix-icon="el-icon-lock"
                  v-model="forgotForm.password"
                  placeholder="请输入新密码"
                  show-password
                />
              </el-form-item>

              <el-form-item class="form-item form-item--submit">
                <el-button class="submit-btn" :loading="loading" @click="handleResetPassword">
                  {{ loading ? '提交中...' : '重置密码' }}
                </el-button>
              </el-form-item>
            </el-form>

            <div class="surface-footer">
              <a href="javascript:void(0)" @click="switchForm('account')">返回登录</a>
            </div>
          </div>

          <div v-show="currentForm === 'wechat'" class="form-surface form-surface--wechat">
            <div class="wechat-panel">
              <div class="wechat-qr-box">
                <img :src="wechatQrUrl" alt="微信公众号二维码" @error="handleWechatQrError" />
              </div>

              <p class="wechat-code-line">
                当前登录码
                <span class="wechat-code">{{ wechatForm.code || "正在获取..." }}</span>
                <button
                  type="button"
                  class="code-refresh"
                  :disabled="wechatForm.refreshCountdown > 0"
                  @click="getWechatLoginCode"
                >
                  {{ wechatRefreshText }}
                </button>
              </p>

              <ol class="wechat-guide">
                <li>使用微信扫描上方公众号二维码，未关注时先完成关注。</li>
                <li>进入公众号聊天窗口，发送页面上的登录码，例如 <strong>DL1234</strong>。</li>
                <li>页面会自动轮询登录状态，成功后自动进入当前站点。</li>
                <li>登录码服务器侧 5 分钟内有效，60 秒后可主动刷新生成新码。</li>
              </ol>

              <div class="wechat-helper">
                <span>如果扫码后没有跳转，先确认发送的是页面上当前显示的登录码。</span>
                <button
                  type="button"
                  class="helper-btn"
                  :disabled="wechatForm.refreshCountdown > 0"
                  @click="getWechatLoginCode"
                >
                  {{ wechatForm.refreshCountdown > 0 ? wechatRefreshText : "刷新登录码" }}
                </button>
              </div>

              <div class="surface-footer">
                <span>想用账号密码？</span>
                <a href="javascript:void(0)" @click="switchForm('account')">返回账号登录</a>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <el-dialog
      title="请拖动滑块完成验证"
      width="360px"
      :visible.sync="isShowSliderVerify"
      :close-on-click-modal="false"
      @close="refresh"
      append-to-body
    >
      <slider-verify
        ref="sliderVerify"
        @success="onSuccess"
        @fail="onFail"
        @again="onAgain"
      />
    </el-dialog>
  </div>
</template>

<script>
import Checkbox from 'element-ui/lib/checkbox'
import 'element-ui/lib/theme-chalk/checkbox.css'
import { disableScroll, enableScroll } from "@/utils/scroll";
import {
  sendEmailCodeApi,
  registerApi,
  forgotPasswordApi,
  getWechatLoginCodeApi,
  getWechatIsLoginApi,
  getAuthRenderApi,
  getCaptchaSwitchApi,
} from "@/api/auth";
import { setCookie, getCookie, removeCookie, setCookieExpires } from "@/utils/cookie";
import { copyText as copyToClipboard } from "@/utils/contact";
import SliderVerify from "./components/SliderVerify.vue";

const TRUSTED_LOGIN_COOKIE = "trusted_login_verified";

export default {
  name: "Login",
  components: {
    ElCheckbox: Checkbox,
    SliderVerify,
  },
  data() {
    return {
      currentForm: "account",
      loading: false,
      scanPlaceholderUrl: new URL("../../assets/scan-placeholder.svg", import.meta.url).href,
      wechatQrUrl: "/boylu-wechat-official-qrcode.jpg",
      wechatForm: {
        code: "",
        showQrcode: false,
        refreshCountdown: 0,
        expireCountdown: 0,
      },
      countdown: 0,
      loginForm: {
        username: "",
        password: "",
        source: "PC",
        nonceStr: "",
        value: "",
      },
      registerForm: {
        nickname: "",
        email: "",
        password: "",
        code: "",
      },
      forgotForm: {
        email: "",
        code: "",
        password: "",
      },
      loginTypes: {
        github: {
          title: "GitHub账号登录",
          icon: "fab fa-github",
        },
        qq: {
          title: "QQ账号登录",
          icon: "fab fa-qq",
        },
        gitee: {
          title: "码云账号登录",
          icon: "fab fa-git-alt",
        },
        weibo: {
          title: "微博账号登录",
          icon: "fab fa-weibo",
        },
      },
      codeSending: false,
      codeButtonText: "发送验证码",
      codeTimer: null,
      pollingTimer: null,
      isShowSliderVerify: false,
      sliderVerify: null,
      rules: {
        nickname: [
          { required: true, message: "请输入昵称", trigger: "blur" },
          {
            min: 3,
            max: 10,
            message: "长度在 3 到 10 个字符",
            trigger: "blur",
          },
        ],
        username: [
          { required: true, message: "请输入用户名", trigger: "blur" },
          {
            min: 3,
            max: 50,
            message: "长度在 3 到 50 个字符",
            trigger: "blur",
          },
        ],
        email: [
          { required: true, message: "请输入邮箱", trigger: "blur" },
          { type: "email", message: "请输入正确的邮箱", trigger: "blur" },
        ],
        password: [
          { required: true, message: "请输入密码", trigger: "blur" },
          {
            min: 6,
            max: 16,
            message: "长度在 6 到 16 个字符",
            trigger: "blur",
          },
        ],
        code: [{ required: true, message: "请输入验证码", trigger: "blur" }],
      },
      rememberMe: false,
      showPassword: false,
      prefersReducedMotion: false,
      sceneActive: false,
      mouseX: 0,
      mouseY: 0,
      isTyping: false,
      isLookingAtEachOther: false,
      isPurpleBlinking: false,
      isBlackBlinking: false,
      isPurplePeeking: false,
      mouseMoveHandler: null,
      resizeHandler: null,
      motionPreferenceQuery: null,
      motionPreferenceHandler: null,
      purpleBlinkTimer: null,
      purpleBlinkResetTimer: null,
      blackBlinkTimer: null,
      blackBlinkResetTimer: null,
      purplePeekTimer: null,
      purplePeekResetTimer: null,
      typingLookTimer: null,
    };
  },
  computed: {
    brandName() {
      return this.$store.state.webSiteInfo?.name || "Boylu Blog";
    },
    brandSummary() {
      return this.$store.state.webSiteInfo?.authorInfo || "登录到你的站点中心";
    },
    shouldReduceMotion() {
      return this.prefersReducedMotion || (typeof window !== "undefined" && window.innerWidth < 1180);
    },
    panelKicker() {
      const map = {
        account: "ACCOUNT LOGIN",
        wechat: "WECHAT LOGIN",
        register: "REGISTER",
        forgot: "RESET PASSWORD",
      };
      return map[this.currentForm] || "ACCOUNT CENTER";
    },
    panelTitle() {
      const map = {
        account: "账号密码登录",
        wechat: "微信扫码登录",
        register: "创建你的账号",
        forgot: "找回账号密码",
      };
      return map[this.currentForm] || "欢迎回来";
    },
    panelSubtitle() {
      const map = {
        account: "继续进入站点，处理你的文章、评论和 AI 对话。",
        wechat: "扫码关注公众号后发送登录码，5 分钟内有效，60 秒后可刷新。",
        register: "注册后即可解锁站点完整能力。",
        forgot: "通过邮箱验证码快速重置密码。",
      };
      return map[this.currentForm] || "请输入你的账号信息。";
    },
    wechatRefreshText() {
      if (this.wechatForm.refreshCountdown > 0) {
        return `${this.wechatForm.refreshCountdown}s 后可刷新`;
      }
      return "重新获取";
    },
    isPasswordVisibleMode() {
      return this.currentForm === "account" && !!this.loginForm.password && this.showPassword;
    },
    isPasswordTypingMode() {
      return this.currentForm === "account" && !!this.loginForm.password && !this.showPassword;
    },
    purpleMetrics() {
      return this.getCharacterMetrics("purpleRef");
    },
    blackMetrics() {
      return this.getCharacterMetrics("blackRef");
    },
    orangeMetrics() {
      return this.getCharacterMetrics("orangeRef");
    },
    yellowMetrics() {
      return this.getCharacterMetrics("yellowRef");
    },
    purpleLookOffset() {
      if (this.isPasswordVisibleMode) {
        return {
          x: this.isPurplePeeking ? 4 : -4,
          y: this.isPurplePeeking ? 5 : -4,
        };
      }
      if (this.isLookingAtEachOther) {
        return { x: 3, y: 4 };
      }
      return this.getLookOffset("purpleRef", 5, 5);
    },
    blackLookOffset() {
      if (this.isPasswordVisibleMode) {
        return { x: -4, y: -4 };
      }
      if (this.isLookingAtEachOther) {
        return { x: 0, y: -4 };
      }
      return this.getLookOffset("blackRef", 4, 4);
    },
    orangeLookOffset() {
      if (this.isPasswordVisibleMode) {
        return { x: -4, y: -3 };
      }
      return this.getLookOffset("orangeRef", 4, 4);
    },
    yellowLookOffset() {
      if (this.isPasswordVisibleMode) {
        return { x: -4, y: -3 };
      }
      return this.getLookOffset("yellowRef", 4, 4);
    },
    purpleCharacterStyle() {
      let transform = `skewX(${this.purpleMetrics.bodySkew}deg)`;

      if (this.isPasswordVisibleMode) {
        transform = "skewX(0deg)";
      } else if (this.isTyping || this.isPasswordTypingMode) {
        transform = `skewX(${this.purpleMetrics.bodySkew - 12}deg) translateX(40px)`;
      }

      return { transform };
    },
    blackCharacterStyle() {
      let skew = this.blackMetrics.bodySkew;
      let transform = `skewX(${skew}deg)`;

      if (this.isPasswordVisibleMode) {
        transform = "skewX(0deg)";
      } else if (this.isLookingAtEachOther) {
        transform = `skewX(${skew * 1.5 + 10}deg) translateX(20px)`;
      } else if (this.isTyping || this.isPasswordTypingMode) {
        transform = `skewX(${skew * 1.5}deg)`;
      }

      return { transform };
    },
    orangeCharacterStyle() {
      return {
        transform: this.isPasswordVisibleMode
          ? "skewX(0deg)"
          : `skewX(${this.orangeMetrics.bodySkew}deg)`,
      };
    },
    yellowCharacterStyle() {
      return {
        transform: this.isPasswordVisibleMode
          ? "skewX(0deg)"
          : `skewX(${this.yellowMetrics.bodySkew}deg)`,
      };
    },
    purpleEyesStyle() {
      if (this.isPasswordVisibleMode) {
        return { left: "20px", top: "35px" };
      }
      if (this.isLookingAtEachOther) {
        return { left: "55px", top: "65px" };
      }
      return {
        left: `${45 + this.purpleMetrics.faceX}px`,
        top: `${40 + this.purpleMetrics.faceY}px`,
      };
    },
    blackEyesStyle() {
      if (this.isPasswordVisibleMode) {
        return { left: "10px", top: "28px" };
      }
      if (this.isLookingAtEachOther) {
        return { left: "32px", top: "12px" };
      }
      return {
        left: `${26 + this.blackMetrics.faceX}px`,
        top: `${32 + this.blackMetrics.faceY}px`,
      };
    },
    orangeEyesStyle() {
      return {
        left: `${this.isPasswordVisibleMode ? 78 : 78}px`,
        top: `${this.isPasswordVisibleMode ? 82 : 86}px`,
      };
    },
    yellowEyesStyle() {
      return {
        left: `${this.isPasswordVisibleMode ? 44 : 44}px`,
        top: `${this.isPasswordVisibleMode ? 34 : 34}px`,
      };
    },

    yellowMouthStyle() {
      return {
        left: `${this.isPasswordVisibleMode ? 10 : 40 + this.yellowMetrics.faceX}px`,
        top: `${this.isPasswordVisibleMode ? 88 : 88 + this.yellowMetrics.faceY}px`,
      };
    },
    purplePupilStyle() {
      return {
        transform: `translate(${this.purpleLookOffset.x}px, ${this.purpleLookOffset.y}px)`,
      };
    },
    blackPupilStyle() {
      return {
        transform: `translate(${this.blackLookOffset.x}px, ${this.blackLookOffset.y}px)`,
      };
    },
    orangePupilStyle() {
      return {
        transform: `translate(${this.orangeLookOffset.x}px, ${this.orangeLookOffset.y}px)`,
      };
    },
    yellowPupilStyle() {
      return {
        transform: `translate(${this.yellowLookOffset.x}px, ${this.yellowLookOffset.y}px)`,
      };
    },
  },
  watch: {
    shouldReduceMotion(value) {
      if (value) {
        this.stopCharacterScene();
      } else {
        this.$nextTick(() => {
          this.initCharacterScene();
        });
      }
    },
    currentForm() {
      this.showPassword = false;
      this.setTypingState(false);
      this.syncPurplePeek();
    },
    showPassword() {
      this.syncPurplePeek();
    },
    "loginForm.password"() {
      this.syncPurplePeek();
    },
    rememberMe(value) {
      if (!value) {
        this.persistRememberMe();
      }
    },
  },
  created() {
    this.handleOauthLoginResult();
    const rememberedUsername = getCookie("remember_username");
    const rememberedEnabled = getCookie("remember_me") === "1";
    removeCookie("remember_password");

    if (rememberedEnabled && rememberedUsername) {
      this.rememberMe = true;
      this.loginForm.username = rememberedUsername;
    }

    Object.keys(this.loginTypes).forEach((key) => {
      if (!this.$store.state.webSiteInfo?.loginTypeList?.includes(key)) {
        delete this.loginTypes[key];
      }
    });
    if (this.currentForm === "wechat") {
      this.getWechatLoginCode();
    }
    this.$nextTick(() => {
      disableScroll();
    });
  },
  mounted() {
    this.setupMotionPreference();
    this.initCharacterScene();
  },
  methods: {
    handleOauthLoginResult() {
      const { oauth, source, message, ...restQuery } = this.$route.query || {};
      if (!oauth) {
        return;
      }

      if (oauth === "cancelled") {
        this.$message.warning(`已取消${source || "第三方"}登录`);
      } else if (oauth === "disabled") {
        this.$message.error(message || "账号已被禁用");
      } else if (oauth === "failed") {
        this.$message.error(message || "第三方登录失败");
      }

      this.$nextTick(() => {
        this.$router.replace({
          path: this.$route.path,
          query: restQuery,
        }).catch(() => {});
      });
    },
    persistRememberMe() {
      if (this.rememberMe && this.loginForm.username) {
        setCookieExpires("remember_me", "1", 30);
        setCookieExpires("remember_username", this.loginForm.username, 30);
        return;
      }

      removeCookie("remember_me");
      removeCookie("remember_username");
      removeCookie("remember_password");
    },
    shouldSkipSliderVerify() {
      return getCookie(TRUSTED_LOGIN_COOKIE) === "1";
    },
    isCaptchaRequiredError(error) {
      return (error?.message || "").includes("SLIDER_VERIFY_REQUIRED");
    },
    setupMotionPreference() {
      if (typeof window === "undefined" || typeof window.matchMedia !== "function") {
        return;
      }

      this.motionPreferenceQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
      this.prefersReducedMotion = this.motionPreferenceQuery.matches;
      this.motionPreferenceHandler = (event) => {
        this.prefersReducedMotion = event.matches;
      };

      if (typeof this.motionPreferenceQuery.addEventListener === "function") {
        this.motionPreferenceQuery.addEventListener("change", this.motionPreferenceHandler);
      } else if (typeof this.motionPreferenceQuery.addListener === "function") {
        this.motionPreferenceQuery.addListener(this.motionPreferenceHandler);
      }

      this.resizeHandler = () => {
        if (this.shouldReduceMotion) {
          this.stopCharacterScene();
        } else {
          this.initCharacterScene();
        }
      };
      window.addEventListener("resize", this.resizeHandler, { passive: true });
    },
    initCharacterScene() {
      if (this.shouldReduceMotion || this.sceneActive) {
        return;
      }

      this.sceneActive = true;
      this.mouseMoveHandler = (event) => {
        this.mouseX = event.clientX;
        this.mouseY = event.clientY;
      };
      window.addEventListener("mousemove", this.mouseMoveHandler, { passive: true });
      this.scheduleBlink("purple");
      this.scheduleBlink("black");
      this.syncPurplePeek();
    },
    scheduleBlink(type) {
      const timerKey = type === "purple" ? "purpleBlinkTimer" : "blackBlinkTimer";
      const resetKey = type === "purple" ? "purpleBlinkResetTimer" : "blackBlinkResetTimer";
      const stateKey = type === "purple" ? "isPurpleBlinking" : "isBlackBlinking";

      this[timerKey] = window.setTimeout(() => {
        this[stateKey] = true;
        this[resetKey] = window.setTimeout(() => {
          this[stateKey] = false;
          this.scheduleBlink(type);
        }, 150);
      }, Math.random() * 4000 + 3000);
    },
    syncPurplePeek() {
      window.clearTimeout(this.purplePeekTimer);
      window.clearTimeout(this.purplePeekResetTimer);
      this.purplePeekTimer = null;
      this.purplePeekResetTimer = null;
      this.isPurplePeeking = false;

      if (!this.isPasswordVisibleMode) {
        return;
      }

      this.purplePeekTimer = window.setTimeout(() => {
        this.isPurplePeeking = true;
        this.purplePeekResetTimer = window.setTimeout(() => {
          this.isPurplePeeking = false;
          this.syncPurplePeek();
        }, 800);
      }, Math.random() * 3000 + 2000);
    },
    setTypingState(active) {
      this.isTyping = active && this.currentForm === "account";
      window.clearTimeout(this.typingLookTimer);

      if (this.isTyping) {
        this.isLookingAtEachOther = true;
        this.typingLookTimer = window.setTimeout(() => {
          this.isLookingAtEachOther = false;
        }, 800);
      } else {
        this.isLookingAtEachOther = false;
      }
    },
    togglePassword() {
      this.showPassword = !this.showPassword;
    },
    stopCharacterScene() {
      this.clearCharacterScene();
      this.sceneActive = false;
      this.mouseX = 0;
      this.mouseY = 0;
      this.isTyping = false;
      this.isLookingAtEachOther = false;
      this.isPurpleBlinking = false;
      this.isBlackBlinking = false;
      this.isPurplePeeking = false;
    },
    clamp(value, min, max) {
      return Math.max(min, Math.min(max, value));
    },
    getCharacterMetrics(refName) {
      const element = this.$refs[refName];
      if (!element || !element.getBoundingClientRect) {
        return { faceX: 0, faceY: 0, bodySkew: 0 };
      }

      const rect = element.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 3;
      const deltaX = this.mouseX - centerX;
      const deltaY = this.mouseY - centerY;

      return {
        faceX: this.clamp(deltaX / 20, -15, 15),
        faceY: this.clamp(deltaY / 30, -10, 10),
        bodySkew: this.clamp(-deltaX / 120, -6, 6),
      };
    },
    getLookOffset(refName, maxX, maxY) {
      const element = this.$refs[refName];
      if (!element || !element.getBoundingClientRect) {
        return { x: 0, y: 0 };
      }

      const rect = element.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 4;
      const deltaX = this.mouseX - centerX;
      const deltaY = this.mouseY - centerY;

      return {
        x: this.clamp(deltaX / 28, -maxX, maxX),
        y: this.clamp(deltaY / 34, -maxY, maxY),
      };
    },
    clearCharacterScene() {
      if (this.mouseMoveHandler) {
        window.removeEventListener("mousemove", this.mouseMoveHandler);
        this.mouseMoveHandler = null;
      }

      [
        "purpleBlinkTimer",
        "purpleBlinkResetTimer",
        "blackBlinkTimer",
        "blackBlinkResetTimer",
        "purplePeekTimer",
        "purplePeekResetTimer",
        "typingLookTimer",
      ].forEach((key) => {
        if (this[key]) {
          window.clearTimeout(this[key]);
          this[key] = null;
        }
      });
    },
    teardownMotionPreference() {
      if (typeof window === "undefined") {
        return;
      }

      if (this.resizeHandler) {
        window.removeEventListener("resize", this.resizeHandler);
        this.resizeHandler = null;
      }

      if (this.motionPreferenceQuery && this.motionPreferenceHandler) {
        if (typeof this.motionPreferenceQuery.removeEventListener === "function") {
          this.motionPreferenceQuery.removeEventListener("change", this.motionPreferenceHandler);
        } else if (typeof this.motionPreferenceQuery.removeListener === "function") {
          this.motionPreferenceQuery.removeListener(this.motionPreferenceHandler);
        }
      }

      this.motionPreferenceQuery = null;
      this.motionPreferenceHandler = null;
    },
    async onSuccess(captcha) {
      this.loginForm.nonceStr = captcha.nonceStr;
      this.loginForm.value = captcha.value;
      this.login();
    },
    async login() {
      this.loading = true;
      try {
        await this.$store.dispatch("loginAction", {
          loginData: this.loginForm,
          rememberMe: this.rememberMe,
        });
        this.persistRememberMe();
        setCookieExpires(TRUSTED_LOGIN_COOKIE, "1", 30);
        this.$refs.sliderVerify?.verifySuccessEvent();
        this.$message.success("登录成功");
        this.handleClose();
      } catch (error) {
        if (this.isCaptchaRequiredError(error)) {
          removeCookie(TRUSTED_LOGIN_COOKIE);
          this.isShowSliderVerify = true;
          return;
        }
        this.$message.error(error.message || "登录失败，请重试");
        this.refresh();
      } finally {
        this.loading = false;
        this.loginForm.nonceStr = "";
        this.loginForm.value = "";
      }
    },
    onFail() {
      this.$message.error("验证失败，请重试");
    },
    onAgain() {
      this.$message.error("验证失败，请重试");
    },
    refresh() {
      this.$refs.sliderVerify.refresh();
    },
    switchForm(form) {
      this.currentForm = form;
      this.loading = false;
      this.clearTimer();
      if (form === "wechat") {
        this.getWechatLoginCode();
      }
    },
    async handleLogin() {
      this.$refs.ruleFrom.validate(async (valid) => {
        if (valid) {
          try {
            const res = await getCaptchaSwitchApi();
            if (res.data && res.data.configValue === "N") {
              this.login();
              return;
            }

            if (this.shouldSkipSliderVerify()) {
              this.login();
            } else {
              this.isShowSliderVerify = true;
            }
          } catch (error) {
            this.$message.warning("验证码配置读取失败，已改用账号密码登录");
            this.login();
          }
        } else {
          return false;
        }
      });
    },
    async handleRegister() {
      this.$refs.registerForm.validate(async (valid) => {
        if (valid) {
          this.loading = true;
          try {
            await registerApi(this.registerForm);
            this.$message.success("注册成功");
            this.switchForm("account");
          } catch (error) {
            this.$message.error(error.message || "注册失败，请重试");
          } finally {
            this.loading = false;
          }
        } else {
          return false;
        }
      });
    },
    async handleResetPassword() {
      this.$refs.forgotForm.validate(async (valid) => {
        if (valid) {
          this.loading = true;
          try {
            await forgotPasswordApi(this.forgotForm);
            this.$message.success("密码重置成功");
            this.switchForm("account");
          } catch (error) {
            this.$message.error(error.message || "重置失败，请重试");
          } finally {
            this.loading = false;
          }
        } else {
          return false;
        }
      });
    },
    async sendVerificationCode() {
      if (this.codeSending) return;

      if (!this.forgotForm.email) {
        this.$message.error("请先输入邮箱");
        return;
      }

      this.codeSending = true;
      this.sendEmailCode(this.forgotForm.email);
    },
    handleThirdPartyLogin(type) {
      if (type === "wechat") {
        this.wechatForm.showQrcode = true;
        this.getWechatLoginCode();
        return;
      }
      getAuthRenderApi(type).then((res) => {
        setCookie("redirectUrl", this.resolveRedirectTarget());
        window.open(res.data, "_self");
      });
    },
    getWechatLoginCode() {
      if (
        this.wechatForm.refreshCountdown > 0 &&
        this.wechatForm.code &&
        this.wechatForm.code !== "验证码已失效" &&
        this.wechatForm.code !== "获取失败"
      ) {
        return;
      }
      this.clearTimer();
      this.wechatForm.code = "正在获取...";
      this.wechatForm.refreshCountdown = 0;
      this.wechatForm.expireCountdown = 0;
      getWechatLoginCodeApi().then((res) => {
        this.wechatForm.code = res.data;
        this.pollingWechatIsLogin();
        this.wechatForm.refreshCountdown = 60;
        this.wechatForm.expireCountdown = 5 * 60;
        this.codeTimer = setInterval(() => {
          if (this.wechatForm.refreshCountdown > 0) {
            this.wechatForm.refreshCountdown--;
          }
          if (this.wechatForm.expireCountdown > 0) {
            this.wechatForm.expireCountdown--;
          }
          if (this.wechatForm.expireCountdown <= 0) {
            this.clearTimer();
            this.wechatForm.code = "验证码已失效";
          }
        }, 1000);
      }).catch((error) => {
        this.wechatForm.code = "获取失败";
        this.$message.error(error.message || "获取微信登录码失败");
      });
    },
    handleWechatQrError() {
      if (this.wechatQrUrl !== this.scanPlaceholderUrl) {
        this.wechatQrUrl = this.scanPlaceholderUrl;
      }
    },
    async copyWechatHandle() {
      const wechat = this.$store.state.webSiteInfo?.wechat;
      if (!wechat) {
        this.$message.warning("站点微信号还没有配置");
        return;
      }

      const copied = await copyToClipboard(wechat);
      if (copied) {
        this.$message.success("微信号已复制");
      } else {
        this.$message.error("复制微信号失败");
      }
    },
    pollingWechatIsLogin() {
      this.pollingTimer = setInterval(() => {
        getWechatIsLoginApi(this.wechatForm.code).then((res) => {
          if (res.code === 200) {
            this.$store.commit("SET_TOKEN", res.data.token);
            this.$store.commit("SET_USER_INFO", res.data);
            clearInterval(this.pollingTimer);
            this.$message.success("登录成功");
            this.handleClose();
          }
        });
      }, 1000);
    },
    handleClose() {
      const redirect = this.resolveRedirectTarget();
      removeCookie("redirectUrl");
      this.$router.replace(redirect || "/");
    },
    resolveRedirectTarget() {
      const redirect =
        (this.$route?.query?.redirect || "").toString().trim() ||
        (getCookie("redirectUrl") || "").toString().trim();

      if (!redirect || redirect === "/login") {
        return "/";
      }

      try {
        const url = new URL(redirect, window.location.origin);
        if (url.origin !== window.location.origin) {
          return "/";
        }
        return `${url.pathname}${url.search}${url.hash}` || "/";
      } catch (error) {
        return "/";
      }
    },
    sendRegisterCode() {
      if (this.codeSending) return;

      if (!this.registerForm.email) {
        this.$message.error("请先输入邮箱");
        return;
      }
      this.codeSending = true;
      this.sendEmailCode(this.registerForm.email);
    },
    sendEmailCode(email) {
      sendEmailCodeApi(email)
        .then(() => {
          this.$message.success("发送成功，请前往邮箱查看验证码");
          let countdown = 60;
          this.codeButtonText = `${countdown} 秒后重试`;

          this.codeTimer = setInterval(() => {
            countdown--;
            if (countdown <= 0) {
              clearInterval(this.codeTimer);
              this.codeSending = false;
              this.codeButtonText = "发送验证码";
            } else {
              this.codeButtonText = `${countdown} 秒后重试`;
            }
          }, 1000);
        })
        .catch((err) => {
          this.$message.error(err.message || "发送失败");
          this.codeSending = false;
        });
    },
    clearTimer() {
      if (this.codeTimer) {
        clearInterval(this.codeTimer);
        this.codeTimer = null;
      }
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer);
        this.pollingTimer = null;
      }
      this.wechatForm.refreshCountdown = 0;
    },
    backToHome() {
      this.$router.push("/");
    },
  },
  beforeDestroy() {
    enableScroll();
    this.clearTimer();
    this.stopCharacterScene();
    this.teardownMotionPreference();
  },
};
</script>

<style scoped lang="scss">
.login-page {
  position: fixed;
  inset: 0;
  z-index: 2000;
  overflow: auto;
  background:
    radial-gradient(circle at top left, rgba(108, 63, 245, 0.12), transparent 30%),
    radial-gradient(circle at bottom right, rgba(255, 155, 107, 0.12), transparent 28%),
    linear-gradient(135deg, #f8faff 0%, #f5f3ff 48%, #fff8f2 100%);
}

.login-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.24fr) minmax(620px, 0.9fr);
}

.brand-panel {
  position: relative;
  overflow: hidden;
  padding: 52px 54px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #fff;
  background: linear-gradient(145deg, #6c3ff5 0%, #5b36db 46%, #44278f 100%);
}

.brand-panel::before {
  content: "";
  position: absolute;
  inset: 22px;
  border-radius: 32px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  pointer-events: none;
}

.brand-glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(24px);
  opacity: 0.26;
}

.brand-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.06) 1px, transparent 1px);
  background-size: 38px 38px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.72), transparent 92%);
  opacity: 0.45;
  pointer-events: none;
}

.glow-a {
  top: 7%;
  right: 10%;
  width: 260px;
  height: 260px;
  background: rgba(255, 255, 255, 0.24);
}

.glow-b {
  bottom: 8%;
  left: 6%;
  width: 360px;
  height: 360px;
  background: rgba(255, 255, 255, 0.12);
}

.brand-top,
.brand-main,
.brand-footer {
  position: relative;
  z-index: 1;
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.16);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
  font-size: 20px;
  font-weight: 800;
}

.brand-mark--light {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: rgba(108, 63, 245, 0.12);
  color: #6c3ff5;
}

.brand-badge strong {
  display: block;
  font-size: 18px;
  font-weight: 700;
}

.brand-badge p {
  margin: 4px 0 0;
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
}

.brand-main {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
}

.brand-footer {
  display: flex;
  gap: 22px;
  font-size: 14px;
}

.brand-footer a {
  color: rgba(255, 255, 255, 0.74);
  text-decoration: none;
  transition: color 0.2s ease;
}

.brand-footer a:hover {
  color: #fff;
}

.character-stage {
  position: relative;
  width: min(620px, 100%);
  height: 480px;
  margin: 0 auto;
}

.scene-orb,
.scene-wave {
  position: absolute;
  pointer-events: none;
}

.scene-orb {
  border-radius: 50%;
  filter: blur(2px);
  opacity: 0.9;
}

.orb-a {
  top: 52px;
  left: 44px;
  width: 132px;
  height: 132px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.32), rgba(255, 255, 255, 0.04) 68%, transparent 78%);
}

.orb-b {
  right: 40px;
  bottom: 38px;
  width: 156px;
  height: 156px;
  background: radial-gradient(circle, rgba(255, 208, 138, 0.22), rgba(255, 255, 255, 0.03) 70%, transparent 80%);
}

.scene-wave {
  left: -10%;
  width: 120%;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.12);
  opacity: 0.55;
}

.wave-a {
  bottom: 116px;
  height: 180px;
  transform: rotate(-6deg);
}

.wave-b {
  bottom: 52px;
  height: 220px;
  border-color: rgba(255, 255, 255, 0.08);
  transform: rotate(4deg);
}

.character {
  position: absolute;
  bottom: 0;
  transform-origin: bottom center;
  transition: transform 0.7s ease, height 0.7s ease;
}

.purple-character {
  left: 78px;
  width: 190px;
  height: 410px;
  border-radius: 12px 12px 0 0;
  background: #6c3ff5;
  z-index: 1;
}

.black-character {
  left: 256px;
  width: 124px;
  height: 320px;
  border-radius: 10px 10px 0 0;
  background: #2d2d2d;
  z-index: 2;
}

.orange-character {
  left: 0;
  width: 250px;
  height: 208px;
  border-radius: 128px 128px 0 0;
  background: #ff9b6b;
  z-index: 3;
}

.yellow-character {
  left: 326px;
  width: 148px;
  height: 236px;
  border-radius: 74px 74px 0 0;
  background: #e8d754;
  z-index: 4;
}

.eye-group,
.dot-eyes,
.yellow-mouth {
  position: absolute;
}

.eye-group {
  display: flex;
  gap: 24px;
  transition: left 0.25s ease, top 0.25s ease;
}

.purple-eyes {
  gap: 32px;
}

.orange-eyes,
.yellow-eyes {
  gap: 20px;
}

.eye-ball {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  overflow: hidden;
  transition: height 0.15s ease;
}

.eye-ball--small {
  width: 16px;
  height: 16px;
}

.eye-ball--dot-shell {
  width: 22px;
  height: 22px;
}

.eye-ball.blinking {
  height: 2px;
}

.eye-pupil {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2d2d2d;
  transition: transform 0.1s ease-out;
}

.eye-pupil--small {
  width: 6px;
  height: 6px;
}

.eye-pupil--dot {
  width: 9px;
  height: 9px;
}

.dot-eyes {
  display: flex;
  gap: 24px;
  transition: left 0.2s ease, top 0.2s ease;
}

.orange-eyes {
  gap: 32px;
}

.dot-pupil {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #2d2d2d;
  transition: transform 0.1s ease-out;
}

.yellow-mouth {
  width: 80px;
  height: 4px;
  border-radius: 999px;
  background: #2d2d2d;
  transition: left 0.2s ease, top 0.2s ease;
}

.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 36px 34px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(18px);
}

.panel-frame {
  width: min(100%, 700px);
  padding: 34px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 20px 58px rgba(76, 29, 149, 0.11);
  border: 1px solid rgba(255, 255, 255, 0.82);
}

.mobile-brand {
  display: none;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  color: #111827;
}

.panel-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}

.icon-btn,
.mode-btn,
.entry-chip,
.helper-btn,
.social-login-btn {
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.icon-btn {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 16px;
}

.icon-btn:hover {
  transform: translateX(-2px);
  color: #6c3ff5;
  background: #ebe9ff;
}

.mode-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 18px;
  border-radius: 999px;
  background: #f3f4f6;
  color: #374151;
  font-size: 14px;
  font-weight: 600;
}

.mode-btn:hover {
  background: #ebe9ff;
  color: #6c3ff5;
}

.panel-header {
  margin-bottom: 26px;
}

.panel-kicker {
  display: inline-flex;
  margin-bottom: 12px;
  color: #6c3ff5;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.panel-header h2 {
  margin: 0 0 12px;
  color: #111827;
  font-size: 40px;
  line-height: 1.12;
}

.panel-header p {
  margin: 0;
  color: #6b7280;
  font-size: 16px;
  line-height: 1.7;
}

.entry-switch {
  display: inline-flex;
  align-self: flex-start;
  gap: 8px;
  margin-bottom: 24px;
  padding: 6px;
  border-radius: 999px;
  background: #f3f4f6;
}

.entry-chip {
  padding: 12px 22px;
  border-radius: 999px;
  background: transparent;
  color: #6b7280;
  font-size: 14px;
  font-weight: 600;
}

.entry-chip.active {
  background: #fff;
  color: #111827;
  box-shadow: 0 8px 18px rgba(17, 24, 39, 0.08);
}

.form-surface {
  padding: 32px;
  border-radius: 26px;
  background: #fff;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.07);
}

.form-surface--wechat {
  padding-top: 28px;
}

.field-label {
  display: block;
  margin-bottom: 10px;
  color: #111827;
  font-size: 15px;
  font-weight: 600;
}

.form-item {
  margin-bottom: 20px;
}

.form-item--submit {
  margin-bottom: 0;
}

:deep(.el-form-item__content) {
  line-height: 1.4;
}

:deep(.el-input__inner) {
  height: 56px;
  border-radius: 16px;
  border-color: rgba(209, 213, 219, 0.9);
  background: #fbfbfd;
  font-size: 16px;
  padding-left: 46px;
}

:deep(.el-input__inner:focus) {
  border-color: #6c3ff5;
}

:deep(.el-input__prefix) {
  left: 14px;
  color: #9ca3af;
}

:deep(.el-input__prefix i) {
  font-size: 17px;
}

:deep(.el-input__suffix) {
  right: 12px;
  display: flex;
  align-items: center;
}

:deep(.el-input-group__append) {
  padding: 0 18px;
  border-radius: 0 16px 16px 0;
}

:deep(.el-input-group__append .el-button) {
  font-size: 14px;
  font-weight: 600;
}

.password-toggle {
  color: #9ca3af;
  cursor: pointer;
  font-size: 16px;
}

.password-toggle:hover {
  color: #6c3ff5;
}

.option-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 24px;
  gap: 12px;
}

:deep(.el-checkbox__label) {
  font-size: 14px;
  color: #4b5563;
}

.inline-link,
.surface-footer a {
  color: #6c3ff5;
  text-decoration: none;
  font-weight: 600;
}

.inline-link:hover,
.surface-footer a:hover {
  color: #5630d6;
}

.submit-btn {
  width: 100%;
  height: 56px;
  border-radius: 16px;
  border: none;
  background: #111827;
  color: #fff;
  font-size: 17px;
  font-weight: 600;
}

.submit-btn:hover,
.submit-btn:focus {
  background: #1f2937;
  transform: translateY(-1px);
}

.panel-divider {
  position: relative;
  margin: 28px 0 22px;
  text-align: center;
}

.panel-divider::before {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 1px;
  background: rgba(229, 231, 235, 0.9);
}

.panel-divider span {
  position: relative;
  padding: 0 12px;
  background: #fff;
  color: #9ca3af;
  font-size: 13px;
}

.third-party-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.social-login-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 52px;
  padding: 0 18px;
  border-radius: 16px;
  background: #f9fafb;
  color: #111827;
  font-size: 14px;
  font-weight: 600;
}

.social-login-btn:hover {
  background: #f3f4f6;
  transform: translateY(-1px);
}

.surface-footer {
  margin-top: 22px;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.7;
}

.footer-dot {
  color: #d1d5db;
}

.wechat-panel {
  text-align: center;
}

.wechat-qr-box {
  width: 232px;
  height: 232px;
  margin: 0 auto 18px;
  padding: 10px;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  box-shadow: inset 0 0 0 1px rgba(229, 231, 235, 0.9);
}

.wechat-qr-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 18px;
}

.wechat-code-line {
  margin: 0;
  color: #4b5563;
  font-size: 15px;
  line-height: 1.7;
}

.wechat-code {
  color: #6c3ff5;
  font-weight: 700;
}

.code-refresh {
  margin-left: 8px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  color: #6c3ff5;
  font-size: 14px;
  font-weight: 600;
}

.code-refresh:disabled {
  cursor: not-allowed;
  color: #9ca3af;
}

.wechat-guide {
  margin: 20px 0 0;
  padding-left: 18px;
  color: #4b5563;
  text-align: left;
  line-height: 1.85;
  font-size: 14px;
}

.wechat-helper {
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  background: rgba(108, 63, 245, 0.08);
  color: #4b5563;
  font-size: 14px;
}

.helper-btn {
  flex-shrink: 0;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(108, 63, 245, 0.12);
  color: #6c3ff5;
  font-weight: 600;
}

.helper-btn:hover {
  background: rgba(108, 63, 245, 0.18);
}

@media (max-width: 1560px) {
  .login-shell {
    grid-template-columns: minmax(0, 1fr) minmax(580px, 720px);
  }

  .brand-panel {
    padding: 40px 40px;
  }

  .brand-main {
    gap: 0;
  }

  .panel-frame {
    width: min(100%, 640px);
  }
}

@media (max-width: 1320px) {
  .login-shell {
    grid-template-columns: minmax(0, 1fr) minmax(560px, 660px);
  }

  .character-stage {
    width: min(540px, 100%);
    height: 430px;
    transform: scale(0.92);
    transform-origin: center;
  }

  .form-panel {
    padding: 34px;
  }

  .panel-frame {
    padding: 28px;
    border-radius: 30px;
  }

  .form-surface {
    padding: 28px;
  }
}

@media (max-width: 1180px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    display: none;
  }

  .form-panel {
    min-height: 100vh;
    padding: 24px 18px;
    align-items: stretch;
  }

  .panel-frame {
    width: min(100%, 760px);
    margin: auto;
  }

  .mobile-brand {
    display: inline-flex;
  }
}

@media (max-width: 860px) {
  .form-panel {
    padding: 18px 14px;
  }

  .panel-frame {
    padding: 22px;
    border-radius: 24px;
  }

  .panel-toolbar {
    margin-bottom: 26px;
  }

  .panel-header h2 {
    font-size: 32px;
  }

  .panel-header p {
    font-size: 14px;
    line-height: 1.65;
  }

  .entry-chip {
    padding: 12px 20px;
    font-size: 14px;
  }

  .form-surface {
    padding: 26px;
    border-radius: 24px;
  }

  :deep(.el-input__inner),
  .submit-btn {
    height: 56px;
  }

  :deep(.el-input__inner) {
    font-size: 16px;
  }

  .submit-btn {
    font-size: 17px;
  }
}

@media (max-width: 640px) {
  .panel-frame {
    padding: 16px;
    border-radius: 20px;
  }

  .form-surface {
    padding: 18px;
    border-radius: 20px;
  }

  .panel-toolbar {
    margin-bottom: 18px;
  }

  .panel-header {
    margin-bottom: 22px;
  }

  .panel-header h2 {
    font-size: 28px;
  }

  .panel-header p {
    font-size: 14px;
  }

  .mode-btn span {
    display: none;
  }

  .entry-switch {
    width: 100%;
  }

  .entry-chip {
    flex: 1;
  }

  .third-party-grid {
    grid-template-columns: 1fr;
  }

  .wechat-helper,
  .option-row,
  .surface-footer {
    flex-direction: column;
    align-items: stretch;
    text-align: center;
  }

  .helper-btn {
    width: 100%;
  }

  .wechat-qr-box {
    width: 190px;
    height: 190px;
  }

  :deep(.el-input__inner) {
    height: 54px;
    font-size: 15px;
  }

  .submit-btn {
    height: 54px;
    font-size: 16px;
  }
}

</style>




