function normalizeContactWay(siteInfo = {}) {
  const value = (siteInfo.contactWay || "email").toString().trim().toLowerCase();
  return ["email", "qq", "wechat", "page"].includes(value) ? value : "email";
}

function isExternalUrl(url = "") {
  return /^(https?:)?\/\//i.test(url);
}

function normalizeContactPage(path = "") {
  const value = (path || "/messages").toString().trim();
  if (!value) {
    return "/messages";
  }
  if (isExternalUrl(value) || value.startsWith("/")) {
    return value;
  }
  return `/${value}`;
}

export function resolveContactAction(siteInfo = {}) {
  const email = (siteInfo.email || "").trim();
  const qqNumber = (siteInfo.qqNumber || "").trim();
  const wechat = (siteInfo.wechat || "").trim();
  const contactWay = normalizeContactWay(siteInfo);
  const contactPage = normalizeContactPage(siteInfo.contactPage);

  const unavailableMap = {
    email: "请先在后台网站配置中填写站长邮箱",
    qq: "请先在后台网站配置中填写站长 QQ",
    wechat: "请先在后台网站配置中填写站长微信号",
    page: "请先在后台网站配置中填写联系页地址",
  };

  switch (contactWay) {
    case "qq":
      return qqNumber
        ? {
            type: "qq",
            icon: "fab fa-qq",
            label: "联系站长",
            path: `https://wpa.qq.com/msgrd?v=3&uin=${encodeURIComponent(qqNumber)}&site=qq&menu=yes`,
            external: true,
            available: true,
          }
        : {
            type: "qq",
            icon: "fab fa-qq",
            label: "联系站长",
            available: false,
            unavailableReason: unavailableMap.qq,
          };
    case "wechat":
      return wechat
        ? {
            type: "wechat",
            icon: "fab fa-weixin",
            label: "联系站长",
            available: true,
            external: false,
            copyValue: wechat,
            copyLabel: "站长微信号",
          }
        : {
            type: "wechat",
            icon: "fab fa-weixin",
            label: "联系站长",
            available: false,
            unavailableReason: unavailableMap.wechat,
          };
    case "page":
      return contactPage
        ? {
            type: "page",
            icon: "fas fa-address-card",
            label: "联系站长",
            path: contactPage,
            external: isExternalUrl(contactPage),
            available: true,
          }
        : {
            type: "page",
            icon: "fas fa-address-card",
            label: "联系站长",
            available: false,
            unavailableReason: unavailableMap.page,
          };
    case "email":
    default:
      return email
        ? {
            type: "email",
            icon: "fas fa-envelope",
            label: "联系站长",
            path: `mailto:${email}`,
            external: true,
            available: true,
          }
        : {
            type: "email",
            icon: "fas fa-envelope",
            label: "联系站长",
            available: false,
            unavailableReason: unavailableMap.email,
          };
  }
}

export async function copyText(text) {
  const normalizedText = String(text || '').trim();
  if (!normalizedText) return false;

  if (navigator.clipboard && window.isSecureContext) {
    try {
      await navigator.clipboard.writeText(normalizedText);
      return true;
    } catch (error) {
      // Fallback below.
    }
  }

  const textarea = document.createElement('textarea');
  textarea.value = normalizedText;
  textarea.setAttribute('readonly', 'readonly');
  textarea.style.position = 'fixed';
  textarea.style.top = '-9999px';
  textarea.style.left = '-9999px';
  textarea.style.opacity = '0';
  document.body.appendChild(textarea);
  textarea.focus();
  textarea.select();
  textarea.setSelectionRange(0, normalizedText.length);
  const copied = document.execCommand('copy');
  document.body.removeChild(textarea);
  return copied;
}

export async function handleContactAction(vm, siteInfo = {}) {
  const action = resolveContactAction(siteInfo);
  const message = vm?.$message;
  const router = vm?.$router;

  if (!action.available) {
    message?.warning(action.unavailableReason || "当前联系方式未配置");
    return false;
  }

  if (action.type === "wechat") {
    const copied = await copyText(action.copyValue);
    if (copied) {
      message?.success(`已复制${action.copyLabel}：${action.copyValue}`);
    } else {
      message?.info(`${action.copyLabel}：${action.copyValue}`);
    }
    return true;
  }

  if (action.type === "email") {
    window.location.href = action.path;
    return true;
  }

  if (action.external) {
    window.open(action.path, "_blank", "noopener");
    return true;
  }

  if (router && action.path && router.currentRoute.path !== action.path) {
    router.push(action.path);
  }
  return true;
}
