import request from '../utils/request';

export function getDictList() {
  return request.get('/dict');
}

export function getDictById(dictId: number) {
  return request.get(`/dict/${dictId}`);
}

export function getDictByCode(dictCode: string) {
  return request.get(`/dict/code/${dictCode}`);
}

export function createDict(data: { dictCode: string; dictName: string; description?: string }) {
  return request.post('/dict', data);
}

export function updateDict(dictId: number, data: { dictCode: string; dictName: string; description?: string }) {
  return request.put(`/dict/${dictId}`, data);
}

export function deleteDict(dictId: number) {
  return request.delete(`/dict/${dictId}`);
}

export function getDictValues(dictId: number) {
  return request.get(`/dict/${dictId}/values`);
}

export function getDictValuesByCode(dictCode: string) {
  return request.get(`/dict/code/${dictCode}/values`);
}

export function createDictValue(dictId: number, data: { label: string; value: string; sortOrder: number; description?: string }) {
  return request.post(`/dict/${dictId}/values`, data);
}

export function updateDictValue(valueId: number, data: { label: string; value: string; sortOrder: number; description?: string }) {
  return request.put(`/dict/values/${valueId}`, data);
}

export function deleteDictValue(valueId: number) {
  return request.delete(`/dict/values/${valueId}`);
}
