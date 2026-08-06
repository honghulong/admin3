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
      <el-table-column prop="incomingUnit" label="来文单位" min-width="180" show-overflow-tooltip></el-table-column>
      <el-table-column prop="receivedNo" label="收文编号" width="160" show-overflow-tooltip></el-table-column>
      <el-table-column prop="receivedDate" label="收文时间" width="140" show-overflow-tooltip></el-table-column>
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
          <div class="document-form-grid">
            <el-form-item label="编号"><el-input v-model="form.code" placeholder="留空自动生成"></el-input></el-form-item>
            <el-form-item label="载体编号"><el-input v-model="form.carrierNo"></el-input></el-form-item>
            <el-form-item label="来文单位" class="grid-span-2"><el-input v-model="form.incomingUnit"></el-input></el-form-item>
            <el-form-item label="收文编号"><el-input v-model="form.receivedNo"></el-input></el-form-item>
            <el-form-item label="收文时间"><el-input v-model="form.receivedDate"></el-input></el-form-item>
            <el-form-item label="文件标题" required class="grid-span-2"><el-input v-model="form.title"></el-input></el-form-item>
            <el-form-item label="来文字号" class="grid-span-2"><el-input v-model="form.docNo"></el-input></el-form-item>
            <el-form-item label="密级"><el-select v-model="form.secrecyLevel" clearable><el-option v-for="item in secrecyOptions" :key="item" :label="item" :value="item"></el-option></el-select></el-form-item>
            <el-form-item label="紧急程度"><el-select v-model="form.urgencyLevel" clearable><el-option v-for="item in urgencyOptions" :key="item" :label="item" :value="item"></el-option></el-select></el-form-item>
            <el-form-item label="经办人"><el-input v-model="form.handler"></el-input></el-form-item>
            <el-form-item label="办公室意见" class="grid-span-4"><el-input v-model="form.officeOpinion" type="textarea" :autosize="{minRows: 2, maxRows: 4}"></el-input></el-form-item>
            <el-form-item label="领导阅批" class="grid-span-4"><el-input v-model="form.leaderInstruction" type="textarea" :autosize="{minRows: 2, maxRows: 5}"></el-input></el-form-item>
            <el-form-item label="阅者签字"><el-input v-model="form.readerSigner"></el-input></el-form-item>
            <el-form-item label="办理情况" class="grid-span-3"><el-input v-model="form.handlingStatus"></el-input></el-form-item>
            <el-form-item label="备注" class="grid-span-4"><el-input v-model="form.remark" type="textarea" :autosize="{minRows: 2, maxRows: 4}"></el-input></el-form-item>
          </div>
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
            <el-table-column label="操作" width="300">
              <template #default="{ row }">
                <el-button text type="primary" @click="preview(row)">预览</el-button>
                <el-button text type="primary" @click="recognize(row)">识别</el-button>
                <el-button text type="primary" :disabled="!row.ocrRawJson" @click="openRestoreDialog(row)">还原</el-button>
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

    <el-dialog title="文件还原预览" v-model="restoreVisible" width="1180px" top="3vh" class="restore-dialog">
      <div class="restore-toolbar">
        <span>{{ restoreSummary }}</span>
        <div>
          <el-button size="small" :loading="restoreLoading" @click="reloadRestore">重新还原</el-button>
          <el-button size="small" type="primary" :disabled="!restoreUrl" @click="openRestorePage">新页打开</el-button>
        </div>
      </div>
      <div class="restore-frame-wrap">
        <iframe v-if="restoreUrl" class="restore-frame" :src="restoreUrl"></iframe>
        <el-empty v-else description="暂无还原预览"></el-empty>
      </div>
    </el-dialog>
    <el-dialog title="字段连线映射" v-model="mappingVisible" width="96%" top="3vh" class="mapping-dialog" @opened="drawLines">
      <el-form label-width="100px" class="mapping-rule-form">
        <el-form-item label="规则名称" required>
          <el-input v-model="mappingRuleName" maxlength="100" show-word-limit placeholder="请输入全局唯一的映射规则名称"></el-input>
        </el-form-item>
      </el-form>
      <div ref="mappingCanvas" class="mapping-canvas">
        <svg class="mapping-lines">
          <line v-for="line in renderedLines" :key="line.targetField + line.sourceField" :class="{selected: selectedMapping?.targetField === line.targetField && selectedMapping?.sourceField === line.sourceField}" :x1="line.x1" :y1="line.y1" :x2="line.x2" :y2="line.y2" @click="selectExistingMapping(line.targetField, line.sourceField)"></line>
        </svg>
        <div class="mapping-panel target-panel">
          <div class="mapping-title">来文资料字段</div>
          <button v-for="field in targetFields" :key="field.field" :ref="setTargetRef(field.field)" type="button" class="field-node target-node" :class="{selected: selectedTarget === field.field}" @click="selectTarget(field.field)">
            <span>{{ field.label }}</span><small>{{ form[field.field] || '-' }}</small>
          </button>
          <div class="mapping-hint" v-if="selectedMapping">
            {{ describeMapping(selectedMapping) }}
          </div>
        </div>
        <div class="mapping-panel source-panel">
          <div class="mapping-title">OCR结构化内容</div>
          <div v-for="group in latestOcrGroups" :key="group.title" class="ocr-group">
            <div class="ocr-group-title">{{ group.title }}</div>
            <div v-for="field in group.fields" :key="field.field" class="ocr-row" :class="{mapped: isSourceMapped(field.field), selected: selectedMapping?.sourceField === field.field}">
              <button :ref="setSourceRef(field.field)" type="button" class="ocr-anchor" :title="field.field" @click="selectSource(field.field)">{{ field.field }}</button>
              <span class="ocr-content" :title="field.text" @click="selectExistingSourceMapping(field.field)">
                <strong>{{ field.text }}</strong>
                <small v-if="field.meta">{{ field.meta }}</small>
              </span>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="resetMapping">重置连线</el-button>
        <el-button @click="removeSelectedMapping" :disabled="!selectedTarget && !selectedMapping">取消当前连线</el-button>
        <el-button type="primary" @click="saveMapping">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onActivated, onMounted, reactive, ref} from 'vue';
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
  restoreIncomingScan,
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
const restoreVisible = ref(false);
const isEdit = ref(false);
const editId = ref<number | null>(null);
const attachmentList = ref<any[]>([]);
const scanList = ref<any[]>([]);
const restoreScan = ref<any>(null);
const restoreSummary = ref('');
const restoreUrl = ref('');
const restoreLoading = ref(false);
const mappingRuleId = ref<number | null>(null);
const mappingRuleName = ref('');
const mappings = ref<any[]>([]);
const selectedTarget = ref('');
const selectedMapping = ref<{targetField: string; sourceField: string} | null>(null);
const mappingCanvas = ref<HTMLElement>();
const targetRefs = new Map<string, HTMLElement>();
const sourceRefs = new Map<string, HTMLElement>();
const renderedLines = ref<any[]>([]);
let activatedOnce = false;
const secrecyOptions = ['绝密', '机密', '秘密', '普通'];
const urgencyOptions = ['特急', '加急', '平急', '普通'];
const targetFields = [
  {field: 'code', label: '编号'},
  {field: 'carrierNo', label: '载体编号'},
  {field: 'incomingUnit', label: '来文单位'},
  {field: 'receivedNo', label: '收文编号'},
  {field: 'receivedDate', label: '收文时间'},
  {field: 'title', label: '文件标题'},
  {field: 'docNo', label: '来文字号'},
  {field: 'secrecyLevel', label: '密级'},
  {field: 'urgencyLevel', label: '紧急程度'},
  {field: 'handler', label: '经办人'},
  {field: 'officeOpinion', label: '办公室意见'},
  {field: 'leaderInstruction', label: '领导阅批'},
  {field: 'readerSigner', label: '阅者签字'},
  {field: 'handlingStatus', label: '办理情况'},
  {field: 'remark', label: '备注'}
];
const emptyForm = {
  code: '',
  carrierNo: '',
  incomingUnit: '',
  title: '',
  docNo: '',
  receivedNo: '',
  receivedDate: '',
  secrecyLevel: '',
  urgencyLevel: '',
  handler: '',
  officeOpinion: '',
  leaderInstruction: '',
  readerSigner: '',
  handlingStatus: '',
  remark: ''
};
const form = reactive<any>({...emptyForm});

const latestOcrFields = computed(() => {
  return latestOcrGroups.value.flatMap(group => group.fields);
});

const latestOcrGroups = computed(() => {
  const latest = [...scanList.value].reverse().find(item => item.ocrRawJson || item.ocrBusinessJson);
  if (!latest) return [];
  const raw = parseJson(latest.ocrRawJson);
  const groups = buildOcrGroups(raw);
  if (groups.length > 0) return groups;

  const fields = parseJson(latest.ocrBusinessJson);
  const fallback = Object.keys(fields).map(field => {
    const parsed = parseOcrFieldValue(String(fields[field] ?? ''));
    return {field, value: parsed.text, text: parsed.text, meta: parsed.meta};
  });
  return fallback.length ? [{title: '兼容字段', fields: fallback}] : [];
});

async function loadData() {
  const res = await getIncomingDocumentList({page: query.pageIndex, size: query.pageSize, keyword: query.keyword});
  tableData.value = res.data.list || [];
  pageTotal.value = res.data.total || 0;
}

async function loadDefaultData() {
  query.keyword = '';
  query.pageIndex = 1;
  await loadData();
}

function resetForm() {
  Object.assign(form, emptyForm);
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
    incomingUnit: data.incomingUnit || '',
    title: data.title || '',
    docNo: data.docNo || '',
    receivedNo: data.receivedNo || '',
    receivedDate: data.receivedDate || '',
    secrecyLevel: data.secrecyLevel || '',
    urgencyLevel: data.urgencyLevel || '',
    handler: data.handler || '',
    officeOpinion: data.officeOpinion || '',
    leaderInstruction: data.leaderInstruction || '',
    readerSigner: data.readerSigner || '',
    handlingStatus: data.handlingStatus || '',
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
  await loadDefaultData();
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
  mappingRuleId.value = res.data?.id || null;
  mappingRuleName.value = res.data?.name || '';
  try {
    mappings.value = JSON.parse(res.data?.mappingJson || '[]');
  } catch {
    mappings.value = [];
  }
  mappingVisible.value = true;
  await nextTick();
  drawLines();
}

function selectTarget(field: string) {
  selectedMapping.value = mappings.value.find(item => item.targetField === field) || null;
  selectedTarget.value = selectedTarget.value === field ? '' : field;
}

function selectSource(field: string) {
  if (!selectedTarget.value) {
    ElMessage.info('请先选择左侧来文资料字段');
    return;
  }
  mappings.value = mappings.value.filter(item => item.targetField !== selectedTarget.value && item.sourceField !== field);
  mappings.value.push(createMappingAnchor(selectedTarget.value, field));
  selectedMapping.value = {targetField: selectedTarget.value, sourceField: field};
  selectedTarget.value = '';
  drawLines();
}

function selectExistingSourceMapping(field: string) {
  const mapping = mappings.value.find(item => item.sourceField === field);
  selectedMapping.value = mapping || null;
  selectedTarget.value = mapping?.targetField || '';
}

function selectExistingMapping(targetField: string, sourceField: string) {
  selectedMapping.value = {targetField, sourceField};
  selectedTarget.value = targetField;
}

async function saveMapping() {
  if (!mappingRuleName.value.trim()) {
    ElMessage.warning('请输入映射规则名称');
    return;
  }
  const res = await saveIncomingOcrMapping({id: mappingRuleId.value, name: mappingRuleName.value.trim(), mappingJson: JSON.stringify(mappings.value)});
  mappingRuleId.value = res.data.id;
  mappingRuleName.value = res.data.name;
  applyMappingToForm();
  if (editId.value) {
    const res = await updateIncomingDocument(editId.value, {...form});
    fillDetail(res.data);
    loadData();
  }
  ElMessage.success('映射已保存并写入来文');
  drawLines();
}

function resetMapping() {
  mappings.value = [];
  selectedMapping.value = null;
  selectedTarget.value = '';
  drawLines();
}

function removeSelectedMapping() {
  const mapping = selectedMapping.value || mappings.value.find(item => item.targetField === selectedTarget.value);
  if (!mapping) return;
  mappings.value = mappings.value.filter(item => item.targetField !== mapping.targetField || item.sourceField !== mapping.sourceField);
  selectedMapping.value = null;
  selectedTarget.value = '';
  drawLines();
}

function applyMappingToForm() {
  const fields = latestOcrFields.value;
  const latest = fields.reduce((acc: any, item) => {
    acc[item.field] = item.value;
    return acc;
  }, {});
  mappings.value.forEach(item => {
    const value = resolveMappedValue(item, fields, latest);
    if (value !== undefined) form[item.targetField] = value;
  });
}

function createMappingAnchor(targetField: string, sourceField: string) {
  const source = latestOcrFields.value.find(item => item.field === sourceField);
  const mapping: any = {
    targetField,
    sourceField,
    sourceType: source?.sourceType || inferSourceType(sourceField),
    sourceText: source?.value || '',
    sourceRect: source?.rect || null,
    sourceIndex: source?.sourceIndex ?? null
  };
  if (source?.sourceType === 'table_cell') {
    const label = findNearestLeftLabel(source, latestOcrFields.value);
    if (label) {
      mapping.spatialAnchor = {
        labelText: label.value,
        labelField: label.field,
        labelRect: label.rect,
        relation: 'right-neighbor',
        sameRow: true,
        nearest: true
      };
    }
  }
  return mapping;
}

function resolveMappedValue(mapping: any, fields: any[], latest: any) {
  const spatial = resolveBySpatialAnchor(mapping, fields);
  if (spatial !== undefined) return spatial;
  if (latest[mapping.sourceField] !== undefined) return latest[mapping.sourceField];
  return undefined;
}

function resolveBySpatialAnchor(mapping: any, fields: any[]) {
  const anchor = mapping?.spatialAnchor;
  if (!anchor?.labelText || anchor.relation !== 'right-neighbor') return undefined;
  const tableCells = fields.filter(item => item.sourceType === 'table_cell' && Array.isArray(item.rect));
  const labels = tableCells
    .filter(item => normalizeText(item.value) === normalizeText(anchor.labelText))
    .sort((a, b) => rectDistance(a.rect, anchor.labelRect) - rectDistance(b.rect, anchor.labelRect));
  const label = labels[0];
  if (!label) return undefined;
  const candidates = tableCells
    .filter(item => item.field !== label.field)
    .filter(item => rectCenterX(item.rect) > rectCenterX(label.rect))
    .filter(item => verticalOverlapRatio(item.rect, label.rect) > 0.45)
    .sort((a, b) => {
      const leftGapA = a.rect[0] - label.rect[2];
      const leftGapB = b.rect[0] - label.rect[2];
      return leftGapA - leftGapB || rectDistance(a.rect, mapping.sourceRect) - rectDistance(b.rect, mapping.sourceRect);
    });
  return candidates[0]?.value;
}

function buildOcrGroups(raw: any) {
  const groups: Array<{title: string; fields: any[]}> = [];
  const cells = extractTableCells(raw);
  if (cells.length > 0) groups.push({title: '表格单元格', fields: cells});

  const lines = extractPageLines(raw);
  if (lines.length > 0) groups.push({title: 'OCR行文本', fields: lines});

  const plainText = extractPlainText(raw);
  if (plainText) {
    groups.push({
      title: '全文',
      fields: [{field: 'plain_text', value: plainText, text: summarizeText(plainText), meta: `全文 ${plainText.length} 字`}]
    });
  }
  return groups;
}

function extractTableCells(raw: any) {
  const cells: any[] = [];
  const tables = raw?.restore?.cell_mapping?.tables;
  if (Array.isArray(tables)) {
    tables.forEach((table: any, tableIndex: number) => {
      const tableCells = Array.isArray(table?.cells) ? table.cells : [];
      tableCells.forEach((cell: any) => {
        const text = normalizeText(cell?.text);
        if (!text) return;
        const field = `cell.${String(cells.length + 1).padStart(3, '0')}`;
        cells.push({
          field,
          value: text,
          text,
          meta: `表格${tableIndex + 1} | 单元格${cell?.index ?? cells.length + 1}${formatRect(cell?.rect)}`,
          rect: normalizeRect(cell?.rect),
          sourceIndex: cell?.index ?? cells.length + 1,
          sourceType: 'table_cell'
        });
      });
    });
  }

  if (cells.length > 0) return cells;
  const structureTables = raw?.structure?.raw_results?.[0]?.res?.parsing_res_list;
  if (!Array.isArray(structureTables)) return [];
  structureTables
    .filter((block: any) => block?.block_label === 'table')
    .forEach((block: any, tableIndex: number) => {
      extractHtmlCellText(String(block?.block_content || '')).forEach((text, cellIndex) => {
        const field = `cell.${String(cells.length + 1).padStart(3, '0')}`;
        cells.push({field, value: text, text, meta: `结构表格${tableIndex + 1} | 单元格${cellIndex + 1}`, rect: null, sourceIndex: cellIndex + 1, sourceType: 'table_cell'});
      });
    });
  return cells;
}

function extractPageLines(raw: any) {
  const lines: any[] = [];
  const pages = Array.isArray(raw?.pages) ? raw.pages : [];
  pages.forEach((page: any) => {
    const pageNo = page?.page_no || lines.length + 1;
    const pageLines = Array.isArray(page?.lines) ? page.lines : [];
    pageLines.forEach((line: any) => {
      const text = normalizeText(line?.text);
      if (!text) return;
      const field = `line.${String(lines.length + 1).padStart(3, '0')}`;
      const conf = typeof line?.confidence === 'number' ? ` | conf ${line.confidence.toFixed(3)}` : '';
      const rect = normalizeRect([line?.x1, line?.y1, line?.x2, line?.y2]);
      lines.push({field, value: text, text, meta: `第${pageNo}页${conf}${formatRect(rect)}`, rect, sourceIndex: lines.length + 1, sourceType: 'ocr_line'});
    });
  });
  return lines;
}

function extractPlainText(raw: any) {
  const pages = Array.isArray(raw?.pages) ? raw.pages : [];
  const text = pages.map((page: any) => normalizeText(page?.plain_text)).filter(Boolean).join('\n');
  if (text) return text;
  return extractPageLines(raw).map(item => item.value).join('\n');
}

function extractHtmlCellText(html: string) {
  return [...html.matchAll(/<td\b[^>]*>([\s\S]*?)<\/td>/gi)]
    .map(match => normalizeText(match[1].replace(/<[^>]*>/g, ' ')))
    .filter(Boolean);
}

function normalizeText(value: any) {
  return String(value ?? '').replace(/\s+/g, ' ').trim();
}

function summarizeText(value: string) {
  return value.length > 80 ? `${value.slice(0, 80)}...` : value;
}

function formatRect(rect: any) {
  if (!Array.isArray(rect) || rect.length < 4 || rect.some(item => typeof item !== 'number')) return '';
  return ` | box ${rect.slice(0, 4).map(item => Math.round(item)).join(',')}`;
}

function normalizeRect(rect: any) {
  if (!Array.isArray(rect) || rect.length < 4) return null;
  const nums = rect.slice(0, 4).map((item: any) => Number(item));
  return nums.some((item: number) => Number.isNaN(item)) ? null : nums;
}

function inferSourceType(field: string) {
  if (field.startsWith('cell.')) return 'table_cell';
  if (field.startsWith('line.')) return 'ocr_line';
  if (field === 'plain_text') return 'plain_text';
  return 'legacy';
}

function findNearestLeftLabel(source: any, fields: any[]) {
  if (!Array.isArray(source?.rect)) return null;
  return fields
    .filter(item => item.sourceType === 'table_cell' && item.field !== source.field && Array.isArray(item.rect))
    .filter(item => rectCenterX(item.rect) < rectCenterX(source.rect))
    .filter(item => verticalOverlapRatio(item.rect, source.rect) > 0.45)
    .sort((a, b) => {
      const gapA = source.rect[0] - a.rect[2];
      const gapB = source.rect[0] - b.rect[2];
      return gapA - gapB || rectDistance(a.rect, source.rect) - rectDistance(b.rect, source.rect);
    })[0] || null;
}

function rectCenterX(rect: number[]) {
  return (rect[0] + rect[2]) / 2;
}

function rectCenterY(rect: number[]) {
  return (rect[1] + rect[3]) / 2;
}

function rectDistance(a: any, b: any) {
  if (!Array.isArray(a) || !Array.isArray(b)) return Number.MAX_SAFE_INTEGER;
  return Math.hypot(rectCenterX(a) - rectCenterX(b), rectCenterY(a) - rectCenterY(b));
}

function verticalOverlapRatio(a: number[], b: number[]) {
  const overlap = Math.max(0, Math.min(a[3], b[3]) - Math.max(a[1], b[1]));
  const minHeight = Math.max(1, Math.min(a[3] - a[1], b[3] - b[1]));
  return overlap / minHeight;
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

function describeMapping(mapping: any) {
  if (mapping?.spatialAnchor?.labelText) {
    return `${mapping.sourceField}：优先取“${mapping.spatialAnchor.labelText}”右侧同一行最近的值`;
  }
  return `${mapping?.sourceField || '-'}：按锚点编号取值`;
}

function parseJson(value: string) {
  if (!value) return {};
  try {
    return JSON.parse(value);
  } catch {
    return {};
  }
}

function parseOcrFieldValue(value: string) {
  const parts = value.split(' | ');
  if (parts.length >= 4 && /^p\d+$/.test(parts[1]) && parts[2].startsWith('conf ') && parts[3].startsWith('box ')) {
    return {text: parts[0], meta: parts.slice(1).join(' | ')};
  }
  return {text: value, meta: ''};
}

function formatJson(value: string) {
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

async function openRestoreDialog(scan: any) {
  if (!scan?.ocrRawJson) {
    ElMessage.warning('请先完成扫描件OCR');
    return;
  }
  restoreScan.value = scan;
  restoreVisible.value = true;
  await loadRestore(false);
}

async function reloadRestore() {
  await loadRestore(true);
}

async function loadRestore(force: boolean) {
  const scan = restoreScan.value;
  if (!editId.value || !scan?.id) return;
  restoreLoading.value = true;
  try {
    const res = await restoreIncomingScan(editId.value, scan.id, force);
    restoreUrl.value = `/admin3${res.data.url}?t=${Date.now()}`;
    restoreSummary.value = `${scan.fileName || '-'} · ${res.data.generated ? '已生成新还原' : '已读取已保存还原'}`;
  } finally {
    restoreLoading.value = false;
  }
}

function openRestorePage() {
  if (restoreUrl.value) window.open(restoreUrl.value, '_blank');
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

onMounted(() => {
  if (!activatedOnce) loadDefaultData();
});

onActivated(() => {
  activatedOnce = true;
  loadDefaultData();
});
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
.incoming-dialog :deep(.el-select),
.incoming-dialog :deep(.el-input),
.incoming-dialog :deep(.el-textarea) {
  width: 100%;
}
.incoming-dialog :deep(.el-form-item) {
  margin-bottom: 12px;
}
.document-form-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  column-gap: 14px;
}
.grid-span-2 {
  grid-column: span 2;
}
.grid-span-4 {
  grid-column: 1 / -1;
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
.restore-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  color: #606266;
  font-size: 13px;
}
.restore-frame-wrap {
  max-height: 72vh;
  overflow: auto;
  padding: 12px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
}
.restore-frame {
  display: block;
  width: 100%;
  height: 72vh;
  margin: 0 auto;
  border: 0;
  background: #fff;
  box-shadow: 0 2px 12px rgba(15, 23, 42, .14);
}
.mapping-rule-form {
  margin-bottom: 8px;
  padding: 0 12px;
}
.mapping-rule-form :deep(.el-form-item) {
  margin-bottom: 8px;
}
.mapping-canvas {
  position: relative;
  display: grid;
  grid-template-columns: 320px minmax(80px, 1fr) minmax(620px, 760px);
  gap: 48px;
  min-height: 620px;
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
  pointer-events: stroke;
  stroke: #409eff;
  stroke-width: 2;
  cursor: pointer;
}
.mapping-lines line.selected {
  stroke: #f56c6c;
  stroke-width: 3;
}
.mapping-panel {
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}
.target-panel {
  grid-column: 1;
}
.source-panel {
  grid-column: 3;
  max-height: 68vh;
  overflow: auto;
  padding-right: 4px;
}
.mapping-title {
  color: #303133;
  font-weight: 600;
  text-align: center;
}
.ocr-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.ocr-group + .ocr-group {
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
}
.ocr-group-title {
  position: sticky;
  top: 0;
  z-index: 2;
  padding: 4px 0;
  color: #606266;
  font-size: 13px;
  font-weight: 600;
  background: #fff;
}
.field-node {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
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
.source-node {
  grid-template-columns: 76px minmax(0, 1fr);
}
.ocr-row {
  display: grid;
  grid-template-columns: 108px minmax(0, 1fr);
  gap: 8px;
  align-items: stretch;
}
.ocr-anchor {
  min-height: 50px;
  padding: 6px 8px;
  color: #303133;
  font-weight: 600;
  text-align: center;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  word-break: break-word;
}
.ocr-content {
  display: flex;
  min-width: 0;
  min-height: 50px;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  padding: 8px 10px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  cursor: pointer;
}
.ocr-row.mapped .ocr-anchor,
.ocr-row.selected .ocr-anchor {
  border-color: #409eff;
  background: #ecf5ff;
}
.ocr-row.selected .ocr-content {
  border-color: #409eff;
}
.ocr-content strong {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  white-space: normal;
  word-break: break-word;
}
.ocr-content small {
  min-width: 0;
  color: #909399;
  font-size: 12px;
  line-height: 16px;
  white-space: normal;
  word-break: break-word;
}
.field-code {
  overflow: hidden;
  color: #303133;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.field-content {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}
.field-content strong {
  overflow: hidden;
  color: #303133;
  font-size: 14px;
  font-weight: 500;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.field-node small {
  min-width: 0;
  overflow: hidden;
  color: #909399;
  font-size: 12px;
  line-height: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.field-node.selected,
.field-node.mapped {
  border-color: #409eff;
  background: #ecf5ff;
}
.mapping-hint {
  padding: 8px 10px;
  color: #606266;
  font-size: 12px;
  line-height: 18px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
</style>
