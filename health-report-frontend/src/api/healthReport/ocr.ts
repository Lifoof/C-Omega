import request from '@/utils/request'

/**
 * OCR识别API
 */

/**
 * OCR识别并提取体检指标
 * @param files 文件列表
 * @param collectionId 采集记录ID（可选）
 */
export function extractIndicators(files: File[], collectionId?: number) {
  const formData = new FormData()
  files.forEach(file => {
    formData.append('files', file)
  })
  if (collectionId) {
    formData.append('collectionId', collectionId.toString())
  }

  return request({
    url: '/healthReport/ocr/extractIndicators',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 120000 // 2分钟超时，OCR识别需要较长时间
  })
}
