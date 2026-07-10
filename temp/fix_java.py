import sys

# 1. Add expanded fields to InvoiceTemp.java
with open("D:\\hongmengProjects\\admin3\\admin3-server\\src\\main\\java\\tech\\wetech\\admin3\\sys\\model\\InvoiceTemp.java", "r", encoding="utf-8") as f:
    c = f.read()

field_block = "\n  /** 金额（不含税） */\n  @Column(precision = 10, scale = 2)\n  private BigDecimal amountBeforeTax;\n\n  /** 税率 */\n  @Column(length = 20)\n  private String taxRate;\n\n  /** 税额 */\n  @Column(precision = 10, scale = 2)\n  private BigDecimal taxAmount;\n\n  /** 收款人 */\n  @Column(length = 100)\n  private String payee;\n\n  /** 复核人 */\n  @Column(length = 100)\n  private String reviewer;\n\n  /** 开票人 */\n  @Column(length = 100)\n  private String issuer;\n\n  /** 购买方地址、电话 */\n  @Column(length = 500)\n  private String buyerAddressPhone;\n\n  /** 购买方开户行及账号 */\n  @Column(length = 500)\n  private String buyerBankInfo;\n\n  /** 销售方地址、电话 */\n  @Column(length = 500)\n  private String sellerAddressPhone;\n\n  /** 销售方开户行及账号 */\n  @Column(length = 500)\n  private String sellerBankInfo;\n\n  /** 备注 */\n  @Column(columnDefinition = \"TEXT\")\n  private String remarks;\n"

gs_block = "\n  public BigDecimal getAmountBeforeTax() { return amountBeforeTax; }\n  public void setAmountBeforeTax(BigDecimal amountBeforeTax) { this.amountBeforeTax = amountBeforeTax; }\n\n  public String getTaxRate() { return taxRate; }\n  public void setTaxRate(String taxRate) { this.taxRate = taxRate; }\n\n  public BigDecimal getTaxAmount() { return taxAmount; }\n  public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }\n\n  public String getPayee() { return payee; }\n  public void setPayee(String payee) { this.payee = payee; }\n\n  public String getReviewer() { return reviewer; }\n  public void setReviewer(String reviewer) { this.reviewer = reviewer; }\n\n  public String getIssuer() { return issuer; }\n  public void setIssuer(String issuer) { this.issuer = issuer; }\n\n  public String getBuyerAddressPhone() { return buyerAddressPhone; }\n  public void setBuyerAddressPhone(String buyerAddressPhone) { this.buyerAddressPhone = buyerAddressPhone; }\n\n  public String getBuyerBankInfo() { return buyerBankInfo; }\n  public void setBuyerBankInfo(String buyerBankInfo) { this.buyerBankInfo = buyerBankInfo; }\n\n  public String getSellerAddressPhone() { return sellerAddressPhone; }\n  public void setSellerAddressPhone(String sellerAddressPhone) { this.sellerAddressPhone = sellerAddressPhone; }\n\n  public String getSellerBankInfo() { return sellerBankInfo; }\n  public void setSellerBankInfo(String sellerBankInfo) { this.sellerBankInfo = sellerBankInfo; }\n\n  public String getRemarks() { return remarks; }\n  public void setRemarks(String remarks) { this.remarks = remarks; }\n"

c = c.replace("private String invoiceStatus;", "private String invoiceStatus;" + field_block)
c = c.replace("public void setInvoiceStatus(String invoiceStatus) {\n    this.invoiceStatus = invoiceStatus;\n  }", "public void setInvoiceStatus(String invoiceStatus) {\n    this.invoiceStatus = invoiceStatus;\n  }" + gs_block)

with open("D:\\hongmengProjects\\admin3\\admin3-server\\src\\main\\java\\tech\\wetech\\admin3\\sys\\model\\InvoiceTemp.java", "w", encoding="utf-8") as f:
    f.write(c)

print("InvoiceTemp.java done")

# 2. Fix ReimbursementMcpConfig.java
with open("D:\\hongmengProjects\\admin3\\admin3-server\\src\\main\\java\\tech\\wetech\\admin3\\infra\\mcp\\xiaoyi\\ReimbursementMcpConfig.java", "r", encoding="utf-8") as f:
    c = f.read()

c = c.replace('String invoiceType = "";', 'String invoiceType = "";\n        String invoiceStatus = "";')
c = c.replace('if (invoiceFields.has("invoice_type")) invoiceType = invoiceFields.get("invoice_type").asText();', 'if (invoiceFields.has("invoice_type")) invoiceType = invoiceFields.get("invoice_type").asText();\n          if (invoiceFields.has("invoice_status")) invoiceStatus = invoiceFields.get("invoice_status").asText();')
c = c.replace('if ("0".equals(totalAmount) && ocrText.contains("小写")) {', 'if (invoiceStatus.isEmpty()) {\n          if (ocrText.contains("作废")) invoiceStatus = "voided";\n          else if (ocrText.contains("红冲") || ocrText.contains("红字")) invoiceStatus = "red";\n          else if (ocrText.contains("正常")) invoiceStatus = "normal";\n        }\n        if ("0".equals(totalAmount) && ocrText.contains("小写")) {')
c = c.replace('"normal",\n          0L', 'invoiceStatus.isEmpty() ? null : invoiceStatus,\n          0L')

with open("D:\\hongmengProjects\\admin3\\admin3-server\\src\\main\\java\\tech\\wetech\\admin3\\infra\\mcp\\xiaoyi\\ReimbursementMcpConfig.java", "w", encoding="utf-8") as f:
    f.write(c)

print("ReimbursementMcpConfig.java done")
