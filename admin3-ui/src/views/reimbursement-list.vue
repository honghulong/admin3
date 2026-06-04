<template>
  <div>
    <div class="container">
      <!-- DEBUG: reimbursement-list.vue 模板已渲染 -->
      <div class="handle-box">
        <el-button type="primary" :icon="Plus" @click="handleAdd" v-action:reimbursement:create>新建报销</el-button>
        <el-select v-model="query.status" placeholder="状态筛选" clearable style="width: 120px; margin-left: 10px" @change="handleSearch">
          <el-option label="草稿" value="draft"></el-option>
          <el-option label="待审批" value="pending"></el-option>
          <el-option label="已通过" value="approved"></el-option>
          <el-option label="已退回" value="rejected"></el-option>
          <el-option label="已撤回" value="recalled"></el-option>
        </el-select>
        <el-select v-model="query.category" placeholder="类别筛选" clearable style="width: 120px; margin-left: 10px" @change="handleSearch">
          <el-option label="差旅" value="travel"></el-option>
          <el-option label="办公" value="office"></el-option>
          <el-option label="交通" value="transport"></el-option>
          <el-option label="餐饮" value="catering"></el-option>
          <el-option label="其他" value="other"></el-option>
        </el-select>
        <el-input v-model="query.keyword" placeholder="搜索标题/说明" clearable style="width: 200px; margin-left: 10px" @clear="handleSearch" @keyup.enter="handleSearch"></el-input>
        <el-button :icon="Search" style="margin-left: 5px" @click="handleSearch">搜索</el-button>
      </div>
      <el-table :data="tableData" border class="table" header-cell-class-name="table-header">
        <el-table-column prop="id" label="ID" width="55" align="center"></el-table-column>
        <el-table-column prop="title" label="报销标题" min-width="150"></el-table-column>
        <el-table-column prop="category" label="类别" width="80" align="center">
          <template #default="{ row }">
            <span>{{ getCategoryLabel(row.category) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120" align="right">
          <template #default="{ row }">
            <span>¥{{ row.amount.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="100"></el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160"></el-table-column>
        <el-table-column label="操作" width="350" fixed="right">
          <template #default="scope">
            <el-button text :icon="View" @click="handleDetail(scope.row)" v-action:reimbursement:view>详情</el-button>
            <el-button text :icon="Edit" @click="handleEdit(scope.row)" v-if="scope.row.status==='draft' || scope.row.status==='rejected' || scope.row.status==='recalled'" v-action:reimbursement:update>编辑</el-button>
            <el-button text :icon="Upload" @click="handleSubmit(scope.row)" v-if="scope.row.status==='draft'" v-action:reimbursement:submit>提交</el-button>
            <el-button text :icon="CircleCheck" @click="handleApprove(scope.row)" v-if="scope.row.status==='pending'" v-action:reimbursement:approve>通过</el-button>
            <el-button text :icon="CloseBold" class="orange" @click="handleReject(scope.row)" v-if="scope.row.status==='pending'" v-action:reimbursement:approve>退回</el-button>
            <el-button text :icon="Refresh" @click="handleRecall(scope.row)" v-if="scope.row.status==='pending'" v-action:reimbursement:recall>撤回</el-button>
            <el-button text :icon="Delete" class="red" @click="handleDelete(scope.row)" v-if="scope.row.status==='draft'" v-action:reimbursement:delete>删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :current-page="query.pageIndex"
          :page-size="query.pageSize"
          :total="pageTotal"
          @current-change="handlePageChange"
        ></el-pagination>
      </div>
    </div>

    <!-- 新建/编辑对话框 -->
    <el-dialog :title="isEdit ? '编辑报销' : '新建报销'" v-model="dialogVisible" width="55%">
      <el-form label-width="110px">
        <el-form-item label="报销标题">
          <el-input v-model="form.title" placeholder="请输入报销标题"></el-input>
        </el-form-item>
        <el-form-item label="报销类别">
          <el-select v-model="form.category" placeholder="请选择类别" style="width: 100%">
            <el-option label="差旅" value="travel"></el-option>
            <el-option label="办公" value="office"></el-option>
            <el-option label="交通" value="transport"></el-option>
            <el-option label="餐饮" value="catering"></el-option>
            <el-option label="其他" value="other"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="报销金额">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" style="width: 100%"></el-input-number>
        </el-form-item>
        <el-form-item label="发票号码">
          <el-input v-model="form.invoiceNo" placeholder="发票号码"></el-input>
        </el-form-item>
        <el-form-item label="发票代码">
          <el-input v-model="form.invoiceCode" placeholder="发票代码"></el-input>
        </el-form-item>
        <el-form-item label="开票日期">
          <el-date-picker v-model="form.invoiceDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ss"></el-date-picker>
        </el-form-item>
        <el-form-item label="购买方名称">
          <el-input v-model="form.buyerName" placeholder="购买方名称"></el-input>
        </el-form-item>
        <el-form-item label="销售方名称">
          <el-input v-model="form.sellerName" placeholder="销售方名称"></el-input>
        </el-form-item>
        <el-form-item label="购买方税号">
          <el-input v-model="form.buyerTaxId" placeholder="购买方税号"></el-input>
        </el-form-item>
        <el-form-item label="销售方税号">
          <el-input v-model="form.sellerTaxId" placeholder="销售方税号"></el-input>
        </el-form-item>
        <el-form-item label="发票类型">
          <el-select v-model="form.invoiceType" placeholder="发票类型" clearable style="width: 100%">
            <el-option label="增值税专用发票" value="special"></el-option>
            <el-option label="增值税普通发票" value="normal"></el-option>
            <el-option label="电子发票" value="electronic"></el-option>
            <el-option label="定额发票" value="fixed"></el-option>
            <el-option label="机动车发票" value="vehicle"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="发票状态">
          <el-select v-model="form.invoiceStatus" placeholder="发票状态" clearable style="width: 100%">
            <el-option label="正常" value="normal"></el-option>
            <el-option label="作废" value="voided"></el-option>
            <el-option label="红冲" value="red"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="报销说明">
          <el-input v-model="form.description" type="textarea" :rows="5" placeholder="请输入报销说明"></el-input>
        </el-form-item>
        <el-form-item label="上传凭证">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :on-change="handleFileChange"
            :file-list="fileList"
            list-type="picture-card"
            accept=".jpg,.png,.gif,.webp,.pdf"
            :limit="9"
            multiple>
            <template #file="{ file }">
              <div>
                <img v-if="file.url" :src="file.url" class="el-upload-list__item-thumbnail" />
                <span class="el-upload-list__item-actions">
                  <span class="el-upload-list__item-preview" @click="handlePictureCardPreview(file)">
                    <el-icon><ZoomIn /></el-icon>
                  </span>
                  <span class="el-upload-list__item-delete" @click="handleRemoveUploadedFile(file)">
                    <el-icon><Delete /></el-icon>
                  </span>
                </span>
                <div v-if="file.ocrStatus === 'processing'" class="ocr-status processing">识别中...</div>
                <div v-else-if="file.ocrStatus === 'completed'" class="ocr-status completed">已识别</div>
                <div v-else-if="file.ocrStatus === 'failed'" class="ocr-status failed">识别失败</div>
                <div v-else-if="file.ocrStatus === 'pending'" class="ocr-status pending">待识别</div>
                <div v-if="file.ocrStatus === 'failed' && file.ocrResult" class="ocr-error" :title="getOcrError(file.ocrResult)">{{ getOcrError(file.ocrResult) }}</div>
              </div>
            </template>
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="saveReimbursement">保 存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog title="报销详情" v-model="detailVisible" width="60%">
      <div v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ detailData.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(detailData.status)">{{ getStatusLabel(detailData.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ detailData.title }}</el-descriptions-item>
          <el-descriptions-item label="类别">{{ getCategoryLabel(detailData.category) }}</el-descriptions-item>
          <el-descriptions-item label="金额">¥{{ detailData.amount.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detailData.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailData.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="发票号码">{{ detailData.invoiceNo || '无' }}</el-descriptions-item>
          <el-descriptions-item label="发票代码">{{ detailData.invoiceCode || '无' }}</el-descriptions-item>
          <el-descriptions-item label="开票日期">{{ detailData.invoiceDate || '无' }}</el-descriptions-item>
          <el-descriptions-item label="购买方名称">{{ detailData.buyerName || '无' }}</el-descriptions-item>
          <el-descriptions-item label="销售方名称">{{ detailData.sellerName || '无' }}</el-descriptions-item>
          <el-descriptions-item label="购买方税号">{{ detailData.buyerTaxId || '无' }}</el-descriptions-item>
          <el-descriptions-item label="销售方税号">{{ detailData.sellerTaxId || '无' }}</el-descriptions-item>
          <el-descriptions-item label="发票类型">{{ detailData.invoiceType || '无' }}</el-descriptions-item>
          <el-descriptions-item label="发票状态">{{ detailData.invoiceStatus || '无' }}</el-descriptions-item>
          <el-descriptions-item label="说明" :span="2">{{ detailData.description || '无' }}</el-descriptions-item>
          <el-descriptions-item label="审批人" v-if="detailData.approverName">{{ detailData.approverName }}</el-descriptions-item>
          <el-descriptions-item label="审批时间" v-if="detailData.approveTime">{{ detailData.approveTime }}</el-descriptions-item>
          <el-descriptions-item label="审批意见" :span="2" v-if="detailData.approveComment">{{ detailData.approveComment }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin-top: 20px">附件</h4>
        <div v-if="detailData.attachments && detailData.attachments.length > 0">
          <el-tag v-for="att in detailData.attachments" :key="att.id" style="margin: 5px" closable @close="handleDeleteAttachment(att)">
            {{ att.fileName }}
            <el-tag v-if="att.ocrStatus === 'pending'" size="small" type="info" style="margin-left: 4px">待识别</el-tag>
            <el-tag v-else-if="att.ocrStatus === 'processing'" size="small" type="warning" style="margin-left: 4px">识别中</el-tag>
            <el-tag v-else-if="att.ocrStatus === 'completed'" size="small" type="success" style="margin-left: 4px">已识别</el-tag>
            <el-tag v-else-if="att.ocrStatus === 'failed'" size="small" type="danger" style="margin-left: 4px">识别失败</el-tag>
          </el-tag>
        </div>
        <div v-else style="color: #999">暂无附件</div>

        <h4 style="margin-top: 20px">审批日志</h4>
        <el-timeline v-if="detailData.approvalLogs && detailData.approvalLogs.length > 0">
          <el-timeline-item
            v-for="log in detailData.approvalLogs"
            :key="log.id"
            :timestamp="log.createdAt"
            :type="getLogType(log.action)">
            <b>{{ log.operatorName }}</b>
            {{ getLogActionLabel(log.action) }}
            <span v-if="log.comment">：{{ log.comment }}</span>
          </el-timeline-item>
        </el-timeline>
        <div v-else style="color: #999">暂无审批日志</div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailVisible = false">关 闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 审批退回对话框 -->
    <el-dialog title="审批退回" v-model="rejectVisible" width="35%">
      <el-form label-width="80px">
        <el-form-item label="退回原因">
          <el-input v-model="rejectComment" type="textarea" :rows="4" placeholder="请输入退回原因（必填）"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="rejectVisible = false">取 消</el-button>
          <el-button type="danger" @click="confirmReject">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {reactive, ref} from 'vue';
import {ElMessage, ElMessageBox} from 'element-plus';
import {CircleCheck, CloseBold, Delete, Edit, Plus, Refresh, Search, Upload, View, ZoomIn} from '@element-plus/icons-vue';
import {
  getReimbursementList,
  createReimbursement,
  updateReimbursement,
  deleteReimbursement,
  submitReimbursement,
  approveReimbursement,
  rejectReimbursement,
  recallReimbursement,
  getReimbursementDetail,
  uploadAttachment,
  deleteAttachment
} from "../api/reimbursement";

interface TableItem {
  id: number;
  title: string;
  category: string;
  amount: number;
  description: string;
  status: string;
  applicantId: number;
  applicantName: string;
  approverId: number;
  approverName: string;
  approveComment: string;
  approveTime: string;
  createdAt: string;
  updatedAt: string;
  attachments: any[];
  approvalLogs: any[];
}

interface DetailData extends TableItem {
  attachments: any[];
  approvalLogs: any[];
}

const tableData = ref<TableItem[]>([]);
const pageTotal = ref(0);
const dialogVisible = ref(false);
const detailVisible = ref(false);
const rejectVisible = ref(false);
const isEdit = ref(false);
const editId = ref<number | null>(null);
const detailData = ref<DetailData | null>(null);
const rejectComment = ref('');
const rejectId = ref<number | null>(null);
const fileList = ref<any[]>([]);
const uploadedAttachmentIds = ref<number[]>([]);

const query = reactive({
  pageIndex: 1,
  pageSize: 10,
  status: '',
  category: '',
  keyword: ''
});

const form = reactive({
  title: '',
  category: '',
  amount: 0.01,
  description: '',
  invoiceNo: '',
  invoiceCode: '',
  invoiceDate: '',
  buyerName: '',
  sellerName: '',
  buyerTaxId: '',
  sellerTaxId: '',
  invoiceType: '',
  invoiceStatus: ''
});

const categoryMap: Record<string, string> = {
  travel: '差旅',
  office: '办公',
  transport: '交通',
  catering: '餐饮',
  other: '其他'
};

const statusMap: Record<string, string> = {
  draft: '草稿',
  pending: '待审批',
  approved: '已通过',
  rejected: '已退回',
  recalled: '已撤回'
};

const statusTypeMap: Record<string, string> = {
  draft: 'info',
  pending: 'warning',
  approved: 'success',
  rejected: 'danger',
  recalled: ''
};

const logActionMap: Record<string, string> = {
  submit: '提交审批',
  approve: '审批通过',
  reject: '审批退回',
  recall: '撤回申请'
};

function getCategoryLabel(category: string) {
  return categoryMap[category] || category;
}

function getStatusLabel(status: string) {
  return statusMap[status] || status;
}

function getStatusType(status: string) {
  return statusTypeMap[status] || 'info';
}

function getLogActionLabel(action: string) {
  return logActionMap[action] || action;
}

function getLogType(action: string) {
  if (action === 'approve') return 'success';
  if (action === 'reject') return 'danger';
  if (action === 'recall') return 'warning';
  return 'primary';
}

function handleSearch() {
  query.pageIndex = 1;
  loadData();
}

function handlePageChange(val: number) {
  query.pageIndex = val;
  loadData();
}

async function loadData() {
  console.log('[DEBUG] loadData called, page:', query.pageIndex, 'size:', query.pageSize);
  const params: any = {
    page: query.pageIndex,
    size: query.pageSize
  };
  if (query.status) params.status = query.status;
  if (query.category) params.category = query.category;
  if (query.keyword) params.keyword = query.keyword;

  try {
    const res = await getReimbursementList(params);
    console.log('[DEBUG] loadData response:', res);
    tableData.value = res.data.list;
    pageTotal.value = res.data.total;
    console.log('[DEBUG] tableData updated, count:', tableData.value.length);
  } catch (e) {
    console.error('[DEBUG] loadData error:', e);
  }
}

async function handleDetail(row: TableItem) {
  const res = await getReimbursementDetail(row.id);
  detailData.value = res.data;
  detailVisible.value = true;
}

async function handleAdd() {
  isEdit.value = false;
  editId.value = null;
  form.title = '';
  form.category = '';
  form.amount = 0.01;
  form.description = '';
  form.invoiceNo = '';
  form.invoiceCode = '';
  form.invoiceDate = '';
  form.buyerName = '';
  form.sellerName = '';
  form.buyerTaxId = '';
  form.sellerTaxId = '';
  form.invoiceType = '';
  form.invoiceStatus = '';
  fileList.value = [];
  uploadedAttachmentIds.value = [];
  dialogVisible.value = true;
  // 自动创建一个草稿报销单，这样上传附件时就有 reimbursementId
  // OCR 完成后后端会自动更新字段，即使用户关掉页面数据也不会丢
  try {
    const res = await createReimbursement({
      title: '新建报销单',
      category: 'other',
      amount: 0.01,
      description: '',
      attachmentIds: []
    });
    editId.value = res.data.id;
    isEdit.value = true;
    console.log('Draft reimbursement created:', editId.value);
  } catch (e: any) {
    console.error('Failed to create draft:', e);
  }
}

async function handleEdit(row: TableItem) {
  isEdit.value = true;
  editId.value = row.id;
  const res = await getReimbursementDetail(row.id);
  const data = res.data;
  form.title = data.title;
  form.category = data.category;
  form.amount = data.amount;
  form.description = data.description || '';
  form.invoiceNo = data.invoiceNo || '';
  form.invoiceCode = data.invoiceCode || '';
  form.invoiceDate = data.invoiceDate || '';
  form.buyerName = data.buyerName || '';
  form.sellerName = data.sellerName || '';
  form.buyerTaxId = data.buyerTaxId || '';
  form.sellerTaxId = data.sellerTaxId || '';
  form.invoiceType = data.invoiceType || '';
  form.invoiceStatus = data.invoiceStatus || '';
  fileList.value = [];
  uploadedAttachmentIds.value = [];
  if (data.attachments) {
    fileList.value = data.attachments.map((a: any) => ({name: a.fileName, url: '/admin3' + a.fileUrl, id: a.id, ocrStatus: a.ocrStatus}));
    uploadedAttachmentIds.value = data.attachments.map((a: any) => a.id);
  }
  dialogVisible.value = true;
}

async function handleFileChange(uploadFile: any) {
  const reimbursementId = editId.value;
  if (!reimbursementId) {
    ElMessage.warning('请先保存报销单再上传附件');
    return;
  }
  try {
    console.log('Uploading file:', uploadFile.raw.name, 'reimbursementId:', reimbursementId);
    const res = await uploadAttachment(uploadFile.raw, reimbursementId);
    const att = res.data;
    console.log('Upload success, attachment:', att);
    uploadedAttachmentIds.value.push(att.id);
    // 更新 fileList 显示缩略图
    fileList.value.push({
      name: att.fileName,
      url: '/admin3' + att.fileUrl,
      id: att.id,
      ocrStatus: att.ocrStatus || 'pending'
    });
    ElMessage.success('上传成功，正在识别中...');
    // 启动 OCR 轮询
    startOcrPolling();
  } catch (e: any) {
    console.error('Upload failed:', e);
    ElMessage.error('上传失败: ' + (e.message || ''));
  }
}

/** OCR 轮询定时器 */
let ocrPollingTimer: any = null;

/** 启动 OCR 轮询：每 2 秒检查一次附件 OCR 状态 */
function startOcrPolling() {
  const id = editId.value;
  if (!id) return;
  // 清除旧定时器
  if (ocrPollingTimer) clearInterval(ocrPollingTimer);
  let attempts = 0;
  const maxAttempts = 30;
  ocrPollingTimer = setInterval(async () => {
    attempts++;
    try {
      const res = await getReimbursementDetail(id);
      const attachments = res.data.attachments || [];
      // 更新 fileList 中的 OCR 状态
      for (const att of attachments) {
        const item = fileList.value.find(f => f.id === att.id);
        if (item) {
          item.ocrStatus = att.ocrStatus;
          item.ocrResult = att.ocrResult;
        }
      }
      // 检查是否所有附件都已完成
      const pending = attachments.filter((a: any) => a.ocrStatus === 'pending' || a.ocrStatus === 'processing');
      if (pending.length === 0) {
        clearInterval(ocrPollingTimer);
        ocrPollingTimer = null;
        // 处理每个附件的结果
        for (const att of attachments) {
          if (att.ocrStatus === 'completed' && att.ocrResult) {
            handleOcrSuccess(att);
          } else if (att.ocrStatus === 'failed') {
            console.warn('OCR failed for attachment:', att.id, att.fileName, att.ocrResult);
          }
        }
        const failed = attachments.filter((a: any) => a.ocrStatus === 'failed');
        if (failed.length > 0) {
          ElMessage.warning(`有 ${failed.length} 个附件识别失败，请查看详情`);
        } else {
          ElMessage.success('发票识别完成，已自动填充字段');
        }
      }
    } catch (e) {
      console.error('OCR polling error:', e);
    }
    if (attempts >= maxAttempts) {
      clearInterval(ocrPollingTimer);
      ocrPollingTimer = null;
      ElMessage.warning('OCR 识别超时，请稍后刷新查看');
    }
  }, 2000);
}

/** OCR 识别成功：解析结果并填充表单字段 */
function handleOcrSuccess(att: any) {
  try {
    const result = JSON.parse(att.ocrResult);
    console.log('OCR result for attachment', att.id, ':', result);
    // 尝试从 result.content[0].text 或直接 result 中提取
    let ocrText = '';
    let invoiceFields: any = null;
    if (result.content && result.content[0] && result.content[0].text) {
      // MCP 响应格式
      const innerText = result.content[0].text;
      try {
        const inner = JSON.parse(innerText);
        ocrText = inner.ocr_text || '';
        invoiceFields = inner.invoice_fields || null;
      } catch {
        ocrText = innerText;
      }
    } else {
      ocrText = result.ocr_text || '';
      invoiceFields = result.invoice_fields || null;
    }
    // 提取字段
    const fields: any = {};
    if (invoiceFields) {
      if (invoiceFields.invoice_no) fields.invoiceNo = invoiceFields.invoice_no;
      if (invoiceFields.invoice_code) fields.invoiceCode = invoiceFields.invoice_code;
      if (invoiceFields.total_amount) fields.amount = parseFloat(invoiceFields.total_amount) || form.amount;
      if (invoiceFields.date) fields.invoiceDate = parseOcrDate(invoiceFields.date);
      if (invoiceFields.buyer_name) fields.buyerName = invoiceFields.buyer_name;
      if (invoiceFields.seller_name) fields.sellerName = invoiceFields.seller_name;
      if (invoiceFields.buyer_tax_id) fields.buyerTaxId = invoiceFields.buyer_tax_id;
      if (invoiceFields.seller_tax_id) fields.sellerTaxId = invoiceFields.seller_tax_id;
      if (invoiceFields.invoice_type) fields.invoiceType = mapInvoiceType(invoiceFields.invoice_type);
    }
    // 从 ocr_text 正则提取补充
    if (ocrText) {
      if (!fields.invoiceNo) {
        const m = ocrText.match(/发票号码[：:\s]*(\d{8})/);
        if (m) fields.invoiceNo = m[1];
      }
      if (!fields.invoiceCode) {
        const m = ocrText.match(/发票代码[：:\s]*(\d{12})/);
        if (m) fields.invoiceCode = m[1];
      }
      if (!fields.buyerName && ocrText.includes('购买方')) {
        const idx = ocrText.indexOf('购买方');
        const after = ocrText.substring(idx);
        const m = after.match(/称[：:\s]*([^\n]{2,50})/);
        if (m) fields.buyerName = m[1].trim();
      }
      if (!fields.sellerName && ocrText.includes('销售方')) {
        const idx = ocrText.indexOf('销售方');
        const after = ocrText.substring(idx);
        const m = after.match(/称[：:\s]*([^\n]{2,50})/);
        if (m) fields.sellerName = m[1].trim();
      }
      if (!fields.invoiceType) {
        if (ocrText.includes('增值税专用发票')) fields.invoiceType = 'special';
        else if (ocrText.includes('增值税普通发票')) fields.invoiceType = 'normal';
        else if (ocrText.includes('电子发票')) fields.invoiceType = 'electronic';
      }
    }
    // 填充表单
    if (fields.invoiceNo) form.invoiceNo = fields.invoiceNo;
    if (fields.invoiceCode) form.invoiceCode = fields.invoiceCode;
    if (fields.amount) form.amount = fields.amount;
    if (fields.invoiceDate) form.invoiceDate = fields.invoiceDate;
    if (fields.buyerName) form.buyerName = fields.buyerName;
    if (fields.sellerName) form.sellerName = fields.sellerName;
    if (fields.buyerTaxId) form.buyerTaxId = fields.buyerTaxId;
    if (fields.sellerTaxId) form.sellerTaxId = fields.sellerTaxId;
    if (fields.invoiceType) form.invoiceType = fields.invoiceType;
    // 更新说明
    const descParts: string[] = [];
    if (fields.invoiceNo) descParts.push(`发票号码: ${fields.invoiceNo}`);
    if (fields.invoiceCode) descParts.push(`发票代码: ${fields.invoiceCode}`);
    if (fields.sellerName) descParts.push(`销售方: ${fields.sellerName}`);
    if (fields.amount) descParts.push(`金额: ¥${fields.amount}`);
    if (descParts.length > 0) {
      form.description = descParts.join('\n');
    }
    console.log('Form auto-filled from OCR:', fields);
  } catch (e) {
    console.error('Failed to parse OCR result:', e, att.ocrResult);
  }
}

/** 解析 OCR 日期格式 */
function parseOcrDate(dateStr: string): string {
  // "2024年1月15日" -> "2024-01-15T00:00:00"
  const m = dateStr.match(/(\d{4})年(\d{1,2})月(\d{1,2})日/);
  if (m) {
    const y = m[1], mo = m[2].padStart(2, '0'), d = m[3].padStart(2, '0');
    return `${y}-${mo}-${d}T00:00:00`;
  }
  return dateStr;
}

/** 从 OCR 结果中提取错误信息 */
function getOcrError(ocrResult: string): string {
  try {
    const result = JSON.parse(ocrResult);
    // 尝试从不同格式中提取错误
    if (result.error) return result.error.length > 30 ? result.error.substring(0, 30) + '...' : result.error;
    if (result.content && result.content[0] && result.content[0].text) {
      const text = result.content[0].text;
      if (text.includes('error') || text.includes('失败')) {
        return text.length > 30 ? text.substring(0, 30) + '...' : text;
      }
    }
    return '识别失败';
  } catch {
    return ocrResult.length > 30 ? ocrResult.substring(0, 30) + '...' : ocrResult;
  }
}

/** 映射发票类型 */
function mapInvoiceType(type: string): string {
  const map: Record<string, string> = {
    '增值税专用发票': 'special',
    '增值税普通发票': 'normal',
    '电子发票': 'electronic',
    '电子普通发票': 'electronic',
    '定额发票': 'fixed',
    '机动车销售统一发票': 'vehicle',
    'special': 'special',
    'normal': 'normal',
    'electronic': 'electronic',
    'fixed': 'fixed',
    'vehicle': 'vehicle'
  };
  return map[type] || type;
}

async function saveReimbursement() {
  if (!form.title || !form.category || !form.amount) {
    ElMessage.warning('请填写完整信息');
    return;
  }
  const data = {
    title: form.title,
    category: form.category,
    amount: form.amount,
    description: form.description,
    invoiceNo: form.invoiceNo || undefined,
    invoiceCode: form.invoiceCode || undefined,
    invoiceDate: form.invoiceDate || undefined,
    buyerName: form.buyerName || undefined,
    sellerName: form.sellerName || undefined,
    buyerTaxId: form.buyerTaxId || undefined,
    sellerTaxId: form.sellerTaxId || undefined,
    invoiceType: form.invoiceType || undefined,
    invoiceStatus: form.invoiceStatus || undefined,
    attachmentIds: uploadedAttachmentIds.value
  };
  try {
    if (editId.value) {
      await updateReimbursement(editId.value, data);
      ElMessage.success('保存成功');
    } else {
      const res = await createReimbursement(data);
      editId.value = res.data.id;
      isEdit.value = true;
      ElMessage.success('创建成功');
    }

    // 停止编辑界面的定时轮询
    if (ocrPollingTimer) {
      clearInterval(ocrPollingTimer);
      ocrPollingTimer = null;
    }
    // 如果有附件正在 OCR，同步轮询等待结果
    if (uploadedAttachmentIds.value.length > 0) {
      await pollOcrResults();
    }

    dialogVisible.value = false;
    loadData();
  } catch (e: any) {
    console.error('Save failed:', e);
    ElMessage.error('操作失败: ' + (e.message || ''));
  }
}

/** 轮询附件 OCR 状态，直到所有附件识别完成或失败（保存时同步等待） */
async function pollOcrResults() {
  const id = editId.value;
  if (!id) return;
  const maxAttempts = 30; // 最多等 60 秒
  for (let i = 0; i < maxAttempts; i++) {
    const res = await getReimbursementDetail(id);
    const attachments = res.data.attachments || [];
    // 更新 fileList 中的 OCR 状态
    for (const att of attachments) {
      const item = fileList.value.find(f => f.id === att.id);
      if (item) {
        item.ocrStatus = att.ocrStatus;
        item.ocrResult = att.ocrResult;
      }
    }
    const pending = attachments.filter((a: any) => a.ocrStatus === 'pending' || a.ocrStatus === 'processing');
    if (pending.length === 0) {
      // 所有附件 OCR 完成
      for (const att of attachments) {
        if (att.ocrStatus === 'completed' && att.ocrResult) {
          handleOcrSuccess(att);
        }
      }
      const failed = attachments.filter((a: any) => a.ocrStatus === 'failed');
      if (failed.length > 0) {
        ElMessage.warning(`有 ${failed.length} 个附件识别失败`);
      } else {
        ElMessage.success('所有附件识别完成');
      }
      return;
    }
    console.log(`OCR polling (${i + 1}/${maxAttempts}): ${pending.length} attachments pending`);
    // 等待 2 秒
    await new Promise(resolve => setTimeout(resolve, 2000));
  }
  ElMessage.warning('OCR 识别超时，请稍后查看详情');
}

async function handleSubmit(row: TableItem) {
  try {
    await ElMessageBox.confirm('确认提交该报销单？', '提示');
    await submitReimbursement(row.id);
    ElMessage.success('提交成功');
    loadData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('提交失败');
  }
}

async function handleApprove(row: TableItem) {
  try {
    await ElMessageBox.confirm('确认审批通过？', '提示');
    await approveReimbursement(row.id);
    ElMessage.success('审批通过');
    loadData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败');
  }
}

function handleReject(row: TableItem) {
  rejectId.value = row.id;
  rejectComment.value = '';
  rejectVisible.value = true;
}

async function confirmReject() {
  if (!rejectComment.value) {
    ElMessage.warning('请输入退回原因');
    return;
  }
  try {
    await rejectReimbursement(rejectId.value!, {comment: rejectComment.value});
    ElMessage.success('已退回');
    rejectVisible.value = false;
    loadData();
  } catch (e) {
    ElMessage.error('操作失败');
  }
}

async function handleRecall(row: TableItem) {
  try {
    await ElMessageBox.confirm('确认撤回该报销单？', '提示');
    await recallReimbursement(row.id);
    ElMessage.success('已撤回');
    loadData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败');
  }
}

async function handleDelete(row: TableItem) {
  try {
    await ElMessageBox.confirm('确认删除该报销单？', '提示', {type: 'warning'});
    await deleteReimbursement(row.id);
    ElMessage.success('删除成功');
    loadData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败');
  }
}

/** 预览附件图片 */
function handlePictureCardPreview(file: any) {
  window.open(file.url, '_blank');
}

/** 从上传列表中移除（不删除后端文件） */
function handleRemoveUploadedFile(file: any) {
  fileList.value = fileList.value.filter(f => f.uid !== file.uid && f.id !== file.id);
  if (file.id) {
    uploadedAttachmentIds.value = uploadedAttachmentIds.value.filter(id => id !== file.id);
  }
}

async function handleDeleteAttachment(att: any) {
  try {
    await ElMessageBox.confirm('确认删除该附件？', '提示', {type: 'warning'});
    console.log('Deleting attachment:', att.id, att.name);
    await deleteAttachment(att.id);
    ElMessage.success('删除成功');
    // 从 fileList 中移除
    fileList.value = fileList.value.filter(f => f.id !== att.id);
    uploadedAttachmentIds.value = uploadedAttachmentIds.value.filter(id => id !== att.id);
    // 如果详情对话框打开，刷新详情
    if (detailData.value) {
      const res = await getReimbursementDetail(detailData.value.id);
      detailData.value = res.data;
    }
  } catch (e: any) {
    if (e !== 'cancel') {
      console.error('Delete attachment failed:', e);
      ElMessage.error('删除失败: ' + (e.message || ''));
    }
  }
}

loadData();
</script>

<style scoped>
.handle-box {
  margin-bottom: 20px;
}
.table {
  width: 100%;
  font-size: 14px;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.orange {
  color: #e6a23c;
}
.red {
  color: #f56c6c;
}
.ocr-status {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 12px;
  padding: 2px 0;
  color: #fff;
  z-index: 1;
}
.el-upload-list__item {
  position: relative !important;
}
.ocr-status.processing {
  background: rgba(230, 162, 60, 0.8);
}
.ocr-status.completed {
  background: rgba(103, 194, 58, 0.8);
}
.ocr-status.failed {
  background: rgba(245, 108, 108, 0.8);
}
.ocr-status.pending {
  background: rgba(144, 147, 153, 0.8);
}
.ocr-error {
  position: absolute;
  bottom: 22px;
  left: 0;
  right: 0;
  font-size: 10px;
  color: #f56c6c;
  text-align: center;
  padding: 1px 2px;
  background: rgba(255, 255, 255, 0.9);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  z-index: 1;
}
</style>