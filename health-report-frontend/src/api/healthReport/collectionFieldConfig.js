import request from '@/utils/request'

// 查询采集字段配置列表
export function listCollectionFieldConfig(query) {
  return request({
    url: '/healthReport/collectionFieldConfig/list',
    method: 'get',
    params: query
  })
}

// 查询采集字段配置详细
export function getCollectionFieldConfig(id) {
  return request({
    url: '/healthReport/collectionFieldConfig/' + id,
    method: 'get'
  })
}

// 新增采集字段配置
export function addCollectionFieldConfig(data) {
  return request({
    url: '/healthReport/collectionFieldConfig',
    method: 'post',
    data: data,
    timeout: 300000
  })
}

// 修改采集字段配置
export function updateCollectionFieldConfig(data) {
  return request({
    url: '/healthReport/collectionFieldConfig',
    method: 'put',
    data: data,
    timeout: 120000
  })
}

// 删除采集字段配置
export function delCollectionFieldConfig(id) {
  return request({
    url: '/healthReport/collectionFieldConfig/' + id,
    method: 'delete',
    timeout: 60000
  })
}
