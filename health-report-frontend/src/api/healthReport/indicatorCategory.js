import request from '@/utils/request'

// 查询指标分类树列表
export function listIndicatorCategory(query) {
  return request({
    url: '/healthReport/indicatorCategory/list',
    method: 'get',
    params: query
  })
}

// 查询指标分类树详细
export function getIndicatorCategory(id) {
  return request({
    url: '/healthReport/indicatorCategory/' + id,
    method: 'get'
  })
}

// 新增指标分类树
export function addIndicatorCategory(data) {
  return request({
    url: '/healthReport/indicatorCategory',
    method: 'post',
    data: data
  })
}

// 修改指标分类树
export function updateIndicatorCategory(data) {
  return request({
    url: '/healthReport/indicatorCategory',
    method: 'put',
    data: data
  })
}

// 删除指标分类树
export function delIndicatorCategory(id) {
  return request({
    url: '/healthReport/indicatorCategory/' + id,
    method: 'delete'
  })
}
