import service from '@/utils/request'

export interface CollectionRecord {
  id: number
  collectionNo: string
  memberId: number
  memberName: string
  memberGender: number
  checkupDate: string
  hospital: string
  sourceType: number
  reportStatus: number
  finishStatus: number
}

export interface CollectionQuery {
  memberName?: string
  startDate?: string
  endDate?: string
  reportStatus?: number
  finishStatus?: number
  current: number
  size: number
}

export interface FieldConfig {
  id: number
  fieldCode: string
  fieldName: string
  fieldNameEn: string
  categoryCode: string
  genderScope: number
  fieldType: string
  unit: string
  unitEn?: string
  refRangeText: string
  refRangeTextEn?: string
  description: string
  isRequired: number
  enumOptions: string
  sortOrder: number
}

export interface CategoryConfig {
  id: number
  categoryCode: string
  categoryName: string
  categoryNameEn: string
  genderScope: number
  icon: string
  sortOrder: number
  fields: FieldConfig[]
}

export interface CategoryData {
  categoryCode: string
  categoryName?: string
  categoryNameEn?: string
  fieldData: Record<string, any>
  filledCount: number
  isCompleted: boolean
}

export interface CollectionDetail {
  record: CollectionRecord
  categoryDataList: CategoryData[]
}

export interface CollectionCreateDTO {
  memberId: number
  checkupDate?: string
  hospital?: string
}

export interface CategorySaveDTO {
  categoryCode: string
  fieldData: Record<string, any>
}

export function getCollectionList(params: CollectionQuery) {
  return service.get('/healthReport/collectionRecord/list', { params })
}

export function getCollectionDetail(id: number) {
  return service.get(`/healthReport/collectionRecord/detail/${id}`,{timeout: 30000})
}

export function createCollection(data: CollectionCreateDTO) {
  return service.post('/healthReport/collectionRecord', data)
}

export function saveCategoryDataItem(collectionId: number, data: CategorySaveDTO) {
  return service.put(`/healthReport/collectionRecord/${collectionId}/categoryItem`, data, {
    timeout: 30000, // 30秒超时
    headers: { repeatSubmit: false } // 禁用重复提交检测，由前端自己控制
  })
}

export function saveBatchCategoryData(data) {
  return service.post(`/healthReport/collectionRecord/saveBatchCategoryData`, data)
}

export function completeCollection(id: number) {
  return service.put(`/healthReport/collectionRecord/${id}/complete`)
}

export function deleteCollection(id: number) {
  return service.delete(`/healthReport/collectionRecord/${id}`)
}

export function getCollectionConfig(gender?: number, categoryName?: string) {
  const params: Record<string, any> = {}
  if (gender !== undefined && gender !== null) {
    params.gender = gender
  }
  // 只要 categoryName 不是 undefined 和 null，就传递（包括空字符串）
  if (categoryName !== undefined && categoryName !== null) {
    params.categoryName = categoryName
  }
  return service.get('/healthReport/collectionConfig/categories', { params,timeout: 200000 })
}

export function getModelConfig(gender?: number, categoryName?: string) {
  const params: Record<string, any> = {}
  if (gender !== undefined && gender !== null) {
    params.gender = gender
  }
  // 只要 categoryName 不是 undefined 和 null，就传递（包括空字符串）
  if (categoryName !== undefined && categoryName !== null) {
    params.categoryName = categoryName
  }
  return service.get('/healthReport/collectionConfig/modelCategories', { params,timeout: 200000 })
}

export function getCollectionConfigName() {
  return service.get('/healthReport/collectionConfig/getCollectionConfigName', )
}

export interface BatchImportResponse {
  totalFiles: number
  successCount: number
  failCount: number
  details: {
    fileName: string
    success: boolean
    message: string
    collectionId?: number
    collectionNo?: string
  }[]
}

export function batchImportCollection(files: File[], overrideIfExist: boolean = false, languageType: string = 'zh') {
  const formData = new FormData()
  files.forEach(file => formData.append('files', file))
  formData.append('overrideIfExist', String(overrideIfExist))
  formData.append('languageType', languageType)
  return service.post('/healthReport/collection/batchImport', formData, {
    headers: { 'Content-Type': undefined },
    timeout: 300000
  })
}