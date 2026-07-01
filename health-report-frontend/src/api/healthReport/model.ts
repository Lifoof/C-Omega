import service from '@/utils/request'

export interface AiModel {
  id: number
  modelCode: string
  modelName: string
  modelNameEn?: string
  category: string
  description: string
  applicableGender: number
  requiredParams: string[]
  paramCount: number
  filledKeyParams?: number
  status: number
  callCount: number
  createdTime: string
  updatedTime: string
}

export interface ModelQuery {
  keyword?: string
  applicableGender?: number
  status?: number
  current: number
  size: number
}

export function getModelList(params: ModelQuery) {
  return service.get('/healthReport/aiModel/list', { params, timeout: 30000 })
}

export function getModelById(id: number) {
  return service.get(`/healthReport/aiModel/${id}`)
}

export function createModel(data: Partial<AiModel>) {
  return service.post('/healthReport/aiModel', data,{ timeout: 120000})
}

export function updateModel(id: number, data: Partial<AiModel>) {
  return service.put(`/healthReport/aiModel/${id}`, data, { timeout: 120000})
}

export function toggleModelStatus(id: number | string, status: number) {
  return service.put(`/healthReport/aiModel/${id}/status`, { status }, { timeout: 60000})
}

export function deleteModel(id: number | string) {
  return service.delete(`/healthReport/aiModel/${id}`, { timeout: 60000})
}

export function getAvailableModels(gender?: number) {
  return service.get('/healthReport/aiModel/available', { params: { gender,timeout: 200000 } })
}

export function getAvailableModelsWithParams(gender?: number, collectionId?: string | number) {
  return service.get('/healthReport/aiModel/availableWithParams', { params: { gender, collectionId,timeout: 200000 } })
}

export interface ModelCheckResult {
  missingRequired: string[]
  keyCompleteness: number
  overallCompleteness: number
  totalKeyParams: number
  filledKeyParams: number
}

export function checkModelParams(modelIds: (number | string)[], collectionId: number | string) {
  const idsParam = modelIds.join(',')
  return service.get(
      `/healthReport/aiModel/check-params/${collectionId}`,
      { params: { modelIds: idsParam } }
  )
}
