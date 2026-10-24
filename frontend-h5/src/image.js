// 图片压缩工具：本地文件 → canvas 重绘 → JPEG dataURL（base64）
// 策略：最长边压到 1080px，质量 0.72（单张约 100-300KB），base64 不含逗号可与 URL 逗号拼接共存
export async function compressImage(file, maxEdge = 1080, quality = 0.72) {
  if (!file || !/^image\//.test(file.type)) return null
  const url = URL.createObjectURL(file)
  try {
    const img = await new Promise((resolve, reject) => {
      const image = new Image()
      image.onload = () => resolve(image)
      image.onerror = () => reject(new Error('图片解码失败'))
      image.src = url
    })
    const scale = Math.min(1, maxEdge / Math.max(img.width, img.height))
    const w = Math.max(1, Math.round(img.width * scale))
    const h = Math.max(1, Math.round(img.height * scale))
    const canvas = document.createElement('canvas')
    canvas.width = w
    canvas.height = h
    canvas.getContext('2d').drawImage(img, 0, 0, w, h)
    return canvas.toDataURL('image/jpeg', quality)
  } catch (e) {
    return null
  } finally {
    URL.revokeObjectURL(url)
  }
}
