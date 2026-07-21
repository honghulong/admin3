const { request } = require('playwright');
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const BASE_URL = (process.env.ADMIN3_BASE_URL || 'http://localhost:9099/admin3').replace(/\/?$/, '/');
const EMPLOYEE_USERNAME = process.env.ADMIN3_EMPLOYEE_USERNAME || 'employee';
const EMPLOYEE_PASSWORD = process.env.ADMIN3_EMPLOYEE_PASSWORD || 'employee';
const FINANCE_USERNAME = process.env.ADMIN3_FINANCE_USERNAME || 'admin';
const FINANCE_PASSWORD = process.env.ADMIN3_FINANCE_PASSWORD || '123456';
const OCR_TIMEOUT_MS = Number(process.env.ADMIN3_OCR_TIMEOUT_MS || 180000);
const TITLE_PREFIX = process.env.ADMIN3_WORKFLOW_TITLE_PREFIX || '自动化报销审批流';
const INVOICE_FILE = path.join(ROOT, 'docs/xlf/organized/01_vat_general_pdf/2026-06-28_vat_26442000007273956556.pdf');

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function tc(id, name) {
  console.log(`\n[${id}] ${name}`);
}

function payload(body) {
  return body && Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body;
}

async function expectOk(response, label) {
  if (!response.ok()) {
    const body = await response.text().catch(() => '');
    throw new Error(`${label} failed: ${response.status()} ${body}`);
  }
  return response.json();
}

async function login(username, password) {
  const anonymous = await request.newContext({ baseURL: BASE_URL });
  try {
    const response = await anonymous.post('login', { data: { username, password } });
    const body = payload(await expectOk(response, `login ${username}`));
    assert(body && body.token, `login ${username} should return token`);
    return request.newContext({
      baseURL: BASE_URL,
      extraHTTPHeaders: { Authorization: `Bearer ${body.token}` },
    });
  } finally {
    await anonymous.dispose();
  }
}

async function createDraft(api, suffix) {
  const response = await api.post('reimbursements', {
    data: {
      title: `${TITLE_PREFIX}-${suffix}-${new Date().toISOString()}`,
      category: 'travel',
      amount: 1,
      matterDescription: `${suffix} 草稿编辑`,
      description: 'Created by reimbursement-workflow.playwright.js',
      attachmentIds: [],
    },
  });
  return payload(await expectOk(response, `create draft ${suffix}`));
}

async function updateDraft(api, id, suffix) {
  const response = await api.put(`reimbursements/${id}`, {
    data: {
      title: `${TITLE_PREFIX}-${suffix}-已修改`,
      category: 'travel',
      amount: 1,
      matterDescription: `${suffix} 退回后修改事项说明`,
      description: `${suffix} 退回后补充说明`,
      attachmentIds: null,
    },
  });
  return payload(await expectOk(response, `update reimbursement ${id}`));
}

async function uploadInvoice(api, reimbursementId) {
  const response = await api.post('attachments/upload', {
    multipart: {
      reimbursementId: String(reimbursementId),
      file: {
        name: path.basename(INVOICE_FILE),
        mimeType: 'application/pdf',
        buffer: fs.readFileSync(INVOICE_FILE),
      },
    },
  });
  return payload(await expectOk(response, `upload invoice for ${reimbursementId}`));
}

async function getDetail(api, id) {
  const response = await api.get(`reimbursements/${id}`);
  return payload(await expectOk(response, `get reimbursement ${id}`));
}

async function waitForInvoice(api, id) {
  const started = Date.now();
  let lastDetail = null;
  while (Date.now() - started < OCR_TIMEOUT_MS) {
    lastDetail = await getDetail(api, id);
    const attachments = lastDetail.attachments || [];
    if (attachments.some((item) => item.ocrStatus === 'failed')) {
      throw new Error(`OCR failed for reimbursement ${id}`);
    }
    if ((lastDetail.invoices || []).length >= 1 && attachments.every((item) => item.ocrStatus === 'completed')) {
      assert(Number(lastDetail.invoiceTotalAmount || 0) > 0, `invoiceTotalAmount should be > 0 for ${id}`);
      return lastDetail;
    }
    await new Promise((resolve) => setTimeout(resolve, 2000));
  }
  throw new Error(`Timed out waiting for invoice. Last detail: ${JSON.stringify(lastDetail)}`);
}

async function submit(api, id) {
  const response = await api.post(`reimbursements/${id}:submit`);
  return payload(await expectOk(response, `submit reimbursement ${id}`));
}

async function reject(api, id, comment) {
  const response = await api.post(`reimbursements/${id}:reject`, { data: { comment } });
  return payload(await expectOk(response, `reject reimbursement ${id}`));
}

async function approve(api, id, comment) {
  const response = await api.post(`reimbursements/${id}:approve`, { data: { comment } });
  return payload(await expectOk(response, `approve reimbursement ${id}`));
}

async function deleteReimbursement(api, id) {
  const response = await api.delete(`reimbursements/${id}`);
  if (response.status() !== 204) {
    const body = await response.text().catch(() => '');
    throw new Error(`delete reimbursement ${id} failed: ${response.status()} ${body}`);
  }
}

async function expectMissing(api, id) {
  const response = await api.get(`reimbursements/${id}`);
  assert(!response.ok(), `deleted reimbursement ${id} should not be readable`);
}

function assertLogs(detail, actions) {
  const actual = (detail.approvalLogs || []).map((log) => log.action);
  for (const action of actions) {
    assert(actual.includes(action), `expected approval log action ${action}, got ${actual.join(',')}`);
  }
}

async function main() {
  assert(fs.existsSync(INVOICE_FILE), `Missing invoice fixture: ${INVOICE_FILE}`);

  const employee = await login(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD);
  const finance = await login(FINANCE_USERNAME, FINANCE_PASSWORD);

  try {
    tc('WF-001', '职工创建草稿、上传发票、保存后提交');
    const deleteBranch = await createDraft(employee, 'delete-branch');
    await uploadInvoice(employee, deleteBranch.id);
    await waitForInvoice(employee, deleteBranch.id);
    let detail = await submit(employee, deleteBranch.id);
    assert(detail.status === 'pending', `expected pending, got ${detail.status}`);

    tc('WF-002', '财务审核退回后，职工删除报销单');
    detail = await reject(finance, deleteBranch.id, '测试退回：删除分支');
    assert(detail.status === 'rejected', `expected rejected, got ${detail.status}`);
    await deleteReimbursement(employee, deleteBranch.id);
    await expectMissing(employee, deleteBranch.id);

    tc('WF-003', '财务退回后，职工修改并再次提交');
    const approveBranch = await createDraft(employee, 'approve-branch');
    await uploadInvoice(employee, approveBranch.id);
    await waitForInvoice(employee, approveBranch.id);
    detail = await submit(employee, approveBranch.id);
    assert(detail.status === 'pending', `expected pending, got ${detail.status}`);
    detail = await reject(finance, approveBranch.id, '测试退回：请补充事项说明');
    assert(detail.status === 'rejected', `expected rejected, got ${detail.status}`);
    detail = await updateDraft(employee, approveBranch.id, 'approve-branch');
    assert(detail.status === 'rejected', `rejected reimbursement should remain rejected after edit, got ${detail.status}`);
    assert((detail.matterDescription || '').includes('退回后修改'), 'expected updated matterDescription');
    detail = await submit(employee, approveBranch.id);
    assert(detail.status === 'pending', `expected pending after resubmit, got ${detail.status}`);

    tc('WF-004', '财务根据业务情况审核通过');
    detail = await approve(finance, approveBranch.id, '测试通过：资料完整，同意报销');
    assert(detail.status === 'approved', `expected approved, got ${detail.status}`);
    assert(detail.approverName === FINANCE_USERNAME, `expected approverName ${FINANCE_USERNAME}, got ${detail.approverName}`);
    assertLogs(detail, ['submit', 'reject', 'submit', 'approve']);

    console.log(`\nPASS reimbursement workflow. approvedId=${approveBranch.id}`);
  } finally {
    await employee.dispose();
    await finance.dispose();
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
