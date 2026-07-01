import request from '@/utils/request'

// 获取概览统计数据
export function getOverviewStats(timeRange) {
  return request({
    url: '/healthReport/dashboard/overview',
    method: 'get',
    params: { timeRange }
  })
}

// 获取用户趋势数据
export function getUserTrend(timeRange) {
  return request({
    url: '/healthReport/dashboard/user-trend',
    method: 'get',
    params: { timeRange }
  })
}

// 获取报告趋势数据
export function getReportTrend(timeRange) {
  return request({
    url: '/healthReport/dashboard/report-trend',
    method: 'get',
    params: { timeRange }
  })
}

// 获取模型使用分布
export function getModelUsageDistribution(timeRange) {
  return request({
    url: '/healthReport/dashboard/model-usage',
    method: 'get',
    params: { timeRange }
  })
}