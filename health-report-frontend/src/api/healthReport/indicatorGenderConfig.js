import request from '@/utils/request'

// 查询指标性别特异性配置列表
export function listIndicatorGenderConfig(query) {
  return request({
    url: '/healthReport/indicatorGenderConfig/list',
    method: 'get',
    params: query
  })
}

// 查询指标性别特异性配置详细
export function getIndicatorGenderConfig(id) {
  return request({
    url: '/healthReport/indicatorGenderConfig/' + id,
    method: 'get'
  })
}

// 新增指标性别特异性配置
export function addIndicatorGenderConfig(data) {
  return request({
    url: '/healthReport/indicatorGenderConfig',
    method: 'post',
    data: data
  })
}

// 修改指标性别特异性配置
export function updateIndicatorGenderConfig(data) {
  return request({
    url: '/healthReport/indicatorGenderConfig',
    method: 'put',
    data: data
  })
}

// 删除指标性别特异性配置
export function delIndicatorGenderConfig(id) {
  return request({
    url: '/healthReport/indicatorGenderConfig/' + id,
    method: 'delete'
  })
}
