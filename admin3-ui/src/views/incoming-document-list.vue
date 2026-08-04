<template>
  <div class="container">
    <div class="handle-box">
      <el-button type="primary" :icon="Plus" @click="handleAdd" v-action:incoming-document:create>新增来文</el-button>
      <el-input v-model="query.keyword" placeholder="搜索编号/标题/字号/载体编号" clearable class="search-input" @keyup.enter="loadData" @clear="loadData"></el-input>
      <el-button :icon="Search" @click="loadData">搜索</el-button>
    </div>

    <el-table :data="tableData" border class="table" header-cell-class-name="table-header">
      <el-table-column prop="code" label="编号" width="150"></el-table-column>
      <el-table-column prop="carrierNo" label="载体编号" width="140"></el-table-column>
      <el-table-column prop="title" label="文件标题" min-width="220" show-overflow-tooltip></el-table-column>
      <el-table-column prop="docNo" label="来文字号" width="160" show-overflow-tooltip></el-table-column>
      <el-table-column prop="secrecyLevel" label="密级" width="90"></el-table-column>
      <el-table-column prop="urgencyLevel" label="紧急程度" width="100"></el-table-column>
      <el-table-column prop="handler" label="经办人" width="110"></el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170"></el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button text :icon="View" @click="handleDetail(row)" v-action:incoming-document:view>详情</el-button>
          <el-button text :icon="Edit" @click="handleEdit(row)" v-action:incoming-document:update>编辑</el-button>
          <el-button text :icon="Delete" class="red" @click="handleDelete(row)" v-action:incoming-document:delete>删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination background layout="total, prev, pager, next" :current-page="query.pageIndex" :page-size="query.pageSize" :total="pageTotal" @current-change="handlePageChange"></el-pagination>
    </div>

    <el-dialog :title="isEdit ? '编辑来文' : '新增来文'" v-model="dialogVisible" width="92%" top="4vh" class="incoming-dialog">
      <el-form label-width="86px" class="incoming-form">
        <div class="form-section">
          <div class="form-section-header">
            <div class="form-section-title">来文信息</div>
            <el-button size="small" :icon="Connection" @click="openMappingDialog" :disabled="latestOcrFields.length === 0">设置映射</el-button>
          </div>
          <el-row :gutter="14">
            <el-col :span="6"><el-form-item label="编号"><el-input v-model="form.code" placeholder="留空自动生成"></el-input></el-form-item></el-col>
            <el-col :span="6"><el-form-item label="载体编号"><el-input v-model="form.carrierNo"></el-input></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="文件标题" required><el-input v-model="form.title"></el-input></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="来文字号"><el-input v-model="form.docNo"></el-input></el-form-item></el-col>
            <el-col :span="4"><el-form-item label="密级"><el-select v-model="form.secrecyLevel" clearable><el-option v-for="item in secrecyOptions" :key="item" :label="item" :value="item"></el-option></el-select></el-form-item></el-col>
            <el-col :span="4"><el-form-item label="紧急程度"><el-select v-model="form.urgencyLevel" clearable><el-option v-for="item in urgencyOptions" :key="item" :label="item" :value="item"></el-option></el-select></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="经办人"><el-input v-model="form.handler"></el-input></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :autosize="{minRows: 2, maxRows: 4}"></el-input></el-form-item></el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="form-section-header">
            <div class="form-section-title">普通附件</div>
            <el-upload v-if="editId" :auto-upload="false" :show-file-list="false" :on-change="handleNormalFileChange">
              <template #trigger><el-button size="small" type="primary" :icon="Plus">上传附件</el-button></template>
            </el-upload>
          </div>
          <el-table :data="attachmentList" border size="small">
            <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip></el-table-column>
            <el-table-column prop="fileSize" label="大小" width="100"><template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template></el-table-column>
            <el-table-column label="操作" width="130">
              <template #default="{ row }">
                <el-button text type="primary" @click="preview(row)">预览</el-button>
                <el-button text type="danger" @click="removeAttachment(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!editId" class="empty-tip">请先保存来文后上传附件</div>
        </div>

        <div class="form-section">
          <div class="form-section-header">
            <div class="form-section-title">扫描件附件</div>
            <el-upload v-if="editId" :auto-upload="false" :show-file-list="false" accept=".jpg,.png,.gif,.webp,.pdf" :on-change="handleScanFileChange">
              <template #trigger><el-button size="small" type="primary" :icon="Plus">上传扫描件</el-button></template>
            </el-upload>
          </div>
          <el-table :data="scanList" border size="small">
            <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip></el-table-column>
            <el-table-column prop="ocrStatus" label="OCR状态" width="110">
              <template #default="{ row }"><el-tag :type="statusTag(row.ocrStatus)" size="small">{{ statusLabel(row.ocrStatus) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="业务字段" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ Object.keys(parseJson(row.ocrBusinessJson)).join('、') || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="250">
              <template #default="{ row }">
                <el-button text type="primary" @click="preview(row)">预览</el-button>
                <el-button text type="primary" @click="recognize(row)">识别</el-button>
                <el-popover v-if="row.ocrRawJson" placement="top" :width="520" trigger="hover">
                  <template #reference><el-button text type="primary">结果</el-button></template>
                  <pre class="ocr-json">{{ formatJson(row.ocrRawJson) }}</pre>
                </el-popover>
                <el-button text type="danger" @click="removeScan(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!editId" class="empty-tip">请先保存来文后上传扫描件</div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDocument">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog title="字段连线映射" v-model="mappingVisible" width="980px" class="mapping-dialog" @opened="drawLines">
      <div ref="mappingCanvas" class="mapping-canvas">
        <svg class="mapping-lines">
          <line v-for="line in renderedLines" :key="line.targetField + line.sourceField" :x1="line.x1" :y1="line.y1" :x2="line.x2" :y2="line.y2"></line>
        </svg>
        <div class="mapping-panel">
          <div class="mapping-title">来文资料字段</div>
          <button v-for="field in targetFields" :key="field.field" :ref="setTargetRef(field.field)" type="button" class="field-node target-node" :class="{selected: selectedTarget === field.field}" @click="selectTarget(field.field)">
            <span>{{ field.label }}</span><small>{{ form[field.field] || '-' }}</small>
          </button>
        </div>
        <div class="mapping-panel">
          <div class="mapping-title">OCR返回字段</div>
          <button v-for="field in latestOcrFields" :key="field.field" :ref="setSourceRef(field.field)" type="button" class="field-node source-node" :class="{mapped: isSourceMapped(field.field)}" @click="selectSource(field.field)">
            <span>{{ field.field }}</span><small>{{ field.value }}</small>
          </button>
        </div>
      </div>
      <template #footer>
        <el-button @click="resetMapping">重置连线</el-button>
        <el-button @click="removeSelectedMapping" :disabled="!selectedTarget">取消当前连线</el-button>
        <el-button type="primary" @click="saveMapping">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, reactive, ref} from 'vue';
import {ElMessage, ElMessageBox} from 'element-plus';
import {Connection, Delete, Edit, Plus, Search, View} from '@element-plus/icons-vue';
import {
  createIncomingDocument,
  deleteIncomingAttachment,
  deleteIncomingDocument,
  deleteIncomingScan,
  getIncomingDocumentDetail,
  getIncomingDocumentList,
  getIncomingOcrMapping,
  recognizeIncomingScan,
  saveIncomingOcrMapping,
  updateIncomingDocument,
  uploadIncomingAttachment,
  uploadIncomingScan
} from '../api/incoming-document';

const query = reactive({pageIndex: 1, pageSize: 10, keyword: ''});
const tableData = ref<any[]>([]);
const pageTotal = ref(0);
const dialogVisible = ref(false);
const mappingVisible = ref(false);
const isEdit = ref(false);
const editId = ref<number | null>(null);
const attachmentList = ref<any[]>([]);
const scanList = ref<any[]>([]);
const mappings = ref<Array<{targetField: string; sourceField: string}>>([]);
const selectedTarget = ref('');
const mappingCanvas = ref<HTMLElement>();
const targetRefs = new Map<string, HTMLElement>();
const sourceRefs = new Map<string, HTMLElement>();
const renderedLines = ref<any[]>([]);
const secrecyOptions = ['绝密', '机密', '秘密', '普通'];
const urgencyOptions = ['特急', '加急', '平急', '普通'];
const targetFields = [
  {field: 'code', label: '编号'},
  {field: 'carrierNo', label: '载体编号'},
  {field: 'title', label: '文件标题'},
  {field: 'docNo', label: '来文字号'},
  {field: 'secrecyLevel', label: '密级'},
  {field: 'urgencyLevel', label: '紧急程度'},
  {field: 'handler', label: '经办人'},
  {field: 'remark', label: '备注'}
];
const form = reactive<any>({code: '', carrierNo: '', title: '', docNo: '', secrecyLevel: '', urgencyLevel: '', handler: '', remark: ''});

const latestOcrFields = computed(() => {
  const latest = [...scanList.value].reverse().find(item => item.ocrBusinessJson);
  const fields = parseJson(latest?.ocrBusinessJson);
  return Object.keys(fields).map(field => ({field, value: String(fields[field] ?? '')}));
});

async function loadData() {
  const res = await getIncomingDocumentList({page: query.pageIndex - 1, size: query.pageSize, keyword: query.keyword});
  tableData.value = res.data.content || [];
  pageTotal.value = res.data.totalElements || 0;
}

function resetForm() {
  Object.assign(form, {code: '', carrierNo: '', title: '', docNo: '', secrecyLevel: '', urgencyLevel: '', handler: '', remark: ''});
  attachmentList.value = [];
  scanList.value = [];
  editId.value = null;
}

function handleAdd() {
  resetForm();
  isEdit.value = false;
  dialogVisible.value = true;
}

async function handleEdit(row: any) {
  resetForm();
  isEdit.value = true;
  const res = await getIncomingDocumentDetail(row.id);
  fillDetail(res.data);
  dialogVisible.value = true;
}

async function handleDetail(row: any) {
  await handleEdit(row);
  isEdit.value = true;
}

function fillDetail(data: any) {
  editId.value = data.id;
  Object.assign(form, {
    code: data.code || '',
    carrierNo: data.carrierNo || '',
    title: data.title || '',
    docNo: data.docNo || '',
    secrecyLevel: data.secrecyLevel || '',
    urgencyLevel: data.urgencyLevel || '',
    handler: data.handler || '',
    remark: data.remark || ''
  });
  attachmentList.value = data.attachments || [];
  scanList.value = data.scanAttachments || [];
}

async function saveDocument() {
  if (!form.title) {
    ElMessage.warning('请输入文件标题');
    return;
  }
  const payload = {...form};
  const res = editId.value ? await updateIncomingDocument(editId.value, payload) : await createIncomingDocument(payload);
  fillDetail(res.data);
  isEdit.value = true;
  ElMessage.success('保存成功');
  loadData();
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确认删除该来文？', '提示', {type: 'warning'});
    await deleteIncomingDocument(row.id);
    ElMessage.success('删除成功');
    loadData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败');
  }
}

async function handleNormalFileChange(uploadFile: any) {
  if (!editId.value) return;
  const res = await uploadIncomingAttachment(editId.value, uploadFile.raw);
  fillDetail(res.data);
  ElMessage.success('附件上传成功');
}

async function removeAttachment(row: any) {
  if (!editId.value) return;
  const res = await deleteIncomingAttachment(editId.value, row.id);
  fillDetail(res.data);
  ElMessage.success('附件已删除');
}

async function handleScanFileChange(uploadFile: any) {
  if (!editId.value) return;
  const res = await uploadIncomingScan(editId.value, uploadFile.raw);
  fillDetail(res.data);
  applyMappingToForm();
  ElMessage.success('扫描件上传并识别完成');
}

async function recognize(row: any) {
  if (!editId.value) return;
  const res = await recognizeIncomingScan(editId.value, row.id);
  fillDetail(res.data);
  applyMappingToForm();
  ElMessage.success('识别完成');
}

async function removeScan(row: any) {
  if (!editId.value) return;
  const res = await deleteIncomingScan(editId.value, row.id);
  fillDetail(res.data);
  ElMessage.success('扫描件已删除');
}

async function openMappingDialog() {
  const res = await getIncomingOcrMapping();
  try {
    mappings.value = JSON.parse(res.data.mappingJson || '[]');
  } catch {
    mappings.value = [];
  }
  mappingVisible.value = true;
  await nextTick();
  drawLines();
}

function selectTarget(field: string) {
  selectedTarget.value = selectedTarget.value === field ? '' : field;
}

function selectSource(field: string) {
  if (!selectedTarget.value) {
    ElMessage.info('请先选择左侧来文资料字段');
    return;
  }
  mappings.value = mappings.value.filter(item => item.targetField !== selectedTarget.value && item.sourceField !== field);
  mappings.value.push({targetField: selectedTarget.value, sourceField: field});
  selectedTarget.value = '';
  drawLines();
}

async function saveMapping() {
  await saveIncomingOcrMapping(JSON.stringify(mappings.value));
  applyMappingToForm();
  ElMessage.success('映射已保存并应用');
  drawLines();
}

function resetMapping() {
  mappings.value = [];
  drawLines();
}

function removeSelectedMapping() {
  mappings.value = mappings.value.filter(item => item.targetField !== selectedTarget.value);
  selectedTarget.value = '';
  drawLines();
}

function applyMappingToForm() {
  const latest = latestOcrFields.value.reduce((acc: any, item) => {
    acc[item.field] = item.value;
    return acc;
  }, {});
  mappings.value.forEach(item => {
    if (latest[item.sourceField] !== undefined) form[item.targetField] = latest[item.sourceField];
  });
}

function drawLines() {
  nextTick(() => {
    const host = mappingCanvas.value?.getBoundingClientRect();
    if (!host) return;
    renderedLines.value = mappings.value.map(item => {
      const target = targetRefs.get(item.targetField)?.getBoundingClientRect();
      const source = sourceRefs.get(item.sourceField)?.getBoundingClientRect();
      if (!target || !source) return null;
      return {
        targetField: item.targetField,
        sourceField: item.sourceField,
        x1: target.right - host.left,
        y1: target.top + target.height / 2 - host.top,
        x2: source.left - host.left,
        y2: source.top + source.height / 2 - host.top
      };
    }).filter(Boolean);
  });
}

function setTargetRef(field: string) {
  return (el: any) => { if (el) targetRefs.set(field, el as HTMLElement); };
}

function setSourceRef(field: string) {
  return (el: any) => { if (el) sourceRefs.set(field, el as HTMLElement); };
}

function isSourceMapped(field: string) {
  return mappings.value.some(item => item.sourceField === field);
}

function parseJson(value: string) {
  if (!value) return {};
  try {
    return JSON.parse(value);
  } catch {
    return {};
  }
}

function formatJson(value: string) {
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function preview(row: any) {
  if (row.fileUrl) window.open('/admin3' + row.fileUrl, '_blank');
}

function handlePageChange(page: number) {
  query.pageIndex = page;
  loadData();
}

function formatFileSize(size: number) {
  if (!size) return '-';
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)}KB`;
  return `${(size / 1024 / 1024).toFixed(1)}MB`;
}

function statusLabel(status: string) {
  return {pending: '待识别', processing: '识别中', completed: '已完成', failed: '失败'}[status] || '-';
}

function statusTag(status: string) {
  return {pending: 'info', processing: 'warning', completed: 'success', failed: 'danger'}[status] || 'info';
}

loadData();
</script>

<style scoped>
.handle-box {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}
.search-input {
  width: 260px;
}
.table {
  width: 100%;
  font-size: 14px;
}
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
.red {
  color: #f56c6c;
}
.incoming-form {
  max-height: 76vh;
  overflow-y: auto;
  padding-right: 6px;
}
.form-section {
  margin-bottom: 10px;
  padding: 10px 12px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}
.form-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}
.form-section-title {
  padding-left: 9px;
  border-left: 3px solid #409eff;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
  line-height: 16px;
}
.empty-tip {
  padding: 10px 0 0;
  color: #909399;
  font-size: 13px;
  text-align: center;
}
.ocr-json {
  max-height: 360px;
  margin: 0;
  overflow: auto;
  padding: 8px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
}
.mapping-canvas {
  position: relative;
  display: grid;
  grid-template-columns: 300px 1fr 300px;
  gap: 260px;
  min-height: 520px;
  padding: 12px;
}
.mapping-lines {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
.mapping-lines line {
  stroke: #409eff;
  stroke-width: 2;
}
.mapping-panel {
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.mapping-title {
  color: #303133;
  font-weight: 600;
  text-align: center;
}
.field-node {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr);
  gap: 8px;
  width: 100%;
  min-height: 38px;
  padding: 6px 10px;
  color: #303133;
  text-align: left;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
}
.field-node small {
  min-width: 0;
  overflow: hidden;
  color: #909399;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.field-node.selected,
.field-node.mapped {
  border-color: #409eff;
  background: #ecf5ff;
}
</style>
