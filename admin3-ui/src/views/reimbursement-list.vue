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
        <el-input v-model="query.keyword" placeholder="搜索标题/说明" clearable style="width: 200px; margin-left: 10px" @clear="handleSearch" @keyup.enter="handleSearch"></el-input>
        <el-button :icon="Search" style="margin-left: 5px" @click="handleSearch">搜索</el-button>
        <el-button type="primary" style="margin-left: 5px" @click="handleBatchExport">批量导出OA表</el-button>
      </div>
      <el-table :data="tableData" border class="table" header-cell-class-name="table-header" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" align="center"></el-table-column>
        <el-table-column prop="id" label="ID" width="55" align="center"></el-table-column>
        <el-table-column prop="title" label="报销标题" width="140">
          <template #default="{ row }">
            <span style="display: inline-block; max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" :title="row.title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="matterDescription" label="报销事项详细描述" min-width="180">
          <template #default="{ row }">
            <span style="display: inline-block; max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" :title="row.matterDescription">{{ row.matterDescription || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceCount" label="发票数" width="80" align="center">
          <template #default="{ row }">
            <span>{{ row.invoiceCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceTotalAmount" label="发票合计" width="110" align="right">
          <template #default="{ row }">
            <span>¥{{ formatMoney(row.invoiceTotalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="110" align="right">
          <template #default="{ row }">
            <span>¥{{ row.amount.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="90"></el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" style="font-size: 12px;">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="150"></el-table-column>
        <el-table-column label="操作" width="350" fixed="right">
          <template #default="scope">
            <el-button text :icon="View" @click="handleDetail(scope.row)" v-action:reimbursement:view>详情</el-button>
            <el-button text :icon="Edit" @click="handleEdit(scope.row)" v-if="isApplicantEditableStatus(scope.row.status)" v-action:reimbursement:update>编辑</el-button>
            <el-button text :icon="Upload" @click="handleSubmit(scope.row)" v-if="isApplicantEditableStatus(scope.row.status)" v-action:reimbursement:submit>提交</el-button>
            <el-button text :icon="CircleCheck" @click="handleApprove(scope.row)" v-if="scope.row.status==='pending'" v-action:reimbursement:approve>通过</el-button>
            <el-button text :icon="CloseBold" class="orange" @click="handleReject(scope.row)" v-if="scope.row.status==='pending'" v-action:reimbursement:approve>退回</el-button>
            <el-button text :icon="Refresh" @click="handleRecall(scope.row)" v-if="scope.row.status==='pending'" v-action:reimbursement:recall>撤回</el-button>
            <el-button text :icon="Delete" class="red" @click="handleDelete(scope.row)" v-if="isApplicantEditableStatus(scope.row.status)" v-action:reimbursement:delete>删除</el-button>
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
    <el-dialog :title="isEdit ? '编辑报销' : '新建报销'" v-model="dialogVisible" width="94%" top="2vh" class="reimbursement-edit-dialog">
      <el-form label-width="92px" class="edit-form">
        <div class="form-section form-section-compact">
          <div class="form-section-title">报销信息</div>
          <el-row :gutter="14">
          <el-col :span="8">
            <el-form-item label="报销标题">
              <el-input v-model="form.title" placeholder="请输入报销标题"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="报销金额">
              <el-input v-model="form.amount" inputmode="decimal" placeholder="由有效发票自动汇总，可手动修正"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="有效发票数">
              <el-input v-model="form.invoiceCount" disabled placeholder="自动计算"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="有效凭证数">
              <el-input :model-value="validSupportCount" disabled placeholder="自动计算"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="报销事项">
              <el-input v-model="form.matterDescription" type="textarea" :autosize="{ minRows: 1, maxRows: 2 }" placeholder="请输入报销事项详细描述"></el-input>
            </el-form-item>
          </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="form-section-header">
            <div class="form-section-title">费用明细</div>
            <el-button type="primary" size="small" :icon="Plus" @click="addExpenseItem">新增费用明细</el-button>
          </div>
          <el-table :data="expenseItemList" border size="small" style="width: 100%;">
            <el-table-column label="序号" width="60" align="center">
              <template #default="{ $index }">{{ $index + 1 }}</template>
            </el-table-column>
            <el-table-column label="日期" width="145">
              <template #default="{ row }">
                <el-date-picker v-model="row.expenseDate" type="date" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 125px"></el-date-picker>
              </template>
            </el-table-column>
            <el-table-column label="费用类型" width="145">
              <template #default="{ row }">
                <el-select v-model="row.expenseType" style="width: 125px">
                  <el-option v-for="item in expenseTypeOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="100">
              <template #default="{ row }"><el-input v-model="row.amount" type="number"></el-input></template>
            </el-table-column>
            <el-table-column label="公里数" width="95">
              <template #default="{ row }"><el-input v-model="row.kilometers" type="number" placeholder="可选"></el-input></template>
            </el-table-column>
            <el-table-column label="发票数" width="70" align="center">
              <template #default="{ row }">{{ expenseInvoiceCount(row) }}</template>
            </el-table-column>
            <el-table-column label="发票合计" width="100" align="right">
              <template #default="{ row }">¥{{ expenseInvoiceTotal(row) }}</template>
            </el-table-column>
            <el-table-column label="说明" min-width="220" show-overflow-tooltip>
              <template #default="{ row }"><el-input v-model="row.description" placeholder="可选"></el-input></template>
            </el-table-column>
            <el-table-column label="操作" min-width="170" fixed="right">
              <template #default="{ $index }">
                <div class="row-actions">
                  <el-button text type="primary" @click="openExpenseInvoiceDialog($index)">关联发票</el-button>
                  <el-button text type="danger" @click="removeExpenseItem($index)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="form-section">
          <div class="form-section-title">发票列表</div>
          <el-table v-if="invoiceList.length > 0" :data="invoiceList" border size="small" style="width: 100%">
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="invoice-expand-detail">
                  <el-row :gutter="16">
                    <el-col :span="6"><span class="expand-label">购买方：</span>{{ row.buyerName }}</el-col>
                    <el-col :span="6"><span class="expand-label">购买方税号：</span>{{ row.buyerTaxId }}</el-col>
                    <el-col :span="6"><span class="expand-label">销售方：</span>{{ row.sellerName }}</el-col>
                    <el-col :span="6"><span class="expand-label">销售方税号：</span>{{ row.sellerTaxId }}</el-col>
                    <el-col :span="6"><span class="expand-label">金额(不含税)：</span>¥{{ row.amountBeforeTax }}</el-col>
                    <el-col :span="6"><span class="expand-label">税率：</span>{{ row.taxRate }}</el-col>
                    <el-col :span="6"><span class="expand-label">税额：</span>¥{{ row.taxAmount }}</el-col>
                    <el-col :span="6"><span class="expand-label">发票类型：</span>{{ row.invoiceType }}</el-col>
                    <el-col :span="6"><span class="expand-label">收款人：</span>{{ row.payee }}</el-col>
                    <el-col :span="6"><span class="expand-label">复核人：</span>{{ row.reviewer }}</el-col>
                    <el-col :span="6"><span class="expand-label">开票人：</span>{{ row.issuer }}</el-col>
                    <el-col :span="12"><span class="expand-label">购买方地址/电话：</span>{{ row.buyerAddressPhone }}</el-col>
                    <el-col :span="12"><span class="expand-label">购买方开户行/账号：</span>{{ row.buyerBankInfo }}</el-col>
                    <el-col :span="12"><span class="expand-label">销售方地址/电话：</span>{{ row.sellerAddressPhone }}</el-col>
                    <el-col :span="12"><span class="expand-label">销售方开户行/账号：</span>{{ row.sellerBankInfo }}</el-col>
                    <el-col :span="6"><span class="expand-label">起点：</span>{{ row.startPlace || '-' }}</el-col>
                    <el-col :span="6"><span class="expand-label">终点：</span>{{ row.endPlace || '-' }}</el-col>
                    <el-col :span="24"><span class="expand-label">备注：</span>{{ row.remarks }}</el-col>
                  </el-row>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="序号" width="60" align="center">
              <template #default="{ $index }">{{ $index + 1 }}</template>
            </el-table-column>
            <el-table-column prop="invoiceNo" label="发票号码" min-width="230" show-overflow-tooltip>
              <template #default="{ row }"><span class="invoice-no-text" :title="row.invoiceNo">{{ row.invoiceNo || '-' }}</span></template>
            </el-table-column>
            <el-table-column prop="invoiceCode" label="发票代码" width="105"></el-table-column>
            <el-table-column prop="invoiceDate" label="开票日期" width="140"></el-table-column>
            <el-table-column prop="sellerName" label="销售方" min-width="150" show-overflow-tooltip></el-table-column>
            <el-table-column prop="totalAmount" label="金额" width="95" align="right">
              <template #default="{ row }">¥{{ formatMoney(row.totalAmount) }}</template>
            </el-table-column>
            <el-table-column prop="startPlace" label="起点" min-width="150" show-overflow-tooltip></el-table-column>
            <el-table-column prop="endPlace" label="终点" min-width="150" show-overflow-tooltip></el-table-column>
            <el-table-column label="操作" min-width="300" fixed="right">
              <template #default="{ row, $index }">
                <div class="invoice-actions">
                  <el-button text size="small" type="primary" @click="editInvoice(row)">编辑</el-button>
                  <el-upload
                    class="invoice-support-upload"
                    :auto-upload="false"
                    :show-file-list="false"
                    accept=".jpg,.png,.gif,.webp,.pdf"
                    :on-change="(file: any) => handleInvoiceSupportFileChange(row, file)">
                    <template #trigger>
                      <el-button text size="small" type="primary">上传凭证</el-button>
                    </template>
                  </el-upload>
                  <el-button text size="small" type="primary" @click="openSupportLinkDialog(row)">关联凭证</el-button>
                  <el-button
                    text
                    size="small"
                    type="primary"
                    :disabled="!row.supportAttachment"
                    @click="previewInvoiceSupportAttachment(row)">
                    查看凭证
                  </el-button>
                  <el-button
                    size="small"
                    :type="row.supportAttachment ? 'warning' : 'info'"
                    plain
                    :disabled="!row.supportAttachment"
                    @click="removeInvoiceSupportAttachment(row)">
                    解除凭证
                  </el-button>
                  <el-button text size="small" type="danger" @click="removeInvoice(row, $index)">删除发票</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="invoiceList.length === 0" style="padding: 20px 0; color: #909399; text-align: center;">
            暂无发票，请选择文件上传并识别
          </div>
        </div>

        <div class="form-section">
          <div class="form-section-title">上传文件</div>
          <el-tabs v-model="uploadTab" class="upload-tabs">
            <el-tab-pane label="上传发票" name="invoice">
              <div class="upload-content">
                <div v-for="(file, index) in fileList" :key="file.id || file.uid" class="file-item">
                  <div class="file-item-header">
                    <span class="file-index">{{ index + 1 }}</span>
                    <span class="file-name" :title="file.name">{{ file.name }}</span>
                    <span class="file-size">{{ formatFileSize(file.size) }}</span>
                    <span class="file-linked-invoice">发票号码：{{ file.linkedInvoiceNo || file.ocrInvoiceNo || '-' }}</span>
                    <el-tag :type="file.linkedInvoiceId ? 'success' : file.associationReason ? 'warning' : 'info'" size="small" effect="plain">
                      {{ file.linkedInvoiceId ? '已关联发票' : (file.associationReason || '未关联发票') }}
                    </el-tag>
                    <el-tag
                      :type="file.ocrStatus === 'completed' ? 'success' : file.ocrStatus === 'failed' ? 'danger' : file.ocrStatus === 'processing' ? 'warning' : 'info'"
                      size="small"
                      effect="dark">
                      <span v-if="file.ocrStatus === 'processing'" class="ocr-animation">
                        <span class="dot-animation">正在OCR识别</span>
                      </span>
                      <span v-else>{{ getStatusLabel2(file.ocrStatus) }}</span>
                    </el-tag>
                    <el-popover v-if="file.ocrResult" placement="top" :width="520" trigger="hover">
                      <template #reference>
                        <el-button text size="small" type="primary">识别结果</el-button>
                      </template>
                      <pre class="file-ocr-popover">{{ formatOcrJson(file.ocrResult) }}</pre>
                    </el-popover>
                    <el-button text size="small" type="primary" @click="previewImage(file)">
                      <el-icon><ZoomIn /></el-icon>
                    </el-button>
                    <el-button text size="small" type="danger" @click="handleDeleteAttachment(file)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                  <div v-if="file.ocrStatus === 'failed' && file.ocrResult" class="file-ocr-error">
                    {{ getOcrError(file.ocrResult) }}
                  </div>
                </div>
                <el-upload
                  ref="uploadRef"
                  :auto-upload="false"
                  :on-change="(file: any) => handleFileChange(file, 'invoice')"
                  :on-exceed="handleUploadExceed"
                  :file-list="[]"
                  accept=".jpg,.png,.gif,.webp,.pdf,.xml,.ofd"
                  :limit="50"
                  :show-file-list="false"
                  multiple>
                  <template #trigger>
                    <el-button type="primary" :icon="Plus">选择发票文件</el-button>
                  </template>
                </el-upload>
              </div>
            </el-tab-pane>
            <el-tab-pane label="上传行程单" name="support">
              <div class="upload-content">
                <div v-for="(file, index) in supportFileList" :key="file.id || file.uid" class="file-item">
                  <div class="file-item-header support-file-header">
                    <span class="file-index">{{ index + 1 }}</span>
                    <span class="file-name" :title="file.name">{{ file.name }}</span>
                    <span class="file-size">{{ formatFileSize(file.size) }}</span>
                    <span class="file-linked-invoice">关联发票：{{ file.linkedInvoiceNo || '-' }}</span>
                    <el-tag :type="file.linkedInvoiceId ? 'success' : file.associationReason ? 'warning' : 'info'" size="small" effect="plain">
                      {{ file.linkedInvoiceId ? '已关联发票' : (file.associationReason || '未关联发票') }}
                    </el-tag>
                    <el-tag
                      :type="file.ocrStatus === 'completed' ? 'success' : file.ocrStatus === 'failed' ? 'danger' : file.ocrStatus === 'processing' ? 'warning' : 'info'"
                      size="small"
                      effect="dark">
                      <span v-if="file.ocrStatus === 'processing'" class="ocr-animation">
                        <span class="dot-animation">正在OCR识别</span>
                      </span>
                      <span v-else>{{ getStatusLabel2(file.ocrStatus) }}</span>
                    </el-tag>
                    <el-popover v-if="file.ocrResult" placement="top" :width="520" trigger="hover">
                      <template #reference>
                        <el-button text size="small" type="primary">识别结果</el-button>
                      </template>
                      <pre class="file-ocr-popover">{{ formatOcrJson(file.ocrResult) }}</pre>
                    </el-popover>
                    <el-button text size="small" type="primary" @click="previewImage(file)">
                      <el-icon><ZoomIn /></el-icon>
                    </el-button>
                    <el-button text size="small" type="danger" @click="handleDeleteAttachment(file)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                  <div v-if="file.ocrStatus === 'failed' && file.ocrResult" class="file-ocr-error">
                    {{ getOcrError(file.ocrResult) }}
                  </div>
                </div>
                <el-upload
                  :auto-upload="false"
                  :on-change="(file: any) => handleFileChange(file, 'support')"
                  :on-exceed="handleUploadExceed"
                  :file-list="[]"
                  accept=".jpg,.png,.gif,.webp,.pdf"
                  :limit="50"
                  :show-file-list="false"
                  multiple>
                  <template #trigger>
                    <el-button type="primary" :icon="Plus">选择行程单文件</el-button>
                  </template>
                </el-upload>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button @click="saveReimbursement(true)">保存不退出</el-button>
          <el-button type="primary" @click="saveReimbursement(false)">保 存</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog title="关联凭证" v-model="supportLinkDialogVisible" width="76%" class="expense-invoice-dialog">
      <div v-if="currentSupportInvoice" class="expense-link-content">
        <el-form label-width="80px" class="expense-link-form">
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="发票号码">
                <el-input :model-value="currentSupportInvoice.invoiceNo || '-'" disabled></el-input>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="销售方">
                <el-input :model-value="currentSupportInvoice.sellerName || '-'" disabled></el-input>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="金额">
                <el-input :model-value="'¥' + formatMoney(currentSupportInvoice.totalAmount)" disabled></el-input>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <el-table :data="supportLinkRows" border size="small" height="360" highlight-current-row @current-change="handleSupportLinkSelection">
          <el-table-column label="文件名" min-width="260" show-overflow-tooltip>
            <template #default="{ row }">{{ row.name }}</template>
          </el-table-column>
          <el-table-column label="识别状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag
                :type="row.ocrStatus === 'completed' ? 'success' : row.ocrStatus === 'failed' ? 'danger' : row.ocrStatus === 'processing' ? 'warning' : 'info'"
                size="small">
                {{ getStatusLabel2(row.ocrStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="当前关联发票" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">{{ row.linkedInvoiceNo || '-' }}</template>
          </el-table-column>
          <el-table-column label="识别文本" width="110" align="center">
            <template #default="{ row }">
              <el-popover v-if="row.ocrResult" placement="top" :width="560" trigger="hover">
                <template #reference>
                  <el-button text size="small" type="primary">查看文本</el-button>
                </template>
                <pre class="file-ocr-popover">{{ extractOcrTextForDisplay(row.ocrResult) }}</pre>
              </el-popover>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="大小" width="100">
            <template #default="{ row }">{{ formatFileSize(row.size) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-popover placement="left" :width="1100" trigger="hover">
                <template #reference>
                  <el-button text size="small" type="primary" @click.stop>预览</el-button>
                </template>
                <div class="support-preview-popover">
                  <iframe v-if="isPdfFile(row)" :src="pdfPreviewUrl(row.url)" class="support-preview-frame"></iframe>
                  <img v-else :src="row.url" class="support-preview-image" />
                </div>
              </el-popover>
              <el-button text size="small" type="primary" @click.stop="previewImage(row)">打开</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="supportLinkDialogVisible = false">取 消</el-button>
          <el-button type="primary" :disabled="!selectedSupportAttachment" @click="confirmSupportLink">关 联</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 费用明细关联发票对话框 -->
    <el-dialog title="关联发票" v-model="expenseInvoiceDialogVisible" width="88%" class="expense-invoice-dialog">
      <div v-if="currentExpenseItem" class="expense-link-content">
        <el-form label-width="80px" class="expense-link-form">
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="费用日期">
                <el-date-picker v-model="currentExpenseItem.expenseDate" type="date" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%"></el-date-picker>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="费用类型">
                <el-select v-model="currentExpenseItem.expenseType" style="width: 100%">
                  <el-option v-for="item in expenseTypeOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
                </el-select>
                <div class="expense-link-tip">仅保存到费用明细，不过滤发票</div>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="费用金额">
                <el-input :model-value="expenseInvoiceTotal(currentExpenseItem)" disabled></el-input>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <div class="expense-link-panels">
          <div class="expense-link-panel">
            <div class="expense-link-title">已关联发票</div>
            <el-table :data="linkedInvoiceRows" border size="small" height="320" @selection-change="handleLinkedInvoiceSelection">
              <el-table-column type="selection" width="45"></el-table-column>
              <el-table-column prop="invoiceNo" label="发票号码" min-width="220" show-overflow-tooltip>
                <template #default="{ row }"><span class="invoice-no-text">{{ row.invoiceNo || '-' }}</span></template>
              </el-table-column>
              <el-table-column prop="invoiceDate" label="开票日期" width="140"></el-table-column>
              <el-table-column prop="sellerName" label="销售方" min-width="150"></el-table-column>
              <el-table-column prop="totalAmount" label="金额" width="100" align="right">
                <template #default="{ row }">¥{{ formatMoney(row.totalAmount) }}</template>
              </el-table-column>
            </el-table>
          </div>
          <div class="expense-link-actions">
            <el-button type="primary" @click="linkSelectedInvoices">← 拉取</el-button>
            <el-button @click="unlinkSelectedInvoices">还原 →</el-button>
          </div>
          <div class="expense-link-panel">
            <div class="expense-link-title">待关联发票</div>
            <el-table :data="availableInvoiceRows" border size="small" height="320" @selection-change="handleAvailableInvoiceSelection">
              <el-table-column type="selection" width="45"></el-table-column>
              <el-table-column prop="invoiceNo" label="发票号码" min-width="220" show-overflow-tooltip>
                <template #default="{ row }"><span class="invoice-no-text">{{ row.invoiceNo || '-' }}</span></template>
              </el-table-column>
              <el-table-column prop="invoiceDate" label="开票日期" width="140"></el-table-column>
              <el-table-column prop="sellerName" label="销售方" min-width="150"></el-table-column>
              <el-table-column prop="totalAmount" label="金额" width="100" align="right">
                <template #default="{ row }">¥{{ formatMoney(row.totalAmount) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </div>
      <template #footer>
        <span>
          <el-button @click="expenseInvoiceDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveExpenseInvoiceDialog">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 发票编辑对话框 -->
    <el-dialog title="编辑发票" v-model="invoiceEditVisible" width="60%">
      <el-form label-width="110px">
        <el-row :gutter="20">
          <el-col :span="24"><el-form-item label="发票号码"><el-input v-model="invoiceEditForm.invoiceNo"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="发票代码"><el-input v-model="invoiceEditForm.invoiceCode"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="开票日期"><el-date-picker v-model="invoiceEditForm.invoiceDate" type="date" style="width:100%" value-format="YYYY-MM-DDTHH:mm:ss"></el-date-picker></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="价税合计"><el-input v-model="invoiceEditForm.totalAmount"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="金额(不含税)"><el-input v-model="invoiceEditForm.amountBeforeTax"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="税率"><el-input v-model="invoiceEditForm.taxRate"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="税额"><el-input v-model="invoiceEditForm.taxAmount"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="购买方名称"><el-input v-model="invoiceEditForm.buyerName"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="销售方名称"><el-input v-model="invoiceEditForm.sellerName"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="购买方税号"><el-input v-model="invoiceEditForm.buyerTaxId"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="销售方税号"><el-input v-model="invoiceEditForm.sellerTaxId"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="发票类型">
            <el-select v-model="invoiceEditForm.invoiceType" style="width:100%">
              <el-option label="增值税专用发票" value="special"></el-option>
              <el-option label="增值税普通发票" value="normal"></el-option>
              <el-option label="电子发票" value="electronic"></el-option>
              <el-option label="定额发票" value="fixed"></el-option>
              <el-option label="机动车发票" value="vehicle"></el-option>
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="发票状态">
            <el-select v-model="invoiceEditForm.invoiceStatus" style="width:100%">
              <el-option label="正常" value="normal"></el-option>
              <el-option label="作废" value="voided"></el-option>
              <el-option label="红冲" value="red"></el-option>
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="收款人"><el-input v-model="invoiceEditForm.payee"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="复核人"><el-input v-model="invoiceEditForm.reviewer"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="开票人"><el-input v-model="invoiceEditForm.issuer"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="购买方地址电话"><el-input v-model="invoiceEditForm.buyerAddressPhone"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="购买方开户行账号"><el-input v-model="invoiceEditForm.buyerBankInfo"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="销售方地址电话"><el-input v-model="invoiceEditForm.sellerAddressPhone"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="销售方开户行账号"><el-input v-model="invoiceEditForm.sellerBankInfo"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="起点"><el-input v-model="invoiceEditForm.startPlace"></el-input></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="终点"><el-input v-model="invoiceEditForm.endPlace"></el-input></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="invoiceEditForm.remarks" type="textarea" :rows="2"></el-input></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <span><el-button @click="invoiceEditVisible = false">取消</el-button><el-button type="primary" @click="saveInvoiceEdit">保存</el-button></span>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog title="报销详情" v-model="detailVisible" width="94%" top="2vh" class="detail-dialog reimbursement-edit-dialog">
      <div v-if="detailData" class="detail-content">
        <div class="form-section form-section-compact">
          <div class="form-section-header">
            <div class="form-section-title">报销信息</div>
            <div class="detail-section-actions">
              <el-button type="primary" size="small" @click="detailData && downloadElectronicInvoiceRegister(detailData.id)">下载电子发票登记表</el-button>
              <el-button type="primary" size="small" @click="detailData && downloadExpenseDetails(detailData.id)">下载报销费用明细表</el-button>
            </div>
          </div>
          <el-form label-width="92px" class="detail-form">
            <el-row :gutter="14">
              <el-col :span="8"><el-form-item label="报销标题"><div class="detail-value">{{ detailData.title }}</div></el-form-item></el-col>
              <el-col :span="6"><el-form-item label="报销金额"><div class="detail-value detail-amount">¥{{ detailData.amount.toFixed(2) }}</div></el-form-item></el-col>
              <el-col :span="5"><el-form-item label="有效发票数"><div class="detail-value">{{ detailData.invoiceCount || 0 }}</div></el-form-item></el-col>
              <el-col :span="5"><el-form-item label="有效凭证数"><div class="detail-value">{{ detailValidSupportCount }}</div></el-form-item></el-col>
              <el-col :span="24"><el-form-item label="报销事项"><div class="detail-value detail-value-multiline">{{ detailData.matterDescription || '无' }}</div></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="申请人"><div class="detail-value">{{ detailData.applicantName }}</div></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="创建时间"><div class="detail-value">{{ detailData.createdAt }}</div></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="当前状态"><div class="detail-value"><el-tag :type="getStatusType(detailData.status)">{{ getStatusLabel(detailData.status) }}</el-tag></div></el-form-item></el-col>
            </el-row>
          </el-form>
        </div>

        <div class="form-section">
          <div class="form-section-title">费用明细</div>
          <el-table v-if="detailData.expenseItems && detailData.expenseItems.length > 0" :data="detailData.expenseItems" border size="small" style="width: 100%">
            <el-table-column type="expand" width="45">
              <template #default="{ row }">
                <el-table :data="invoicesByExpenseItem(detailData.invoices || [], row.id)" border size="small">
                  <el-table-column prop="invoiceNo" label="发票号码" min-width="220" show-overflow-tooltip>
                    <template #default="{ row: invoice }"><span class="invoice-no-text">{{ invoice.invoiceNo || '-' }}</span></template>
                  </el-table-column>
                  <el-table-column prop="sellerName" label="销售方" min-width="160"></el-table-column>
                  <el-table-column prop="totalAmount" label="金额" width="110" align="right">
                    <template #default="{ row: invoice }">¥{{ formatMoney(invoice.totalAmount) }}</template>
                  </el-table-column>
                  <el-table-column prop="invoiceDate" label="开票日期" width="160"></el-table-column>
                </el-table>
              </template>
            </el-table-column>
            <el-table-column prop="expenseDate" label="日期" width="145"></el-table-column>
            <el-table-column prop="expenseType" label="费用类型" width="145">
              <template #default="{ row }">{{ getExpenseTypeLabel(row.expenseType) }}</template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" width="100" align="right">
              <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="kilometers" label="公里数" width="95" align="right">
              <template #default="{ row }">{{ row.kilometers || '-' }}</template>
            </el-table-column>
            <el-table-column prop="invoiceCount" label="发票数" width="80" align="center"></el-table-column>
            <el-table-column prop="invoiceTotalAmount" label="发票合计" width="110" align="right">
              <template #default="{ row }">¥{{ formatMoney(row.invoiceTotalAmount) }}</template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="240" show-overflow-tooltip></el-table-column>
          </el-table>
          <div v-else class="detail-empty">暂无费用明细</div>
        </div>

        <div class="form-section">
          <div class="form-section-title">发票列表</div>
          <el-table v-if="detailData.invoices && detailData.invoices.length > 0" :data="detailData.invoices" border size="small" style="width: 100%">
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="invoice-expand-detail">
                  <el-row :gutter="16">
                    <el-col :span="6"><span class="expand-label">购买方：</span>{{ row.buyerName }}</el-col>
                    <el-col :span="6"><span class="expand-label">购买方税号：</span>{{ row.buyerTaxId }}</el-col>
                    <el-col :span="6"><span class="expand-label">销售方：</span>{{ row.sellerName }}</el-col>
                    <el-col :span="6"><span class="expand-label">销售方税号：</span>{{ row.sellerTaxId }}</el-col>
                    <el-col :span="6"><span class="expand-label">金额(不含税)：</span>¥{{ row.amountBeforeTax }}</el-col>
                    <el-col :span="6"><span class="expand-label">税率：</span>{{ row.taxRate }}</el-col>
                    <el-col :span="6"><span class="expand-label">税额：</span>¥{{ row.taxAmount }}</el-col>
                    <el-col :span="6"><span class="expand-label">发票类型：</span>{{ row.invoiceType }}</el-col>
                    <el-col :span="6"><span class="expand-label">起点：</span>{{ row.startPlace || '-' }}</el-col>
                    <el-col :span="6"><span class="expand-label">终点：</span>{{ row.endPlace || '-' }}</el-col>
                    <el-col :span="24"><span class="expand-label">备注：</span>{{ row.remarks }}</el-col>
                  </el-row>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="invoiceNo" label="发票号码" min-width="230" show-overflow-tooltip>
              <template #default="{ row }"><span class="invoice-no-text">{{ row.invoiceNo || '-' }}</span></template>
            </el-table-column>
            <el-table-column prop="invoiceCode" label="发票代码" width="105"></el-table-column>
            <el-table-column prop="invoiceDate" label="开票日期" width="140"></el-table-column>
            <el-table-column prop="sellerName" label="销售方" min-width="150" show-overflow-tooltip></el-table-column>
            <el-table-column prop="totalAmount" label="金额" width="95" align="right">
              <template #default="{ row }">¥{{ formatMoney(row.totalAmount) }}</template>
            </el-table-column>
            <el-table-column prop="startPlace" label="起点" min-width="150" show-overflow-tooltip></el-table-column>
            <el-table-column prop="endPlace" label="终点" min-width="150" show-overflow-tooltip></el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button text size="small" type="primary" :disabled="!row.supportAttachment" @click="previewInvoiceSupportAttachment(row)">查看凭证</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="detail-empty">暂无发票</div>
        </div>

        <div class="form-section">
          <div class="form-section-title">附件</div>
          <el-tabs class="detail-attachment-tabs">
            <el-tab-pane label="发票附件">
              <div v-if="detailInvoiceAttachments(detailData.attachments || [], detailData.invoices || []).length > 0" class="detail-attachment-list">
                <div v-for="att in detailInvoiceAttachments(detailData.attachments || [], detailData.invoices || [])" :key="att.id" class="detail-attachment-item">
                  <span class="detail-attachment-name" :title="att.fileName">{{ att.fileName }}</span>
                  <span class="detail-attachment-invoice">发票号码：{{ att.linkedInvoiceNo || extractOcrInvoiceNo(att.ocrResult) || '-' }}</span>
                  <el-tag v-if="att.ocrStatus === 'pending'" size="small" type="info">待识别</el-tag>
                  <el-tag v-else-if="att.ocrStatus === 'processing'" size="small" type="warning">识别中</el-tag>
                  <el-tag v-else-if="att.ocrStatus === 'completed'" size="small" type="success">已识别</el-tag>
                  <el-tag v-else-if="att.ocrStatus === 'failed'" size="small" type="danger">识别失败</el-tag>
                  <el-button text size="small" type="primary" @click="previewAttachment(att)">
                    <el-icon><ZoomIn /></el-icon>
                  </el-button>
                </div>
              </div>
              <div v-else class="detail-empty">暂无发票附件</div>
            </el-tab-pane>
            <el-tab-pane label="行程单">
              <div v-if="detailSupportAttachments(detailData.attachments || [], detailData.invoices || []).length > 0" class="detail-attachment-list">
                <div v-for="att in detailSupportAttachments(detailData.attachments || [], detailData.invoices || [])" :key="att.id" class="detail-attachment-item">
                  <span class="detail-attachment-name" :title="att.fileName">{{ att.fileName }}</span>
                  <span class="detail-attachment-invoice">关联发票：{{ att.linkedInvoiceNo || '-' }}</span>
                  <el-tag :type="att.linkedInvoiceId ? 'success' : 'info'" size="small" effect="plain">
                    {{ att.linkedInvoiceId ? '已关联发票' : '未关联发票' }}
                  </el-tag>
                  <el-tag v-if="att.ocrStatus === 'pending'" size="small" type="info">待识别</el-tag>
                  <el-tag v-else-if="att.ocrStatus === 'processing'" size="small" type="warning">识别中</el-tag>
                  <el-tag v-else-if="att.ocrStatus === 'completed'" size="small" type="success">已识别</el-tag>
                  <el-tag v-else-if="att.ocrStatus === 'failed'" size="small" type="danger">识别失败</el-tag>
                  <el-button text size="small" type="primary" @click="previewAttachment(att)">
                    <el-icon><ZoomIn /></el-icon>
                  </el-button>
                </div>
              </div>
              <div v-else class="detail-empty">暂无行程单</div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <div class="form-section">
          <div class="form-section-title">审批日志</div>
          <el-timeline v-if="detailData.approvalLogs && detailData.approvalLogs.length > 0" class="detail-timeline">
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
          <div v-else class="detail-empty">暂无审批日志</div>
        </div>
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
import {computed, reactive, ref} from 'vue';
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
  deleteAttachment,
  deleteInvoice,
  uploadInvoiceSupportAttachment,
  uploadSupportAttachment,
  linkInvoiceSupportAttachment,
  deleteInvoiceSupportAttachment,
  saveExpenseItems,
  regroupExpenseItems,
  exportElectronicInvoiceRegister,
  exportExpenseDetails,
  exportOaPackage
} from "../api/reimbursement";
import {getDictValuesByCode} from "../api/dict";

interface TableItem {
  id: number;
  title: string;
  category: string;
  amount: number;
  description: string;
  matterDescription?: string;
  invoiceCount?: number;
  invoiceTotalAmount?: number;
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
  invoiceNo?: string;
  invoiceCode?: string;
  invoiceDate?: string;
  buyerName?: string;
  sellerName?: string;
  buyerTaxId?: string;
  sellerTaxId?: string;
  invoiceType?: string;
  invoiceStatus?: string;
  invoiceCount?: number;
  invoiceTotalAmount?: number;
  ocrRawJson?: string;
  invoices?: any[];
  expenseItems?: any[];
  invoiceTemps?: any[];
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
const supportFileList = ref<any[]>([]);
const uploadTab = ref('invoice');
const uploadedAttachmentIds = ref<number[]>([]);
const selectedRows = ref<TableItem[]>([]);

const query = reactive({
  pageIndex: 1,
  pageSize: 10,
  status: '',
  keyword: ''
});

const form = reactive({
  title: '',
  category: 'other',
  amount: 0,
  description: '',
  matterDescription: '',
  ocrRawJson: '',
  ocrText: '',
  invoiceNo: '',
  invoiceCode: '',
  invoiceDate: '',
  buyerName: '',
  sellerName: '',
  buyerTaxId: '',
  sellerTaxId: '',
  invoiceType: '',
  invoiceStatus: '',
  invoiceCount: 0,
  amountBeforeTax: '',
  taxRate: '',
  taxAmount: '',
  payee: '',
  reviewer: '',
  issuer: '',
  buyerAddressPhone: '',
  buyerBankInfo: '',
  sellerAddressPhone: '',
  sellerBankInfo: '',
  remarks: ''
});

const invoiceList = ref<any[]>([]);
const expenseItemList = ref<any[]>([]);
const expenseInvoiceDialogVisible = ref(false);
const currentExpenseIndex = ref(-1);
const selectedLinkedInvoices = ref<any[]>([]);
const selectedAvailableInvoices = ref<any[]>([]);
const supportLinkDialogVisible = ref(false);
const currentSupportInvoice = ref<any | null>(null);
const selectedSupportAttachment = ref<any | null>(null);
const invoiceEditVisible = ref(false);
const editingInvoiceIndex = ref(-1);
const invoiceEditForm = reactive({
  invoiceNo: '',
  invoiceCode: '',
  invoiceDate: '',
  invoiceType: '',
  invoiceStatus: '',
  totalAmount: '',
  amountBeforeTax: '',
  taxRate: '',
  taxAmount: '',
  buyerName: '',
  sellerName: '',
  buyerTaxId: '',
  sellerTaxId: '',
  payee: '',
  reviewer: '',
  issuer: '',
  buyerAddressPhone: '',
  buyerBankInfo: '',
  sellerAddressPhone: '',
  sellerBankInfo: '',
  startPlace: '',
  endPlace: '',
  remarks: ''
});

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

const defaultExpenseTypeOptions = [
  {label: '油费金额', value: 'fuel'},
  {label: '高速费', value: 'toll'},
  {label: '高铁/动车/火车费', value: 'rail'},
  {label: '地铁费', value: 'metro'},
  {label: '网约车费', value: 'ride_hailing'},
  {label: '顺风车费', value: 'carpool'},
  {label: '货拉拉费', value: 'freight'},
  {label: '停车费', value: 'parking'},
  {label: '餐费', value: 'meal'},
  {label: '机票/航空费', value: 'air_travel'},
  {label: '住宿费', value: 'hotel'},
  {label: '保险费', value: 'insurance'},
  {label: '出租车费', value: 'taxi'},
  {label: '公交/客运费', value: 'bus'},
  {label: '租车费', value: 'rental_car'},
  {label: '车辆维修保养', value: 'vehicle_maintenance'},
  {label: '充电费', value: 'charging'},
  {label: '快递/配送费', value: 'delivery'},
  {label: '办公用品', value: 'office'},
  {label: '通讯费', value: 'communication'},
  {label: '会议/会务费', value: 'conference'},
  {label: '培训费', value: 'training'},
  {label: '专业服务费', value: 'service'},
  {label: '招待费', value: 'entertainment'},
  {label: '物料/低值易耗品', value: 'supplies'},
  {label: '其他', value: 'other'}
];
const expenseTypeOptions = ref([...defaultExpenseTypeOptions]);

function getStatusLabel(status: string) {
  return statusMap[status] || status;
}

function getStatusType(status: string) {
  return statusTypeMap[status] || 'info';
}

function isApplicantEditableStatus(status: string) {
  return status === 'draft' || status === 'rejected' || status === 'recalled';
}

function getLogActionLabel(action: string) {
  return logActionMap[action] || action;
}

function getExpenseTypeLabel(type: string) {
  return expenseTypeOptions.value.find(item => item.value === type)?.label || type || '-';
}

async function loadExpenseTypeDict() {
  try {
    const res = await getDictValuesByCode('reimbursement_expense_type');
    const values = (res.data || [])
      .map((item: any) => ({label: item.label, value: item.value}))
      .filter((item: any) => item.label && item.value);
    if (values.length > 0) expenseTypeOptions.value = values;
  } catch (e) {
    expenseTypeOptions.value = [...defaultExpenseTypeOptions];
  }
}

function getLogType(action: string) {
  if (action === 'approve') return 'success';
  if (action === 'reject') return 'danger';
  if (action === 'recall') return 'warning';
  return 'primary';
}

function getStatusLabel2(status: string) {
  const map: Record<string, string> = {
    pending: '待识别',
    processing: '识别中',
    completed: '已识别',
    failed: '识别失败',
    skipped: '不识别'
  };
  return map[status] || status;
}

function supportAttachmentIds(invoices: any[]) {
  return new Set((invoices || []).map((invoice: any) => invoice.supportAttachmentId).filter(Boolean));
}

function invoiceAttachmentIds(attachments: any[], invoices: any[]) {
  return (attachments || []).filter((a: any) => attachmentRole(a, invoices) !== 'support').map((a: any) => a.id);
}

function attachmentRole(attachment: any, invoices: any[] = []) {
  if (attachment.attachmentRole) return attachment.attachmentRole;
  const supportIds = supportAttachmentIds(invoices);
  if (supportIds.has(attachment.id)) return 'support';
  return 'invoice';
}

function mapInvoiceAttachmentFiles(attachments: any[], invoices: any[] = []) {
  return (attachments || [])
    .filter((a: any) => attachmentRole(a, invoices) !== 'support')
    .map(mapAttachmentFile);
}

function mapSupportAttachmentFiles(attachments: any[], invoices: any[] = []) {
  return (attachments || [])
    .filter((a: any) => attachmentRole(a, invoices) === 'support')
    .map(mapAttachmentFile);
}

function mapAttachmentFile(a: any) {
  return {
    name: a.fileName,
    url: '/admin3' + a.fileUrl,
    id: a.id,
    size: a.fileSize,
    ocrStatus: a.ocrStatus,
    ocrResult: a.ocrResult || '',
    linkedInvoiceId: a.linkedInvoiceId || null,
    linkedInvoiceNo: a.linkedInvoiceNo || '',
    associationReason: a.associationReason || '',
    ocrInvoiceNo: extractOcrInvoiceNo(a.ocrResult),
    attachmentRole: a.attachmentRole || null,
    showOcrJson: false
  };
}

function extractOcrInvoiceNo(ocrResult: string) {
  if (!ocrResult) return '';
  try {
    const result = JSON.parse(ocrResult);
    return result?.invoice_fields?.invoice_no || '';
  } catch {
    return '';
  }
}

function extractOcrTextForDisplay(ocrResult: string) {
  if (!ocrResult) return '';
  try {
    const result = JSON.parse(ocrResult);
    if (result.content && result.content[0]?.text) {
      try {
        const inner = JSON.parse(result.content[0].text);
        return inner.ocr_text || result.content[0].text || formatOcrJson(ocrResult);
      } catch {
        return result.content[0].text || formatOcrJson(ocrResult);
      }
    }
    return result.ocr_text || formatOcrJson(ocrResult);
  } catch {
    return ocrResult;
  }
}

function syncAttachmentLists(attachments: any[] = [], invoices: any[] = []) {
  fileList.value = mapInvoiceAttachmentFiles(attachments, invoices);
  supportFileList.value = mapSupportAttachmentFiles(attachments, invoices);
  uploadedAttachmentIds.value = invoiceAttachmentIds(attachments, invoices);
}

function detailInvoiceAttachments(attachments: any[], invoices: any[] = []) {
  return (attachments || []).filter((a: any) => attachmentRole(a, invoices) !== 'support');
}

function detailSupportAttachments(attachments: any[], invoices: any[] = []) {
  return (attachments || []).filter((a: any) => attachmentRole(a, invoices) === 'support');
}

function formatFileSize(bytes: number) {
  if (!bytes) return '';
  if (bytes < 1024) return bytes + 'B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB';
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB';
}

function formatOcrJson(jsonStr: string) {
  if (!jsonStr) return '';
  try {
    const obj = JSON.parse(jsonStr);
    return JSON.stringify(obj, null, 2);
  } catch {
    return jsonStr;
  }
}

function handleSearch() {
  query.pageIndex = 1;
  loadData();
}

function handlePageChange(val: number) {
  query.pageIndex = val;
  loadData();
}

function handleSelectionChange(rows: TableItem[]) {
  selectedRows.value = rows;
}

async function loadData() {
  console.log('[DEBUG] loadData called, page:', query.pageIndex, 'size:', query.pageSize);
  const params: any = {
    page: query.pageIndex,
    size: query.pageSize
  };
  if (query.status) params.status = query.status;
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
  form.category = 'other';
  form.amount = 0;
  form.description = '';
  form.matterDescription = '';
  form.ocrRawJson = '';
  form.ocrText = '';
  form.invoiceNo = '';
  form.invoiceCode = '';
  form.invoiceDate = '';
  form.buyerName = '';
  form.sellerName = '';
  form.buyerTaxId = '';
  form.sellerTaxId = '';
  form.invoiceType = '';
  form.invoiceStatus = '';
  form.invoiceCount = 0;
  form.amountBeforeTax = '';
  form.taxRate = '';
  form.taxAmount = '';
  form.payee = '';
  form.reviewer = '';
  form.issuer = '';
  form.buyerAddressPhone = '';
  form.buyerBankInfo = '';
  form.sellerAddressPhone = '';
  form.sellerBankInfo = '';
  form.remarks = '';
  invoiceList.value = [];
  expenseItemList.value = [];
  fileList.value = [];
  supportFileList.value = [];
  uploadedAttachmentIds.value = [];
  dialogVisible.value = true;
  // 自动创建一个草稿报销单，这样上传附件时就有 reimbursementId
  // OCR 完成后后端会自动更新字段，即使用户关掉页面数据也不会丢
  try {
    const res = await createReimbursement({
      title: '新建报销单',
      category: 'other',
      amount: 0,
      description: '',
      matterDescription: '',
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
  form.category = data.category || 'other';
  form.amount = data.amount;
  form.description = data.description || '';
  form.matterDescription = data.matterDescription || '';
  form.ocrRawJson = data.ocrRawJson || '';
  form.ocrText = data.ocrText || '';
  form.invoiceNo = data.invoiceNo || '';
  form.invoiceCode = data.invoiceCode || '';
  form.invoiceDate = data.invoiceDate || '';
  form.buyerName = data.buyerName || '';
  form.sellerName = data.sellerName || '';
  form.buyerTaxId = data.buyerTaxId || '';
  form.sellerTaxId = data.sellerTaxId || '';
  form.invoiceType = data.invoiceType || '';
  form.invoiceStatus = data.invoiceStatus || '';
  form.amountBeforeTax = data.amountBeforeTax || '';
  form.taxRate = data.taxRate || '';
  form.taxAmount = data.taxAmount || '';
  form.payee = data.payee || '';
  form.reviewer = data.reviewer || '';
  form.issuer = data.issuer || '';
  form.buyerAddressPhone = data.buyerAddressPhone || '';
  form.buyerBankInfo = data.buyerBankInfo || '';
  form.sellerAddressPhone = data.sellerAddressPhone || '';
  form.sellerBankInfo = data.sellerBankInfo || '';
  form.remarks = data.remarks || '';
  fileList.value = [];
  supportFileList.value = [];
  uploadedAttachmentIds.value = [];
  if (data.attachments) {
    syncAttachmentLists(data.attachments, data.invoices || []);
  }
  invoiceList.value = data.invoices || [];
  expenseItemList.value = (data.expenseItems || []).map(normalizeExpenseItemForEdit);
  syncReimbursementSummary();
  dialogVisible.value = true;
}

async function handleFileChange(uploadFile: any, uploadType: 'invoice' | 'support' = 'invoice') {
  const reimbursementId = editId.value;
  if (!reimbursementId) {
    ElMessage.warning('请先保存报销单再上传附件');
    return;
  }
  const fileName = uploadFile.raw?.name || uploadFile.name || '';
  const targetList = uploadType === 'support' ? supportFileList.value : fileList.value;
  if (fileName && targetList.some((file: any) => file.name === fileName)) {
    ElMessage.warning(uploadType === 'support' ? '该行程单文件已上传，请勿重复上传' : '该发票文件已上传，请勿重复上传');
    return;
  }
  try {
    console.log('Uploading file:', uploadFile.raw.name, 'reimbursementId:', reimbursementId);
    const res = uploadType === 'support'
      ? await uploadSupportAttachment(reimbursementId, uploadFile.raw)
      : await uploadAttachment(uploadFile.raw, reimbursementId);
    const att = res.data;
    console.log('Upload success, attachment:', att);
    if (res.data?.attachments) {
      invoiceList.value = res.data.invoices || invoiceList.value;
      expenseItemList.value = res.data.expenseItems ? res.data.expenseItems.map(normalizeExpenseItemForEdit) : expenseItemList.value;
      syncAttachmentLists(res.data.attachments || [], res.data.invoices || []);
    } else {
      uploadedAttachmentIds.value.push(att.id);
      targetList.push(mapAttachmentFile(att));
    }
    ElMessage.success(uploadType === 'support' ? '行程单上传成功，正在识别并尝试关联发票...' : '上传成功，正在识别中...');
    // 启动 OCR 轮询
    startOcrPolling();
  } catch (e: any) {
    console.error('Upload failed:', e);
    ElMessage.error('上传失败: ' + (e.message || ''));
  }
}

function handleUploadExceed() {
  ElMessage.warning('一次最多选择 50 个文件，请分批上传');
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
  const maxAttempts = 900;
  ocrPollingTimer = setInterval(async () => {
    attempts++;
    try {
      const res = await getReimbursementDetail(id);
      const data = res.data;
      const attachments = data.attachments || [];
      // 更新报销单的 ocrRawJson
      if (data.ocrRawJson) {
        form.ocrRawJson = data.ocrRawJson;
      }
      invoiceList.value = data.invoices || invoiceList.value;
      expenseItemList.value = data.expenseItems ? data.expenseItems.map(normalizeExpenseItemForEdit) : expenseItemList.value;
      syncAttachmentLists(attachments, data.invoices || []);
      syncReimbursementSummary();
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
          await autoRegroupExpenseItems('发票识别完成，已自动归集费用明细');
        }
      }
    } catch (e) {
      console.error('OCR polling error:', e);
    }
    if (attempts >= maxAttempts) {
      clearInterval(ocrPollingTimer);
      ocrPollingTimer = null;
      ElMessage.info('仍有文件在后台识别，关闭窗口或稍后刷新都可以继续查看结果');
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
      if (invoiceFields.invoice_status) fields.invoiceStatus = invoiceFields.invoice_status;
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
    if (fields.invoiceDate) form.invoiceDate = fields.invoiceDate;
    if (fields.buyerName) form.buyerName = fields.buyerName;
    if (fields.sellerName) form.sellerName = fields.sellerName;
    if (fields.buyerTaxId) form.buyerTaxId = fields.buyerTaxId;
    if (fields.sellerTaxId) form.sellerTaxId = fields.sellerTaxId;
    if (fields.invoiceType) form.invoiceType = fields.invoiceType;
    // 更新 OCR 原始 JSON（取最后一次有效识别的完整结果）
    form.ocrRawJson = att.ocrResult;
    // 更新 OCR 识别文本（可读的纯文本内容）
    form.ocrText = ocrText;
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

async function saveReimbursement(stayOpen = false) {
  if (!form.title || !form.category || form.amount === null || form.amount === undefined || String(form.amount) === '') {
    ElMessage.warning('请填写完整信息');
    return;
  }
  const data = {
    title: form.title,
    category: form.category,
    amount: form.amount,
    description: form.description,
    matterDescription: form.matterDescription,
    invoiceList: invoiceList.value,
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
    const pendingOcrCount = [...fileList.value, ...supportFileList.value]
      .filter((file: any) => file.ocrStatus === 'pending' || file.ocrStatus === 'processing')
      .length;
    if (pendingOcrCount > 0) {
      ElMessage.warning(`${pendingOcrCount} 个文件仍在 OCR，系统会继续后台识别，可先进入下一步，稍后刷新查看结果`);
      startOcrPolling();
    }
    const expenseRes = await saveExpenseItems(editId.value!, normalizeExpenseItems());
    applyReimbursementDetailToEditor(expenseRes.data);

    dialogVisible.value = stayOpen;
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
  const maxAttempts = 900; // 最多跟踪 30 分钟，批量 OCR 仍由后台异步执行
  for (let i = 0; i < maxAttempts; i++) {
    const res = await getReimbursementDetail(id);
    const data = res.data;
    const attachments = data.attachments || [];
    // 更新报销单的 ocrRawJson
    if (data.ocrRawJson) {
      form.ocrRawJson = data.ocrRawJson;
    }
    invoiceList.value = data.invoices || invoiceList.value;
    expenseItemList.value = data.expenseItems ? data.expenseItems.map(normalizeExpenseItemForEdit) : expenseItemList.value;
    syncAttachmentLists(attachments, data.invoices || []);
    syncReimbursementSummary();
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
        await autoRegroupExpenseItems('所有附件识别完成，已自动归集费用明细');
      }
      return;
    }
    console.log(`OCR polling (${i + 1}/${maxAttempts}): ${pending.length} attachments pending`);
    // 等待 2 秒
    await new Promise(resolve => setTimeout(resolve, 2000));
  }
  ElMessage.info('仍有文件在后台识别，请稍后查看详情');
}
function formatMoney(value: any): string {
  const num = Number(value || 0);
  return Number.isFinite(num) ? num.toFixed(2) : '0.00';
}

async function autoRegroupExpenseItems(successMessage: string) {
  if (!editId.value) return;
  const res = await regroupExpenseItems(editId.value);
  applyReimbursementDetailToEditor(res.data);
  ElMessage.success(successMessage);
}

function compactInvoiceNo(invoiceNo: string): string {
  if (!invoiceNo) return '-';
  return invoiceNo.length > 16 ? `${invoiceNo.slice(0, 8)}...${invoiceNo.slice(-6)}` : invoiceNo;
}

function applyReimbursementDetailToEditor(data: any) {
  if (!data) return;
  form.amount = data.amount;
  form.invoiceCount = data.invoiceCount || 0;
  invoiceList.value = data.invoices || invoiceList.value;
  expenseItemList.value = data.expenseItems ? data.expenseItems.map(normalizeExpenseItemForEdit) : expenseItemList.value;
  syncReimbursementSummary();
}

function invoiceOptionLabel(invoice: any): string {
  return `${invoice.invoiceNo || '未识别号码'} / ¥${formatMoney(invoice.totalAmount)} / ${invoice.sellerName || '-'}`;
}

function invoiceLabelById(invoices: any[], invoiceId: number) {
  const invoice = invoices.find(item => item.id === invoiceId);
  return invoice ? invoiceOptionLabel(invoice) : '-';
}

function normalizeExpenseItemForEdit(item: any) {
  const invoiceIds = new Set<number>(item.invoiceIds || (item.invoiceId ? [item.invoiceId] : []));
  if (item.id) {
    invoiceList.value
      .filter(invoice => invoice.expenseItemId === item.id)
      .forEach(invoice => invoiceIds.add(invoice.id));
  }
  return {
    ...item,
    invoiceIds: Array.from(invoiceIds)
  };
}

const currentExpenseItem = computed(() => {
  if (currentExpenseIndex.value < 0) return null;
  return expenseItemList.value[currentExpenseIndex.value] || null;
});

const linkedInvoiceRows = computed(() => {
  const item = currentExpenseItem.value;
  if (!item) return [];
  const ids = new Set(item.invoiceIds || []);
  return invoiceList.value.filter(invoice => ids.has(invoice.id) || (item.id && invoice.expenseItemId === item.id));
});

const availableInvoiceRows = computed(() => {
  const item = currentExpenseItem.value;
  const currentIds = new Set(item?.invoiceIds || []);
  const usedByOtherItems = new Set<number>();
  expenseItemList.value.forEach((item, index) => {
    if (index === currentExpenseIndex.value) return;
    (item.invoiceIds || []).forEach((id: number) => usedByOtherItems.add(id));
  });
  return invoiceList.value
    .filter(invoice => !invoice.expenseItemId || (item?.id && invoice.expenseItemId === item.id) || currentIds.has(invoice.id) || !usedByOtherItems.has(invoice.id))
    .filter(invoice => !currentIds.has(invoice.id));
});

const supportLinkRows = computed(() => {
  const invoiceId = currentSupportInvoice.value?.id;
  return supportFileList.value.filter(file => !file.linkedInvoiceId || file.linkedInvoiceId === invoiceId);
});

const validSupportCount = computed(() => countValidSupportAttachments(invoiceList.value));

const detailValidSupportCount = computed(() => countValidSupportAttachments(detailData.value?.invoices || []));

function countValidSupportAttachments(invoices: any[] = []) {
  return new Set((invoices || [])
    .map((invoice: any) => invoice.supportAttachmentId)
    .filter(Boolean)).size;
}

function invoicesByExpenseItem(invoices: any[], expenseItemId: number) {
  return invoices.filter(invoice => invoice.expenseItemId === expenseItemId);
}

function expenseInvoiceTotal(item: any): string {
  const ids = new Set(item.invoiceIds || []);
  const total = invoiceList.value
    .filter(invoice => ids.has(invoice.id) || (item.id && invoice.expenseItemId === item.id))
    .reduce((sum, invoice) => sum + Number(invoice.totalAmount || 0), 0);
  return formatMoney(total || item.invoiceTotalAmount || item.amount);
}

function expenseInvoiceCount(item: any): number {
  const ids = new Set(item.invoiceIds || []);
  return invoiceList.value
    .filter(invoice => ids.has(invoice.id) || (item.id && invoice.expenseItemId === item.id))
    .length;
}

function createEmptyExpenseItem() {
  return {
    expenseDate: '',
    expenseType: 'other',
    amount: 0,
    kilometers: '',
    invoiceIds: [],
    remark: '',
    description: '',
    source: 'manual'
  };
}

function addExpenseItem() {
  expenseItemList.value.push(createEmptyExpenseItem());
  openExpenseInvoiceDialog(expenseItemList.value.length - 1);
}

function removeExpenseItem(index: number) {
  const removed = expenseItemList.value[index];
  if (removed) {
    const removedIds = new Set(removed.invoiceIds || []);
    invoiceList.value.forEach(invoice => {
      if ((removed.id && invoice.expenseItemId === removed.id) || removedIds.has(invoice.id)) {
        invoice.expenseItemId = null;
      }
    });
  }
  expenseItemList.value.splice(index, 1);
  syncReimbursementSummary();
}

function openExpenseInvoiceDialog(index: number) {
  currentExpenseIndex.value = index;
  syncExpenseItemInvoiceIdsFromInvoiceList(expenseItemList.value[index]);
  selectedLinkedInvoices.value = [];
  selectedAvailableInvoices.value = [];
  expenseInvoiceDialogVisible.value = true;
}

function syncExpenseItemInvoiceIdsFromInvoiceList(item: any) {
  if (!item || !item.id) return;
  const ids = new Set(item.invoiceIds || []);
  invoiceList.value
    .filter(invoice => invoice.expenseItemId === item.id)
    .forEach(invoice => ids.add(invoice.id));
  item.invoiceIds = Array.from(ids);
}

function handleLinkedInvoiceSelection(rows: any[]) {
  selectedLinkedInvoices.value = rows;
}

function handleAvailableInvoiceSelection(rows: any[]) {
  selectedAvailableInvoices.value = rows;
}

function linkSelectedInvoices() {
  const item = currentExpenseItem.value;
  if (!item || selectedAvailableInvoices.value.length === 0) return;
  const ids = new Set(item.invoiceIds || []);
  selectedAvailableInvoices.value.forEach(invoice => ids.add(invoice.id));
  selectedAvailableInvoices.value.forEach(invoice => {
    if (item.id) invoice.expenseItemId = item.id;
  });
  item.invoiceIds = Array.from(ids);
  applyInvoiceSuggestionToExpenseItem(item, selectedAvailableInvoices.value);
  updateExpenseItemAmount(item);
  syncReimbursementSummary();
  selectedAvailableInvoices.value = [];
}

function unlinkSelectedInvoices() {
  const item = currentExpenseItem.value;
  if (!item || selectedLinkedInvoices.value.length === 0) return;
  const removeIds = new Set(selectedLinkedInvoices.value.map(invoice => invoice.id));
  item.invoiceIds = (item.invoiceIds || []).filter((id: number) => !removeIds.has(id));
  selectedLinkedInvoices.value.forEach(invoice => {
    if (item.id && invoice.expenseItemId === item.id) invoice.expenseItemId = null;
  });
  updateExpenseItemAmount(item);
  syncReimbursementSummary();
  selectedLinkedInvoices.value = [];
}

function saveExpenseInvoiceDialog() {
  const item = currentExpenseItem.value;
  if (item) updateExpenseItemAmount(item);
  syncReimbursementSummary();
  expenseInvoiceDialogVisible.value = false;
}

function updateExpenseItemAmount(item: any) {
  item.amount = Number(expenseInvoiceTotal(item));
}

function syncReimbursementSummary() {
  const invoiceTotal = invoiceList.value.reduce((sum, invoice) => sum + Number(invoice.totalAmount || 0), 0);
  form.invoiceCount = invoiceList.value.length;
  expenseItemList.value.forEach(item => updateExpenseItemAmount(item));
  form.amount = Number(formatMoney(invoiceTotal));
}

function applyInvoiceSuggestionToExpenseItem(item: any, invoices: any[]) {
  const firstInvoice = invoices[0];
  if (!firstInvoice) return;
  if (!item.expenseDate && firstInvoice.invoiceDate) {
    item.expenseDate = firstInvoice.invoiceDate;
  }
  if (!item.expenseType || item.expenseType === 'other') {
    item.expenseType = classifyInvoiceExpenseType(firstInvoice);
  }
  if (!item.remark && firstInvoice.sellerName) {
    item.remark = firstInvoice.sellerName;
  }
}

function classifyInvoiceExpenseType(invoice: any): string {
  const text = `${invoice.remarks || ''}${invoice.sellerName || ''}${invoice.invoiceContentShortName || ''}`.toLowerCase();
  if (containsAny(text, ['加油', '汽油', '柴油', '石化', '石油', '油品'])) return 'fuel';
  if (containsAny(text, ['高速', '通行费', '过路', 'etc'])) return 'toll';
  if (containsAny(text, ['高铁', '动车', '火车', '铁路'])) return 'rail';
  if (containsAny(text, ['地铁', '轨道交通'])) return 'metro';
  if (containsAny(text, ['网约车', '滴滴', '出行', '客运服务'])) return 'ride_hailing';
  if (containsAny(text, ['顺风车'])) return 'carpool';
  if (containsAny(text, ['货拉拉', '货运', '物流', '运输'])) return 'freight';
  if (containsAny(text, ['停车'])) return 'parking';
  if (containsAny(text, ['餐', '饭店', '餐饮', '酒楼', '美食'])) return 'meal';
  return 'other';
}

function containsAny(text: string, keywords: string[]) {
  return keywords.some(keyword => text.includes(keyword.toLowerCase()));
}

function normalizeExpenseItems() {
  return expenseItemList.value.map(item => ({
    id: item.id || null,
    invoiceId: null,
    invoiceIds: item.invoiceIds || [],
    expenseDate: item.expenseDate || null,
    startPlace: item.startPlace || null,
    endPlace: item.endPlace || null,
    kilometers: item.kilometers === '' || item.kilometers == null ? null : Number(item.kilometers),
    expenseType: item.expenseType || 'other',
    amount: item.invoiceIds && item.invoiceIds.length > 0 ? Number(expenseInvoiceTotal(item)) : (item.amount === '' || item.amount == null ? 0 : Number(item.amount)),
    projectCostCategory: item.projectCostCategory || null,
    customerName: item.customerName || null,
    remark: item.source === 'ocr' || (item.invoiceIds && item.invoiceIds.length > 0) ? null : (item.remark || null),
    description: item.description || null,
    source: item.source || 'manual'
  }));
}

async function downloadElectronicInvoiceRegister(reimbursementId: number) {
  const res = await exportElectronicInvoiceRegister(reimbursementId);
  downloadBlob(res.data, '电子发票登记表.xlsx');
}

async function downloadExpenseDetails(reimbursementId: number) {
  const res = await exportExpenseDetails(reimbursementId);
  downloadBlob(res.data, '报销费用明细表.xlsx');
}

async function handleBatchExport() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择需要导出的报销单');
    return;
  }
  const ids = selectedRows.value.map(row => row.id);
  const res = await exportOaPackage(ids);
  downloadBlob(res.data, 'OA报销表导出包.zip');
}

function downloadBlob(data: Blob, filename: string) {
  const url = window.URL.createObjectURL(data);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  window.URL.revokeObjectURL(url);
}

function addInvoice() {
  editingInvoiceIndex.value = -1;
  invoiceEditVisible.value = true;
}

function editInvoice(row: any) {
  const idx = invoiceList.value.indexOf(row);
  editingInvoiceIndex.value = idx;
  Object.assign(invoiceEditForm, row);
  invoiceEditVisible.value = true;
}

function saveInvoiceEdit() {
  const invoice = {...invoiceEditForm};
  if (editingInvoiceIndex.value >= 0) {
    invoiceList.value[editingInvoiceIndex.value] = invoice;
  } else {
    invoiceList.value.push(invoice);
  }
  syncReimbursementSummary();
  invoiceEditVisible.value = false;
}

async function removeInvoice(row: any, index: number) {
  try {
    await ElMessageBox.confirm('确认删除该发票？删除后会同步解除费用明细关联。', '提示');
    if (row.id && editId.value) {
      const res = await deleteInvoice(editId.value, row.id);
      invoiceList.value = res.data.invoices || [];
      syncAttachmentLists(res.data.attachments || [], res.data.invoices || []);
      expenseItemList.value = (res.data.expenseItems || []).map(normalizeExpenseItemForEdit);
      form.invoiceCount = res.data.invoiceCount || 0;
      form.amount = res.data.amount || form.amount;
      syncReimbursementSummary();
    } else {
      invoiceList.value.splice(index, 1);
      expenseItemList.value.forEach(item => {
        item.invoiceIds = (item.invoiceIds || []).filter((id: number) => id !== row.id);
        updateExpenseItemAmount(item);
      });
      syncReimbursementSummary();
    }
    ElMessage.success('删除成功');
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败');
  }
}

async function handleInvoiceSupportFileChange(row: any, uploadFile: any) {
  if (!editId.value || !row.id) {
    ElMessage.warning('请先保存报销单和发票后再上传凭证文件');
    return;
  }
  try {
    const res = await uploadInvoiceSupportAttachment(editId.value, row.id, uploadFile.raw);
    invoiceList.value = res.data.invoices || invoiceList.value;
    syncAttachmentLists(res.data.attachments || [], res.data.invoices || []);
    ElMessage.success('凭证文件上传成功，正在识别行程信息...');
    await waitInvoiceSupportOcr(row.id);
  } catch (e: any) {
    ElMessage.error('凭证文件上传失败: ' + (e.message || ''));
  }
}

function openSupportLinkDialog(row: any) {
  currentSupportInvoice.value = row;
  selectedSupportAttachment.value = null;
  supportLinkDialogVisible.value = true;
}

function handleSupportLinkSelection(row: any) {
  selectedSupportAttachment.value = row || null;
}

async function confirmSupportLink() {
  if (!editId.value || !currentSupportInvoice.value?.id || !selectedSupportAttachment.value?.id) return;
  try {
    const res = await linkInvoiceSupportAttachment(
      editId.value,
      currentSupportInvoice.value.id,
      selectedSupportAttachment.value.id
    );
    invoiceList.value = res.data.invoices || invoiceList.value;
    expenseItemList.value = res.data.expenseItems ? res.data.expenseItems.map(normalizeExpenseItemForEdit) : expenseItemList.value;
    syncAttachmentLists(res.data.attachments || [], res.data.invoices || []);
    syncReimbursementSummary();
    supportLinkDialogVisible.value = false;
    ElMessage.success('凭证已关联');
  } catch (e: any) {
    ElMessage.error('凭证关联失败: ' + (e.message || ''));
  }
}

function previewInvoiceSupportAttachment(row: any) {
  const fileUrl = row.supportAttachment?.fileUrl;
  if (fileUrl) window.open('/admin3' + fileUrl, '_blank');
}

async function waitInvoiceSupportOcr(invoiceId: number) {
  if (!editId.value) return;
  for (let i = 0; i < 60; i++) {
    await new Promise(resolve => setTimeout(resolve, 2000));
    const res = await getReimbursementDetail(editId.value);
    const data = res.data;
    invoiceList.value = data.invoices || invoiceList.value;
    expenseItemList.value = data.expenseItems ? data.expenseItems.map(normalizeExpenseItemForEdit) : expenseItemList.value;
    syncAttachmentLists(data.attachments || [], data.invoices || []);
    const invoice = (data.invoices || []).find((item: any) => item.id === invoiceId);
    const status = invoice?.supportAttachment?.ocrStatus;
    if (status === 'completed') {
      ElMessage.success('凭证文件识别完成，已同步发票行程字段');
      return;
    }
    if (status === 'failed') {
      ElMessage.warning('凭证文件识别失败，请查看文件或手工填写行程字段');
      return;
    }
  }
  ElMessage.warning('凭证文件识别仍在处理中，请稍后刷新查看');
}

async function removeInvoiceSupportAttachment(row: any) {
  if (!editId.value || !row.id) return;
  try {
    await ElMessageBox.confirm('确认解除该发票与凭证的关联？凭证文件不会删除。', '提示', {type: 'warning'});
    const res = await deleteInvoiceSupportAttachment(editId.value, row.id);
    invoiceList.value = res.data.invoices || invoiceList.value;
    syncAttachmentLists(res.data.attachments || [], res.data.invoices || []);
    ElMessage.success('凭证关联已解除');
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('凭证关联解除失败');
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

/** 预览附件图片 */
function handlePictureCardPreview(file: any) {
  window.open(file.url, '_blank');
}

/** 预览图片（编辑弹窗） */
function previewImage(file: any) {
  const url = file.url;
  if (url) window.open(url, '_blank');
}

function isPdfFile(file: any) {
  const name = String(file?.name || file?.fileName || file?.url || '').toLowerCase();
  return name.endsWith('.pdf') || String(file?.fileType || '').toLowerCase().includes('pdf');
}

function pdfPreviewUrl(url: string) {
  if (!url) return '';
  const separator = url.includes('#') ? '&' : '#';
  return `${url}${separator}toolbar=0&navpanes=0&scrollbar=1&view=FitH`;
}

/** 预览图片（详情弹窗） */
function previewAttachment(att: any) {
  const url = '/admin3' + att.fileUrl;
  if (att.fileUrl) window.open(url, '_blank');
}

async function handleDeleteAttachment(att: any) {
  try {
    if (att.attachmentRole === 'support' && att.linkedInvoiceId) {
      ElMessage.warning('该行程单已关联发票，请先在发票列表解除凭证关联');
      return;
    }
    await ElMessageBox.confirm('确认删除该附件？', '提示', {type: 'warning'});
    console.log('Deleting attachment:', att.id, att.name);
    await deleteAttachment(att.id);
    ElMessage.success('删除成功');
    // 从 fileList 中移除
    fileList.value = fileList.value.filter(f => f.id !== att.id);
    supportFileList.value = supportFileList.value.filter(f => f.id !== att.id);
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

loadExpenseTypeDict();
loadData();
</script>

<style scoped>
.invoice-expand-detail {
  padding: 8px 16px;
  font-size: 13px;
  line-height: 2.2;
}
.invoice-expand-detail .expand-label {
  color: #606266;
  font-weight: 600;
  margin-right: 4px;
}

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
.file-item {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 6px 10px;
  margin-bottom: 6px;
  background: #fafafa;
}
.file-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.support-file-header {
  display: grid;
  grid-template-columns: 28px minmax(260px, 1fr) 72px 300px 90px 84px 82px 32px 32px;
  gap: 8px;
  align-items: center;
  min-width: 980px;
}
.support-file-header .file-size {
  text-align: right;
}
.support-file-header .file-linked-invoice,
.support-file-header .file-name {
  min-width: 0;
}
.support-file-header .file-linked-invoice {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.support-file-header :deep(.el-tag),
.support-file-header :deep(.el-button) {
  justify-self: start;
}
.support-file-header :deep(.el-button) {
  margin-left: 0;
}
.file-name {
  flex: 0 1 560px;
  min-width: 360px;
  max-width: 48vw;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}
.file-size {
  color: #999;
  font-size: 12px;
  white-space: nowrap;
}
.support-file-header .file-name {
  min-width: 0;
  max-width: none;
}
.file-ocr-popover {
  margin: 0;
  max-height: 360px;
  overflow: auto;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 8px;
  font-size: 11px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}
.support-preview-popover {
  width: min(1068px, 92vw);
  height: 86vh;
  overflow: hidden;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
}
.support-preview-frame,
.support-preview-image {
  width: 100%;
  height: 100%;
  border: 0;
  object-fit: contain;
  background: #fff;
}
.file-ocr-error {
  margin-top: 4px;
  font-size: 12px;
  color: #f56c6c;
}
.detail-content,
.edit-form {
  max-height: 82vh;
  overflow-y: auto;
  padding: 0 6px 0 0;
}
.detail-section,
.form-section {
  margin-bottom: 8px;
  padding: 8px 12px 0;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}
.form-section-compact {
  padding-top: 8px;
}
.detail-section:last-child,
.form-section:last-child {
  margin-bottom: 0;
}
.detail-section-title,
.form-section-title {
  margin-bottom: 7px;
  padding-left: 9px;
  border-left: 3px solid #409eff;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
  line-height: 16px;
}
.form-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}
.form-section-header .form-section-title {
  margin-bottom: 0;
}
.detail-section-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.edit-form :deep(.el-form-item) {
  margin-bottom: 6px;
}
.reimbursement-edit-dialog :deep(.el-dialog__header) {
  padding: 12px 20px 6px;
  margin-right: 0;
}
.reimbursement-edit-dialog :deep(.el-dialog__body) {
  padding: 4px 20px 8px;
}
.reimbursement-edit-dialog :deep(.el-dialog__footer) {
  padding: 6px 20px 10px;
}
.reimbursement-edit-dialog :deep(.el-input__wrapper) {
  min-height: 28px;
}
.reimbursement-edit-dialog :deep(.el-input__inner) {
  height: 28px;
  line-height: 28px;
}
.reimbursement-edit-dialog :deep(.el-textarea__inner) {
  min-height: 34px !important;
  padding-top: 5px;
  padding-bottom: 5px;
}
.reimbursement-edit-dialog :deep(.el-table--small .el-table__cell) {
  padding: 3px 0;
}
.reimbursement-edit-dialog :deep(.el-button--small) {
  height: 24px;
  padding: 4px 8px;
}
.upload-content {
  width: 100%;
  overflow-x: auto;
}
.detail-form :deep(.el-form-item) {
  margin-bottom: 6px;
}
.detail-form :deep(.el-form-item__label) {
  color: #606266;
}
.detail-value {
  width: 100%;
  min-height: 28px;
  box-sizing: border-box;
  padding: 3px 11px;
  color: #303133;
  line-height: 20px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  word-break: break-word;
}
.detail-value-multiline {
  min-height: 34px;
  white-space: pre-wrap;
}
.detail-amount {
  color: #f56c6c;
  font-weight: 600;
}
.detail-attachment-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-bottom: 8px;
}
.detail-attachment-item {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 300px 96px 84px 32px;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 6px 10px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}
.detail-attachment-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}
.detail-attachment-invoice {
  min-width: 0;
  overflow: hidden;
  color: #606266;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.detail-attachment-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}
.detail-attachment-tabs :deep(.el-button) {
  margin-left: 0;
}
.file-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 22px;
  flex: 0 0 24px;
  color: #606266;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
}
.detail-empty {
  padding: 12px 0;
  color: #909399;
  text-align: center;
}
.invoice-no-text {
  font-family: Consolas, "Courier New", monospace;
  white-space: nowrap;
}
.invoice-support-upload {
  display: inline-block;
  margin: 0 2px;
  vertical-align: middle;
}
.invoice-actions {
  display: grid;
  grid-template-columns: repeat(3, max-content);
  align-items: center;
  gap: 6px 8px;
}
.invoice-actions :deep(.el-button) {
  margin-left: 0;
}
.row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}
.row-actions :deep(.el-button) {
  margin-left: 0;
}
.expense-link-form :deep(.el-form-item) {
  margin-bottom: 12px;
}
.expense-link-tip {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  line-height: 16px;
}
.expense-link-panels {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 86px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}
.expense-link-title {
  margin-bottom: 8px;
  color: #303133;
  font-weight: 600;
}
.expense-link-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: stretch;
}
.detail-timeline {
  padding: 4px 0 0 8px;
}
.ocr-animation {
  display: inline-flex;
  align-items: center;
}
.dot-animation::after {
  content: '';
  animation: dot-pulse 1.5s steps(4, end) infinite;
}
@keyframes dot-pulse {
  0% { content: ''; }
  25% { content: ' .'; }
  50% { content: ' ..'; }
  75% { content: ' ...'; }
  100% { content: ''; }
}
</style>

