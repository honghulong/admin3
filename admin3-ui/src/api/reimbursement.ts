import request from '../utils/request';
import {BASE_URI} from './base';

export function getReimbursementList(params: {
    page: number;
    size: number;
    status?: string;
    applicantId?: number;
    category?: string;
    dateFrom?: string;
    dateTo?: string;
    keyword?: string;
}) {
    return request({
        url: `${BASE_URI}/reimbursements`,
        method: 'get',
        params: params
    });
};

export function getMyReimbursementList(params: { page: number; size: number }) {
    return request({
        url: `${BASE_URI}/reimbursements/my`,
        method: 'get',
        params: params
    });
};

export function getPendingApprovals(params: { page: number; size: number }) {
    return request({
        url: `${BASE_URI}/reimbursements/pending-approvals`,
        method: 'get',
        params: params
    });
};

export function getReimbursementDetail(id: number) {
    return request({
        url: `${BASE_URI}/reimbursements/${id}`,
        method: 'get'
    });
};

export function createReimbursement(data: {
    title: string;
    category: string;
    amount: number;
    description?: string;
    invoiceNo?: string;
    invoiceCode?: string;
    invoiceDate?: string;
    buyerName?: string;
    sellerName?: string;
    buyerTaxId?: string;
    sellerTaxId?: string;
    invoiceType?: string;
    invoiceStatus?: string;
    attachmentIds?: number[];
}) {
    return request({
        url: `${BASE_URI}/reimbursements`,
        method: 'post',
        data: data
    });
};

export function updateReimbursement(id: number, data: {
    title: string;
    category: string;
    amount: number;
    description?: string;
    invoiceNo?: string;
    invoiceCode?: string;
    invoiceDate?: string;
    buyerName?: string;
    sellerName?: string;
    buyerTaxId?: string;
    sellerTaxId?: string;
    invoiceType?: string;
    invoiceStatus?: string;
    attachmentIds?: number[];
}) {
    return request({
        url: `${BASE_URI}/reimbursements/${id}`,
        method: 'put',
        data: data
    });
};

export function deleteReimbursement(id: number) {
    return request({
        url: `${BASE_URI}/reimbursements/${id}`,
        method: 'delete'
    });
};

export function submitReimbursement(id: number) {
    return request({
        url: `${BASE_URI}/reimbursements/${id}:submit`,
        method: 'post'
    });
};

export function approveReimbursement(id: number, data?: { comment?: string }) {
    return request({
        url: `${BASE_URI}/reimbursements/${id}:approve`,
        method: 'post',
        data: data
    });
};

export function rejectReimbursement(id: number, data: { comment: string }) {
    return request({
        url: `${BASE_URI}/reimbursements/${id}:reject`,
        method: 'post',
        data: data
    });
};

export function recallReimbursement(id: number) {
    return request({
        url: `${BASE_URI}/reimbursements/${id}:recall`,
        method: 'post'
    });
};

export function uploadAttachment(file: File, reimbursementId?: number) {
    const formData = new FormData();
    formData.append('file', file);
    if (reimbursementId) {
        formData.append('reimbursementId', String(reimbursementId));
    }
    return request({
        url: `${BASE_URI}/attachments/upload`,
        method: 'post',
        headers: {'Content-Type': 'multipart/form-data'},
        data: formData
    });
};

export function deleteAttachment(id: number) {
    return request({
        url: `${BASE_URI}/attachments/${id}`,
        method: 'delete'
    });
};
