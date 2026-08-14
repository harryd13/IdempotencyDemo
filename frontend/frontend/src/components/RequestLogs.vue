<script setup>
const props = defineProps({
  logs: {
    type: Array,
    default: () => [],
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

    <p v-if="props.logs.length === 0" class="empty-state">
      Run the demo to see request activity.
    </p>

    <ul v-else class="log-list">
      <li v-for="log in props.logs" :key="log.id" class="log-item">
        <div class="log-row">
          <span class="log-label">Request</span>
          <strong>#{{ log.requestNumber }}</strong>
        </div>
        <div class="log-row">
          <span class="log-label">Status</span>
          <span class="status-badge" :class="`status-${normalizeStatus(log.status).toLowerCase()}`">
            {{ normalizeStatus(log.status) }}
          </span>
        </div>
        <div class="log-row">
          <span class="log-label">Message</span>
          <span>{{ log.message }}</span>
        </div>
        <div class="log-row">
          <span class="log-label">Session ID</span>
          <code>{{ log.sessionId }}</code>
        </div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.request-logs {
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 1px solid #e5e7eb;
}

.section-header h2 {
  margin: 0;
  font-size: 1.25rem;
}

.section-header p {
  margin: 0.5rem 0 0;
  color: #64748b;
}

.empty-state {
  margin: 1.5rem 0 0;
  padding: 1rem 1.1rem;
  border-radius: 16px;
  background: #f9fafb;
  border: 1px dashed #cbd5e1;
  color: #475569;
}

.log-list {
  list-style: none;
  padding: 0;
  margin: 1.5rem 0 0;
  display: grid;
  gap: 1rem;
}

.log-item {
  padding: 1rem 1.1rem;
  border-radius: 16px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
}

.log-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 0.55rem;
}

.log-row:first-child {
  margin-top: 0;
}

.log-label {
  color: #64748b;
  font-weight: 600;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 96px;
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.status-created {
  background: #dcfce7;
  color: #166534;
}

.status-replayed {
  background: #dbeafe;
  color: #1d4ed8;
}

.status-processing {
  background: #fef3c7;
  color: #b45309;
}

.status-conflict {
  background: #fee2e2;
  color: #b91c1c;
}

.status-failed {
  background: #e5e7eb;
  color: #374151;
}

code {
  font-family: Consolas, "Courier New", monospace;
  color: #1d4ed8;
}

@media (max-width: 640px) {
  .log-row {
    flex-direction: column;
    gap: 0.25rem;
  }
}
</style>
