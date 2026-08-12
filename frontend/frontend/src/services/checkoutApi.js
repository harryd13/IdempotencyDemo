const CHECKOUT_API_URL = 'http://localhost:8080/api/checkout/sessions'

export async function createCheckoutSession({
  orderId,
  quantity,
  requestNumber,
  idempotencyKey,
}) {
  const headers = {
    'Content-Type': 'application/json',
    'X-Demo-Request-Number': String(requestNumber),
  }

  if (idempotencyKey) {
    headers['Idempotency-Key'] = idempotencyKey
  }

  const response = await fetch(CHECKOUT_API_URL, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      orderId,
      quantity,
    }),
  })

  if (!response.ok) {
    let message = 'Checkout request failed.'

    try {
      const errorBody = await response.json()
      message = errorBody.message || message
    } catch {
      // Keep the fallback message when the response body is not JSON.
    }

    throw new Error(message)
  }

  return response.json()
}
