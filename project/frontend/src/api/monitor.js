import axios from 'axios'

const actuator = axios.create({
  baseURL: '/bcsports',
  timeout: 10000,
  withCredentials: true
})

export function getHealth() {
  return actuator.get('/actuator/health').then(r => r.data)
}

export function getInfo() {
  return actuator.get('/actuator/info').then(r => r.data)
}

export function getMetricNames() {
  return actuator.get('/actuator/metrics').then(r => r.data)
}

export function getMetricDetail(name) {
  return actuator.get(`/actuator/metrics/${name}`).then(r => r.data)
}

export function getLoggers() {
  return actuator.get('/actuator/loggers', { timeout: 30000 }).then(r => r.data)
}

export function setLoggerLevel(name, level) {
  return actuator.post(`/actuator/loggers/${name}`, { configuredLevel: level })
}

export function getEnv() {
  return actuator.get('/actuator/env').then(r => r.data)
}

// ===== Jenkins任务状态监控（明细页）=====
import request from './request'

export function getJenkinsJobPage(params) {
  return request.get('/api/monitor/jenkins/page', { params })
}

export function getJenkinsInfo() {
  return request.get('/api/monitor/jenkins/info')
}

export function syncJenkinsNow() {
  return request.post('/api/monitor/jenkins/sync', null, { timeout: 30000 })
}
