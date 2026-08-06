<template>
  <div class="container">
    <div class="handle-box">
      <el-input v-model="query.keyword" placeholder="搜索规则名称/模块/表单类型" clearable class="search-input" @keyup.enter="loadData" @clear="loadData"></el-input>
      <el-button :icon="Search" @click="loadData">搜索</el-button>
    </div>

    <el-table :data="tableData" border class="table" header-cell-class-name="table-header">
      <el-table-column prop="name" label="规则名称" min-width="180" show-overflow-tooltip></el-table-column>
      <el-table-column prop="module" label="模块" width="160"></el-table-column>
      <el-table-column prop="formType" label="表单类型" width="180"></el-table-column>
      <el-table-column label="映射关系" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">{{ mappingSummary(row.mappingJson) }}</template>
      </el-table-column>
      <el-table-column prop="createdBy" label="增加人" width="100"></el-table-column>
      <el-table-column prop="createdAt" label="增加时间" width="170"></el-table-column>
      <el-table-column prop="updatedBy" label="最后修改人" width="110"></el-table-column>
      <el-table-column prop="updatedAt" label="最后修改时间" width="170"></el-table-column>
      <el-table-column label="删除标志" width="90">
        <template #default="{ row }">{{ row.deleted ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button text :icon="View" @click="openDetail(row)" v-action:ocr-mapping-rule:view>查看</el-button>
          <el-button text :icon="Edit" @click="openEdit(row)" v-action:ocr-mapping-rule:update>编辑</el-button>
          <el-button text :icon="Delete" class="red" @click="removeRule(row)" v-action:ocr-mapping-rule:delete>删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination background layout="total, prev, pager, next" :current-page="query.pageIndex" :page-size="query.pageSize" :total="pageTotal" @current-change="handlePageChange"></el-pagination>
    </div>

    <el-dialog :title="readonly ? '查看映射规则' : '编辑映射规则'" v-model="dialogVisible" width="860px" top="5vh">
      <el-form label-width="92px" class="rule-form">
        <el-form-item label="规则名称" required>
          <el-input v-model="form.name" :disabled="readonly"></el-input>
        </el-form-item>
        <el-form-item label="模块">
          <el-input v-model="form.module" disabled></el-input>
        </el-form-item>
        <el-form-item label="表单类型">
          <el-input v-model="form.formType" disabled></el-input>
        </el-form-item>
        <el-form-item label="映射关系">
          <el-input v-model="form.mappingJson" type="textarea" :autosize="{minRows: 14, maxRows: 24}" :disabled="readonly"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button v-if="!readonly" type="primary" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {onMounted, reactive, ref} from 'vue';
import {ElMessage, ElMessageBox} from 'element-plus';
import {Delete, Edit, Search, View} from '@element-plus/icons-vue';
import {deleteOcrMappingRule, getOcrMappingRuleList, updateOcrMappingRule} from '../api/ocr-mapping-rule';

const query = reactive({pageIndex: 1, pageSize: 10, keyword: ''});
const tableData = ref<any[]>([]);
const pageTotal = ref(0);
const dialogVisible = ref(false);
const readonly = ref(true);
const form = reactive<any>({id: null, name: '', module: '', formType: '', mappingJson: ''});

async function loadData() {
  const res = await getOcrMappingRuleList({page: query.pageIndex, size: query.pageSize, keyword: query.keyword});
  tableData.value = res.data.list || [];
  pageTotal.value = res.data.total || 0;
}

function openDetail(row: any) {
  fillForm(row);
  readonly.value = true;
  dialogVisible.value = true;
}

function openEdit(row: any) {
  fillForm(row);
  readonly.value = false;
  dialogVisible.value = true;
}

function fillForm(row: any) {
  Object.assign(form, {
    id: row.id,
    name: row.name || '',
    module: row.module || '',
    formType: row.formType || '',
    mappingJson: formatJson(row.mappingJson || '[]')
  });
}

async function saveRule() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入规则名称');
    return;
  }
  let mappingJson = '[]';
  try {
    mappingJson = normalizeJson(form.mappingJson);
  } catch {
    ElMessage.error('映射关系不是合法 JSON');
    return;
  }
  await updateOcrMappingRule(form.id, {
    name: form.name.trim(),
    module: form.module,
    formType: form.formType,
    mappingJson
  });
  ElMessage.success('保存成功');
  dialogVisible.value = false;
  loadData();
}

async function removeRule(row: any) {
  try {
    await ElMessageBox.confirm('确认删除该映射规则？', '提示', {type: 'warning'});
    await deleteOcrMappingRule(row.id);
    ElMessage.success('删除成功');
    loadData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败');
  }
}

function handlePageChange(page: number) {
  query.pageIndex = page;
  loadData();
}

function mappingSummary(value: string) {
  try {
    const items = JSON.parse(value || '[]');
    if (!Array.isArray(items) || items.length === 0) return '-';
    return items.map((item: any) => `${item.targetField} <- ${item.sourceField}`).join('、');
  } catch {
    return '-';
  }
}

function formatJson(value: string) {
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function normalizeJson(value: string) {
  try {
    return JSON.stringify(JSON.parse(value || '[]'));
  } catch {
    throw new Error('映射关系不是合法 JSON');
  }
}

onMounted(loadData);
</script>

<style scoped>
.handle-box {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}
.search-input {
  width: 280px;
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
.rule-form :deep(.el-input),
.rule-form :deep(.el-textarea) {
  width: 100%;
}
</style>
