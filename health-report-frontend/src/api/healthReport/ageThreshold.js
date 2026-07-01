import request from '@/utils/request'

// 查询指标年龄阈值列表
export function listAgeThreshold(query) {
  return request({
    url: '/healthReport/ageThreshold/list',
    method: 'get',
    params: query
  })
}

// 查询指标年龄阈值详细
export function getAgeThreshold(id) {
  return request({
    url: '/healthReport/ageThreshold/' + id,
    method: 'get'
  })
}

// 新增指标年龄阈值
export function addAgeThreshold(data) {
  return request({
    url: '/healthReport/ageThreshold',
    method: 'post',
    data: data,
    timeout: 120000
  })
}

// 修改指标年龄阈值
export function updateAgeThreshold(data) {
  return request({
    url: '/healthReport/ageThreshold',
    method: 'put',
    data: data,
    timeout: 120000
  })
}

// 删除指标年龄阈值
export function delAgeThreshold(id) {
  return request({
    url: '/healthReport/ageThreshold/' + id,
    method: 'delete',
    timeout: 60000
  })
}
