<template>
  <div>
    <div class="container">
      <div class="handle-box">
        <el-button type="primary" :icon="Plus" @click="handleAdd" v-action:leave:create>新增请假</el-button>
      </div>
      <el-table :data="tableData" border class="table" ref="multipleTable" header-cell-class-name="table-header">
        <el-table-column prop="id" label="ID" width="55" align="center"></el-table-column>
        <el-table-column prop="user.username" label="请假人" width="120"></el-table-column>
        <el-table-column prop="leaveTypeLabel" label="请假类型" width="100"></el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="160"></el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="160"></el-table-column>
        <el-table-column prop="leaveReason" label="请假原因" min-width="150"></el-table-column>
        <el-table-column prop="leaveStatusLabel" label="审批状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.leaveStatus)">{{ row.leaveStatusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cancelTime" label="销假时间" width="160"></el-table-column>
        <el-table-column label="操作" width="350" fixed="right">
          <template #default="scope">
            <el-button text :icon="Edit" @click="handleEdit(scope.row)" v-action:leave:update>编辑</el-button>
            <el-button text :icon="CircleCheck" @click="handleApprove(scope.row)" v-if="scope.row.leaveStatus==='0'" v-action:leave:approve>通过</el-button>
            <el-button text :icon="CloseBold" class="orange" @click="handleReject(scope.row)" v-if="scope.row.leaveStatus==='0'" v-action:leave:approve>退回</el-button>
            <el-button text :icon="Refresh" @click="handleCancel(scope.row)" v-if="scope.row.leaveStatus==='0'" v-action:leave:cancel>销假</el-button>
            <el-button text :icon="Delete" class="red" @click="handleDelete(scope.row)" v-action:leave:delete>删除</el-button>
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

    <el-dialog :title="isEdit ? '编辑请假' : '新增请假'" v-model="dialogVisible" width="35%">
      <el-form label-width="80px">
        <el-form-item label="请假类型">
          <el-select v-model="form.leaveType" placeholder="请选择请假类型" style="width: 100%">
            <el-option label="病假" value="sick"></el-option>
            <el-option label="事假" value="personal"></el-option>
            <el-option label="产假" value="maternity"></el-option>
            <el-option label="调休" value="offshift"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="请假原因">
          <el-input v-model="form.leaveReason" type="textarea" :rows="3" placeholder="请输入请假原因"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="saveLeave">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {reactive, ref} from 'vue';
import {ElMessage, ElMessageBox} from 'element-plus';
import {CircleCheck, CloseBold, Delete, Edit, Plus, Refresh} from '@element-plus/icons-vue';
import {createLeave, deleteLeave, getLeaveList, approveLeave, rejectLeave, cancelLeave, updateLeave} from "../api/leave";

interface TableItem {
  id: number;
  user: { id: number; username: string; };
  leaveType: string;
  leaveTypeLabel: string;
  startTime: string;
  endTime: string;
  leaveReason: string;
  leaveStatus: string;
  leaveStatusLabel: string;
  cancelTime: string;
}

const query = reactive({
  pageIndex: 1,
  pageSize: 10
});

const tableData = ref<TableItem[]>([]);
const pageTotal = ref(0);
const dialogVisible = ref(false);
const isEdit = ref(false);
let editId: number | null = null;

const form = reactive({
  leaveType: 'personal',
  startTime: '',
  endTime: '',
  leaveReason: ''
});

const getStatusType = (status: string) => {
  switch (status) {
    case '0':
      return 'warning';
    case '1':
      return 'success';
    case '2':
      return 'danger';
    case '3':
      return 'info';
    default:
      return '';
  }
};

const getData = () => {
  getLeaveList({
    page: query.pageIndex,
    size: query.pageSize
  }).then(res => {
    tableData.value = res.data.list;
    pageTotal.value = res.data.total;
  });
};
getData();

const handlePageChange = (val: number) => {
  query.pageIndex = val;
  getData();
};

const resetForm = () => {
  form.leaveType = 'personal';
  form.startTime = '';
  form.endTime = '';
  form.leaveReason = '';
};

const handleAdd = () => {
  isEdit.value = false;
  editId = null;
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row: TableItem) => {
  isEdit.value = true;
  editId = row.id;
  form.leaveType = row.leaveType;
  form.startTime = row.startTime;
  form.endTime = row.endTime;
  form.leaveReason = row.leaveReason;
  dialogVisible.value = true;
};

const saveLeave = () => {
  if (isEdit.value && editId) {
    updateLeave(editId, form).then(() => {
      ElMessage.success('更新成功');
      dialogVisible.value = false;
      getData();
    });
  } else {
    createLeave(form).then(() => {
      ElMessage.success('创建成功');
      dialogVisible.value = false;
      getData();
    });
  }
};

const handleApprove = (row: TableItem) => {
  ElMessageBox.confirm('确定要通过该请假申请吗？', '提示', {
    type: 'warning'
  }).then(() => {
    approveLeave(row.id).then(() => {
      ElMessage.success('已通过');
      getData();
    });
  }).catch(() => {});
};

const handleReject = (row: TableItem) => {
  ElMessageBox.confirm('确定要退回该请假申请吗？', '提示', {
    type: 'warning'
  }).then(() => {
    rejectLeave(row.id).then(() => {
      ElMessage.success('已退回');
      getData();
    });
  }).catch(() => {});
};

const handleCancel = (row: TableItem) => {
  ElMessageBox.confirm('确定要销假吗？', '提示', {
    type: 'warning'
  }).then(() => {
    cancelLeave(row.id).then(() => {
      ElMessage.success('销假成功');
      getData();
    });
  }).catch(() => {});
};

const handleDelete = (row: TableItem) => {
  ElMessageBox.confirm('确定要删除该请假记录吗？', '提示', {
    type: 'warning'
  }).then(() => {
    deleteLeave(row.id).then(() => {
      ElMessage.success('删除成功');
      getData();
    });
  }).catch(() => {});
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

.mr10 {
  margin-right: 10px;
}

.orange {
  color: #e6a23c;
}
</style>
