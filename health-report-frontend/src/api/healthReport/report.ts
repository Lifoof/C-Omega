import service from '@/utils/request'

export interface Report {
  id: string | number
  reportNo: string
  memberId: string | number
  memberName: string
  memberGender: number
  collectionId: string | number
  collectionNo: string
  modelId: string | number
  modelName: string
  completeness: number
  status: number
  reportContent: string
  filePath: string
  filePathEn: string
  generationTime: string
  createdTime: string
}

export interface ReportQuery {
  memberName?: string
  modelName?: string
  collectionId?: number
  startDate?: string
  endDate?: string
  current: number
  size: number
}

export function getReportList(params: ReportQuery) {
  return service.get('/healthReport/report/list', { params })
}

export function getReportById(id: number | string) {
  return service.get(`/healthReport/report/${id}`)
}
export interface GenerateReportRequest {
  collectionId: number | string;
  modelIds: (number | string)[]; // 多个模型ID ✅
}
export function generateReport(params: GenerateReportRequest) {
  return service.post('/healthReport/report/generate', params);
}

export function deleteReport(id: number | string) {
  return service.delete(`/healthReport/report/${id}`)
}

export function downloadReport(id: number | string, lang?: string) {
  return service.get(`/healthReport/report/${id}/download`, {
    params: { lang: lang || 'zh' },
    responseType: 'blob'
  })
}

export function getShareLink(id: number | string) {
  return service.post(`/healthReport/report/${id}/share`)
}

export function getSharedReport(token: string) {
  return service.get(`/healthReport/report/shared/${token}`)
}
