import request from '@/utils/request'

// 查询AI模型定义列表
export function listAiModel(query) {
  return request({
    url: '/healthReport/aiModel/list',
    method: 'get',
    params: query
  })
}

// 查询AI模型定义详细
export function getAiModel(id) {
  return request({
    url: '/healthReport/aiModel/' + id,
    method: 'get'
  })
}

// 新增AI模型定义
export function addAiModel(data) {
  return request({
    url: '/healthReport/aiModel',
    method: 'post',
    data: data
  })
}

// 修改AI模型定义
export function updateAiModel(data) {
  return request({
    url: '/healthReport/aiModel',
    method: 'put',
    data: data
  })
}

// 删除AI模型定义
export function delAiModel(id) {
  return request({
    url: '/healthReport/aiModel/' + id,
    method: 'delete'
  })
}
