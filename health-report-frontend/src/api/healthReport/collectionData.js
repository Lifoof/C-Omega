import request from '@/utils/request'

// 查询采集数据明细列表
export function listCollectionData(query) {
  return request({
    url: '/healthReport/collectionData/list',
    method: 'get',
    params: query
  })
}

// 查询采集数据明细详细
export function getCollectionData(id) {
  return request({
    url: '/healthReport/collectionData/' + id,
    method: 'get'
  })
}

// 新增采集数据明细
export function addCollectionData(data) {
  return request({
    url: '/healthReport/collectionData',
    method: 'post',
    data: data
  })
}

// 修改采集数据明细
export function updateCollectionData(data) {
  return request({
    url: '/healthReport/collectionData',
    method: 'put',
    data: data
  })
}

// 删除采集数据明细
export function delCollectionData(id) {
  return request({
    url: '/healthReport/collectionData/' + id,
    method: 'delete'
  })
}
