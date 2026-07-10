import sys
sys.stdout.reconfigure(encoding='utf-8')

vue_path = "D:\\hongmengProjects\\admin3\\admin3-ui\\src\\views\\reimbursement-list.vue"
with open(vue_path, "r", encoding="utf-8") as f:
    content = f.read()

# Find the section to replace
start_marker = '<div class="form-section-title">发票信息</div>'
end_section = '<div class="form-section">\n          <div class="form-section-title">补充信息</div>'

start_idx = content.find(start_marker)
end_idx = content.find(end_section)

if start_idx < 0 or end_idx < 0:
    print("ERROR: Could not find markers")
    sys.exit(1)

# Find the beginning of this section (the div.form-section before invoice info)
prev_section = content.rfind('<div class="form-section">', 0, start_idx)

# The new invoice table section
new_section = '''        <div class="form-section">
          <div class="form-section-title">发票列表</div>
          <div style="margin-bottom: 10px">
            <el-button type="primary" size="small" @click="addInvoice">+ 添加发票</el-button>
          </div>
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
                    <el-col :span="24"><span class="expand-label">备注：</span>{{ row.remarks }}</el-col>
                  </el-row>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="invoiceNo" label="发票号码" width="120"></el-table-column>
            <el-table-column prop="invoiceCode" label="发票代码" width="120"></el-table-column>
            <el-table-column prop="invoiceDate" label="开票日期" width="140"></el-table-column>
            <el-table-column prop="sellerName" label="销售方" width="150"></el-table-column>
            <el-table-column prop="totalAmount" label="金额" width="100" align="right">
              <template #default="{ row }">¥{{ formatMoney(row.totalAmount) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row, $index }">
                <el-button text size="small" type="primary" @click="editInvoice(row)">编辑</el-button>
                <el-button text size="small" type="danger" @click="removeInvoice($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="invoiceList.length === 0" style="padding: 20px 0; color: #909399; text-align: center;">
            暂无发票，请上传凭证或点击"添加发票"手动录入
          </div>
        </div>'''

# Replace the old section
old_section = content[prev_section:end_idx]
content = content.replace(old_section, new_section, 1)

# Write back
with open(vue_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Invoice section replaced successfully")
print(f"Old length: {len(old_section)}, New length: {len(new_section)}")
