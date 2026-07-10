import sys
vue_path = "D:\\hongmengProjects\\admin3\\admin3-ui\\src\\views\\reimbursement-list.vue"
with open(vue_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add invoice list reactive data after the form definition
old_form_end = "  remarks: ''\n});\n"
new_form_end = old_form_end + """const invoiceList = ref<any[]>([]);
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
  remarks: ''
});
"""

if old_form_end in content:
    content = content.replace(old_form_end, new_form_end, 1)
    print("1. Added invoice reactive data")
else:
    print("1. ERROR: Could not find form end marker")

# 2. Add functions after the existing functions
func_insert = """function addInvoice() {
  editingInvoiceIndex.value = -1;
  Object.assign(invoiceEditForm, {
    invoiceNo: '', invoiceCode: '', invoiceDate: '', invoiceType: '', invoiceStatus: '',
    totalAmount: '', amountBeforeTax: '', taxRate: '', taxAmount: '',
    buyerName: '', sellerName: '', buyerTaxId: '', sellerTaxId: '',
    payee: '', reviewer: '', issuer: '',
    buyerAddressPhone: '', buyerBankInfo: '', sellerAddressPhone: '', sellerBankInfo: '', remarks: ''
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
  ElMessage.success('发票已保存');
}

function removeInvoice(index: number) {
  invoiceList.value.splice(index, 1);
  ElMessage.success('发票已移除');
}
"""

# Find a good insertion point - after handleDeleteInvoice function
search_marker = "async function handleDeleteInvoice"
idx = content.find(search_marker)
if idx >= 0:
    # Find the end of this function (next async function or non-fn code)
    after_fn = content.find("\nfunction ", idx + 50)
    if after_fn < 0:
        after_fn = content.find("\nconst ", idx + 50)
    if after_fn < 0:
        after_fn = content.find("\nlet ", idx + 50)
    if after_fn >= 0:
        content = content[:after_fn] + "\n" + func_insert + content[after_fn:]
        print(f"2. Added invoice functions at position {after_fn}")
    else:
        print("2. ERROR: Could not find function insertion point")
else:
    print("2. ERROR: Could not find handleDeleteInvoice")

# 3. Modify handleEdit to populate invoiceList
old_handle_edit_load = """    fileList.value = [];
    uploadedAttachmentIds.value = [];
    if (data.attachments) {
      dialogVisible.value = true;
"""

new_handle_edit_load = """    fileList.value = [];
    uploadedAttachmentIds.value = [];
    if (data.invoices) {
      invoiceList.value = data.invoices.map((inv: any) => ({ ...inv }));
    } else if (data.invoiceTemps) {
      invoiceList.value = data.invoiceTemps.map((t: any) => ({ ...t }));
    } else {
      invoiceList.value = [];
    }
    if (data.attachments) {
      dialogVisible.value = true;
"""

if old_handle_edit_load in content:
    content = content.replace(old_handle_edit_load, new_handle_edit_load, 1)
    print("3. Modified handleEdit")
else:
    print("3. ERROR: Could not find handleEdit marker")

# 4. Modify form reset to clear invoiceList
old_reset = """  form.remarks = '';
  fileList.value = [];"""
new_reset = """  form.remarks = '';
  invoiceList.value = [];
  fileList.value = [];"""

if old_reset in content:
    content = content.replace(old_reset, new_reset, 1)
    print("4. Updated form reset")
else:
    print("4. ERROR: Could not find form reset marker")

# 5. Modify handleOcrSuccess to add to invoiceList instead of filling flat fields
old_ocr_fill = """    // update description
    const descParts: string[] = [];
    if (fields.invoiceNo) descParts.push(`发票号码: ${fields.invoiceNo}`);
    if (fields.invoiceCode) descParts.push(`发票代码: ${fields.invoiceCode}`);
    if (fields.invoiceDate) descParts.push(`开票日期 ${fields.invoiceDate.substring(0, 10)}`);
    if (fields.buyerName) descParts.push(`购买方: ${fields.buyerName}`);
    if (fields.sellerName) descParts.push(`销售方: ${fields.sellerName}`);
    if (fields.buyerTaxId) descParts.push(`购买方税号: ${fields.buyerTaxId}`);
    if (fields.sellerTaxId) descParts.push(`销售方税号: ${fields.sellerTaxId}`);
    if (fields.invoiceType) descParts.push(`发票类型: ${fields.invoiceType}`);
    if (fields.amount) descParts.push(`金额: ¥${fields.amount}`);
    if (descParts.length > 0 && !form.description) {
      form.description = descParts.join('\\n');
    }"""

new_ocr_fill = """    // add to invoice list instead of filling flat fields
    const newInvoice: any = {
      invoiceNo: fields.invoiceNo || '',
      invoiceCode: fields.invoiceCode || '',
      invoiceDate: fields.invoiceDate || '',
      invoiceType: fields.invoiceType || '',
      invoiceStatus: fields.invoiceStatus || '',
      totalAmount: fields.amount || '',
      amountBeforeTax: fields.amountBeforeTax || '',
      taxRate: fields.taxRate || '',
      taxAmount: fields.taxAmount || '',
      buyerName: fields.buyerName || '',
      sellerName: fields.sellerName || '',
      buyerTaxId: fields.buyerTaxId || '',
      sellerTaxId: fields.sellerTaxId || '',
      payee: fields.payee || '',
      reviewer: fields.reviewer || '',
      issuer: fields.issuer || '',
      buyerAddressPhone: fields.buyerAddressPhone || '',
      buyerBankInfo: fields.buyerBankInfo || '',
      sellerAddressPhone: fields.sellerAddressPhone || '',
      sellerBankInfo: fields.sellerBankInfo || '',
      remarks: ''
    };
    invoiceList.value.push(newInvoice);"""

if old_ocr_fill in content:
    content = content.replace(old_ocr_fill, new_ocr_fill, 1)
    print("5. Modified handleOcrSuccess")
else:
    print("5. ERROR: Could not find OCR fill marker")

# 6. Remove flat form field references from save function  
# Remove invoiceStatus from save payload (replaced by invoiceList)
# Also remove other flat invoice fields
old_save_remove = """    invoiceNo: form.invoiceNo || undefined,
    invoiceCode: form.invoiceCode || undefined,
    invoiceDate: form.invoiceDate || undefined,
    buyerName: form.buyerName || undefined,
    sellerName: form.sellerName || undefined,
    buyerTaxId: form.buyerTaxId || undefined,
    sellerTaxId: form.sellerTaxId || undefined,
    invoiceType: form.invoiceType || undefined,
    invoiceStatus: form.invoiceStatus || undefined,
    amountBeforeTax: form.amountBeforeTax || undefined,
    taxRate: form.taxRate || undefined,
    taxAmount: form.taxAmount || undefined,
    payee: form.payee || undefined,
    reviewer: form.reviewer || undefined,
    issuer: form.issuer || undefined,
    buyerAddressPhone: form.buyerAddressPhone || undefined,
    buyerBankInfo: form.buyerBankInfo || undefined,
    sellerAddressPhone: form.sellerAddressPhone || undefined,
    sellerBankInfo: form.sellerBankInfo || undefined,
    remarks: form.remarks || undefined,"""

new_save = """    invoiceList: invoiceList.value,"""

if old_save_remove in content:
    content = content.replace(old_save_remove, new_save, 1)
    print("6. Updated save function")
else:
    print("6. ERROR: Could not find save function marker")
    # Try a shorter marker
    alt_marker = "sellerBankInfo: form.sellerBankInfo || undefined,"
    if alt_marker in content:
        print("   Found shorter marker")
    else:
        print("   Shorter marker also not found")

# Write back
with open(vue_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Done!")
