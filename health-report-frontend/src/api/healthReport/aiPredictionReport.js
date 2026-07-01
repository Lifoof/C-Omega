import request from '@/utils/request'

// 查询报告列表列表
export function listAiPredictionReport(query) {
  return request({
    url: '/healthReport/aiPredictionReport/list',
    method: 'get',
    params: query
  })
}

// 查询报告列表详细
export function getAiPredictionReport(id) {
  return request({
    url: '/healthReport/aiPredictionReport/' + id,
    method: 'get'
  })
}

// 新增报告列表
export function addAiPredictionReport(data) {
  return request({
    url: '/healthReport/aiPredictionReport',
    method: 'post',
    data: data
  })
}

// 修改报告列表
export function updateAiPredictionReport(data) {
  return request({
    url: '/healthReport/aiPredictionReport',
    method: 'put',
    data: data
  })
}

// 删除报告列表
export function delAiPredictionReport(id) {
  return request({
    url: '/healthReport/aiPredictionReport/' + id,
    method: 'delete'
  })
}
