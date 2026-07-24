import request from '../utils/request';
import {BASE_URI} from './base';

export interface OcrEngineConfig {
  id?: number | null;
  name: string;
  providerType: string;
  baseUrl?: string;
  modelName?: string;
  apiKey?: string;
  protocol?: string;
  tenantId?: string;
  environment?: string;
  prompt?: string;
  enabled?: boolean;
  isDefault?: boolean;
  fallbackEnabled?: boolean;
  apiKeyConfigured?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export function getOcrEngineConfigList() {
  return request({
    url: `${BASE_URI}/ocr-engine-configs`,
    method: 'get'
  });
}

export function createOcrEngineConfig(data: OcrEngineConfig) {
  return request({
    url: `${BASE_URI}/ocr-engine-configs`,
    method: 'post',
    data
  });
}

export function updateOcrEngineConfig(id: number, data: OcrEngineConfig) {
  return request({
    url: `${BASE_URI}/ocr-engine-configs/${id}`,
    method: 'put',
    data
  });
}

export function deleteOcrEngineConfig(id: number) {
  return request({
    url: `${BASE_URI}/ocr-engine-configs/${id}`,
    method: 'delete'
  });
}

export function markAsDefaultOcrEngineConfig(id: number) {
  return request({
    url: `${BASE_URI}/ocr-engine-configs/${id}:markAsDefault`,
    method: 'post'
  });
}
