import request from './request'

export function getScheduleTasks() {
  return request.get('/api/schedule/tasks')
}

export function validateCron(cron) {
  return request.get('/api/schedule/cron/validate', { params: { cron } })
}

export function getScheduleJobPage(params) {
  return request.get('/api/schedule/job/page', { params })
}

export function getScheduleJob(id) {
  return request.get(`/api/schedule/job/${id}`)
}

export function createScheduleJob(data) {
  return request.post('/api/schedule/job', data)
}

export function updateScheduleJob(id, data) {
  return request.put(`/api/schedule/job/${id}`, data)
}

export function deleteScheduleJob(id) {
  return request.delete(`/api/schedule/job/${id}`)
}

export function pauseScheduleJob(id) {
  return request.put(`/api/schedule/job/${id}/pause`)
}

export function resumeScheduleJob(id) {
  return request.put(`/api/schedule/job/${id}/resume`)
}

export function runScheduleJob(id, params) {
  return request.post(`/api/schedule/job/${id}/run`, params || {})
}

export function getScheduleRunStatus() {
  return request.get('/api/schedule/run-status')
}

export function getScheduleLogPage(params) {
  return request.get('/api/schedule/log/page', { params })
}

export function cleanScheduleLog(params) {
  return request.delete('/api/schedule/log/clean', { params })
}
