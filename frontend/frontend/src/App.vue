<script setup>
import { computed, ref } from 'vue'
import OperationDetails from './components/OperationDetails.vue'
import RequestLogs from './components/RequestLogs.vue'
import ResultSummary from './components/ResultSummary.vue'
import { createCheckoutSession } from './services/checkoutApi'

const idempotencyEnabled = ref(true)
const retryCount = ref(3)
const requestMode = ref('parallel')
const simulateSlowResponse = ref(false)
const logs = ref([])
const isRunning = ref(false)
const activeIdempotencyKey = ref(null)

const buttonLabel = computed(() =>
  isRunning.value ? 'Running Requests...' : `Send ${retryCount.value} Requests`
)

const requestCountLabel = computed(() =>
  retryCount.value === 1 ? '1 request' : `${retryCount.value} requests`
)

const operationDetails = computed(() => {
  if (!logs.value.length) {
    return null
  }

  const latestLog = [...logs.value].sort((left, right) =>
    right.startedAt.localeCompare(left.startedAt)
  )[0]
  const createdOrReplayedLog = logs.value.find(
    (log) => log.sessionId && log.sessionId !== 'Not created'
  )

  return {
    idempotencyKey: activeIdempotencyKey.value || 'Not used',
    operationName: 'CHECKOUT_SESSION_CREATE',
    currentStatus: latestLog?.status || 'CREATED',
    stripeSessionId: createdOrReplayedLog?.sessionId || 'Not created',
    requestCount: logs.value.length,
    replayedRequestCount: logs.value.filter(
      (log) => (log.status || '').toUpperCase() === 'REPLAYED'
    ).length,
  }
})

function generateIdempotencyKey() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return `payment-retry-lab-${crypto.randomUUID()}`
  }

  return `payment-retry-lab-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function formatTimestamp(date) {
  return new Intl.DateTimeFormat('en-IN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    fractionalSecondDigits: 3,
  }).format(date)
}

function wait(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

function appendLog(log) {
  logs.value = [...logs.value, log].sort((left, right) => {
    if (left.requestNumber !== right.requestNumber) {
      return left.requestNumber - right.requestNumber
    }

    return left.startedAt.localeCompare(right.startedAt)
  })
}

async function executeCheckoutRequest(requestNumber, requestIdempotencyKey, attempt = 0) {
  const startedAt = formatTimestamp(new Date())

  return createCheckoutSession({
    orderId: 'ORDER-1001',
    quantity: 1,
    requestNumber,
    idempotencyKey: requestIdempotencyKey,
  })
    .then((response) => {
      const requestStatus = response.requestStatus || 'CREATED'
      const log = {
        id: `${requestNumber}-${response.stripeSessionId}-${attempt}`,
        requestNumber: response.requestNumber,
        startedAt,
        status: requestStatus,
        message:
          requestStatus === 'REPLAYED'
            ? 'Previously stored checkout response was returned without calling Stripe again.'
            : response.idempotencyUsed
              ? 'Stripe returned a session using the supplied idempotency key.'
              : 'Stripe created a session without idempotency protection.',
        sessionId: response.stripeSessionId,
      }

      appendLog(log)
    })
    .catch((error) => {
      const backendResponse = error.response
      const requestStatus = backendResponse?.requestStatus

      if (requestStatus === 'PROCESSING') {
        appendLog({
          id: `${requestNumber}-processing-${attempt}`,
          requestNumber: backendResponse.requestNumber ?? requestNumber,
          startedAt,
          status: 'PROCESSING',
          message: 'Another request with this idempotency key is currently executing.',
          sessionId: 'Not created',
        })

        if (attempt === 0) {
          return wait(1000).then(() =>
            executeCheckoutRequest(requestNumber, requestIdempotencyKey, attempt + 1)
          )
        }

        return
      }

      if (requestStatus === 'CONFLICT') {
        appendLog({
          id: `${requestNumber}-conflict-${attempt}`,
          requestNumber: backendResponse.requestNumber ?? requestNumber,
          startedAt,
          status: 'CONFLICT',
          message: 'The same idempotency key was reused with different request data.',
          sessionId: 'Not created',
        })
        return
      }

      appendLog({
        id: `${requestNumber}-error-${attempt}`,
        requestNumber,
        startedAt,
        status: 'FAILED',
        message: error.message,
        sessionId: 'Not created',
      })
    })
}

async function runSimulation() {
  if (isRunning.value) {
    return
  }

  isRunning.value = true
  logs.value = []

  const sharedIdempotencyKey = idempotencyEnabled.value ? generateIdempotencyKey() : null
  activeIdempotencyKey.value = sharedIdempotencyKey
  const requestNumbers = Array.from({ length: retryCount.value }, (_, index) => index + 1)

  if (requestMode.value === 'parallel') {
    const tasks = requestNumbers.map((requestNumber) =>
      executeCheckoutRequest(requestNumber, sharedIdempotencyKey ?? null)
    )
    await Promise.allSettled(tasks)
  } else {
    for (const requestNumber of requestNumbers) {
      await executeCheckoutRequest(requestNumber, sharedIdempotencyKey ?? null)
    }
  }

  isRunning.value = false
}
</script>

<template>
  <main class="app-shell">
    <section class="lab-card">
      <header class="hero">
        <h1>Payment Retry Lab</h1>
        <p class="subtitle">
          See how idempotency protects payment operations from duplicate retries.
        </p>
      </header>

      <section class="workspace">
        <section class="left-column-stack">
          <section class="panel experiment-panel">
            <div class="panel-header">
              <h2>Experiment Controls</h2>
              <p>Run a compact payment retry experiment and inspect the request outcomes.</p>
            </div>

            <div class="details-grid" aria-label="Payment details">
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
                <strong>&#8377;499</strong>
              </div>
            </div>
          </section>

          <aside class="control-column">
            <section class="panel controls" aria-label="Payment controls">
              <label class="toggle-row">
                <span>
                  <span class="control-label">Idempotency Protection</span>
                  <span class="control-hint">Reuse one key across retries to protect a single payment operation.</span>
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

              <section class="simulation-panel" aria-label="Failure simulation">
                <div class="section-copy">
                  <span class="control-label">Failure Simulation</span>
                </div>

                <label class="field">
                  <span class="control-label">Request Mode</span>
                  <select v-model="requestMode" class="select-input">
                    <option value="sequential">Sequential</option>
                    <option value="parallel">Parallel</option>
                  </select>
                </label>

                <label class="toggle-row">
                  <span>
                    <span class="control-label">Simulate Slow Response</span>
                    <span class="control-hint">Prepared for backend delay testing and overlap scenarios.</span>
                  </span>
                  <button
                    type="button"
                    class="toggle"
                    :class="{ active: simulateSlowResponse }"
                    :aria-pressed="simulateSlowResponse"
                    @click="simulateSlowResponse = !simulateSlowResponse"
                  >
                    <span class="toggle-knob"></span>
                  </button>
                </label>
              </section>

              <div class="run-strip">
                <div class="run-meta">
                  <span class="run-label">Current run</span>
                  <strong>{{ requestMode }} / {{ requestCountLabel }}</strong>
                </div>

                <button
                  type="button"
                  class="primary-button"
                  :disabled="isRunning"
                  @click="runSimulation"
                >
                  {{ buttonLabel }}
                </button>
              </div>
            </section>
          </aside>
        </section>

        <section class="panel request-panel">
          <RequestLogs :logs="logs" :is-running="isRunning" />
        </section>
      </section>

      <section class="summary-row">
        <section class="panel">
          <ResultSummary :logs="logs" :is-running="isRunning" />
        </section>
      </section>

      <section v-if="operationDetails" class="details-footer">
        <section class="panel">
          <OperationDetails :operation-details="operationDetails" />
        </section>
      </section>
    </section>
  </main>
</template>

<style scoped>
:global(body) {
  margin: 0;
  min-width: 320px;
  font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.08), transparent 28%),
    #f3f4f6;
  color: #172033;
}

:global(#app) {
  min-height: 100vh;
}

.app-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 0.85rem;
  box-sizing: border-box;
}

.lab-card {
  --desktop-experiment-height: 250px;
  --desktop-controls-height: 548px;
  --desktop-workspace-gap: 1rem;
  width: min(100%, 1180px);
  margin: 0 auto;
  min-height: calc(100vh - 0.9rem);
  background: #ffffff;
  border: 1px solid #dbe4f0;
  border-radius: 24px;
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.08);
  padding: 0.85rem 1rem 0.95rem;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.hero {
  margin-bottom: 1rem;
}

h1 {
  margin: 0;
  font-size: clamp(2rem, 3vw, 2.95rem);
  line-height: 1;
  letter-spacing: -0.04em;
  color: #172033;
}

.subtitle {
  margin: 0.45rem 0 0;
  max-width: 40rem;
  font-size: 0.98rem;
  line-height: 1.5;
  color: #44506a;
}

.workspace {
  display: grid;
  grid-template-columns: minmax(280px, 420px) minmax(0, 1fr);
  gap: 1rem;
  align-items: stretch;
}

.left-column-stack,
.control-column,
.details-footer {
  display: grid;
  gap: 1rem;
  min-height: 0;
}

.left-column-stack {
  min-width: 0;
  grid-template-rows: var(--desktop-experiment-height) var(--desktop-controls-height);
}

.experiment-panel,
.control-column {
  min-width: 0;
}

.experiment-panel,
.controls {
  height: 100%;
}

.request-panel {
  height: calc(
    var(--desktop-experiment-height) + var(--desktop-controls-height) + var(--desktop-workspace-gap)
  );
  display: flex;
  min-height: 0;
  overflow: hidden;
}

.summary-row {
  margin-top: 1rem;
  margin-bottom: 1rem;
}

.panel {
  background: #fbfcfe;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 1rem;
}

.panel-header h2 {
  margin: 0;
  font-size: 1.12rem;
  line-height: 1.2;
}

.panel-header p {
  margin: 0.35rem 0 0;
  color: #64748b;
  line-height: 1.45;
  font-size: 0.92rem;
}

.details-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.65rem;
  margin-top: 0.85rem;
}

.detail-item {
  padding: 0.8rem 0.85rem;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid #dbe4f0;
}

.detail-label,
.control-label {
  display: block;
  font-size: 0.84rem;
  font-weight: 600;
  color: #475569;
  margin-bottom: 0.22rem;
}

.controls {
  display: grid;
  gap: 0.75rem;
}

.simulation-panel {
  display: grid;
  gap: 0.65rem;
  padding: 0.85rem;
  border: 1px solid #dbe4f0;
  border-radius: 16px;
  background: #f2f7ff;
}

.section-copy {
  display: grid;
  gap: 0.25rem;
}

.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.9rem;
  padding: 0.8rem 0.9rem;
  border: 1px solid #dbe4f0;
  border-radius: 16px;
  background: #ffffff;
}

.control-hint {
  display: block;
  font-size: 0.86rem;
  color: #64748b;
  line-height: 1.35;
}

.toggle {
  width: 44px;
  height: 26px;
  border: 0;
  border-radius: 999px;
  background: #cbd5e1;
  padding: 2px;
  cursor: pointer;
  flex: 0 0 auto;
}

.toggle.active {
  background: #2563eb;
}

.toggle-knob {
  display: block;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #ffffff;
  transition: transform 0.2s ease;
}

.toggle.active .toggle-knob {
  transform: translateX(18px);
}

.field {
  display: grid;
  gap: 0.35rem;
}

.select-input {
  width: 100%;
  padding: 0.72rem 0.85rem;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  font: inherit;
  font-size: 0.96rem;
  color: #172033;
  background: #ffffff;
}

.select-input:focus-visible,
.primary-button:focus-visible,
.toggle:focus-visible {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
}

.run-strip {
  display: grid;
  gap: 0.55rem;
}

.run-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.8rem;
  color: #475569;
  font-size: 0.9rem;
}

.run-label {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.primary-button {
  border: 0;
  border-radius: 12px;
  padding: 0.82rem 1rem;
  font: inherit;
  font-weight: 700;
  font-size: 0.98rem;
  color: #ffffff;
  background: #2563eb;
  cursor: pointer;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.2);
}

.primary-button:disabled {
  opacity: 0.72;
  cursor: wait;
}

@media (max-width: 960px) {
  .app-shell {
    display: block;
    padding: 1rem;
  }

  .workspace {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .lab-card {
    min-height: auto;
  }

  .panel {
    padding: 0.95rem;
  }

  .left-column-stack {
    grid-template-rows: none;
  }

  .controls {
    height: auto;
  }

  .request-panel {
    height: auto;
  }
}

@media (max-width: 720px) {
  .app-shell {
    padding: 0.6rem;
  }

  .lab-card {
    padding: 0.95rem;
    border-radius: 18px;
  }

  .details-grid {
    grid-template-columns: 1fr;
  }

  .toggle-row,
  .run-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  h1 {
    font-size: 1.85rem;
  }

  .subtitle,
  .panel-header p,
  .control-hint {
    font-size: 0.9rem;
  }

  .primary-button {
    width: 100%;
  }
}
</style>
