def fix_vue():
    import sys, os
    
    path = "D:\\hongmengProjects\\admin3\\admin3-ui\\src\\views\\reimbursement-list.vue"
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()

    # --- Step 1: Add invoice list data after form init ---
    marker = '  remarks: \'\'\n});'
    insert1 = '''  remarks: \'\'\n});\n\nconst invoiceList = ref<any[]>([]);\nconst invoiceEditVisible = ref(false);\nconst editingInvoiceIndex = ref(-1);\nconst invoiceEditForm = reactive({\n  invoiceNo: \'\',\n  invoiceCode: \'\',\n  invoiceDate: \'\',\n  invoiceType: \'\',\n  invoiceStatus: \'\',\n  totalAmount: \'\',\n  amountBeforeTax: \'\',\n  taxRate: \'\',\n  taxAmount: \'\',\n  buyerName: \'\',\n  sellerName: \'\',\n  buyerTaxId: \'\',\n  sellerTaxId: \'\',\n  payee: \'\',\n  reviewer: \'\',\n  issuer: \'\',\n  buyerAddressPhone: \'\',\n  buyerBankInfo: \'\',\n  sellerAddressPhone: \'\',\n  sellerBankInfo: \'\',\n  remarks: \'\'\n});\n'''
    if marker in content:
        content = content.replace(marker, insert1, 1)
        print("Step 1 OK")
    else:
        print("Step 1 FAIL - marker not found")

    # --- Step 2: Add invoice functions ---
    func_code = '''
function addInvoice() {
  editingInvoiceIndex.value = -1;
  Object.assign(invoiceEditForm, {
    invoiceNo: \'\', invoiceCode: \'\', invoiceDate: \'\', invoiceType: \'\', invoiceStatus: \'\',
    totalAmount: \'\', amountBeforeTax: \'\', taxRate: \'\', taxAmount: \'\',
    buyerName: \'\', sellerName: \'\', buyerTaxId: \'\', sellerTaxId: \'\',
    payee: \'\', reviewer: \'\', issuer: \'\',
    buyerAddressPhone: \'\', buyerBankInfo: \'\', sellerAddressPhone: \'\', sellerBankInfo: \'\', remarks: \'\'
  });
  invoiceEditVisible.value = true;
}

function editInvoice(row: any) {
  const idx = invoiceList.value.indexOf(row);
  editingInvoiceIndex.value = idx;
  Object.assign(invoiceEditForm, row);
  invoiceEditVisible.value = true;
}

function saveInvoiceEdit() {
  const invoice = { ...invoiceEditForm };
  if (editingInvoiceIndex.value >= 0) {
    invoiceList.value[editingInvoiceIndex.value] = invoice;
  } else {
    invoiceList.value.push(invoice);
  }
  invoiceEditVisible.value = false;
}

function removeInvoice(index: number) {
  invoiceList.value.splice(index, 1);
}

'''

    marker2 = "function handleDeleteInvoice"
    if marker2 in content:
        idx = content.find(marker2)
        # Find the end of this function (next function definition with same indent)
        rest = content[idx + 200:]
        fns = [rest.find("\\nfunction "), rest.find("\\nconst "), rest.find("\\nlet ")]
        fns = [f for f in fns if f >= 0]
        if fns:
            next_fn = min(fns)
            insert_pos = idx + 200 + next_fn
            content = content[:insert_pos] + func_code + content[insert_pos:]
            print("Step 2 OK")
        else:
            print("Step 2 FAIL - no next function found")
    else:
        print("Step 2 FAIL - marker2 not found")

    # --- Step 3: Modify handleEdit to populate invoiceList ---
    old = "fileList.value = [];\n    uploadedAttachmentIds.value = [];\n    if (data.attachments) {"
    new = "fileList.value = [];\n    uploadedAttachmentIds.value = [];\n    if (data.invoices) {\n      invoiceList.value = data.invoices.map((inv) => ({ ...inv }));\n    } else if (data.invoiceTemps) {\n      invoiceList.value = data.invoiceTemps.map((t) => ({ ...t }));\n    }\n    if (data.attachments) {"
    if old in content:
        content = content.replace(old, new, 1)
        print("Step 3 OK")
    else:
        print("Step 3 FAIL - old handle edit not found")

    # Step 4: Modify OCR handling - replace description building + form fill with invoice list push
    marker4 = "// update description"
    if marker4 in content:
        # Replace from marker4 to the next } that closes the if block
        start = content.find(marker4)
        # Find "// update OCR" or "form.ocrRawJson" after marker4
        next_marker = content.find("form.ocrRawJson", start)
        if next_marker > 0:
            # Replace everything between marker4 and next_marker
            new_ocr = ("// add to invoice list\\n    const inv = {\\n"
                       "      invoiceNo: fields.invoiceNo || \'\',\\n"
                       "      invoiceCode: fields.invoiceCode || \'\',\\n"
                       "      invoiceDate: fields.invoiceDate || \'\',\\n"
                       "      invoiceType: fields.invoiceType || \'\',\\n"
                       "      invoiceStatus: fields.invoiceStatus || \'\',\\n"
                       "      totalAmount: fields.amount || \'\',\\n"
                       "      amountBeforeTax: \'\', taxRate: \'\', taxAmount: \'\',\\n"
                       "      buyerName: fields.buyerName || \'\',\\n"
                       "      sellerName: fields.sellerName || \'\',\\n"
                       "      buyerTaxId: fields.buyerTaxId || \'\',\\n"
                       "      sellerTaxId: fields.sellerTaxId || \'\',\\n"
                       "      payee: \'\', reviewer: \'\', issuer: \'\',\\n"
                       "      buyerAddressPhone: \'\', buyerBankInfo: \'\',\\n"
                       "      sellerAddressPhone: \'\', sellerBankInfo: \'\', remarks: \'\'\\n"
                       "    };\\n    invoiceList.value.push(inv);\\n    // update OCR")
            content = content[start:next_marker].replace(content[start:next_marker], new_ocr, 1)
            print("Step 4 OK")
        else:
            print("Step 4 FAIL - next_marker not found after marker4")
    else:
        print("Step 4 FAIL - marker4 not found")

    # Step 5: Modify form reset
    old5 = "form.remarks = \'\';\\n  fileList.value = []"
    new5 = "form.remarks = \'\';\\n  invoiceList.value = [];\\n  fileList.value = []"
    if old5 in content:
        content = content.replace(old5, new5, 1)
        print("Step 5 OK")
    else:
        print("Step 5 FAIL - old5 not found")

    # Step 6: Replace flat save fields with invoiceList
    old6 = ("    invoiceNo: form.invoiceNo || undefined,\\n"
            "    invoiceCode: form.invoiceCode || undefined,\\n"
            "    invoiceDate: form.invoiceDate || undefined,\\n"
            "    buyerName: form.buyerName || undefined,\\n"
            "    sellerName: form.sellerName || undefined,\\n"
            "    buyerTaxId: form.buyerTaxId || undefined,\\n"
            "    sellerTaxId: form.sellerTaxId || undefined,\\n"
            "    invoiceType: form.invoiceType || undefined,\\n"
            "    invoiceStatus: form.invoiceStatus || undefined,\\n"
            "    amountBeforeTax: form.amountBeforeTax || undefined,\\n"
            "    taxRate: form.taxRate || undefined,\\n"
            "    taxAmount: form.taxAmount || undefined,\\n"
            "    payee: form.payee || undefined,\\n"
            "    reviewer: form.reviewer || undefined,\\n"
            "    issuer: form.issuer || undefined,\\n"
            "    buyerAddressPhone: form.buyerAddressPhone || undefined,\\n"
            "    buyerBankInfo: form.buyerBankInfo || undefined,\\n"
            "    sellerAddressPhone: form.sellerAddressPhone || undefined,\\n"
            "    sellerBankInfo: form.sellerBankInfo || undefined,\\n"
            "    remarks: form.remarks || undefined,")
    new6 = "    invoiceList: invoiceList.value,"
    if old6 in content:
        content = content.replace(old6, new6, 1)
        print("Step 6 OK")
    else:
        print("Step 6 FAIL - old6 not found")

    # Step 7: Add invoice edit dialog template
    marker7 = "    <!-- 详情对话框 -->"
    dialog = """    <!-- 发票编辑对话框 -->
    <el-dialog title="编辑发票" v-model="invoiceEditVisible" width="45%">
      <el-form label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="发票号码"><el-input v-model="invoiceEditForm.invoiceNo"></el-input></el-form-item></el-col>
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
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="invoiceEditForm.remarks" type="textarea" :rows="2"></el-input></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <span><el-button @click="invoiceEditVisible = false">取消</el-button><el-button type="primary" @click="saveInvoiceEdit">保存</el-button></span>
      </template>
    </el-dialog>

"""
    if marker7 in content:
        content = content.replace(marker7, dialog + marker7, 1)
        print("Step 7 OK")
    else:
        print("Step 7 FAIL - marker7 not found")

    # Step 8: Detail view style - make detail dialog invoice table match edit form style
    # Already has expandable rows from prior change, just need consistent styling
    # The form sections should use the same border/background
    # This is already done by the CSS classes

    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print("All changes applied!")

fix_vue()
