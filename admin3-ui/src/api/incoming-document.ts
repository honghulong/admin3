import request from '../utils/request';
import {BASE_URI} from './base';

export function getIncomingDocumentList(params: { page: number; size: number; keyword?: string }) {
  return request({url: `${BASE_URI}/incoming-documents`, method: 'get', params});
}

export function getIncomingDocumentDetail(id: number) {
  return request({url: `${BASE_URI}/incoming-documents/${id}`, method: 'get'});
}

export function createIncomingDocument(data: any) {
  return request({url: `${BASE_URI}/incoming-documents`, method: 'post', data});
}

export function updateIncomingDocument(id: number, data: any) {
  return request({url: `${BASE_URI}/incoming-documents/${id}`, method: 'put', data});
}

export function deleteIncomingDocument(id: number) {
  return request({url: `${BASE_URI}/incoming-documents/${id}`, method: 'delete'});
}

export function uploadIncomingAttachment(id: number, file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return request({
    url: `${BASE_URI}/incoming-documents/${id}/attachments`,
    method: 'post',
    headers: {'Content-Type': 'multipart/form-data'},
    data: formData
  });
}

export function deleteIncomingAttachment(id: number, attachmentId: number) {
  return request({url: `${BASE_URI}/incoming-documents/${id}/attachments/${attachmentId}`, method: 'delete'});
}

export function uploadIncomingScan(id: number, file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return request({
    url: `${BASE_URI}/incoming-documents/${id}/scans`,
    method: 'post',
    headers: {'Content-Type': 'multipart/form-data'},
    data: formData
  });
}

export function recognizeIncomingScan(id: number, scanId: number) {
  return request({url: `${BASE_URI}/incoming-documents/${id}/scans/${scanId}:recognize`, method: 'post'});
}

export function deleteIncomingScan(id: number, scanId: number) {
  return request({url: `${BASE_URI}/incoming-documents/${id}/scans/${scanId}`, method: 'delete'});
}

export function getIncomingOcrMapping() {
  return request({url: `${BASE_URI}/incoming-documents/ocr-mapping`, method: 'get'});
}

export function saveIncomingOcrMapping(mappingJson: string) {
  return request({url: `${BASE_URI}/incoming-documents/ocr-mapping`, method: 'put', data: {mappingJson}});
}
