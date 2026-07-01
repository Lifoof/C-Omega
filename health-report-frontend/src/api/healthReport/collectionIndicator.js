import request from '@/utils/request'

// 查询体检指标定义列表
export function listCollectionIndicator(query) {
  return request({
    url: '/healthReport/collectionIndicator/list',
    method: 'get',
    params: query
  })
}

// 查询体检指标定义详细
export function getCollectionIndicator(id) {
  return request({
    url: '/healthReport/collectionIndicator/' + id,
    method: 'get'
  })
}

// 新增体检指标定义
export function addCollectionIndicator(data) {
  return request({
    url: '/healthReport/collectionIndicator',
    method: 'post',
    data: data
  })
}

// 修改体检指标定义
export function updateCollectionIndicator(data) {
  return request({
    url: '/healthReport/collectionIndicator',
    method: 'put',
    data: data
  })
}

// 删除体检指标定义
export function delCollectionIndicator(id) {
  return request({
    url: '/healthReport/collectionIndicator/' + id,
    method: 'delete'
  })
}
