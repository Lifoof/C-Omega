import service from '@/utils/request'

export interface MemberInfo {
  id: string | number
  userId?: string | number
  memberNo: string
  name: string
  relationship: number
  gender: number
  birthDate: string
  country: string
  city: string
  district: string
  contact: string
  address: string
  ethnicity: string
  birthplace: string
  remark: string
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
}

export interface MemberInfoQuery {
  pageNum?: number
  pageSize?: number
  name?: string
  relationship?: number
  gender?: number
}

// 查询个人档案列表
export function listMemberInfo(params: MemberInfoQuery) {
  return service.get('/healthReport/memberInfo/list', { params })
}

// 查询个人档案详细
export function getMemberInfo(id: string | number) {
  return service.get(`/healthReport/memberInfo/${id}`)
}

// 新增个人档案
export function addMemberInfo(data: Partial<MemberInfo>) {
  return service.post('/healthReport/memberInfo', data, { timeout: 120000 })
}

// 修改个人档案
export function updateMemberInfo(data: Partial<MemberInfo>) {
  return service.put('/healthReport/memberInfo', data, { timeout: 120000 })
}

// 删除个人档案
export function delMemberInfo(id: string | number) {
  return service.delete(`/healthReport/memberInfo/${id}`, { timeout: 60000 })
}
