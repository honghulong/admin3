<template>
  <div>
    <div class="container">
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
    <el-dialog :title="isEdit ? '编辑报销' : '新建报销'" v-model="dialogVisible" width="50%">
      <el-form label-width="100px">
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
        <el-form-item label="报销说明">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入报销说明"></el-input>
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
    <el-dialog title="报销详情" v-model="detailVisible" width="55%">
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
          <el-descriptions-item label="说明" :span="2">{{ detailData.description || '无' }}</el-descriptions-item>
          <el-descriptions-item label="审批人" v-if="detailData.approverName">{{ detailData.approverName }}</el-descriptions-item>
          <el-descriptions-item label="审批时间" v-if="detailData.approveTime">{{ detailData.approveTime }}</el-descriptions-item>
          <el-descriptions-item label="审批意见" :span="2" v-if="detailData.approveComment">{{ detailData.approveComment }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin-top: 20px">附件</h4>
        <div v-if="detailData.attachments && detailData.attachments.length > 0">
          <el-tag v-for="att in detailData.attachments" :key="att.id" style="margin: 5px" closable @close="handleDeleteAttachment(att)">
            {{ att.fileName }}
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
import {CircleCheck, CloseBold, Delete, Edit, Plus, Refresh, Search, Upload, View} from '@element-plus/icons-vue';
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
  description: ''
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
  const params: any = {
    page: query.pageIndex - 1,
    size: query.pageSize
  };
  if (query.status) params.status = query.status;
  if (query.category) params.category = query.category;
  if (query.keyword) params.keyword = query.keyword;

  const res = await getReimbursementList(params);
  tableData.value = res.data.list;
  pageTotal.value = res.data.total;
}

async function handleDetail(row: TableItem) {
  const res = await getReimbursementDetail(row.id);
  detailData.value = res.data;
  detailVisible.value = true;
}

function handleAdd() {
  isEdit.value = false;
  editId.value = null;
  form.title = '';
  form.category = '';
  form.amount = 0.01;
  form.description = '';
  fileList.value = [];
  uploadedAttachmentIds.value = [];
  dialogVisible.value = true;
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
  fileList.value = [];
  uploadedAttachmentIds.value = [];
  if (data.attachments) {
    fileList.value = data.attachments.map((a: any) => ({name: a.fileName, url: a.fileUrl}));
    uploadedAttachmentIds.value = data.attachments.map((a: any) => a.id);
  }
  dialogVisible.value = true;
}

async function handleFileChange(uploadFile: any) {
  const formData = new FormData();
  formData.append('file', uploadFile.raw);
  try {
    const res = await uploadAttachment(uploadFile.raw);
    uploadedAttachmentIds.value.push(res.data.id);
    ElMessage.success('上传成功');
  } catch (e) {
    ElMessage.error('上传失败');
  }
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
    attachmentIds: uploadedAttachmentIds.value
  };
  try {
    if (isEdit.value && editId.value) {
      await updateReimbursement(editId.value, data);
      ElMessage.success('修改成功');
    } else {
      await createReimbursement(data);
      ElMessage.success('创建成功');
    }
    dialogVisible.value = false;
    loadData();
  } catch (e) {
    ElMessage.error('操作失败');
  }
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

async function handleDeleteAttachment(att: any) {
  try {
    await ElMessageBox.confirm('确认删除该附件？', '提示', {type: 'warning'});
    await deleteAttachment(att.id);
    ElMessage.success('删除成功');
    if (detailData.value) {
      const res = await getReimbursementDetail(detailData.value.id);
      detailData.value = res.data;
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败');
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
</style>