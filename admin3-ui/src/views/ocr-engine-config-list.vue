<template>
  <div>
    <div class="container">
      <div class="handle-box">
        <el-button type="primary" :icon="Plus" @click="handleAdd" v-action:[OCR_CREATE]>
          新增 OCR 引擎
        </el-button>
      </div>
      <el-table :data="tableData" border class="table" header-cell-class-name="table-header">
        <el-table-column prop="name" label="名称" min-width="140"></el-table-column>
        <el-table-column prop="providerType" label="引擎类型" width="150">
          <template #default="{ row }">
            <el-tag>{{ row.providerType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="protocol" label="协议" width="150"></el-table-column>
        <el-table-column prop="modelName" label="模型" min-width="140"></el-table-column>
        <el-table-column prop="baseUrl" label="Base URL" min-width="240" show-overflow-tooltip></el-table-column>
        <el-table-column prop="prompt" label="提示词" min-width="260" show-overflow-tooltip></el-table-column>
        <el-table-column label="API Key" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.apiKeyConfigured" type="success">已配置</el-tag>
            <el-tag v-else type="info">未配置</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.enabled" type="success">启用</el-tag>
            <el-tag v-else type="info">停用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前使用" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.enabled" effect="dark">使用中</el-tag>
            <el-popconfirm v-else title="切换为该 OCR 引擎?" @confirm="handleMarkAsDefault(row)">
              <template #reference>
                <el-button link v-action:[OCR_MARK_AS_DEFAULT]>使用此引擎</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button-group>
              <el-button text :icon="Edit" @click="handleEdit(row)" v-action:[OCR_UPDATE]>编辑</el-button>
              <el-button text :icon="Delete" class="red" @click="handleDelete(row)" v-action:[OCR_DELETE]>删除</el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="720px">
      <el-form label-width="110px">
        <el-form-item label="名称">
          <el-input v-model="form.name"/>
        </el-form-item>
        <el-form-item label="引擎类型">
          <el-select v-model="form.providerType" style="width: 100%" @change="handleProviderChange">
            <el-option label="内部 OCR" value="INTERNAL"/>
            <el-option label="通义千问 Qwen" value="QWEN"/>
            <el-option label="Kimi" value="KIMI"/>
            <el-option label="OpenAI 兼容" value="OPENAI_COMPATIBLE"/>
          </el-select>
        </el-form-item>
        <el-form-item label="协议">
          <el-select v-model="form.protocol" style="width: 100%">
            <el-option label="内部 HTTP" value="INTERNAL_HTTP"/>
            <el-option label="OpenAI 兼容" value="OPENAI_COMPATIBLE"/>
          </el-select>
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input v-model="form.baseUrl" placeholder="例如 http://localhost:8765 或 https://.../v1"/>
        </el-form-item>
        <el-form-item label="模型">
          <el-input v-model="form.modelName" placeholder="例如 qwen3.5-ocr、kimi-k2.5"/>
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="留空表示不修改已保存密钥"/>
        </el-form-item>
        <el-form-item label="环境">
          <el-input v-model="form.environment" placeholder="例如 prod、test"/>
        </el-form-item>
        <el-form-item label="租户">
          <el-input v-model="form.tenantId" placeholder="可选"/>
        </el-form-item>
        <el-form-item label="提示词">
          <el-input
            v-model="form.prompt"
            type="textarea"
            :rows="12"
            placeholder="留空使用系统默认 OCR 提取提示词"
          />
        </el-form-item>
        <el-form-item label="选项">
          <el-checkbox v-model="form.enabled">启用</el-checkbox>
          <el-checkbox v-model="form.fallbackEnabled">失败回退内部 OCR</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, reactive, ref} from 'vue';
import {Delete, Edit, Plus} from '@element-plus/icons-vue';
import {ElMessage, ElMessageBox} from 'element-plus';
import {
  createOcrEngineConfig,
  deleteOcrEngineConfig,
  getOcrEngineConfigList,
  markAsDefaultOcrEngineConfig,
  updateOcrEngineConfig
} from '../api/ocr-engine-config';
import type {OcrEngineConfig} from '../api/ocr-engine-config';

const OCR_CREATE = 'ocr-engine:create';
const OCR_UPDATE = 'ocr-engine:update';
const OCR_DELETE = 'ocr-engine:delete';
const OCR_MARK_AS_DEFAULT = 'ocr-engine:markAsDefault';

class OcrEngineConfigImpl implements OcrEngineConfig {
  id: number | null = null;
  name = '';
  providerType = 'INTERNAL';
  baseUrl = 'http://localhost:8765';
  modelName = '';
  apiKey = '';
  protocol = 'INTERNAL_HTTP';
  tenantId = '';
  environment = 'default';
  prompt = '';
  enabled = true;
  isDefault = false;
  fallbackEnabled = true;
}

const tableData = ref<OcrEngineConfig[]>([]);
const dialogVisible = ref(false);
const editing = ref(false);
const form = reactive<OcrEngineConfig>(new OcrEngineConfigImpl());

const dialogTitle = computed(() => editing.value ? '编辑 OCR 引擎' : '新增 OCR 引擎');

const resetForm = () => Object.assign(form, new OcrEngineConfigImpl());

const getData = () => {
  getOcrEngineConfigList().then(res => {
    tableData.value = res.data;
  });
};

const handleAdd = () => {
  editing.value = false;
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (record: OcrEngineConfig) => {
  editing.value = true;
  Object.assign(form, record, {apiKey: ''});
  dialogVisible.value = true;
};

const handleProviderChange = () => {
  if (form.providerType === 'INTERNAL') {
    form.protocol = 'INTERNAL_HTTP';
    form.baseUrl = form.baseUrl || 'http://localhost:8765';
    form.modelName = '';
    return;
  }
  form.protocol = 'OPENAI_COMPATIBLE';
};

const handleSubmit = () => {
  if (editing.value && form.id) {
    updateOcrEngineConfig(form.id, form).then(() => {
      ElMessage.success('修改成功');
      dialogVisible.value = false;
      getData();
    });
    return;
  }
  createOcrEngineConfig(form).then(() => {
    ElMessage.success('新增成功');
    dialogVisible.value = false;
    getData();
  });
};

const handleDelete = (record: OcrEngineConfig) => {
  ElMessageBox.confirm('确定要删除该 OCR 引擎配置吗?', '提示', {
    type: 'warning'
  }).then(() => {
    deleteOcrEngineConfig(record.id || -1).then(() => {
      ElMessage.success('删除成功');
      getData();
    });
  }).catch(() => {});
};

const handleMarkAsDefault = (record: OcrEngineConfig) => {
  markAsDefaultOcrEngineConfig(record.id || -1).then(() => {
    ElMessage.success('设置成功');
    getData();
  });
};

getData();
</script>

<style scoped>
.handle-box {
  margin-bottom: 20px;
}

.table {
  width: 100%;
  font-size: 14px;
}
</style>
