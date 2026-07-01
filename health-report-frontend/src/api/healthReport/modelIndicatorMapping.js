import request from '@/utils/request'

// 查询模型与指标关联列表
export function listModelIndicatorMapping(query) {
  return request({
    url: '/healthReport/modelIndicatorMapping/list',
    method: 'get',
    params: query
  })
}

// 查询模型与指标关联详细
export function getModelIndicatorMapping(id) {
  return request({
    url: '/healthReport/modelIndicatorMapping/' + id,
    method: 'get'
  })
}

// 新增模型与指标关联
export function addModelIndicatorMapping(data) {
  return request({
    url: '/healthReport/modelIndicatorMapping',
    method: 'post',
    data: data
  })
}

// 修改模型与指标关联
export function updateModelIndicatorMapping(data) {
  return request({
    url: '/healthReport/modelIndicatorMapping',
    method: 'put',
    data: data
  })
}

// 删除模型与指标关联
export function delModelIndicatorMapping(id) {
  return request({
    url: '/healthReport/modelIndicatorMapping/' + id,
    method: 'delete'
  })
}
