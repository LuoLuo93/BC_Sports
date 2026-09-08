import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import { vPermission } from './directives/permission'
import './styles/index.css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/notification/style/css'

// Element Plus 不做全量 app.use：模板组件由 vite 的 unplugin-vue-components
// (ElementPlusResolver) 按需注入，全量注册会让按需构建失效、首包多打几百 KB。
// 中文 locale 由 App.vue 的 <el-config-provider> 提供。
const app = createApp(App)

app.use(createPinia())
app.use(router)
app.directive('permission', vPermission)

app.mount('#app')
