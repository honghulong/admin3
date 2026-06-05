import request from '../utils/request';
import {BASE_URI} from './base';

export function getDictList() {
  return request.get(`${BASE_URI}/dict`);
}

export function getDictById(dictId: number) {
  return request.get(`${BASE_URI}/dict/${dictId}`);
}

export function getDictByCode(dictCode: string) {
  return request.get(`${BASE_URI}/dict/code/${dictCode}`);
}

export function createDict(data: { dictCode: string; dictName: string; description?: string }) {
  return request.post(`${BASE_URI}/dict`, data);
}

export function updateDict(dictId: number, data: { dictCode: string; dictName: string; description?: string }) {
  return request.put(`${BASE_URI}/dict/${dictId}`, data);
}

export function deleteDict(dictId: number) {
  return request.delete(`${BASE_URI}/dict/${dictId}`);
}

export function getDictValues(dictId: number) {
  return request.get(`${BASE_URI}/dict/${dictId}/values`);
}

export function getDictValuesByCode(dictCode: string) {
  return request.get(`${BASE_URI}/dict/code/${dictCode}/values`);
}

export function createDictValue(dictId: number, data: { label: string; value: string; sortOrder: number; description?: string }) {
  return request.post(`${BASE_URI}/dict/${dictId}/values`, data);
}

export function updateDictValue(valueId: number, data: { label: string; value: string; sortOrder: number; description?: string }) {
  return request.put(`${BASE_URI}/dict/values/${valueId}`, data);
}

export function deleteDictValue(valueId: number) {
  return request.delete(`${BASE_URI}/dict/values/${valueId}`);
}
