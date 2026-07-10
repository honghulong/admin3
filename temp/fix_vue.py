import sys
try:
    sys.stdout.reconfigure(encoding="utf-8")
except:
    pass

with open("D:\\hongmengProjects\\admin3\\admin3-ui\\src\\views\\reimbursement-list.vue", "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add invoiceStatus to parsed fields in invoice_fields block
content = content.replace(
    'if (invoiceFields.invoice_type) fields.invoiceType = mapInvoiceType(invoiceFields.invoice_type);',
    'if (invoiceFields.invoice_type) fields.invoiceType = mapInvoiceType(invoiceFields.invoice_type);\n      if (invoiceFields.invoice_status) fields.invoiceStatus = invoiceFields.invoice_status;'
)

# 2. Add regex fallback for sellerTaxId, buyerTaxId, invoiceStatus
content = content.replace(
    'if (!fields.sellerName && ocrText.includes(\'\u9500\u552e\u65b9\')) {\n        const idx = ocrText.indexOf(\'\u9500\u552e\u65b9\');\n        const after = ocrText.substring(idx);\n        const m = after.match(/\u540d[\u79f0:]\s*([^\n]{2,50})/);\n        if (m) fields.sellerName = m[1].trim();\n      }',
    'if (!fields.sellerName && ocrText.includes(\'\u9500\u552e\u65b9\')) {\n        const idx = ocrText.indexOf(\'\u9500\u552e\u65b9\');\n        const after = ocrText.substring(idx);\n        const m = after.match(/\u540d[\u79f0:]\s*([^\n]{2,50})/);\n        if (m) fields.sellerName = m[1].trim();\n      }\n      if (!fields.sellerTaxId) {\n        const sellerIdx = ocrText.indexOf(\'\u9500\u552e\u65b9\');\n        if (sellerIdx >= 0) {\n          const afterSeller = ocrText.substring(sellerIdx);\n          const taxMatch = afterSeller.match(/\u7eb3\u7a0e\u4eba\u8bc6\u522b\u53f7[\uff1a:]\s*([\\dA-Za-z]{15,20})/);\n          if (taxMatch) fields.sellerTaxId = taxMatch[1];\n        }\n      }\n      if (!fields.buyerTaxId) {\n        const buyerIdx = ocrText.indexOf(\'\u8d2d\u4e70\u65b9\');\n        if (buyerIdx >= 0) {\n          const afterBuyer = ocrText.substring(buyerIdx);\n          const taxMatch = afterBuyer.match(/\u7eb3\u7a0e\u4eba\u8bc6\u522b\u53f7[\uff1a:]\s*([\\dA-Za-z]{15,20})/);\n          if (taxMatch) fields.buyerTaxId = taxMatch[1];\n        }\n      }\n      if (!fields.invoiceStatus) {\n        if (ocrText.includes(\'\u4f5c\u5e9f\')) fields.invoiceStatus = \'voided\';\n        else if (ocrText.includes(\'\u7ea2\u51b2\') or ocrText.includes(\'\u7ea2\u5b57\')) fields.invoiceStatus = \'red\';\n        else if (ocrText.includes(\'\u6b63\u5e38\')) fields.invoiceStatus = \'normal\';\n      }'
)

# 3. Add invoiceStatus to form fill
content = content.replace(
    'if (fields.invoiceType) form.invoiceType = fields.invoiceType;\n    // update description',
    'if (fields.invoiceType) form.invoiceType = fields.invoiceType;\n    if (fields.invoiceStatus) form.invoiceStatus = fields.invoiceStatus;\n    // update description'
)

# 4. Make description manual
content = content.replace(
    'if (descParts.length > 0) {\n      form.description = descParts.join(\'\\n\');\n    }',
    'if (descParts.length > 0 && !form.description) {\n      form.description = descParts.join(\'\\n\');\n    }'
)

# 5. Add invoiceCount to edit form
old_section = '<el-col :span="12">\n            <el-form-item label="\u62a5\u9500\u91d1\u989d">\n              <el-input v-model="form.amount" type="number" step="0.01" min="0.01" placeholder="\u8bf7\u8f93\u5165\u91d1\u989d"></el-input>\n            </el-form-item>\n          </el-col>\n          </el-row>'
new_section = '<el-col :span="12">\n            <el-form-item label="\u62a5\u9500\u91d1\u989d">\n              <el-input v-model="form.amount" type="number" step="0.01" min="0.01" placeholder="\u8bf7\u8f93\u5165\u91d1\u989d"></el-input>\n            </el-form-item>\n          </el-col>\n          <el-col :span="12">\n            <el-form-item label="\u6709\u6548\u53d1\u7968\u6570">\n              <el-input v-model="form.invoiceCount" disabled placeholder="\u81ea\u52a8\u8ba1\u7b97" style="color: #909399"></el-input>\n            </el-form-item>\n          </el-col>\n          </el-row>'
content = content.replace(old_section, new_section)

print("Step 5 done")

# 6. Add expanded invoice fields after invoiceStatus
old_end = '          </el-col>\n          </el-row>\n        </div>\n\n        <div class="form-section">\n          <div class="form-section-title">\u8865\u5145\u4fe1\u606f</div>'
new_end = old_end[:0]
print("Old end found:", old_end in content)
content = content.replace(
    '          </el-col>\n          </el-row>\n        </div>\n\n        <div class="form-section">\n          <div class="form-section-title">补充信息</div>',
    '          </el-col>\n          </el-row>\n          <el-row :gutter="20">\n          <el-col :span="12">\n            <el-form-item label="金额(不含税)">\n              <el-input v-model="form.amountBeforeTax" placeholder="不含税金额"></el-input>\n            </el-form-item>\n          </el-col>\n          <el-col :span="12">\n            <el-form-item label="税率">\n              <el-input v-model="form.taxRate" placeholder="税率"></el-input>\n            </el-form-item>\n          </el-col>\n          <el-col :span="12">\n            <el-form-item label="税额">\n              <el-input v-model="form.taxAmount" placeholder="税额"></el-input>\n            </el-form-item>\n          </el-col>\n          <el-col :span="12">\n            <el-form-item label="收款人">\n              <el-input v-model="form.payee" placeholder="收款人"></el-input>\n            </el-form-item>\n          </el-col>\n          <el-col :span="12">\n            <el-form-item label="复核人">\n              <el-input v-model="form.reviewer" placeholder="复核人"></el-input>\n            </el-form-item>\n          </el-col>\n          <el-col :span="12">\n            <el-form-item label="开票人">\n              <el-input v-model="form.issuer" placeholder="开票人"></el-input>\n            </el-form-item>\n          </el-col>\n          <el-col :span="12">\n            <el-form-item label="购买方地址/电话">\n              <el-input v-model="form.buyerAddressPhone" placeholder="购买方地址电话"></el-input>\n            </el-form-item>\n          </el-col>\n          <el-col :span="12">\n            <el-form-item label="购买方开户行账号">\n              <el-input v-model="form.buyerBankInfo" placeholder="购买方开户行及账号"></el-input>\n            </el-form-item>\n          </el-col>\n          <el-col :span="12">\n            <el-form-item label="销售方地址/电话">\n              <el-input v-model="form.sellerAddressPhone" placeholder="销售方地址电话"></el-input>\n            </el-form-item>\n          </el-col>\n          <el-col :span="12">\n            <el-form-item label="销售方开户行账号">\n              <el-input v-model="form.sellerBankInfo" placeholder="销售方开户行及账号"></el-input>\n            </el-form-item>\n          </el-col>\n          <el-col :span="24">\n            <el-form-item label="备注">\n              <el-input v-model="form.remarks" type="textarea" :rows="2" placeholder="备注"></el-input>\n            </el-form-item>\n          </el-col>\n          </el-row>\n        </div>\n\n        <div class="form-section">\n          <div class="form-section-title">补充信息</div>'
)

print("Step 6 done")

# 7. Add expandable column to detail invoice table
old_table = '<el-table v-if="detailData.invoices && detailData.invoices.length > 0" :data="detailData.invoices" border size="small" class="invoice-table">'
new_rows = '''              <el-table-column type="expand">
                <template #default="{ row }">
                  <div class="invoice-expand-detail">
                    <el-row :gutter="16">
                      <el-col :span="8"><span class="expand-label">购买方：</span>{{ row.buyerName || \'-\' }}</el-col>
                      <el-col :span="8"><span class="expand-label">购买方税号：</span>{{ row.buyerTaxId || \'-\' }}</el-col>
                      <el-col :span="8"><span class="expand-label">销售方税号：</span>{{ row.sellerTaxId || \'-\' }}</el-col>
                      <el-col :span="8"><span class="expand-label">金额(不含税)：</span>&yen;{{ formatMoney(row.amountBeforeTax) }}</el-col>
                      <el-col :span="8"><span class="expand-label">税率：</span>{{ row.taxRate || \'-\' }}</el-col>
                      <el-col :span="8"><span class="expand-label">税额：</span>&yen;{{ formatMoney(row.taxAmount) }}</el-col>
                      <el-col :span="8"><span class="expand-label">收款人：</span>{{ row.payee || \'-\' }}</el-col>
                      <el-col :span="8"><span class="expand-label">复核人：</span>{{ row.reviewer || \'-\' }}</el-col>
                      <el-col :span="8"><span class="expand-label">开票人：</span>{{ row.issuer || \'-\' }}</el-col>
                      <el-col :span="12"><span class="expand-label">购买方地址/电话：</span>{{ row.buyerAddressPhone || \'-\' }}</el-col>
                      <el-col :span="12"><span class="expand-label">购买方开户行/账号：</span>{{ row.buyerBankInfo || \'-\' }}</el-col>
                      <el-col :span="12"><span class="expand-label">销售方地址/电话：</span>{{ row.sellerAddressPhone || \'-\' }}</el-col>
                      <el-col :span="12"><span class="expand-label">销售方开户行/账号：</span>{{ row.sellerBankInfo || \'-\' }}</el-col>
                      <el-col :span="24"><span class="expand-label">备注：</span>{{ row.remarks || \'-\' }}</el-col>
                    </el-row>
                  </div>
                </template>
              </el-table-column>'''
content = content.replace(old_table, old_table + '\n' + new_rows)

# 8. Add expanded fields to form data init
content = content.replace(
    'invoiceStatus: \'\'\n});\n',
    'invoiceStatus: \'\',\n  amountBeforeTax: \'\',\n  taxRate: \'\',\n  taxAmount: \'\',\n  payee: \'\',\n  reviewer: \'\',\n  issuer: \'\',\n  buyerAddressPhone: \'\',\n  buyerBankInfo: \'\',\n  sellerAddressPhone: \'\',\n  sellerBankInfo: \'\',\n  remarks: \'\'\n});\n'
)

# 9. Add expanded fields to form reset
content = content.replace(
    "form.invoiceStatus = '';\n  fileList.value = [];",
    "form.invoiceStatus = '';\n  form.amountBeforeTax = '';\n  form.taxRate = '';\n  form.taxAmount = '';\n  form.payee = '';\n  form.reviewer = '';\n  form.issuer = '';\n  form.buyerAddressPhone = '';\n  form.buyerBankInfo = '';\n  form.sellerAddressPhone = '';\n  form.sellerBankInfo = '';\n  form.remarks = '';\n  fileList.value = [];"
)

# 10. Add expanded fields to handleEdit
content = content.replace(
    "form.invoiceStatus = data.invoiceStatus || '';\n  fileList.value = [];",
    "form.invoiceStatus = data.invoiceStatus || '';\n  form.amountBeforeTax = data.amountBeforeTax || '';\n  form.taxRate = data.taxRate || '';\n  form.taxAmount = data.taxAmount || '';\n  form.payee = data.payee || '';\n  form.reviewer = data.reviewer || '';\n  form.issuer = data.issuer || '';\n  form.buyerAddressPhone = data.buyerAddressPhone || '';\n  form.buyerBankInfo = data.buyerBankInfo || '';\n  form.sellerAddressPhone = data.sellerAddressPhone || '';\n  form.sellerBankInfo = data.sellerBankInfo || '';\n  form.remarks = data.remarks || '';\n  fileList.value = [];"
)

# 11. Add expanded fields to save payload
content = content.replace(
    "invoiceStatus: form.invoiceStatus || undefined,\n    attachmentIds: uploadedAttachmentIds.value",
    "invoiceStatus: form.invoiceStatus || undefined,\n    amountBeforeTax: form.amountBeforeTax || undefined,\n    taxRate: form.taxRate || undefined,\n    taxAmount: form.taxAmount || undefined,\n    payee: form.payee || undefined,\n    reviewer: form.reviewer || undefined,\n    issuer: form.issuer || undefined,\n    buyerAddressPhone: form.buyerAddressPhone || undefined,\n    buyerBankInfo: form.buyerBankInfo || undefined,\n    sellerAddressPhone: form.sellerAddressPhone || undefined,\n    sellerBankInfo: form.sellerBankInfo || undefined,\n    remarks: form.remarks || undefined,\n    attachmentIds: uploadedAttachmentIds.value"
)

# 12. Add CSS for expandable detail
css_block = '.invoice-expand-detail {\n  padding: 8px 16px;\n  font-size: 13px;\n  line-height: 2.2;\n}\n.invoice-expand-detail .expand-label {\n  color: #606266;\n  font-weight: 600;\n  margin-right: 4px;\n}\n'
content = content.replace('<style scoped>', '<style scoped>\n' + css_block)

with open("D:\\hongmengProjects\\admin3\\admin3-ui\\src\\views\\reimbursement-list.vue", "w", encoding="utf-8") as f:
    f.write(content)

print("All frontend changes applied!")
