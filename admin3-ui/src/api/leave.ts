import request from '../utils/request';
import {BASE_URI} from './base';

export function getLeaveList(data: { page: number; size: number }) {
    return request({
        url: `${BASE_URI}/leaves`,
        method: 'get',
        params: data
    });
};

export function getMyLeaveList(data: { page: number; size: number }) {
    return request({
        url: `${BASE_URI}/leaves/my`,
        method: 'get',
        params: data
    });
};

export function createLeave(data: { leaveType: string; startTime: string; endTime: string; leaveReason: string }) {
    return request({
        url: `${BASE_URI}/leaves`,
        method: 'post',
        data: data
    });
}

export function updateLeave(leaveId: number, data: { leaveType: string; startTime: string; endTime: string; leaveReason: string }) {
    return request({
        url: `${BASE_URI}/leaves/${leaveId}`,
        method: 'put',
        data: data
    });
}

export function deleteLeave(leaveId: number) {
    return request({
        url: `${BASE_URI}/leaves/${leaveId}`,
        method: 'delete'
    });
}

export function approveLeave(leaveId: number) {
    return request({
        url: `${BASE_URI}/leaves/${leaveId}:approve`,
        method: 'post'
    });
}

export function rejectLeave(leaveId: number) {
    return request({
        url: `${BASE_URI}/leaves/${leaveId}:reject`,
        method: 'post'
    });
}

export function cancelLeave(leaveId: number) {
    return request({
        url: `${BASE_URI}/leaves/${leaveId}:cancel`,
        method: 'post'
    });
}
