import request from '@/utils/request'

// 查询采集分组配置列表
export function listCollectionCategoryConfig(query) {
  return request({
    url: '/healthReport/collectionCategoryConfig/list',
    method: 'get',
    params: query
  })
}

// 查询采集分组配置详细
export function getCollectionCategoryConfig(id) {
  return request({
    url: '/healthReport/collectionCategoryConfig/' + id,
    method: 'get'
  })
}

// 新增采集分组配置
export function addCollectionCategoryConfig(data) {
  return request({
    url: '/healthReport/collectionCategoryConfig',
    method: 'post',
    data: data
  })
}

// 修改采集分组配置
export function updateCollectionCategoryConfig(data) {
  return request({
    url: '/healthReport/collectionCategoryConfig',
    method: 'put',
    data: data
  })
}

// 删除采集分组配置
export function delCollectionCategoryConfig(id) {
  return request({
    url: '/healthReport/collectionCategoryConfig/' + id,
    method: 'delete'
  })
}
