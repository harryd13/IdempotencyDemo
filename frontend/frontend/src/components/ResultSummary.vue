<script setup>
import { computed } from 'vue'

const props = defineProps({
  logs: {
    type: Array,
    default: () => [],
  },
})

const requestsSent = computed(() => props.logs.length)
const successfulResponses = computed(
  () => props.logs.filter((log) => log.status === 'completed').length
)
const uniqueSessions = computed(
  () => new Set(props.logs.map((log) => log.sessionId).filter(Boolean)).size
)

const resultMessage = computed(() => {
  if (uniqueSessions.value > 1) {
    return 'Unsafe: multiple payment operations were created.'
  }

  if (uniqueSessions.value === 1 && requestsSent.value > 1) {
    return 'Protected: retries resolved to one payment operation.'
  }

  return ''
})
</script>

<template>
  <section class="result-summary">
    <div class="section-header">
      <h2>Result Summary</h2>
      <p>Compare total responses against the number of resulting payment operations.</p>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <span class="summary-label">Requests Sent</span>
        <strong>{{ requestsSent }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">Successful Responses</span>
        <strong>{{ successfulResponses }}</strong>
      </div>
      <div class="summary-card">
        <span class="summary-label">Unique Session IDs</span>
        <strong>{{ uniqueSessions }}</strong>
      </div>
    </div>

    <p v-if="resultMessage" class="result-message">
      {{ resultMessage }}
    </p>
  </section>
</template>

<style scoped>
.result-summary {
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

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1.5rem;
}

.summary-card {
  padding: 1rem 1.1rem;
  border-radius: 16px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
}

.summary-label {
  display: block;
  margin-bottom: 0.4rem;
  color: #64748b;
  font-size: 0.9rem;
  font-weight: 600;
}

.result-message {
  margin: 1rem 0 0;
  padding: 1rem 1.1rem;
  border-radius: 16px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  font-weight: 600;
}

@media (max-width: 640px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
