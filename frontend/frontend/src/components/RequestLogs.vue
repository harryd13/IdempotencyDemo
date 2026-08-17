<script setup>
const props = defineProps({
  logs: {
    type: Array,
    default: () => [],
  },
  isRunning: {
    type: Boolean,
    default: false,
  },
})

function normalizeStatus(status) {
  const normalizedStatus = (status || '').toUpperCase()

  if (normalizedStatus === 'COMPLETED' || normalizedStatus === 'SUCCESS') {
    return 'CREATED'
  }

  if (normalizedStatus === 'FAILED' || normalizedStatus === 'ERROR') {
    return 'FAILED'
  }

  if (
    ['CREATED', 'REPLAYED', 'PROCESSING', 'CONFLICT', 'FAILED'].includes(normalizedStatus)
  ) {
    return normalizedStatus
  }

  return 'PROCESSING'
}
</script>

<template>
  <section class="request-logs">
    <div class="section-header">
      <h2>Request Activity</h2>
      <p>Inspect each retry attempt and the resulting Stripe session data.</p>
    </div>

    <div v-if="props.isRunning && props.logs.length === 0" class="empty-state empty-state-loading">
      <span class="loading-spinner"></span>
      <div>
        <strong>Requests are being sent.</strong>
        <p>Activity will appear here as each response returns.</p>
      </div>
    </div>

    <p v-else-if="props.logs.length === 0" class="empty-state">
      Run the demo to see request activity.
    </p>

    <ul v-else class="log-list">
      <li v-for="log in props.logs" :key="log.id" class="log-item">
        <div class="log-header">
          <strong class="request-id">Request #{{ log.requestNumber }}</strong>
          <span class="status-badge" :class="`status-${normalizeStatus(log.status).toLowerCase()}`">
            {{ normalizeStatus(log.status) }}
          </span>
        </div>

        <div class="log-body">
          <div class="log-row">
            <span class="log-label">Started</span>
            <span>{{ log.startedAt }}</span>
          </div>
          <div class="log-row">
            <span class="log-label">Message</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
          <div class="log-row">
            <span class="log-label">Session</span>
            <code :title="log.sessionId">{{ log.sessionId }}</code>
          </div>
        </div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.request-logs {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.section-header h2 {
  margin: 0;
  font-size: 1.3rem;
  line-height: 1.2;
}

.section-header p {
  margin: 0.45rem 0 0;
  color: #64748b;
  font-size: 0.96rem;
  line-height: 1.45;
}

.empty-state {
  margin: 1.1rem 0 0;
  padding: 1rem 1.05rem;
  border-radius: 16px;
  background: #ffffff;
  border: 1px dashed #cbd5e1;
  color: #475569;
  line-height: 1.45;
}

.empty-state-loading {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.empty-state-loading p {
  margin: 0.3rem 0 0;
}

.loading-spinner {
  width: 1rem;
  height: 1rem;
  border-radius: 50%;
  border: 2px solid rgba(37, 99, 235, 0.18);
  border-top-color: #2563eb;
  flex: 0 0 auto;
  animation: spin 0.85s linear infinite;
}

.log-list {
  list-style: none;
  padding: 0;
  margin: 1.1rem 0 0;
  display: grid;
  gap: 0.9rem;
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 0.25rem;
}

.log-item {
  padding: 0.95rem 1rem;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #dbe4f0;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
}

.log-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
}

.request-id {
  font-size: 1.02rem;
  line-height: 1.2;
}

.log-body {
  display: grid;
  gap: 0.65rem;
  margin-top: 0.8rem;
}

.log-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.log-label {
  color: #64748b;
  font-weight: 600;
  flex: 0 0 74px;
  font-size: 0.82rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 108px;
  padding: 0.42rem 0.82rem;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  border: 1px solid transparent;
}

.status-created {
  background: rgba(37, 99, 235, 0.1);
  border-color: rgba(37, 99, 235, 0.18);
  color: #1d4ed8;
}

.status-replayed {
  background: rgba(14, 165, 233, 0.12);
  border-color: rgba(14, 165, 233, 0.18);
  color: #0369a1;
}

.status-processing {
  background: rgba(245, 158, 11, 0.14);
  border-color: rgba(245, 158, 11, 0.22);
  color: #b45309;
}

.status-conflict {
  background: rgba(239, 68, 68, 0.12);
  border-color: rgba(239, 68, 68, 0.2);
  color: #b91c1c;
}

.status-failed {
  background: rgba(100, 116, 139, 0.12);
  border-color: rgba(100, 116, 139, 0.2);
  color: #334155;
}

.log-message {
  flex: 1;
  color: #172033;
  line-height: 1.45;
}

code {
  display: block;
  max-width: 100%;
  white-space: normal;
  overflow-wrap: anywhere;
  font-family: Consolas, "Courier New", monospace;
  color: #1e40af;
  text-align: right;
  line-height: 1.45;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 640px) {
  .log-header,
  .log-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.35rem;
  }

  .log-label {
    flex-basis: auto;
  }

  code {
    text-align: left;
  }

  .status-badge {
    min-width: 0;
  }
}
</style>
