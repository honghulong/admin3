<template>
  <div>
    <div class="container">
      <div class="handle-box">
        <el-button type="primary" :icon="Plus" @click="handleAddDict" v-action:dict:create>
          新增字典
        </el-button>
      </div>
      <el-table :data="dictTableData" border class="table" header-cell-class-name="table-header" @row-click="handleRowClick">
        <el-table-column prop="dictCode" label="字典编码" width="180"></el-table-column>
        <el-table-column prop="dictName" label="字典名称" width="180"></el-table-column>
        <el-table-column prop="description" label="描述"></el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button-group>
              <el-button text :icon="Edit" @click.stop="handleEditDict(scope.row)" v-action:dict:update>
                编辑
              </el-button>
              <el-button text :icon="Delete" class="red" @click.stop="handleDeleteDict(scope.row)" v-action:dict:delete>
                删除
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog title="新增字典" v-model="addDictVisible" width="40%">
      <el-form label-width="100px">
        <el-form-item label="字典编码">
          <el-input v-model="dictForm.dictCode" placeholder="如：leave_type"/>
        </el-form-item>
        <el-form-item label="字典名称">
          <el-input v-model="dictForm.dictName" placeholder="如：请假类型"/>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="dictForm.description" type="textarea" :rows="3"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addDictVisible = false">取 消</el-button>
          <el-button type="primary" @click="handleCreateDict">确 定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog title="编辑字典" v-model="editDictVisible" width="40%">
      <el-form label-width="100px">
        <el-form-item label="字典编码">
          <el-input v-model="dictForm.dictCode"/>
        </el-form-item>
        <el-form-item label="字典名称">
          <el-input v-model="dictForm.dictName"/>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="dictForm.description" type="textarea" :rows="3"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDictVisible = false">取 消</el-button>
          <el-button type="primary" @click="handleUpdateDict">确 定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog title="字典项管理" v-model="valueVisible" width="55%" :close-on-click-modal="false">
      <div style="margin-bottom: 15px; font-weight: bold; color: #409eff;">
        当前字典：{{ currentDict?.dictName }} ({{ currentDict?.dictCode }})
      </div>
      <div class="handle-box" style="margin-bottom: 10px;">
        <el-button type="primary" :icon="Plus" size="small" @click="handleAddValue" v-action:dict:create>
          新增字典项
        </el-button>
      </div>
      <el-table :data="valueTableData" border size="small" header-cell-class-name="table-header">
        <el-table-column prop="label" label="标签" width="150"></el-table-column>
        <el-table-column prop="value" label="值" width="150"></el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80"></el-table-column>
        <el-table-column prop="description" label="描述"></el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="scope">
            <el-button-group>
              <el-button text :icon="Edit" size="small" @click="handleEditValue(scope.row)" v-action:dict:update>
                编辑
              </el-button>
              <el-button text :icon="Delete" class="red" size="small" @click="handleDeleteValue(scope.row)" v-action:dict:delete>
                删除
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog :title="valueForm.id ? '编辑字典项' : '新增字典项'" v-model="addValueVisible" width="40%">
      <el-form label-width="100px">
        <el-form-item label="标签">
          <el-input v-model="valueForm.label" placeholder="如：病假"/>
        </el-form-item>
        <el-form-item label="值">
          <el-input v-model="valueForm.value" placeholder="如：sick"/>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="valueForm.sortOrder" :min="1"/>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="valueForm.description" type="textarea" :rows="2"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addValueVisible = false">取 消</el-button>
          <el-button type="primary" @click="handleSaveValue">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {reactive, ref} from 'vue';
import {Delete, Edit, Plus} from '@element-plus/icons-vue';
import {
  createDict,
  createDictValue,
  deleteDict,
  deleteDictValue,
  getDictList,
  getDictValues,
  updateDict,
  updateDictValue
} from "../api/dict";
import {ElMessage, ElMessageBox} from "element-plus";

interface DictForm {
  id?: number | null;
  dictCode: string;
  dictName: string;
  description?: string;
}

interface ValueForm {
  id?: number | null;
  label: string;
  value: string;
  sortOrder: number;
  description?: string;
}

const dictTableData = ref<any[]>([]);
const valueTableData = ref<any[]>([]);
const currentDict = ref<any>(null);

const addDictVisible = ref(false);
const editDictVisible = ref(false);
const valueVisible = ref(false);
const addValueVisible = ref(false);

const dictForm = reactive<DictForm>({
  id: null,
  dictCode: '',
  dictName: '',
  description: ''
});

const valueForm = reactive<ValueForm>({
  id: null,
  label: '',
  value: '',
  sortOrder: 1,
  description: ''
});

const getData = () => {
  getDictList().then(res => {
    dictTableData.value = res.data;
  });
};
getData();

const resetDictForm = () => {
  dictForm.id = null;
  dictForm.dictCode = '';
  dictForm.dictName = '';
  dictForm.description = '';
};

const resetValueForm = () => {
  valueForm.id = null;
  valueForm.label = '';
  valueForm.value = '';
  valueForm.sortOrder = 1;
  valueForm.description = '';
};

const handleAddDict = () => {
  resetDictForm();
  addDictVisible.value = true;
};

const handleEditDict = (row: any) => {
  dictForm.id = row.id;
  dictForm.dictCode = row.dictCode;
  dictForm.dictName = row.dictName;
  dictForm.description = row.description;
  editDictVisible.value = true;
};

const handleCreateDict = () => {
  createDict({
    dictCode: dictForm.dictCode,
    dictName: dictForm.dictName,
    description: dictForm.description
  }).then(() => {
    getData();
    ElMessage.success('新增成功');
    addDictVisible.value = false;
  });
};

const handleUpdateDict = () => {
  updateDict(dictForm.id!, {
    dictCode: dictForm.dictCode,
    dictName: dictForm.dictName,
    description: dictForm.description
  }).then(() => {
    getData();
    ElMessage.success('修改成功');
    editDictVisible.value = false;
  });
};

const handleDeleteDict = (row: any) => {
  ElMessageBox.confirm('确定要删除该字典吗？删除后字典项也会一并删除！', '提示', {
    type: 'warning'
  }).then(() => {
    deleteDict(row.id).then(() => {
      getData();
      ElMessage.success('删除成功');
    });
  });
};

const handleRowClick = (row: any) => {
  currentDict.value = row;
  getDictValues(row.id).then(res => {
    valueTableData.value = res.data;
    valueVisible.value = true;
  });
};

const handleAddValue = () => {
  resetValueForm();
  addValueVisible.value = true;
};

const handleEditValue = (row: any) => {
  valueForm.id = row.id;
  valueForm.label = row.label;
  valueForm.value = row.value;
  valueForm.sortOrder = row.sortOrder;
  valueForm.description = row.description;
  addValueVisible.value = true;
};

const handleSaveValue = () => {
  if (valueForm.id) {
    updateDictValue(valueForm.id, {
      label: valueForm.label,
      value: valueForm.value,
      sortOrder: valueForm.sortOrder,
      description: valueForm.description
    }).then(() => {
      refreshValues();
      ElMessage.success('修改成功');
      addValueVisible.value = false;
    });
  } else {
    createDictValue(currentDict.value.id, {
      label: valueForm.label,
      value: valueForm.value,
      sortOrder: valueForm.sortOrder,
      description: valueForm.description
    }).then(() => {
      refreshValues();
      ElMessage.success('新增成功');
      addValueVisible.value = false;
    });
  }
};

const handleDeleteValue = (row: any) => {
  ElMessageBox.confirm('确定要删除该字典项吗？', '提示', {
    type: 'warning'
  }).then(() => {
    deleteDictValue(row.id).then(() => {
      refreshValues();
      ElMessage.success('删除成功');
    });
  });
};

const refreshValues = () => {
  getDictValues(currentDict.value.id).then(res => {
    valueTableData.value = res.data;
  });
};
</script>

<style scoped>
.handle-box {
  margin-bottom: 20px;
}

.table {
  width: 100%;
  font-size: 14px;
}

.red {
  color: #F56C6C;
}
</style>
