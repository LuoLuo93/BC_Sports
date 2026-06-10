<template>
  <div class="property-panel">
    <template v-if="element">
      <!-- 通用属性 -->
      <div class="panel-section">
        <div class="section-title">位置与尺寸</div>
        <div class="prop-grid">
          <div class="prop-item">
            <label>X (mm)</label>
            <el-input-number v-model="form.x" :controls="false" size="small" :precision="1" :step="0.5" @change="updateElement" />
          </div>
          <div class="prop-item">
            <label>Y (mm)</label>
            <el-input-number v-model="form.y" :controls="false" size="small" :precision="1" :step="0.5" @change="updateElement" />
          </div>
          <div class="prop-item" v-if="element.type !== 'line' && element.type !== 'text'">
            <label>宽 (mm)</label>
            <el-input-number v-model="form.width" :controls="false" size="small" :precision="1" :step="0.5" :min="1" @change="updateElement" />
          </div>
          <div class="prop-item" v-if="element.type !== 'line' && element.type !== 'text'">
            <label>高 (mm)</label>
            <el-input-number v-model="form.height" :controls="false" size="small" :precision="1" :step="0.5" :min="1" @change="updateElement" />
          </div>
          <div class="prop-item" v-if="element.type === 'line'">
            <label>长度 (mm)</label>
            <el-input-number v-model="form.width" :controls="false" size="small" :precision="1" :step="0.5" :min="1" @change="updateElement" />
          </div>
          <div class="prop-item">
            <label>旋转 (°)</label>
            <el-input-number v-model="form.rotation" :controls="false" size="small" :precision="0" :step="5" :min="0" :max="360" @change="updateElement" />
          </div>
        </div>
      </div>

      <!-- 文本属性 -->
      <template v-if="element.type === 'text'">
        <div class="panel-section">
          <div class="section-title">文本内容</div>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="2"
            size="small"
            placeholder="输入文本，支持 {{字段名}}"
            @change="updateElement"
          />
          <div class="field-hint">
            <span>动态字段：</span>
            <el-tag
              v-for="f in dataFields"
              :key="f.key"
              size="small"
              :class="['field-tag', isFieldUsed(f.key) ? 'field-tag-active' : '']"
              @click="insertField(f.key)"
            >
              {{ f.label }}
            </el-tag>
          </div>
        </div>

        <div class="panel-section">
          <div class="section-title">字体</div>
          <div class="prop-grid one-col">
            <div class="prop-item">
              <label>字号 (mm)</label>
              <el-input-number v-model="form.fontSize" :controls="false" size="small" :precision="1" :step="0.5" :min="1" :max="30" @change="updateElement" />
            </div>
            <div class="prop-item">
              <label>粗细</label>
              <el-select v-model="form.fontWeight" size="small" @change="updateElement">
                <el-option label="正常" value="normal" />
                <el-option label="粗体" value="bold" />
              </el-select>
            </div>
            <div class="prop-item">
              <label>对齐</label>
              <el-select v-model="form.textAlign" size="small" @change="updateElement">
                <el-option label="左对齐" value="left" />
                <el-option label="居中" value="center" />
                <el-option label="右对齐" value="right" />
              </el-select>
            </div>
          </div>
        </div>
      </template>

      <!-- 条码属性 -->
      <template v-if="element.type === 'barcode'">
        <div class="panel-section">
          <div class="section-title">条码设置</div>
          <div class="prop-grid">
            <div class="prop-item span-2">
              <label>条码类型</label>
              <el-select v-model="form.barcodeType" size="small" @change="updateElement">
                <el-option label="EAN-13" value="EAN13" />
                <el-option label="Code128" value="Code128" />
                <el-option label="EAN-8" value="EAN8" />
              </el-select>
            </div>
            <div class="prop-item span-2">
              <label>数据绑定</label>
              <el-select v-model="form.dataField" size="small" clearable placeholder="选择字段" @change="updateElement">
                <el-option v-for="f in dataFields" :key="f.key" :label="f.label" :value="f.key" />
              </el-select>
            </div>
            <div class="prop-item span-2">
              <el-checkbox v-model="form.showText" @change="updateElement">显示条码文本</el-checkbox>
            </div>
          </div>
        </div>
      </template>

      <!-- 二维码属性 -->
      <template v-if="element.type === 'qrcode'">
        <div class="panel-section">
          <div class="section-title">二维码设置</div>
          <div class="prop-grid">
            <div class="prop-item span-2">
              <label>数据绑定</label>
              <el-select v-model="form.dataField" size="small" clearable placeholder="选择字段" @change="updateElement">
                <el-option v-for="f in dataFields" :key="f.key" :label="f.label" :value="f.key" />
              </el-select>
            </div>
            <div class="prop-item">
              <label>模块大小</label>
              <el-input-number v-model="form.moduleSize" size="small" :min="2" :max="10" @change="updateElement" />
            </div>
            <div class="prop-item">
              <label>纠错级别</label>
              <el-select v-model="form.errorCorrectionLevel" size="small" @change="updateElement">
                <el-option label="L (7%)" value="L" />
                <el-option label="M (15%)" value="M" />
                <el-option label="Q (25%)" value="Q" />
                <el-option label="H (30%)" value="H" />
              </el-select>
            </div>
          </div>
        </div>
      </template>

      <!-- 线条属性 -->
      <template v-if="element.type === 'line'">
        <div class="panel-section">
          <div class="section-title">线条设置</div>
          <div class="prop-grid">
            <div class="prop-item span-2">
              <label>线宽 (px)</label>
              <el-input-number v-model="form.strokeWidth" size="small" :min="1" :max="5" @change="updateElement" />
            </div>
          </div>
        </div>
      </template>

      <!-- 矩形属性 -->
      <template v-if="element.type === 'rect'">
        <div class="panel-section">
          <div class="section-title">矩形设置</div>
          <div class="prop-grid">
            <div class="prop-item span-2">
              <label>边框宽度 (px)</label>
              <el-input-number v-model="form.strokeWidth" size="small" :min="1" :max="5" @change="updateElement" />
            </div>
            <div class="prop-item span-2">
              <el-checkbox v-model="form.fill" @change="updateElement">填充</el-checkbox>
            </div>
          </div>
        </div>
      </template>
    </template>

    <!-- 无选中提示 -->
    <template v-else>
      <div class="empty-hint">
        <svg viewBox="0 0 24 24" fill="none" stroke="#cbd5e1" stroke-width="1.5" width="32" height="32">
          <path d="M3 3l7.07 16.97 2.51-7.39 7.39-2.51L3 3z"/>
        </svg>
        <p>点击画布元素进行编辑</p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { reactive, watch } from 'vue'
import { DATA_FIELDS } from '@/utils/zplGenerator'

const props = defineProps({
  element: { type: Object, default: null }
})

const emit = defineEmits(['update'])

const dataFields = DATA_FIELDS

// 判断某个字段是否已在文本内容中被使用
function isFieldUsed(key) {
  return form.content && form.content.includes(`{{${key}}}`)
}

const form = reactive({
  x: 0, y: 0, width: 0, height: 0, rotation: 0,
  content: '', fontSize: 3, fontWeight: 'normal', textAlign: 'left',
  barcodeType: 'EAN13', dataField: '', showText: true,
  moduleSize: 4, errorCorrectionLevel: 'M',
  strokeWidth: 1, fill: false
})

// 元素变化时同步表单（只监听引用变化，避免 deep 导致表单输入被覆盖）
watch(() => props.element, (el) => {
  if (!el) return
  Object.keys(form).forEach(key => {
    if (el[key] !== undefined) {
      form[key] = el[key]
    }
  })
}, { immediate: true })

function updateElement() {
  emit('update', { ...form })
}

function insertField(fieldKey) {
  const tag = `{{${fieldKey}}}`
  form.content = form.content ? form.content + tag : tag
  updateElement()
}
</script>

<style scoped>
.property-panel {
  width: 260px;
  background: #fff;
  border-left: 1px solid #e5e7eb;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 12px;
}

.panel-section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 11px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}

.prop-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.prop-grid.one-col {
  grid-template-columns: 1fr;
}

.prop-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.prop-item label {
  font-size: 10px;
  color: #94a3b8;
}

.prop-item.span-2 {
  grid-column: span 2;
}

.prop-item :deep(.el-input-number) {
  width: 100%;
}

.prop-item :deep(.el-input-number .el-input__inner) {
  text-align: left;
}

.prop-item :deep(.el-select) {
  width: 100%;
}

.prop-item :deep(.el-input) {
  width: 100%;
}

.field-hint {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.field-hint > span {
  font-size: 10px;
  color: #94a3b8;
}

.field-tag {
  cursor: pointer;
  font-size: 10px;
  transition: all 0.15s;
}

.field-tag:hover {
  color: #0ea5e9;
}

.field-tag-active {
  background-color: #f97316 !important;
  border-color: #f97316 !important;
  color: #fff !important;
}

.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #94a3b8;
  text-align: center;
}

.empty-hint p {
  margin-top: 12px;
  font-size: 12px;
}
</style>
