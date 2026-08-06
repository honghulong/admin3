import request from '../utils/request';
import {BASE_URI} from './base';

export function getOcrMappingRuleList(params: { page: number; size: number; keyword?: string }) {
  return request({url: `${BASE_URI}/ocr-mapping-rules`, method: 'get', params});
}

export function getOcrMappingRuleDetail(id: number) {
  return request({url: `${BASE_URI}/ocr-mapping-rules/${id}`, method: 'get'});
}

export function updateOcrMappingRule(id: number, data: any) {
  return request({url: `${BASE_URI}/ocr-mapping-rules/${id}`, method: 'put', data});
}

export function deleteOcrMappingRule(id: number) {
  return request({url: `${BASE_URI}/ocr-mapping-rules/${id}`, method: 'delete'});
}
