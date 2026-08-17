<script setup>
defineProps({
  operationDetails: {
    type: Object,
    default: null,
  },
})
</script>

<template>
  <section v-if="operationDetails" class="operation-details">
    <div class="section-header">
      <h2>Operation Details</h2>
      <p>Snapshot of the current idempotent checkout operation.</p>
    </div>

    <div class="details-grid">
      <div class="detail-card">
        <span class="detail-label">Idempotency Key</span>
        <code>{{ operationDetails.idempotencyKey }}</code>
      </div>
      <div class="detail-card">
        <span class="detail-label">Operation Name</span>
        <strong>{{ operationDetails.operationName }}</strong>
      </div>
      <div class="detail-card">
        <span class="detail-label">Current Status</span>
        <strong>{{ operationDetails.currentStatus }}</strong>
      </div>
      <div class="detail-card">
        <span class="detail-label">Stripe Session ID</span>
        <code>{{ operationDetails.stripeSessionId }}</code>
      </div>
      <div class="detail-card">
        <span class="detail-label">Number of Requests</span>
        <strong>{{ operationDetails.requestCount }}</strong>
      </div>
      <div class="detail-card">
        <span class="detail-label">Replayed Requests</span>
        <strong>{{ operationDetails.replayedRequestCount }}</strong>
      </div>
    </div>
  </section>
</template>

<style scoped>
.section-header h2 {
  margin: 0;
  font-size: 1.2rem;
  line-height: 1.2;
}

.section-header p {
  margin: 0.4rem 0 0;
  color: #64748b;
  font-size: 0.95rem;
  line-height: 1.45;
}

.details-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
  margin-top: 1.05rem;
}

.detail-card {
  padding: 0.95rem 1rem;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #dbe4f0;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
}

.detail-label {
  display: block;
  margin-bottom: 0.32rem;
  color: #64748b;
  font-size: 0.82rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.detail-card strong {
  font-size: 0.98rem;
  line-height: 1.35;
}

code {
  display: block;
  max-width: 100%;
  white-space: normal;
  overflow-wrap: anywhere;
  font-family: Consolas, "Courier New", monospace;
  color: #1d4ed8;
  line-height: 1.45;
}

@media (max-width: 900px) {
  .details-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .details-grid {
    grid-template-columns: 1fr;
  }
}
</style>
