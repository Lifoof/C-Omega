import request from '@/utils/request'

// 查询信息采集列表
export function listCollectionInfo(query) {
  return request({
    url: '/healthReport/collectionInfo/list',
    method: 'get',
    params: query
  })
}

// 查询信息采集详细
export function getCollectionInfo(id) {
  return request({
    url: '/healthReport/collectionInfo/' + id,
    method: 'get'
  })
}

// 新增信息采集
export function addCollectionInfo(data) {
  return request({
    url: '/healthReport/collectionInfo',
    method: 'post',
    data: data
  })
}

// 修改信息采集
export function updateCollectionInfo(data) {
  return request({
    url: '/healthReport/collectionInfo',
    method: 'put',
    data: data
  })
}

// 删除信息采集
export function delCollectionInfo(id) {
  return request({
    url: '/healthReport/collectionInfo/' + id,
    method: 'delete'
  })
}
