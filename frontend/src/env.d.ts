/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_APP_TITLE: string
  readonly VITE_ICP_NUMBER: string
  readonly VITE_PS_RECORD_NUMBER: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module 'nprogress'
declare module 'element-plus/dist/locale/zh-cn.mjs'
