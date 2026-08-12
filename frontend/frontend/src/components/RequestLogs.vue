<script setup>
defineProps({
  logs: {
    type: Array,
    default: () => [],
  },
})
</script>

<template>
  <section class="request-logs">
    <div class="section-header">
      <h2>Request Activity</h2>
      <p>Inspect each retry attempt and the resulting Stripe session data.</p>
    </div>

    <p v-if="logs.length === 0" class="empty-state">
      Run the demo to see request activity.
    </p>

    <ul v-else class="log-list">
      <li v-for="log in logs" :key="log.id" class="log-item">
        <div class="log-row">
          <span class="log-label">Request</span>
          <strong>#{{ log.requestNumber }}</strong>
        </div>
        <div class="log-row">
          <span class="log-label">Status</span>
          <span>{{ log.status }}</span>
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
