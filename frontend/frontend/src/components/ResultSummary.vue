<script setup>
import { computed } from 'vue'

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

const successfulStatuses = new Set(['CREATED', 'REPLAYED'])

const requestsSent = computed(() => props.logs.length)
const successfulResponses = computed(() =>
  props.logs.filter((log) => successfulStatuses.has((log.status || '').toUpperCase())).length
)
const uniqueSessions = computed(() =>
  new Set(
    props.logs
      .map((log) => log.sessionId)
      .filter((sessionId) => sessionId && sessionId !== 'Not created')
  ).size
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

    <div v-if="!props.logs.length && !props.isRunning" class="empty-note">
      <strong>No results yet.</strong>
      <p>Summary metrics will appear after you run the experiment.</p>
    </div>

    <div v-else-if="props.isRunning && !props.logs.length" class="empty-note empty-note-loading">
      <span class="loading-spinner"></span>
      <div>
        <strong>Collecting responses.</strong>
        <p>Waiting for the first checkout result to arrive.</p>
      </div>
    </div>

    <p v-if="resultMessage" class="result-message">
      {{ resultMessage }}
    </p>
  </section>
</template>

<style scoped>
.section-header h2 {
  margin: 0;
  font-size: 1.15rem;
  line-height: 1.2;
}

.section-header p {
  margin: 0.4rem 0 0;
  color: #64748b;
  font-size: 0.92rem;
  line-height: 1.45;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
  margin-top: 1rem;
}

.summary-card {
  padding: 0.95rem 1rem;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #dbe4f0;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
}

.summary-label {
  display: block;
  margin-bottom: 0.35rem;
  color: #64748b;
  font-size: 0.82rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.summary-card strong {
  font-size: 1.5rem;
  line-height: 1;
}

.empty-note {
  margin: 1rem 0 0;
  padding: 0.95rem 1rem;
  border-radius: 16px;
  background: #ffffff;
  border: 1px dashed #cbd5e1;
  color: #64748b;
  line-height: 1.45;
}

.empty-note p {
  margin: 0.25rem 0 0;
}

.empty-note-loading {
  display: flex;
  align-items: center;
  gap: 0.8rem;
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

.result-message {
  margin: 1rem 0 0;
  padding: 0.95rem 1rem;
  border-radius: 16px;
  background: rgba(37, 99, 235, 0.1);
  border: 1px solid rgba(37, 99, 235, 0.2);
  color: #1d4ed8;
  font-weight: 600;
  line-height: 1.45;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 640px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .summary-card strong {
    font-size: 1.3rem;
  }

  .empty-note-loading {
    align-items: flex-start;
  }
}
</style>
