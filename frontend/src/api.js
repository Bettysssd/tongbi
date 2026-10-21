export async function postJson(url, body) {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body == null ? {} : body)
  })
  let data = {}
  try {
    data = await response.json()
  } catch (error) {
    data = {}
  }
  if (!response.ok) {
    throw new Error(data.message || '请求失败')
  }
  return data
}
