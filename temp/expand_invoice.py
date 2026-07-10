import re

with open("D:\\hongmengProjects\\admin3\\admin3-ui\\src\\views\\reimbursement-list.vue", "r", encoding="utf-8") as f:
    content = f.read()

# Add expandable column to the detail invoice table
old_table = """<el-table v-if="detailData.invoices && detailData.invoices.length > 0" :data="detailData.invoices" border size="small" class="invoice-table">"""

new_table = old_table + """
              <el-table-column type="expand">
                <template #default="{ row }">
                  <div style="padding: 8px 16px; font-size: 13px; line-height: 2;">
                    <el-row :gutter="16">
                      <el-col :span="8"><b>&#36141;&#20080;&#26041;&#65306;</b>{{ row.buyerName || '-' }}</el-col>
                      <el-col :span="8"><b>&#36141;&#20080;&#26041;&#31246;&#21495;&#65306;</b>{{ row.buyerTaxId || '-' }}</el-col>
                      <el-col :span="8"><b>&#38144;&#21806;&#26041;&#31246;&#21495;&#65306;</b>{{ row.sellerTaxId || '-' }}</el-col>
                      <el-col :span="8"><b>&#37329;&#39069;(&#19981;&#21547;&#31246;):</b>&yen;{{ formatMoney(row.amountBeforeTax) }}</el-col>
                      <el-col :span="8"><b>&#31246;&#29575;&#65306;</b>{{ row.taxRate || '-' }}</el-col>
                      <el-col :span="8"><b>&#31246;&#39069;&#65306;</b>&yen;{{ formatMoney(row.taxAmount) }}</el-col>
                      <el-col :span="8"><b>&#25910;&#27454;&#20154;&#65306;</b>{{ row.payee || '-' }}</el-col>
                      <el-col :span="8"><b>&#22797;&#26680;&#20154;&#65306;</b>{{ row.reviewer || '-' }}</el-col>
                      <el-col :span="8"><b>&#24320;&#31080;&#20154;&#65306;</b>{{ row.issuer || '-' }}</el-col>
                      <el-col :span="12"><b>&#36141;&#20080;&#26041;&#22320;&#22336;/&#30005;&#35805;&#65306;</b>{{ row.buyerAddressPhone || '-' }}</el-col>
                      <el-col :span="12"><b>&#36141;&#20080;&#26041;&#24320;&#25143;&#34892;/&#36134;&#21495;&#65306;</b>{{ row.buyerBankInfo || '-' }}</el-col>
                      <el-col :span="12"><b>&#38144;&#21806;&#26041;&#22320;&#22336;/&#30005;&#35805;&#65306;</b>{{ row.sellerAddressPhone || '-' }}</el-col>
                      <el-col :span="12"><b>&#38144;&#21806;&#26041;&#24320;&#25143;&#34892;/&#36134;&#21495;&#65306;</b>{{ row.sellerBankInfo || '-' }}</el-col>
                      <el-col :span="24"><b>&#22791;&#27880;&#65306;</b>{{ row.remarks || '-' }}</el-col>
                    </el-row>
                  </div>
                </template>
              </el-table-column>"""

content = content.replace(old_table, new_table)

with open("D:\\hongmengProjects\\admin3\\admin3-ui\\src\\views\\reimbursement-list.vue", "w", encoding="utf-8") as f:
    f.write(content)

print("Detail invoice table updated with expandable rows")
