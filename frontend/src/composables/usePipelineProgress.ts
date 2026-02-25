import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import type { PipelineProgressEvent } from '@/types/pipeline'

export function usePipelineProgress(projectId: string) {
  const steps = ref<PipelineProgressEvent[]>([])
  const currentStep = ref('')
  const isRunning = ref(false)
  const isComplete = ref(false)

  const client = new Client({
    brokerURL: `ws://${location.host}/ws/pipeline/${projectId}`,
    reconnectDelay: 3000,
  })

  client.onConnect = () => {
    client.subscribe(`/topic/pipeline/${projectId}/progress`, (msg) => {
      const event: PipelineProgressEvent = JSON.parse(msg.body)
      handleEvent(event)
    })
  }

  function handleEvent(event: PipelineProgressEvent) {
    switch (event.type) {
      case 'stepStart':
        isRunning.value = true
        currentStep.value = event.displayName
        steps.value.push(event)
        break
      case 'stepComplete':
      case 'stepFailed':
        const idx = steps.value.findIndex(s => s.step === event.step)
        if (idx >= 0) steps.value[idx] = event
        else steps.value.push(event)
        break
      case 'pipelineComplete':
        isRunning.value = false
        isComplete.value = true
        break
    }
  }

  function connect() { client.activate() }
  function disconnect() { client.deactivate() }

  onUnmounted(() => client.deactivate())

  return { steps, currentStep, isRunning, isComplete, connect, disconnect }
}
