<script setup>
import { computed, ref } from 'vue'
import RequestLogs from './components/RequestLogs.vue'
import ResultSummary from './components/ResultSummary.vue'
import { createCheckoutSession } from './services/checkoutApi'

const idempotencyEnabled = ref(true)
const retryCount = ref(3)
const logs = ref([])
const isRunning = ref(false)

const buttonLabel = computed(() => `Send ${retryCount.value} Requests`)

function generateIdempotencyKey() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return `payment-retry-lab-${crypto.randomUUID()}`
  }

  return `payment-retry-lab-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

async function runSimulation() {
  if (isRunning.value) {
    return
  }

  isRunning.value = true
  logs.value = []

  const sharedIdempotencyKey = idempotencyEnabled.value ? generateIdempotencyKey() : null
  const tasks = Array.from({ length: retryCount.value }, (_, index) => {
    const requestNumber = index + 1
    const requestIdempotencyKey = sharedIdempotencyKey ?? null

    return createCheckoutSession({
      orderId: 'ORDER-1001',
      quantity: 1,
      requestNumber,
      idempotencyKey: requestIdempotencyKey,
    })
      .then((response) => {
        const requestStatus = response.requestStatus || 'CREATED'
        const log = {
          id: `${requestNumber}-${response.stripeSessionId}`,
          requestNumber: response.requestNumber,
          status: requestStatus,
          message:
            requestStatus === 'REPLAYED'
              ? 'Previously stored checkout response was returned without calling Stripe again.'
              : response.idempotencyUsed
                ? 'Stripe returned a session using the supplied idempotency key.'
                : 'Stripe created a session without idempotency protection.',
          sessionId: response.stripeSessionId,
        }

        logs.value = [...logs.value, log].sort(
          (left, right) => left.requestNumber - right.requestNumber
        )
      })
      .catch((error) => {
        const backendResponse = error.response
        const requestStatus = backendResponse?.requestStatus

        if (requestStatus === 'PROCESSING' || requestStatus === 'CONFLICT') {
          const log = {
            id: `${requestNumber}-${requestStatus.toLowerCase()}`,
            requestNumber: backendResponse.requestNumber ?? requestNumber,
            status: requestStatus,
            message:
              requestStatus === 'PROCESSING'
                ? 'Another request with the same idempotency key is still being processed.'
                : 'The same idempotency key was reused with different request data.',
            sessionId: 'Not created',
          }

          logs.value = [...logs.value, log].sort(
            (left, right) => left.requestNumber - right.requestNumber
          )
          return
        }

        const log = {
          id: `${requestNumber}-error`,
          requestNumber,
          status: 'failed',
          message: error.message,
          sessionId: 'Not created',
        }

        logs.value = [...logs.value, log].sort(
          (left, right) => left.requestNumber - right.requestNumber
        )
      })
  })

  await Promise.allSettled(tasks)
  isRunning.value = false
}
</script>

<template>
  <main class="app-shell">
    <section class="lab-card">
      <header class="hero">
        <p class="eyebrow">Payment Retry Lab</p>
        <h1>Payment Retry Lab</h1>
        <p class="subtitle">
          See how idempotency protects payment operations from duplicate retries.
        </p>
      </header>

      <section class="details-grid" aria-label="Payment details">
        <div class="detail-item">
          <span class="detail-label">Order</span>
          <strong>ORDER-1001</strong>
        </div>
        <div class="detail-item">
          <span class="detail-label">Product</span>
          <strong>Demo Pro Plan</strong>
        </div>
        <div class="detail-item">
          <span class="detail-label">Amount</span>
          <strong>₹499</strong>
        </div>
      </section>

      <section class="controls" aria-label="Payment controls">
        <label class="toggle-row">
          <span>
            <span class="control-label">Idempotency Protection</span>
            <span class="control-hint">Reuse the same key across retries.</span>
          </span>
          <button
            type="button"
            class="toggle"
            :class="{ active: idempotencyEnabled }"
            :aria-pressed="idempotencyEnabled"
            @click="idempotencyEnabled = !idempotencyEnabled"
          >
            <span class="toggle-knob"></span>
          </button>
        </label>

        <label class="field">
          <span class="control-label">Request Count</span>
          <select v-model="retryCount" class="select-input">
            <option :value="1">1</option>
            <option :value="2">2</option>
            <option :value="3">3</option>
            <option :value="5">5</option>
          </select>
        </label>

        <button type="button" class="primary-button" :disabled="isRunning" @click="runSimulation">
          {{ buttonLabel }}
        </button>
      </section>

      <ResultSummary :logs="logs" />
      <RequestLogs :logs="logs" />
    </section>
  </main>
</template>

<style scoped>
:global(body) {
  margin: 0;
  min-width: 320px;
  font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
  background: #f3f4f6;
  color: #172033;
}

:global(#app) {
  min-height: 100vh;
}

.app-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 2rem;
  box-sizing: border-box;
}

.lab-card {
  width: min(100%, 760px);
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
  padding: 2rem;
  box-sizing: border-box;
}

.hero {
  text-align: center;
}

.eyebrow {
  margin: 0 0 0.75rem;
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #47607f;
}

h1 {
  margin: 0;
  font-size: clamp(2.4rem, 7vw, 4.8rem);
  line-height: 1;
}

.subtitle {
  margin: 1.25rem auto 0;
  max-width: 34rem;
  font-size: 1.1rem;
  line-height: 1.7;
  color: #44506a;
}

.details-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 2rem;
}

.detail-item {
  padding: 1rem 1.1rem;
  border-radius: 16px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
}

.detail-label,
.control-label {
  display: block;
  font-size: 0.9rem;
  font-weight: 600;
  color: #475569;
  margin-bottom: 0.35rem;
}

.controls {
  margin-top: 2rem;
  display: grid;
  gap: 1.25rem;
}

.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.1rem;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #ffffff;
}

.control-hint {
  display: block;
  font-size: 0.92rem;
  color: #64748b;
}

.toggle {
  width: 58px;
  height: 34px;
  border: 0;
  border-radius: 999px;
  background: #cbd5e1;
  padding: 4px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.toggle.active {
  background: #2563eb;
}

.toggle-knob {
  display: block;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #ffffff;
  transition: transform 0.2s ease;
}

.toggle.active .toggle-knob {
  transform: translateX(24px);
}

.field {
  display: grid;
  gap: 0.5rem;
}

.select-input {
  width: 100%;
  padding: 0.9rem 1rem;
  border: 1px solid #cbd5e1;
  border-radius: 14px;
  font: inherit;
  color: #172033;
  background: #ffffff;
}

.primary-button {
  border: 0;
  border-radius: 14px;
  padding: 1rem 1.25rem;
  font: inherit;
  font-weight: 700;
  color: #ffffff;
  background: #2563eb;
  cursor: pointer;
}

.primary-button:disabled {
  opacity: 0.7;
  cursor: wait;
}

@media (max-width: 640px) {
  .app-shell {
    padding: 1rem;
  }

  .lab-card {
    padding: 1.25rem;
  }

  .details-grid {
    grid-template-columns: 1fr;
  }

  .toggle-row {
    align-items: flex-start;
  }
}
</style>
