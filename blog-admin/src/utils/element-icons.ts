import { markRaw, type Component } from 'vue'

type ElementIconModule = Record<string, Component>

let iconsModulePromise: Promise<ElementIconModule> | null = null
let cachedIcons: ElementIconModule | null = null

export const normalizeElementIconName = (name?: string) => {
  if (!name) {
    return ''
  }

  const trimmedName = name.replace(/^el-icon-/i, '')
  return trimmedName.charAt(0).toUpperCase() + trimmedName.slice(1)
}

export const loadElementPlusIcons = async () => {
  if (cachedIcons) {
    return cachedIcons
  }

  if (!iconsModulePromise) {
    iconsModulePromise = import('@element-plus/icons-vue').then((module) => {
      cachedIcons = Object.fromEntries(
        Object.entries(module).map(([iconName, iconComponent]) => [
          iconName,
          markRaw(iconComponent as Component)
        ])
      )
      return cachedIcons
    })
  }

  return iconsModulePromise
}

export const resolveElementPlusIcon = async (name?: string) => {
  const normalizedName = normalizeElementIconName(name)

  if (!normalizedName) {
    return null
  }

  const icons = await loadElementPlusIcons()
  return icons[normalizedName] ?? null
}
