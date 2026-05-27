import { createApp } from 'vue'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import router from './router'
import App from './App.vue'
import { vPermission } from './directives/permission'
import './styles/index.css'
import './utils/echarts'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.directive('permission', vPermission)

app.mount('#app')
