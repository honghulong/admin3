const { request } = require('playwright');
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const BASE_URL = (process.env.ADMIN3_BASE_URL || 'http://localhost:9099/admin3').replace(/\/?$/, '/');
const USERNAME = process.env.ADMIN3_USERNAME;
const PASSWORD = process.env.ADMIN3_PASSWORD;
const OCR_TIMEOUT_MS = Number(process.env.ADMIN3_OCR_TIMEOUT_MS || 180000);
const TITLE_PREFIX = process.env.ADMIN3_TEST_TITLE_PREFIX || '自动化报销重点链路';

const fixtures = {
  vat001: path.join(ROOT, 'docs/xlf/organized/01_vat_general_pdf/2026-06-28_vat_26442000007273956556.pdf'),
  vat002: path.join(ROOT, 'docs/xlf/organized/01_vat_general_pdf/2026-07-09_vat_26442000007779083686.pdf'),
  didi001: path.join(ROOT, 'docs/xlf/organized/02_didi_invoice_pdf/2026-06-04_didi-invoice_26117000000804123309.pdf'),
};

function requireEnv() {
  const missing = [];
  if (!USERNAME) missing.push('ADMIN3_USERNAME');
  if (!PASSWORD) missing.push('ADMIN3_PASSWORD');
  for (const [name, file] of Object.entries(fixtures)) {
    if (!fs.existsSync(file)) missing.push(`fixture:${name}:${file}`);
  }
  if (missing.length) {
    throw new Error(`Missing required configuration/files: ${missing.join(', ')}`);
  }
}

function tc(id, name) {
  console.log(`\n[${id}] ${name}`);
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function money(value) {
  return Math.round(Number(value || 0) * 100) / 100;
}

async function expectOk(response, label) {
  if (!response.ok()) {
    const body = await response.text().catch(() => '');
    throw new Error(`${label} failed: ${response.status()} ${body}`);
  }
  return response.json();
}

function payload(body) {
  return body && Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body;
}

async function login(api) {
  const response = await api.post('login', {
    data: { username: USERNAME, password: PASSWORD },
  });
  const body = payload(await expectOk(response, 'login'));
  assert(body && body.token, 'login response should contain token');
  return body.token;
}

async function authedRequest(token) {
  return request.newContext({
    baseURL: BASE_URL,
    extraHTTPHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });
}

async function createDraft(api) {
  const title = `${TITLE_PREFIX}-${new Date().toISOString()}`;
  const response = await api.post('reimbursements', {
    data: {
      title,
      category: 'travel',
      amount: 1,
      matterDescription: 'TC-001/TC-007/TC-008/TC-020/TC-021 自动化重点链路测试',
      description: 'Created by reimbursement-critical-flow.playwright.js',
      attachmentIds: [],
    },
  });
  return payload(await expectOk(response, 'create reimbursement'));
}

async function getDetail(api, reimbursementId) {
  const response = await api.get(`reimbursements/${reimbursementId}`);
  return payload(await expectOk(response, `get reimbursement ${reimbursementId}`));
}

async function uploadAttachment(api, reimbursementId, filePath) {
  const response = await api.post('attachments/upload', {
    multipart: {
      reimbursementId: String(reimbursementId),
      file: {
        name: path.basename(filePath),
        mimeType: 'application/pdf',
        buffer: fs.readFileSync(filePath),
      },
    },
  });
  return payload(await expectOk(response, `upload ${filePath}`));
}

async function waitForInvoices(api, reimbursementId, expectedCount) {
  const started = Date.now();
  let lastDetail = null;
  while (Date.now() - started < OCR_TIMEOUT_MS) {
    lastDetail = await getDetail(api, reimbursementId);
    const attachments = lastDetail.attachments || [];
    const invoices = lastDetail.invoices || [];
    const failed = attachments.filter((item) => item.ocrStatus === 'failed');
    if (failed.length) {
      throw new Error(`OCR failed for ${failed.length} attachment(s): ${failed.map((item) => item.fileName).join(', ')}`);
    }
    if (invoices.length >= expectedCount && attachments.every((item) => item.ocrStatus === 'completed')) {
      return lastDetail;
    }
    await new Promise((resolve) => setTimeout(resolve, 2000));
  }
  throw new Error(`Timed out waiting for ${expectedCount} invoice(s). Last detail: ${JSON.stringify(lastDetail)}`);
}

async function waitForExpenseInvoiceLink(api, reimbursementId, expenseType, invoiceNoSuffix) {
  const started = Date.now();
  let lastDetail = null;
  while (Date.now() - started < OCR_TIMEOUT_MS) {
    lastDetail = await getDetail(api, reimbursementId);
    const invoice = (lastDetail.invoices || []).find((item) => String(item.invoiceNo || '').endsWith(invoiceNoSuffix));
    const item = (lastDetail.expenseItems || []).find((entry) => entry.expenseType === expenseType
      && invoice
      && ((entry.invoiceIds || []).includes(invoice.id) || invoice.expenseItemId === entry.id));
    if (invoice && item) return lastDetail;
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }
  throw new Error(`Timed out waiting for ${expenseType} expense item to link invoice ${invoiceNoSuffix}. Last detail: ${JSON.stringify(lastDetail)}`);
}

async function saveExpenseItems(api, reimbursementId, items) {
  const response = await api.put(`reimbursements/${reimbursementId}/expense-items`, {
    data: { items },
  });
  return payload(await expectOk(response, 'save expense items'));
}

async function deleteInvoice(api, reimbursementId, invoiceId) {
  const response = await api.delete(`reimbursements/${reimbursementId}/invoices/${invoiceId}`);
  return payload(await expectOk(response, `delete invoice ${invoiceId}`));
}

async function submit(api, reimbursementId) {
  const response = await api.post(`reimbursements/${reimbursementId}:submit`);
  return payload(await expectOk(response, 'submit reimbursement'));
}

async function main() {
  requireEnv();

  const anonymous = await request.newContext({ baseURL: BASE_URL });
  const token = await login(anonymous);
  await anonymous.dispose();
  const api = await authedRequest(token);

  try {
    tc('TC-001', '新建草稿报销单');
    const created = await createDraft(api);
    const reimbursementId = created.id;
    assert(reimbursementId, 'created reimbursement should have id');
    assert(created.status === 'draft', `expected draft, got ${created.status}`);

    tc('TC-007', '上传单张有效发票并校验主表汇总');
    await uploadAttachment(api, reimbursementId, fixtures.vat001);
    let detail = await waitForInvoices(api, reimbursementId, 1);
    assert((detail.invoices || []).length >= 1, 'expected at least one invoice after first upload');
    assert(Number(detail.invoiceCount) >= 1, `expected invoiceCount >= 1, got ${detail.invoiceCount}`);
    assert(money(detail.invoiceTotalAmount) > 0, 'expected invoiceTotalAmount > 0');

    tc('TC-008', '上传多张发票并校验发票合计');
    await uploadAttachment(api, reimbursementId, fixtures.vat002);
    detail = await waitForInvoices(api, reimbursementId, 2);
    const invoices = detail.invoices || [];
    const invoiceTotal = money(invoices.reduce((sum, invoice) => sum + Number(invoice.totalAmount || 0), 0));
    assert(invoices.length >= 2, `expected at least two invoices, got ${invoices.length}`);
    assert(money(detail.invoiceTotalAmount) === invoiceTotal, `invoiceTotalAmount mismatch: ${detail.invoiceTotalAmount} vs ${invoiceTotal}`);

    tc('TC-021/TC-029', '一条费用明细关联多张发票，主表金额按有效发票合计');
    const invoiceIds = invoices.slice(0, 2).map((invoice) => invoice.id);
    detail = await saveExpenseItems(api, reimbursementId, [{
      invoiceId: null,
      invoiceIds,
      expenseDate: invoices[0].invoiceDate || null,
      startPlace: '自动化始发地',
      endPlace: '自动化目的地',
      kilometers: null,
      expenseType: 'other',
      amount: 123.45,
      projectCostCategory: null,
      customerName: null,
      remark: 'TC-021 多发票关联',
      description: 'TC-029 验证主表金额取有效发票合计',
      source: 'manual',
    }]);
    assert((detail.expenseItems || []).length === 1, `expected one expense item, got ${(detail.expenseItems || []).length}`);
    const expense = detail.expenseItems[0];
    assert(money(expense.amount) === invoiceTotal, `expense amount mismatch: ${expense.amount} vs ${invoiceTotal}`);
    assert(money(detail.amount) === money(detail.invoiceTotalAmount), `reimbursement amount should equal invoiceTotalAmount: ${detail.amount} vs ${detail.invoiceTotalAmount}`);
    assert((detail.invoices || []).filter((invoice) => invoice.expenseItemId === expense.id).length >= 2, 'expected both invoices bound to expense item');
    assert((expense.invoiceIds || []).length >= 2, `expected expense invoice count >= 2, got ${(expense.invoiceIds || []).length}`);

    tc('TC-042', '取消费用明细全部发票关联后清理空明细并重算主表');
    detail = await saveExpenseItems(api, reimbursementId, [{
      ...expense,
      invoiceIds: [],
      amount: 0,
    }]);
    assert((detail.expenseItems || []).every((item) => item.id !== expense.id), 'expected empty expense item removed after unlink all invoices');
    assert((detail.invoices || []).every((invoice) => invoice.expenseItemId !== expense.id), 'expected invoices unbound after unlink all invoices');
    assert(money(detail.amount) === money(detail.invoiceTotalAmount), `amount should fallback after empty expense removed: ${detail.amount} vs ${detail.invoiceTotalAmount}`);

    detail = await saveExpenseItems(api, reimbursementId, [{
      invoiceId: null,
      invoiceIds,
      expenseDate: invoices[0].invoiceDate || null,
      startPlace: '自动化始发地',
      endPlace: '自动化目的地',
      kilometers: null,
      expenseType: 'other',
      amount: 0,
      projectCostCategory: null,
      customerName: null,
      remark: 'TC-021 多发票关联',
      description: 'TC-029 验证主表金额取有效发票合计',
      source: 'manual',
    }]);

    tc('TC-016', '删除已关联发票后校验费用明细与主表汇总扣减');
    const deletedInvoiceId = invoiceIds[0];
    const deletedInvoiceAmount = money(invoices.find((invoice) => invoice.id === deletedInvoiceId).totalAmount);
    detail = await deleteInvoice(api, reimbursementId, deletedInvoiceId);
    const expectedAfterDelete = money(invoiceTotal - deletedInvoiceAmount);
    const afterDeleteExpenseTotal = money((detail.expenseItems || []).reduce((sum, item) => sum + Number(item.amount || 0), 0));
    assert((detail.invoices || []).every((invoice) => invoice.id !== deletedInvoiceId), 'deleted invoice should not appear in active invoice list');
    assert(money(detail.invoiceTotalAmount) === expectedAfterDelete, `invoice total should decrease to ${expectedAfterDelete}, got ${detail.invoiceTotalAmount}`);
    assert(afterDeleteExpenseTotal === expectedAfterDelete, `expense total should decrease to ${expectedAfterDelete}, got ${afterDeleteExpenseTotal}`);

    tc('TC-019/TC-028', '删除所有费用明细后金额回退为发票合计');
    detail = await saveExpenseItems(api, reimbursementId, []);
    assert((detail.expenseItems || []).length === 0, 'expected no expense items');
    assert(money(detail.amount) === money(detail.invoiceTotalAmount), `amount should fallback to invoiceTotalAmount: ${detail.amount} vs ${detail.invoiceTotalAmount}`);

    tc('TC-041', '既有手工网约车费用明细自动关联新增识别发票');
    detail = await saveExpenseItems(api, reimbursementId, [{
      invoiceId: null,
      invoiceIds: [],
      expenseDate: null,
      startPlace: '手工网约车始发地',
      endPlace: '手工网约车目的地',
      kilometers: null,
      expenseType: 'ride_hailing',
      amount: 0,
      projectCostCategory: null,
      customerName: null,
      remark: 'TC-041 既有手工网约车费用',
      description: '后续上传识别的网约车发票应自动关联到本明细',
      source: 'manual',
    }]);
    assert(money(detail.amount) === money(detail.invoiceTotalAmount), `manual expense amount must not change reimbursement amount: ${detail.amount} vs ${detail.invoiceTotalAmount}`);
    const invoiceCountBeforeDidi = (detail.invoices || []).length;
    await uploadAttachment(api, reimbursementId, fixtures.didi001);
    detail = await waitForInvoices(api, reimbursementId, invoiceCountBeforeDidi + 1);
    detail = await waitForExpenseInvoiceLink(api, reimbursementId, 'ride_hailing', '804123309');
    const didiInvoice = (detail.invoices || []).find((invoice) => String(invoice.invoiceNo || '').endsWith('804123309'));
    assert(didiInvoice && didiInvoice.invoiceNo === '26117000000804123309', `expected full Didi invoice number, got ${didiInvoice && didiInvoice.invoiceNo}`);
    assert(didiInvoice.sellerName, 'expected Didi invoice sellerName extracted');
    assert(didiInvoice.buyerTaxId, 'expected Didi invoice buyerTaxId extracted');
    assert(didiInvoice.sellerTaxId, 'expected Didi invoice sellerTaxId extracted');
    assert(money(didiInvoice.amountBeforeTax) > 0, `expected Didi amountBeforeTax > 0, got ${didiInvoice.amountBeforeTax}`);
    assert(didiInvoice.taxRate, 'expected Didi taxRate extracted');
    assert(money(didiInvoice.taxAmount) > 0, `expected Didi taxAmount > 0, got ${didiInvoice.taxAmount}`);
    const rideHailingItem = (detail.expenseItems || []).find((item) => item.expenseType === 'ride_hailing');
    assert(rideHailingItem, 'expected existing ride_hailing expense item');
    assert((rideHailingItem.invoiceIds || []).length >= 1, 'expected ride_hailing expense item to include OCR invoice');
    assert(money(rideHailingItem.amount) > 0, `expected ride_hailing item amount > 0, got ${rideHailingItem.amount}`);
    assert((detail.invoices || []).some((invoice) => invoice.expenseItemId === rideHailingItem.id), 'expected an invoice bound to ride_hailing expense item');

    tc('TC-043', '删除费用明细最后一张关联发票后清理明细并重算主表');
    detail = await deleteInvoice(api, reimbursementId, didiInvoice.id);
    assert((detail.invoices || []).every((invoice) => invoice.id !== didiInvoice.id), 'expected deleted Didi invoice removed from active invoice list');
    assert((detail.expenseItems || []).every((item) => item.id !== rideHailingItem.id), 'expected empty ride_hailing expense item removed after deleting its last invoice');
    assert(money(detail.amount) === money(detail.invoiceTotalAmount), `amount should fallback after deleting last linked invoice: ${detail.amount} vs ${detail.invoiceTotalAmount}`);

    tc('TC-030', '提交草稿报销单');
    detail = await submit(api, reimbursementId);
    assert(detail.status === 'pending', `expected pending after submit, got ${detail.status}`);
    assert((detail.approvalLogs || []).some((log) => log.action === 'submit'), 'expected submit approval log');

    console.log(`\nPASS reimbursement critical flow. reimbursementId=${reimbursementId}`);
  } finally {
      await api.dispose();
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
