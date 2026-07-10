import sys

path = "D:\\hongmengProjects\\admin3\\admin3-ui\\src\\views\\reimbursement-list.vue"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

# Step 2: Find handleDeleteInvoice and insert invoice functions before it
marker = "function handleDeleteInvoice"
if marker in content:
    idx = content.find(marker)
    rest = content[idx + 200:]
    next_fn = sorted([x for x in [rest.find("\nfunction "), rest.find("\nconst ")] if x >= 0])
    if next_fn:
        ins = idx + 200 + next_fn[0]
        funcs = "\nfunction addInvoice() {\n  editingInvoiceIndex.value = -1;\n  Object.assign(invoiceEditForm, {\n    invoiceNo: '', invoiceCode: '', invoiceDate: '', invoiceType: '', invoiceStatus: '',\n    totalAmount: '', amountBeforeTax: '', taxRate: '', taxAmount: '',\n    buyerName: '', sellerName: '', buyerTaxId: '', sellerTaxId: '',\n    payee: '', reviewer: '', issuer: '',\n    buyerAddressPhone: '', buyerBankInfo: '', sellerAddressPhone: '', sellerBankInfo: '', remarks: ''\n  });\n  invoiceEditVisible.value = true;\n}\n\nfunction editInvoice(row) {\n  const idx = invoiceList.value.indexOf(row);\n  editingInvoiceIndex.value = idx;\n  Object.assign(invoiceEditForm, row);\n  invoiceEditVisible.value = true;\n}\n\nfunction saveInvoiceEdit() {\n  const invoice = { ...invoiceEditForm };\n  if (editingInvoiceIndex.value >= 0) {\n    invoiceList.value[editingInvoiceIndex.value] = invoice;\n  } else {\n    invoiceList.value.push(invoice);\n  }\n  invoiceEditVisible.value = false;\n}\n\nfunction removeInvoice(index) {\n  invoiceList.value.splice(index, 1);\n}\n"
        content = content[:ins] + funcs + content[ins:]
        print("Step 2 OK")
    else:
        print("Step 2 NO NEXT FN")
else:
    print("Step 2 NO MARKER")

# Step 3: handleEdit - populate invoiceList
old3 = 'fileList.value = [];\n    uploadedAttachmentIds.value = [];\n    if (data.attachments) {'
new3 = 'fileList.value = [];\n    uploadedAttachmentIds.value = [];\n    if (data.invoices) {\n      invoiceList.value = data.invoices.map(function(inv) { return { ...inv }; });\n    } else if (data.invoiceTemps) {\n      invoiceList.value = data.invoiceTemps.map(function(t) { return { ...t }; });\n    }\n    if (data.attachments) {'
if old3 in content:
    content = content.replace(old3, new3, 1)
    print("Step 3 OK")
else:
    print("Step 3 NOT FOUND")

# Step 4: OCR - replace desc building + form fill with invoice list push
m4 = "// update description"
if m4 in content:
    s = content.find(m4)
    e = content.find("// update OCR", s)
    if e > 0:
        new_ocr = "// add to invoice list\n    invoiceList.value.push({\n      invoiceNo: fields.invoiceNo || '',\n      invoiceCode: fields.invoiceCode || '',\n      invoiceDate: fields.invoiceDate || '',\n      invoiceType: fields.invoiceType || '',\n      invoiceStatus: fields.invoiceStatus || '',\n      totalAmount: fields.amount || '',\n      amountBeforeTax: fields.amountBeforeTax || '',\n      taxRate: fields.taxRate || '',\n      taxAmount: fields.taxAmount || '',\n      buyerName: fields.buyerName || '',\n      sellerName: fields.sellerName || '',\n      buyerTaxId: fields.buyerTaxId || '',\n      sellerTaxId: fields.sellerTaxId || '',\n      payee: fields.payee || '',\n      reviewer: fields.reviewer || '',\n      issuer: fields.issuer || '',\n      buyerAddressPhone: fields.buyerAddressPhone || '',\n      buyerBankInfo: fields.buyerBankInfo || '',\n      sellerAddressPhone: fields.sellerAddressPhone || '',\n      sellerBankInfo: fields.sellerBankInfo || '',\n      remarks: ''\n    });\n    // update OCR"
        content = content[:s] + new_ocr + content[e:]
        print("Step 4 OK")
    else:
        print("Step 4 NO END")
else:
    print("Step 4 NO MARKER")

# Step 5: Form reset
if "form.remarks = ''" in content:
    content = content.replace("form.remarks = '';\n  fileList.value = []", "form.remarks = '';\n  invoiceList.value = [];\n  fileList.value = []", 1)
    print("Step 5 OK")
else:
    print("Step 5 NOT FOUND")

# Step 6: Save payload - replace flat fields with invoiceList
old6 = "invoiceNo: form.invoiceNo || undefined,\n    invoiceCode: form.invoiceCode || undefined,\n    invoiceDate: form.invoiceDate || undefined,\n    buyerName: form.buyerName || undefined,\n    sellerName: form.sellerName || undefined,\n    buyerTaxId: form.buyerTaxId || undefined,\n    sellerTaxId: form.sellerTaxId || undefined,\n    invoiceType: form.invoiceType || undefined,\n    invoiceStatus: form.invoiceStatus || undefined,\n    amountBeforeTax: form.amountBeforeTax || undefined,\n    taxRate: form.taxRate || undefined,\n    taxAmount: form.taxAmount || undefined,\n    payee: form.payee || undefined,\n    reviewer: form.reviewer || undefined,\n    issuer: form.issuer || undefined,\n    buyerAddressPhone: form.buyerAddressPhone || undefined,\n    buyerBankInfo: form.buyerBankInfo || undefined,\n    sellerAddressPhone: form.sellerAddressPhone || undefined,\n    sellerBankInfo: form.sellerBankInfo || undefined,\n    remarks: form.remarks || undefined,"
new6 = "    invoiceList: invoiceList.value,"
if old6 in content:
    content = content.replace(old6, new6, 1)
    print("Step 6 OK")
else:
    print("Step 6 NOT FOUND")
    # Try shorter match
    for i in range(0, len(content) - 50):
        if content[i:i+30] == "invoiceNo: form.invoiceNo":
            print(f"  Found 'invoiceNo: form.invoiceNo' at position {i}")
            break

with open(path, "w", encoding="utf-8") as f:
    f.write(content)
print("Done!")
